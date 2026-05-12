package miniimp;
import java.util.*;
import miniimp.MiniImp.*;


public class DeadStoreEliminator {

    /*
        The idea is:
        Getting all the blocks of the graphs.
        For each block, I save the variables that are surely alive at the end of the block (its output set). I use a new HashSet because I don't want to modify the original data.
        I allocate a list where I put the commands who deserve to survive. I choose Linked List because in this way I will have an efficient inserting at first.
        After this, I cycle on the block commands starting from the last command (I do a reverse for loop). I do this because to know if an assignment is useless, I must first look at what happens after it.
        Assume that in the reverse for loop I meet an assignment x := y + 1 and the variable x is not contained in the current block output set, then nobody cares about x: the command is a dead store.
        Otherwise, x will be useful, so I need to save the command. So:
        1. I push the command on the top of the linked list. I do this because I'm reverse cycling the all commands and pushing on the top guarantee that the list has the original order.
        2. I remove x from the live variables
        3. To compute x, the command needs to read y, so y becomes alive ( java instruction currentLiveVars.addAll(extractVariables(assign.expr())); )

        If i meet an if or a while, I can't eliminate anything because they control the flow of the program. I always save them on the top of the linked list, because the program always needs to read them to determine where to go.
        In the end of the algorithm, I simply substitute the current block list of commands with the optimized new list of commands, where all the useless assignments are deleted.
     */
    public void optimize(CFG cfg)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        for(CFG.Node block : allNodes)
        {
            Set<String> currentLiveVars = new HashSet<>(block.getOutputSets(LiveVariablesAnalysis.LIVE_VARS));
            List<Cmd> optimizedCommands = new LinkedList<>();
            for(int i = block.commands.size() - 1; i >= 0; i--)
            {
                Cmd cmd = block.commands.get(i);
                if(cmd instanceof AssignCmd assign)
                {
                    if(currentLiveVars.contains(assign.varName()))
                    {
                        optimizedCommands.addFirst(assign);
                        currentLiveVars.remove(assign.varName());
                        currentLiveVars.addAll(extractVariables(assign.expr()));
                    }
                }
                else if(cmd instanceof IfCmd ifCmd)
                {
                    optimizedCommands.addFirst(ifCmd);
                    currentLiveVars.addAll(extractVariables(ifCmd.cond()));
                }
                else if (cmd instanceof WhileCmd whileCmd)
                {
                    optimizedCommands.addFirst(whileCmd);
                    currentLiveVars.addAll(extractVariables(whileCmd.cond()));
                }
            }
            block.commands = optimizedCommands;
        }
    }


    //Navigate the AST of an expression and extract all the variables names that are read
    private List<String> extractVariables(Node node)
    {
        List<String> readVariables = new ArrayList<>();

        if (node == null) return readVariables;

        switch (node) {
            case VarExpr varExpr -> {
                readVariables.add(varExpr.name());
            }
            case IntExpr intExpr -> {}
            case TrueBool t -> {}
            case FalseBool f -> {}


            case BinOpExpr binOp -> {
                // Search for the variables on the left and right of one of the operators: {+, -, *}
                readVariables.addAll(extractVariables(binOp.left()));
                readVariables.addAll(extractVariables(binOp.right()));
            }
            case RelOpBool relOp -> {
                // Search for the variables on the left and right of <
                readVariables.addAll(extractVariables(relOp.left()));
                readVariables.addAll(extractVariables(relOp.right()));
            }
            case AndBool andBool -> {
                // Search for the variables on the left and right of a logical and
                readVariables.addAll(extractVariables(andBool.left()));
                readVariables.addAll(extractVariables(andBool.right()));
            }
            case NotBool notBool -> {
                readVariables.addAll(extractVariables(notBool.b()));
            }

            default -> throw new RuntimeException("Not supported node in extractVariables function: " + node.getClass());
        }

        return readVariables;
    }


    //Get all CFG nodes simply doing a BFS visit
    private List<CFG.Node> getAllNodes(CFG cfg) {
        List<CFG.Node> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Queue<CFG.Node> queue = new LinkedList<>();

        queue.add(cfg.entryNode);
        visited.add(cfg.entryNode.id);
        result.add(cfg.entryNode);
        while (!queue.isEmpty()) {
            CFG.Node corrente = queue.poll();
            for (CFG.Edge arco : corrente.outgoingEdges) {
                if (!visited.contains(arco.target().id)) {
                    visited.add(arco.target().id);
                    queue.add(arco.target());
                    result.add(arco.target());
                }
            }
        }
        return result;
    }
}
