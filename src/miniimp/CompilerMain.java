package miniimp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import miniimp.MiniImp.*;

public class CompilerMain {

    public static void main(String[] args) {

        if(args.length < 1)
        {
            System.out.println("ERROR: no argument passed! You must specify which file to compile!");
            System.out.println("Try java CompilerMain program.imp");
        }
        String filePath = args[0];
        String code = "";

        try
        {
            code = Files.readString(Path.of(filePath));
        } catch (Exception e) {
            System.err.println("ERROR: Can't read the file " + filePath);
            System.exit(1);
        }

        System.out.println("Compilation of the file: " + filePath + "...\n");

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

            // =====================================================================
            // 4. GENERAZIONE CODICE LLVM
            // =====================================================================
            System.out.println("\n--- 4. GENERAZIONE CODICE LLVM ---");
            LLVMGenerator llvmGen = new LLVMGenerator();

            // Generiamo il file "program.ll" partendo dal CFG che abbiamo appena ottimizzato.
            // Passiamo ast.inputVar() e ast.outputVar() per gestire correttamente i registri.
            llvmGen.generateToFile(cfg, ast.inputVar(), ast.outputVar(), "program.ll");
            // =====================================================================

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