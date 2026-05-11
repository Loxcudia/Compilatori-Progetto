package miniimp;
import java.util.*;
import miniimp.MiniImp.*;


public class ReachingDefinitionsAnalysis {
    public static final String REACHING_DEFS = "REACHED_DEFINITIONS";

    public void runAnalysis(CFG cfg)
    {
        System.out.println("Starting the analysis for Reached Definitions...");
        computeGenAndKill(cfg);
        computeFixpoint(cfg);
    }

    /*
        I build the gen and kill sets for reaching definitions in two passes:
        1st pass: I collect all the program definitions in one big Map (called allDefinitions)
        2nd pass: For each block, I check which variable it assigns and put into the gen set its definition. Then, I put into
        the kill set all the other definitions of those variables (taken from allDefinitions Map)
     */
    public void computeGenAndKill(CFG cfg)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        /*
            The 1st pass: I collect all the definitions of the entire program into a Map that looks as follows:
                                   "variable name" -> ["x@1", "x@5", "x@7"]
                             Where "x@n" stays for "the variable x is defined in the block n"
         */
        Map<String, Set<String>> allDefinitions = new HashMap<>();
        for(CFG.Node block : allNodes)
        {
            for(Cmd cmd : block.commands)
            {
                if(cmd instanceof AssignCmd assign)
                {
                    String varName = assign.varName();
                    allDefinitions.putIfAbsent(varName, new HashSet<>());
                    allDefinitions.get(varName).add(varName + "@" + block.id);
                }
            }
        }
        //The 2nd pass: build gen and kill block to block
        for(CFG.Node block : allNodes)
        {
            Set<String> genSet = new HashSet<>();
            Set<String> killSet = new HashSet<>();
            //Which variables are assigned in this block?
            Set<String> blockVars = new HashSet<>();
            for(Cmd cmd : block.commands)
            {
                if(cmd instanceof AssignCmd assign)
                    blockVars.add(assign.varName());
            }
            // Creation of gen and kill sets
            for(String varName : blockVars)
            {
                String definition = varName + "@" + block.id;
                //This definition survived
                genSet.add(definition);
                //I'm going to get all other definitions and put them on kill set
                for(String externalDefinition : allDefinitions.get(varName))
                {
                    if(!externalDefinition.equals(definition))
                        killSet.add(externalDefinition);
                }
            }
            block.setGenSets(REACHING_DEFS, genSet);
            block.setKillSets(REACHING_DEFS, killSet);
        }
    }

    public void computeFixpoint(CFG cfg)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        Map<CFG.Node, List<CFG.Node>> predecessors = new HashMap<>();
        for(CFG.Node block : allNodes)
        {
            block.setInputSets(REACHING_DEFS, new HashSet<>());
            block.setOutputSets(REACHING_DEFS, new HashSet<>());
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
       boolean changed = true;
        while(changed)
        {
            changed = false;
            for(CFG.Node block : allNodes)
            {
                List<CFG.Node> preds = predecessors.get(block);
                Set<String> newInputSet = new HashSet<>();
                //In(b) = Union of all Out(p) where p is predecessor of b:
                if(preds != null && !preds.isEmpty())
                {
                    for(int i = 0; i < preds.size(); i++)
                        newInputSet.addAll(preds.get(i).getOutputSets(REACHING_DEFS));
                }
                block.setInputSets(REACHING_DEFS, newInputSet);
                //Out(b) = Gen(b) U (In(b) \ Kill(b))
                Set<String> oldOutputSet = block.getOutputSets(REACHING_DEFS); //This is for the stability check
                Set<String> inMinusKill = new HashSet<>(newInputSet); //copy of input sets to not modify the original
                inMinusKill.removeAll(block.getKillSets(REACHING_DEFS)); //In(b) \ Kill(b)
                //Unify the result with Gen(b):
                Set<String> newOutputSet = new HashSet<>(block.getGenSets(REACHING_DEFS));
                newOutputSet.addAll(inMinusKill);
                //Stability check to discover if I have reached the fixpoint:
                if(!oldOutputSet.equals(newOutputSet))
                {
                    block.setOutputSets(REACHING_DEFS, newOutputSet); //For performance reasons, I update the output set if and only if I have not reached the fixpoint yet
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
