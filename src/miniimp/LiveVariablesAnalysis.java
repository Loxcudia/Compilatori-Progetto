package miniimp;
import java.util.*;
import miniimp.MiniImp.*;


public class LiveVariablesAnalysis {

    public static final String LIVE_VARS = "LIVE_VARIABLES";

    public void runAnalysis(CFG cfg, String inputVar)
    {
        System.out.println("Starting the analysis for Live Variables...");
        computeGenAndKill(cfg);
        computeFixpoint(cfg, inputVar);
    }

    /*
        FUNCTION FOR CALCULATE GEN AND KILL SETS FOR LIVE VARIABLES:
        For every block I start with gen and kill both empty. Next, I visit one at a time the commands of the block (starting from the first).
        In the case of assignment commands, I look at all the variables at the right of the assignment. If one variable is not yet in the kill set, then I add it to the gen set. After this, I look at the variable
        at the left of the assignment and I add it to the kill set (the variable at the left is fated to death).
        The if/while cases are similar with the difference in the fact that there are only readings, so I don't need to add any variable in the kill set.
     */
    public void computeGenAndKill(CFG cfg)
    {
        Set<Integer> visited = new HashSet<>();
        Queue<CFG.Node> queue = new LinkedList<>();

        queue.add(cfg.entryNode);
        visited.add(cfg.entryNode.id);

        while(!queue.isEmpty())
        {
            CFG.Node block = queue.poll();
            Set<String> genSet = new HashSet<>();
            Set<String> killSet = new HashSet<>();

            for(Cmd command : block.commands)
            {
                if(command instanceof AssignCmd assign)
                {
                    List<String> readVariables = extractVariables(assign.expr());
                    for(String variable : readVariables)
                    {
                        //If the variable is not killed yet, then it's alive
                        if(!killSet.contains(variable))
                            genSet.add(variable);
                    }
                    //the variable at the left dies
                    killSet.add(assign.varName());
                }
                //In the ifs and in the whiles there are only readings, no variable can be written
                else if(command instanceof IfCmd ifCmd)
                {
                    List<String> readVariables = extractVariables(ifCmd.cond());
                    for(String variable : readVariables)
                    {
                        if(!killSet.contains(variable))
                            genSet.add(variable);
                    }
                }
                else if(command instanceof WhileCmd whileCmd)
                {
                    List<String> readVariables = extractVariables(whileCmd.cond());
                    for(String variable : readVariables)
                    {
                        if(!killSet.contains(variable))
                            genSet.add(variable);
                    }
                }
            }
            block.setGenSets(LIVE_VARS, genSet);
            block.setKillSets(LIVE_VARS, killSet);

            for(CFG.Edge edge : block.outgoingEdges)
            {
                if(!visited.contains(edge.target().id))
                {
                    visited.add(edge.target().id);
                    queue.add(edge.target());
                }
            }
        }
    }

    private void computeFixpoint(CFG cfg, String inputVar)
    {

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
}
