package miniimp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import miniimp.MiniImp.*;



public class TestInterpreter {
    public static void main(String[] args)
    {
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
        try {
            // 2. Lexing e Parsing (Costruzione dell'AST)
            List<Token> tokens = Lexer.tokenize(code);
            Parser parser = new Parser(tokens);
            Prog programAST = parser.parseProg();

            // 3. Esecuzione tramite Interprete
            int inputTest = 200; // Scegli il numero che vuoi simulare in input!
            System.out.println("Eseguo il programma con input x = " + inputTest + "...");

            Interpreter evaluator = new Interpreter();
            int risultato = evaluator.evalProg(programAST, inputTest);

            System.out.println("Risultato finale (y) = " + risultato);

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
