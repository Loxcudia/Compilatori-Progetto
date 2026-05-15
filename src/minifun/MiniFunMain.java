package minifun;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import minifun.MiniFun.*;
import minifun.Value.*;

public class MiniFunMain {
    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("ERROR: You must specify which minifun file to compile!");
            System.exit(1);
        }
        String filePath = args[0];
        System.out.println("STARTING MINIFUN COMPILATION");

        try {
            String code = Files.readString(Path.of(filePath));
            System.out.println("Execution of MiniFun: " + filePath + " ---\n");
            List<Token> tokens = Lexer.tokenize(code);
            Parser parser = new Parser(tokens);
            Expr ast = parser.parseExpr();
            System.out.println("Parsing terminate with success!\n");
            System.out.println("STARTING TYPE CHECKING");

            TypeChecker typeChecker = new TypeChecker();
            TypeEnvironment startingEnvironment = new TypeEnvironment();
            Type finalType = typeChecker.check(ast, startingEnvironment);

            System.out.println("The program is well typed!");
            System.out.println("-> the output type of the program is " + finalType);

            System.out.println("EVALUATING THE PROGRAM");
            Evaluator evaluator = new Evaluator();
            Value result = evaluator.eval(ast, new Environment());

            System.out.println("\nOUTPUT RESULT: " + result);
        } catch (TypeException e) {
            System.err.println("\nERROR: TYPE CHECKING FAILED:");
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("\nSYSTEM OR PARSING ERROR:");
            e.printStackTrace();
        }
    }
}