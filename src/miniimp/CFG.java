package miniimp;
import java.util.ArrayList;
import java.util.List;
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
