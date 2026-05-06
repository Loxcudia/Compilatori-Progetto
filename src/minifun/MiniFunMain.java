package minifun;

import java.util.List;
import minifun.MiniFun.*;
import minifun.Value.*;

public class MiniFunMain {
    //Generated Main by AI to fully test minifun
    public static void main(String[] args) {


        String code = """
            letfun sum : int -> int = 
                fun x : int => 
                    if x < 1 then 0 
                    else x + sum (x - 1)
            in
            let mySum = sum 5 in
            mySum
            """;

        System.out.println("--- FASE 1: Compilazione (Lexing + Parsing) ---");
        try {
            List<Token> tokens = Lexer.tokenize(code);
            Parser parser = new Parser(tokens);
            Expr ast = parser.parseExpr();
            System.out.println("Parsing completato con successo!\n");
            System.out.println("--- FASE 2: Analisi Statica (Type Checking) ---");

            TypeChecker typeChecker = new TypeChecker();
            TypeEnvironment ambienteIniziale = new TypeEnvironment();
            Type finalType = typeChecker.check(ast, ambienteIniziale);

            System.out.println("✅ Il programma è ben tipato! Nessuna incoerenza trovata.");
            System.out.println("-> Il tipo finale dedotto per questo programma è: " + finalType);

        } catch (TypeException e) {
            System.err.println("\n❌ TYPE CHECKING FALLITO:");
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("\n❌ ERRORE DI SISTEMA O DI PARSING:");
            e.printStackTrace();
        }
    }
}