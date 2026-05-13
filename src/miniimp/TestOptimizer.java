package miniimp;

import java.util.*;
import miniimp.MiniImp.*;

public class TestOptimizer {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   TEST PIPELINE DI OTTIMIZZAZIONE MINI-IMP");
        System.out.println("=================================================\n");

        // 1. IL PROGRAMMA "TRAPPOLA"
        // Questo programma è progettato per far brillare la tua pipeline.
        // Calcola un sacco di cose inutili, ha rami morti e variabili che spariscono.
        String code = """
            def main with input inp output z as
            a := 10 ;
            b := 2 * 5 ;
            c := a + b ;
            d := c - 10 ;
            x := inp ;
            if d < 50 then
                y := d + x
            else
                y := 0 ;
            z := y
            """;

        try {
            System.out.println("--- 1. LEXING E PARSING ---");
            List<Token> tokens = Lexer.tokenize(code);
            Parser parser = new Parser(tokens);
            Prog ast = parser.parseProg();
            System.out.println("AST generato con successo. Variabile di Input: " + ast.inputVar() + ", Output: " + ast.outputVar());

            System.out.println("\n--- 2. COSTRUZIONE DEL CFG ---");
            CFGBuilder builder = new CFGBuilder();
            CFG cfg = builder.build(ast);

            System.out.println("\n[CFG ORIGINALE]");
            stampaCFG(cfg);

            // 4. LANCIO DELLA PIPELINE
            System.out.println("\n--- 4. ESECUZIONE OTTIMIZZATORE ---");
            Optimizer optimizer = new Optimizer();

            // Attenzione: la pipeline ricalcola le Live Variables per tagliare il codice morto.
            // Le Live Variables hanno bisogno della variabile di OUTPUT (z) per iniziare il viaggio all'indietro!
            // Nel tuo Optimizer.java il parametro si chiama "inputVar", ma noi gli passiamo ast.outputVar()
            optimizer.optimizingPipeline(cfg, ast.inputVar(), ast.outputVar());

            // 5. RISULTATO FINALE
            System.out.println("\n=================================================");
            System.out.println("             CFG OTTIMIZZATO FINALE              ");
            System.out.println("=================================================");
            stampaCFG(cfg);

        } catch (Exception e) {
            System.err.println("\nERRORE FATALE durante il test:");
            e.printStackTrace();
        }
    }

    // Metodo di supporto per stampare il CFG in modo leggibile,
    // visitando tutti i nodi con una ricerca in ampiezza (BFS)
    private static void stampaCFG(CFG cfg) {
        Set<Integer> visited = new HashSet<>();
        Queue<CFG.Node> queue = new LinkedList<>();

        queue.add(cfg.entryNode);
        visited.add(cfg.entryNode.id);

        while (!queue.isEmpty()) {
            CFG.Node block = queue.poll();
            System.out.println("\nBLOCCO " + block.id + " (Istruzioni: " + block.commands.size() + ")");

            if (block.commands.isEmpty()) {
                System.out.println("  (Nessuna istruzione)");
            } else {
                for (Cmd cmd : block.commands) {
                    System.out.println("  > " + cmd);
                }
            }

            // Metto in coda i successori
            for (CFG.Edge edge : block.outgoingEdges) {
                if (!visited.contains(edge.target().id)) {
                    visited.add(edge.target().id);
                    queue.add(edge.target());
                }
            }
        }
    }
}