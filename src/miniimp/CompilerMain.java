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
            System.out.println("--- 1. LEXING AND PARSING ---");
            List<Token> tokens = Lexer.tokenize(code);
            Parser parser = new Parser(tokens);
            Prog ast = parser.parseProg();
            System.out.println("AST generated with success. Input variable: " + ast.inputVar() + ", Output: " + ast.outputVar());

            System.out.println("\n--- 2. CFG CONSTRUCTION ---");
            CFGBuilder builder = new CFGBuilder();
            CFG cfg = builder.build(ast);

            System.out.println("\n[ORIGINAL CFG]");
            printCFG(cfg);

            System.out.println("\n--- 4. EXECUTE THE OPTIMIZER ---");
            Optimizer optimizer = new Optimizer();
            optimizer.optimizingPipeline(cfg, ast.inputVar(), ast.outputVar());
            System.out.println("\n=================================================");
            System.out.println("             FINAL OPTIMIZED CFG             ");
            System.out.println("=================================================");
            printCFG(cfg);
            System.out.println("\n--- 4. LLVM CODE GENERATION ---");
            LLVMGenerator llvmGen = new LLVMGenerator();
            llvmGen.generateToFile(cfg, ast.inputVar(), ast.outputVar(), "program.ll");

        } catch (Exception e) {
            System.err.println("\nFATAL ERROR:");
            e.printStackTrace();
        }
    }

    private static void printCFG(CFG cfg) {
        Set<Integer> visited = new HashSet<>();
        Queue<CFG.Node> queue = new LinkedList<>();

        queue.add(cfg.entryNode);
        visited.add(cfg.entryNode.id);

        while (!queue.isEmpty()) {
            CFG.Node block = queue.poll();
            System.out.println("\nBLOCK " + block.id + " (Instructions: " + block.commands.size() + ")");

            if (block.commands.isEmpty()) {
                System.out.println("  (Empty instructions)");
            } else {
                for (Cmd cmd : block.commands) {
                    System.out.println("  > " + cmd);
                }
            }
            for (CFG.Edge edge : block.outgoingEdges) {
                if (!visited.contains(edge.target().id)) {
                    visited.add(edge.target().id);
                    queue.add(edge.target());
                }
            }
        }
    }
}