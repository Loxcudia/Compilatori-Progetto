package miniimp;
import java.util.*;
import miniimp.MiniImp.*;

public class DefinedVariablesAnalysis {

    public static final String DEFINED_VARS = "DEFINED_VARIABLES";

    public void runAnalysis(CFG cfg, String inputVar) {
        System.out.println("Starting the analysis for Defined Variables...");
        computeGen(cfg);
        computeFixpoint(cfg, inputVar);
    }

    /* GEN SET CALCULATING FUNCTION:
            Given a CFG node, I look into its list of commands.
            The variables change its values if and only if I meet an assignment command. In all other cases (sequence, if, while, skip) the variables are read but not defined.
            Every time I pick an assignment, I pick the variable name (the left part of the assignment), called varName in my AssignCmd record and I add it in the gen set.
            The management of duplicates is automatic because duplicates are illegal in the Set collection.
     */
    public void computeGen(CFG cfg) {

        //Preparing the queue to visit the graph starting from the entry node
        Set<Integer> visited = new HashSet<>();
        Queue<CFG.Node> queue = new LinkedList<>();

        queue.add(cfg.entryNode);
        visited.add(cfg.entryNode.id);

        while (!queue.isEmpty()) {
            CFG.Node block = queue.poll();

            //I create the new empty gen set for this block
            Set<String> genSet = new HashSet<>();

            // I go down into the instructions of the maximal block
            for (Cmd command : block.commands) {
                // If the command is assigned, the variables is defined
                if (command instanceof AssignCmd assign) {
                    genSet.add(assign.varName());
                }
            }
            block.setGenSets(DEFINED_VARS, genSet);
            // I put the other nodes into the queue to continue the visit
            for (CFG.Edge edge : block.outgoingEdges) {
                if (!visited.contains(edge.target().id)) {
                    visited.add(edge.target().id);
                    queue.add(edge.target());
                }
            }
        }
    }

    /*

     */
    private void computeFixpoint(CFG cfg, String inputVar)
    {
        Set<String> universalSet = new HashSet<>();
        Map<CFG.Node, List<CFG.Node>> predecessors = new HashMap<>();
        List<CFG.Node> allNodes = getAllNodes(cfg);
        universalSet.add(inputVar);
        for(CFG.Node block : allNodes)
        {
            universalSet.addAll(block.getGenSets(DEFINED_VARS));
            predecessors.put(block, new ArrayList<>());
        }
        for(CFG.Node fatherNode : allNodes)
        {
            for(CFG.Edge edge : fatherNode.outgoingEdges)
            {
                CFG.Node sonNode = edge.target();
                predecessors.get(sonNode).add(fatherNode);
            }
        }
        for(CFG.Node block : allNodes)
        {
            if(block == cfg.entryNode)
            {
                //The entry node always has the input set = {inp}
                block.setInputSets(DEFINED_VARS, Set.of(inputVar));
                //For the output set, I just follow the equation out(entry node) = gen(entry node) U in(entry node)
                Set<String> entryOut = new HashSet<>(block.getGenSets(DEFINED_VARS));
                entryOut.addAll(block.getInputSets(DEFINED_VARS));
                block.setOutputSets(DEFINED_VARS, entryOut);
            }
            else
            {
                //In any other case, just start with empty input set and output set equals to universal set, because if I start with output set empty all the intersections with it will be empty
                block.setInputSets(DEFINED_VARS, new HashSet<>());
                block.setOutputSets(DEFINED_VARS, new HashSet<>(universalSet));
            }
        }
        boolean changed = true;
        while(changed)
        {
            changed = false;
            for(CFG.Node block : allNodes)
            {
                if(block == cfg.entryNode) continue;
                List<CFG.Node> preds = predecessors.get(block);
                Set<String> newInputSet = new HashSet<>();

                //In = intersect of all out of the predecessors
                if(preds != null && !preds.isEmpty())
                {
                    newInputSet.addAll(preds.get(0).getOutputSets(DEFINED_VARS));
                    for(int i = 0; i < preds.size(); i++)
                        newInputSet.retainAll(preds.get(i).getOutputSets(DEFINED_VARS));
                }
                block.setInputSets(DEFINED_VARS, newInputSet);

                //Out(block) = gen(block) U In(block)
                Set<String> oldOut = block.getOutputSets(DEFINED_VARS);
                Set<String> newOut = new HashSet<>(block.getGenSets(DEFINED_VARS));
                newOut.addAll(newInputSet);

                //I look only at the output sets to determine if I reached the fixpoint, that's because the input sets are built from the output sets of the predecessors, so the element of the input sets are output element themselves
                if(!oldOut.equals(newOut))
                {
                    block.setOutputSets(DEFINED_VARS, newOut); //For performance reasons, I update the output set if and only if I have not reached the fixpoint yet
                    changed = true;
                }
            }
        }

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