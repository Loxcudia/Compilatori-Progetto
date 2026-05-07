package miniimp;
import miniimp.MiniImp.*;
public class CFGBuilder {

    public CFG build(Prog programAST) {
        System.out.println(">>> STARTING BUILDING THE GRAPH<<<");
        CFG.Node startNode = new CFG.Node();

        if (programAST.body() == null) {
            System.out.println("ERROR: the program body is null!");
        }

        CFG.Node endNode = buildGraph(programAST.body(), startNode);
        return new CFG(startNode, endNode);
    }

    private CFG.Node buildGraph(Cmd cmd, CFG.Node currentBlock) {
        System.out.println("[LOG] I'm evaluating the node: " + cmd.getClass().getSimpleName() + " in the block " + currentBlock.id);

        return switch (cmd) {
            case AssignCmd a -> {
                System.out.println("      -> Added assignment to block " + currentBlock.id);
                currentBlock.addCommand(a);
                yield currentBlock;
            }
            case SkipCmd s -> {
                currentBlock.addCommand(s);
                yield currentBlock;
            }
            case SeqCmd seq -> {
                System.out.println("      -> Got a sequence. I evaluate the left part...");
                CFG.Node outOfC1 = buildGraph(seq.left(), currentBlock);
                System.out.println("      -> I evaluate the right part...");
                yield buildGraph(seq.right(), outOfC1);
            }
            case IfCmd ifCmd -> {
                System.out.println("      -> Got IF. I create the fork.");
                currentBlock.addCommand(ifCmd);
                CFG.Node mergeNode = new CFG.Node();

                CFG.Node thenStart = new CFG.Node();
                currentBlock.addEdge(thenStart, CFG.EdgeType.TRUE);
                CFG.Node thenEnd = buildGraph(ifCmd.thenBranch(), thenStart);
                thenEnd.addEdge(mergeNode, CFG.EdgeType.NORMAL);

                CFG.Node elseStart = new CFG.Node();
                currentBlock.addEdge(elseStart, CFG.EdgeType.FALSE);
                CFG.Node elseEnd = buildGraph(ifCmd.elseBranch(), elseStart);
                elseEnd.addEdge(mergeNode, CFG.EdgeType.NORMAL);

                yield mergeNode;
            }
            case WhileCmd whileCmd -> {
                System.out.println("      -> Got WHILE. I create the ring.");
                CFG.Node conditionNode = new CFG.Node();
                currentBlock.addEdge(conditionNode, CFG.EdgeType.NORMAL);
                conditionNode.addCommand(whileCmd);

                CFG.Node exitNode = new CFG.Node();

                CFG.Node bodyStart = new CFG.Node();
                conditionNode.addEdge(bodyStart, CFG.EdgeType.TRUE);
                CFG.Node bodyEnd = buildGraph(whileCmd.body(), bodyStart);

                bodyEnd.addEdge(conditionNode, CFG.EdgeType.NORMAL);
                conditionNode.addEdge(exitNode, CFG.EdgeType.FALSE);

                yield exitNode;
            }
            default -> {
                System.out.println("ERROR: Unknown type -> " + cmd.getClass().getName());
                throw new RuntimeException("Not supported command by the CFG Builder: " + cmd);
            }
        };
    }
}