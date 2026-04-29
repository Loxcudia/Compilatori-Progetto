package minifun;

import java.util.List;
import minifun.MiniFun.*;
import minifun.Value.*;

public class MiniFunMain {
    //Generated Main by AI to fully test minifun
    public static void main(String[] args) {

        // 1. Il codice sorgente scritto in MiniFun
        // Testiamo letfun, let, lambda (fun), scope lessicale e chiamate!
        String code = """
            letfun add x = fun y => x + y in
            let addFive = add 5 in
            addFive 7
            """;

        System.out.println("--- FASE 1: Compilazione MiniFun ---");
        System.out.println("Analizzo il seguente codice:\n" + code);

        try {
            // 2. Lexing: trasforma la stringa in token
            List<Token> tokens = Lexer.tokenize(code);

            // 3. Parsing: costruisce l'Albero Sintattico (AST)
            Parser parser = new Parser(tokens);
            Expr ast = parser.parseExpr();

            System.out.println("Parsing completato con successo!");

            // 4. Esecuzione: avvia l'interprete funzionale
            System.out.println("\n--- FASE 2: Esecuzione ---");

            Evaluator interprete = new Evaluator();

            // In funzionale si parte SEMPRE da un ambiente vuoto
            Environment ambienteIniziale = new Environment();

            // Valutiamo l'intero albero
            Value risultato = interprete.eval(ast, ambienteIniziale);

            // 5. Stampa del risultato
            System.out.println("Oggetto restituito: " + risultato);

            // Per renderlo più leggibile all'utente finale
            if (risultato instanceof IntValue(int n)) {
                System.out.println("-> Il valore matematico finale è: " + n);
            } else if (risultato instanceof BoolValue(boolean b)) {
                System.out.println("-> Il valore logico finale è: " + b);
            } else if (risultato instanceof ClosureValue c) {
                System.out.println("-> Il risultato è una Funzione (Closure) che aspetta il parametro: " + c.param());
            }

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}