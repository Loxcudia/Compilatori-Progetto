package miniimp;
import java.util.*;
import miniimp.MiniImp.*;

public class CFG {

    public enum EdgeType {
        NORMAL,             // Standard link between two blocks
        TRUE,               // Then branch of an if or the body of a while
        FALSE               // Else branch of an if or the exit of a while
    }

    public static class Node {
        private static int idCounter = 0;

        public final int id;
        public final List<Cmd> commands;
        public final List<Edge> outgoingEdges;

        //The String keys of the following maps are identifiers for the analysis (for example: DEFINED_VARIABLES)
        private final Map<String, Set<?>> inputSets = new HashMap<>();
        private final Map<String, Set<?>> outputSets = new HashMap<>();
        private final Map<String, Set<?>> genSets = new HashMap<>();
        private final Map<String, Set<?>> killSets = new HashMap<>();



        public Node()
        {
            this.id = idCounter++;
            this.commands = new ArrayList<>();
            this.outgoingEdges = new ArrayList<>();
        }

        public void addCommand(Cmd cmd)
        {
            this.commands.add(cmd);
        }

        public void addEdge(Node target, EdgeType type)
        {
            this.outgoingEdges.add(new Edge(target, type));
        }

        @SuppressWarnings("unchecked")
        public <T> Set<T> getInputSets(String analysisName)
        {
            //getOrDefault return an empty new hashset if the output set does not exist
            return (Set<T>) inputSets.getOrDefault(analysisName, new HashSet<>());
        }

        public <T> void setInputSets(String analysisName, Set<T> set)
        {
            inputSets.put(analysisName, new HashSet<>(set));
        }

        @SuppressWarnings("unchecked")
        public <T> Set<T> getOutputSets(String analysisName)
        {
            return (Set<T>) outputSets.getOrDefault(analysisName, new HashSet<>());
        }

        public <T> void setOutputSets(String analysisName, Set<T> set)
        {
            outputSets.put(analysisName, new HashSet<>(set));
        }

        @SuppressWarnings("unchecked")
        public <T> Set<T> getGenSets(String analysisName)
        {
            return (Set<T>) genSets.getOrDefault(analysisName, new HashSet<>());
        }

        public <T> void setGenSets(String analysisName, Set<T> set)
        {
            genSets.put(analysisName, new HashSet<>(set));
        }

        @SuppressWarnings("unchecked")
        public <T> Set<T> getKillSets(String analysisName)
        {
            return (Set<T>) killSets.getOrDefault(analysisName, new HashSet<>());
        }

        public <T> void setKillSets(String analysisName, Set<T> set)
        {
            killSets.put(analysisName, new HashSet<>(set));
        }




        @Override
        public String toString()
        {
            return "Block " + id + " " + commands;
        }
    }

    public record Edge(Node target, EdgeType type) {}

    public final Node entryNode;
    public final Node exitNode;

    public CFG(Node entryNode, Node exitNode) {
        this.entryNode = entryNode;
        this.exitNode = exitNode;
    }

}
