package miniimp;

import java.util.List;
import miniimp.MiniImp.*;

public class TestDataFlowAnalysisReachingDefinitions {

    public static void main(String[] args) {
        System.out.println("=== SUITE DI TEST: REACHING DEFINITIONS ===\n");

        // TEST 1: Sovrascrittura (Verifica che la vecchia definizione venga uccisa)
        // x viene definita due volte. La prima (x@0) non dovrebbe MAI raggiungere la fine.
        String test1 = "def main with input inp output out as " +
                "x := 1; " +
                "y := x + 1; " +
                "x := 2";
        eseguiTest("TEST 1 - Sovrascrittura Lineare", test1);

        // TEST 2: Il Bivio IF (Il test supremo per l'Unione Forward)
        // Entrambi i rami definiscono 'x'. Il nodo z:=x finale dovrebbe ricevere ENTRAMBE le definizioni!
        String test2 = "def main with input inp output out as " +
                "(if inp < 0 then x := 1 else x := 2); " +
                "z := x";
        eseguiTest("TEST 2 - Bivio IF (Convergenza Definizioni)", test2);

        // TEST 3: Ciclo While (Il ritorno all'indietro)
        // x è definita prima del ciclo e DENTRO il ciclo. Il nodo condizione deve vederle entrambe.
        String test3 = "def main with input inp output out as " +
                "x := 0; " +
                "(while x < 10 do ( y := 1; x := x + 1 )); " +
                "out := x";
        eseguiTest("TEST 3 - Ciclo While (Fixpoint Forward)", test3);
    }

    private static void eseguiTest(String nomeTest, String codiceSorgente) {
        System.out.println("==================================================");
        System.out.println(nomeTest);
        System.out.println("Codice: " + codiceSorgente.substring(38));
        System.out.println("==================================================");

        try {
            List<Token> tokens = Lexer.tokenize(codiceSorgente);
            Parser parser = new Parser(tokens);
            Prog program = parser.parseProg();

            CFGBuilder builder = new CFGBuilder();
            CFG grafo = builder.build(program);

            // Avvio l'analisi. Assicurati che ReachingDefinitionsAnalysis abbia un metodo runAnalysis!
            ReachingDefinitionsAnalysis analisi = new ReachingDefinitionsAnalysis();
            analisi.runAnalysis(grafo); // Niente inputVar stavolta!

            stampaRisultati(grafo);

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\n");
    }

    private static void stampaRisultati(CFG cfg) {
        System.out.println("\n--- RISULTATI DEI BLOCCHI ---");
        List<CFG.Node> tuttiINodi = getAllNodes(cfg);

        for (CFG.Node nodo : tuttiINodi) {
            System.out.println("Blocco [" + nodo.id + "]");
            System.out.print("  Codice: ");

            if (nodo.commands == null || nodo.commands.isEmpty()) {
                System.out.println("(Vuoto / Entry / Exit / Condizione)");
            } else {
                for (Cmd stmt : nodo.commands) {
                    System.out.print(stmt.getClass().getSimpleName() + " ");
                }
                System.out.println();
            }

            // Stampa degli insiemi calcolati
            String analisi = ReachingDefinitionsAnalysis.REACHED_DEFS;

            System.out.println("  GEN  : " + nodo.getGenSets(analisi));
            System.out.println("  KILL : " + nodo.getKillSets(analisi));
            System.out.println("  IN   : " + nodo.getInputSets(analisi));
            System.out.println("  OUT  : " + nodo.getOutputSets(analisi));
            System.out.println("-----------------------------------");
        }
    }

    private static List<CFG.Node> getAllNodes(CFG cfg) {
        java.util.List<CFG.Node> result = new java.util.ArrayList<>();
        java.util.Set<Integer> visitati = new java.util.HashSet<>();
        java.util.Queue<CFG.Node> coda = new java.util.LinkedList<>();

        coda.add(cfg.entryNode);
        visitati.add(cfg.entryNode.id);
        result.add(cfg.entryNode);

        while (!coda.isEmpty()) {
            CFG.Node corrente = coda.poll();
            for (CFG.Edge arco : corrente.outgoingEdges) {
                if (!visitati.contains(arco.target().id)) {
                    visitati.add(arco.target().id);
                    coda.add(arco.target());
                    result.add(arco.target());
                }
            }
        }
        return result;
    }
}
