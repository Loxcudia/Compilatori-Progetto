package miniimp;
import java.util.*;
import miniimp.MiniImp.*;


public class LiveVariablesAnalysis {

    public static final String LIVE_VARS = "LIVE_VARIABLES";

    public void runAnalysis(CFG cfg, String outputVar)
    {
        System.out.println("Starting the analysis for Live Variables...");
        computeGenAndKill(cfg);
        computeFixpoint(cfg, outputVar);
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
    /*

     */
    private void computeFixpoint(CFG cfg, String outputVar)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        Collections.reverse(allNodes);
        for(CFG.Node block : allNodes)
        {
            block.setInputSets(LIVE_VARS, new HashSet<>());
            if(block.outgoingEdges.isEmpty())
                block.setOutputSets(LIVE_VARS, new HashSet<>(Set.of(outputVar)));
            else
                block.setOutputSets(LIVE_VARS, new HashSet<>());
        }

        boolean changed = true;
        while(changed)
        {
            changed = false;
            for(CFG.Node block: allNodes)
            {
                Set<String> newOut = new HashSet<>();
                //Out(b) = Union of all In(s) for all s successor of b
                if(block.outgoingEdges.isEmpty())
                    newOut.add(outputVar);
                else
                {
                    for(CFG.Edge edge: block.outgoingEdges)
                    {
                        CFG.Node successor = edge.target();
                        newOut.addAll(successor.getInputSets(LIVE_VARS));
                    }
                }
                block.setOutputSets(LIVE_VARS, newOut);
                //In(b) = Gen(b) U (Out(b) \ Kill(b))
                Set<String> oldIn = block.getInputSets(LIVE_VARS); //This is for the stability check
                Set<String> outMinusKill = new HashSet<>(newOut); //I do a copy of out set to not modify the original
                outMinusKill.removeAll(block.getKillSets(LIVE_VARS)); //(Out(b) \ Kill(b))
                //I unify the result with Gen(b) to complete the equation:
                Set<String> newIn = new HashSet<>(block.getGenSets(LIVE_VARS));
                newIn.addAll(outMinusKill);

                //Stability check to discover if I have reached the fixpoint
                if(!oldIn.equals(newIn))
                {
                    block.setInputSets(LIVE_VARS, newIn); //For performance reasons, I update the input set if and only if I have not reached the fixpoint yet
                    changed = true;
                }
            }
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
