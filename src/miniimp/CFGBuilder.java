package miniimp;
import miniimp.MiniImp.*;

public class CFGBuilder {

    public CFG build(Prog AST)
    {
        CFG.Node startNode = new CFG.Node();
        CFG.Node endNode = new CFG.Node();
        return new CFG(startNode, endNode);
    }

    private CFG.Node buildGraph(Cmd command, CFG.Node currentBlock)
    {
        return switch(command)
        {
            case AssignCmd cmd -> {
                currentBlock.addCommand(cmd);
                yield currentBlock;
            }

            case SkipCmd cmd -> {
                currentBlock.addCommand(cmd);
                yield currentBlock;
            }

            //  (c1;c2). Build the graph for c1 and use it as input of the build for the graph of c2
            case SeqCmd seqCmd -> {
                CFG.Node outofC1 = buildGraph(seqCmd.left(), currentBlock);
                yield buildGraph(seqCmd.right(), outofC1);
            }

            //if b then c1 else c2.
            // Add the condition at the current block -> Create two initial blocks: one for then branch and another for the else branch -> the two branches are merged in one skip node
            case IfCmd ifCmd -> {
                currentBlock.addCommand(ifCmd);
                CFG.Node mergeNode = new CFG.Node();

                //Then branch
                CFG.Node thenStart = new CFG.Node();
                currentBlock.addEdge(thenStart, CFG.EdgeType.TRUE);
                CFG.Node thenEnd = buildGraph(ifCmd.thenBranch(), thenStart);
                thenEnd.addEdge(mergeNode, CFG.EdgeType.NORMAL);

                //Else branch
                CFG.Node elseStart = new CFG.Node();
                currentBlock.addEdge(elseStart, CFG.EdgeType.FALSE);
                CFG.Node elseEnd = buildGraph(ifCmd.elseBranch(), elseStart);
                elseEnd.addEdge(mergeNode, CFG.EdgeType.NORMAL);

                yield mergeNode;
            }

            case WhileCmd whileCmd -> {
                CFG.Node conditionNode = new CFG.Node();
                currentBlock.addEdge(conditionNode, CFG.EdgeType.NORMAL);
                conditionNode.addCommand(whileCmd);

                CFG.Node exitNode = new CFG.Node();

                //While body
                CFG.Node bodyStart = new CFG.Node();
                conditionNode.addEdge(bodyStart, CFG.EdgeType.TRUE);
                CFG.Node bodyEnd = buildGraph(whileCmd.body(), bodyStart);

                //Return to the condition
                bodyEnd.addEdge(conditionNode, CFG.EdgeType.NORMAL);

                //While exiting
                conditionNode.addEdge(exitNode, CFG.EdgeType.FALSE);

                yield exitNode;
            }

            default -> throw new RuntimeException("Command not supported by the CFG");
        };
    }
}
