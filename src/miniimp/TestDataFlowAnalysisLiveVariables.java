package miniimp;

import java.util.List;
import miniimp.MiniImp.*;

public class TestDataFlowAnalysisLiveVariables {

    //Generated class by AI to test the dataflow analysis for live variables
    public static void main(String[] args) {
        System.out.println("=== SUITE DI TEST: LIVE VARIABLES ===\n");

        // TEST 1: Assegnazione Lineare (Verifica Gen/Kill base e flusso all'indietro)
        String test1 = "def main with input inp output out as " +
                "x := 1; " +
                "y := x + 1";
        eseguiTest("TEST 1 - Assegnazione Lineare", test1);

        // TEST 2: Il Bivio IF (Verifica dell'Unione all'indietro)
        // Nota: Poiché z := 3 non legge x e y, queste variabili dovrebbero risultare "morte" (non in IN)!
        String test2 = "def main with input inp output out as " +
                "(if inp < 0 then x := 1 else y := 2); " +
                "z := 3";
        eseguiTest("TEST 2 - IF e Dead Code", test2);

        // TEST 3: Ciclo While (Verifica del Fixpoint all'indietro)
        String test3 = "def main with input inp output out as " +
                "x := 0; " +
                "(while x < 10 do ( y := 1; x := x + 1 )); " +
                "out := y"; // Ho cambiato z in 'out' per farti vedere come la var di output resta viva alla fine!
        eseguiTest("TEST 3 - Ciclo While (Fixpoint Backward)", test3);
    }

    /**
     * Metodo di supporto che incapsula tutta la pipeline (Lexer -> Parser -> CFG -> Analisi)
     */
    private static void eseguiTest(String nomeTest, String codiceSorgente) {
        System.out.println("==================================================");
        System.out.println(nomeTest);
        System.out.println("Codice: " + codiceSorgente.substring(38)); // Salto l'intestazione per pulizia visiva
        System.out.println("==================================================");

        try {
            // 1. Lexer & Parser
            List<Token> tokens = Lexer.tokenize(codiceSorgente);
            Parser parser = new Parser(tokens);
            Prog program = parser.parseProg();

            // 2. Costruzione del CFG
            CFGBuilder builder = new CFGBuilder();
            CFG grafo = builder.build(program);

            // 3. Analisi Data-Flow (Live Variables viaggia all'indietro, parte dall'OUTPUT!)
            LiveVariablesAnalysis analisi = new LiveVariablesAnalysis();
            analisi.runAnalysis(grafo, program.outputVar());

            // 4. Stampa dei risultati
            stampaRisultati(grafo);

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione del test: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\n");
    }

    /**
     * Stampa i risultati in modo leggibile.
     */
    private static void stampaRisultati(CFG cfg) {
        System.out.println("\n--- RISULTATI DEI BLOCCHI ---");
        List<CFG.Node> tuttiINodi = getAllNodes(cfg);

        // Nelle Live Variables può essere più intuitivo leggere l'output al contrario (dall'Exit all'Entry)
        // ma per coerenza con l'altro test li stampiamo nell'ordine normale.
        for (CFG.Node nodo : tuttiINodi) {
            System.out.println("Blocco [" + nodo.id + "]");

            // Stampiamo il codice contenuto nel blocco per orientarci
            System.out.print("  Codice: ");
            if (nodo.commands == null || nodo.commands.isEmpty()) {
                System.out.println("(Vuoto / Entry / Exit / Condizione)");
            } else {
                for (Cmd stmt : nodo.commands) {
                    System.out.print(stmt.getClass().getSimpleName() + " ");
                }
                System.out.println();
            }

            // Stampa degli insiemi calcolati con la chiave corretta!
            String analisi = LiveVariablesAnalysis.LIVE_VARS;

            System.out.println("  GEN  : " + nodo.getGenSets(analisi));
            System.out.println("  KILL : " + nodo.getKillSets(analisi));
            System.out.println("  IN   : " + nodo.getInputSets(analisi));
            System.out.println("  OUT  : " + nodo.getOutputSets(analisi));
            System.out.println("-----------------------------------");
        }
    }

    /**
     * Metodo BFS di supporto per estrarre tutti i nodi dal grafo.
     */
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