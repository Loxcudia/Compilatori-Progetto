package miniimp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import miniimp.MiniImp.*;
public class TestCFG {
    //Generated class by AI to test CFG

    public static void main(String[] args) {

        // ==========================================
        // 1. COSTRUIAMO L'AST DI TEST
        // Codice: x := 0 ; while (x < 3) do x := x + 1
        // ==========================================

        // x := 0
        Cmd initCmd = new AssignCmd("x", new IntExpr(0));

        // Condizione: x < 3
        BoolExpr loopCond = new RelOpBool(new VarExpr("x"), new IntExpr(3));

        // Corpo del loop: x := x + 1
        Cmd loopBody = new AssignCmd("x", new BinOpExpr("+", new VarExpr("x"), new IntExpr(1)));

        // Il costrutto while
        Cmd whileCmd = new WhileCmd(loopCond, loopBody);

        // Sequenza principale: initCmd ; whileCmd
        Cmd programBody = new SeqCmd(initCmd, whileCmd);

        // Programma completo
        Prog myProgram = new Prog("in", "out", programBody);

        // ==========================================
        // 2. GENERIAMO IL CFG
        // ==========================================
        System.out.println("--- GENERAZIONE CFG ---");
        CFGBuilder builder = new CFGBuilder();
        CFG grafo = builder.build(myProgram);
        System.out.println("Grafo generato con successo!\n");

        // ==========================================
        // 3. STAMPIAMO IL GRAFO (Visita BFS)
        // ==========================================
        System.out.println("--- STRUTTURA DEL GRAFO ---");
        stampaGrafo(grafo);
    }

    // Metodo di supporto per stampare il grafo senza finire in loop infiniti
    public static void stampaGrafo(CFG cfg) {
        Set<Integer> visitati = new HashSet<>();
        Queue<CFG.Node> coda = new LinkedList<>();

        // Partiamo dal nodo iniziale
        coda.add(cfg.entryNode);
        visitati.add(cfg.entryNode.id);

        while (!coda.isEmpty()) {
            CFG.Node corrente = coda.poll();

            // Stampiamo il contenuto del nodo
            System.out.println("NODO " + corrente.id + " " + (corrente == cfg.entryNode ? "[ENTRY]" : ""));
            System.out.println("  Istruzioni: " + corrente.commands);

            if (corrente.outgoingEdges.isEmpty()) {
                System.out.println("  -> [FINE PROGRAMMA / EXIT NODE]");
            }

            // Stampiamo dove puntano gli archi uscenti
            for (CFG.Edge arco : corrente.outgoingEdges) {
                System.out.println("  --(" + arco.type() + ")--> Vai al NODO " + arco.target().id);

                // Se non abbiamo ancora visitato il nodo bersaglio, lo mettiamo in coda
                if (!visitati.contains(arco.target().id)) {
                    visitati.add(arco.target().id);
                    coda.add(arco.target());
                }
            }
            System.out.println("-----------------------------------");
        }
    }
}
