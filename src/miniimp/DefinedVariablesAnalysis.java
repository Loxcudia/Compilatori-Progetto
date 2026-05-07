package miniimp;
import java.util.*;
import miniimp.MiniImp.*;

public class DefinedVariablesAnalysis {

    public static final String DEFINED_VARS = "DEFINED_VARIABLES";

    protected String getAnalysisName() { return "DEFINED_VARS"; }

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
            for (Cmd stmt : block.commands) {
                // If the command is assigned, the variables is defined
                if (stmt instanceof AssignCmd assign) {
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
}