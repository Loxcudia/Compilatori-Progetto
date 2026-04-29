package miniimp;

import java.util.List;
public class MiniImpMain {


    //Generated Main by AI to fully test miniimp
    public static void main(String[] args) {

        String code = """
            def main with input in output out as
            out := 1 ;
            i := 1 ;
            while i < in + 1 do (
                out := out * i ;
                i := i + 1
            )
            """;

        try {
            // 2. Lexing: trasforma la stringa in token
            // (Assumendo che tu abbia messo Lexer e Parser in una classe MiniImpParserFull)
            List<MiniImp.Token> tokens = MiniImp.Lexer.tokenize(code);

            // 3. Parsing: costruisce l'Albero Sintattico Astratto (AST)
            MiniImp.Parser parser = new MiniImp.Parser(tokens);
            MiniImp.Prog ast = parser.parseProg();

            System.out.println("Parsing completato con successo!");

            // 4. Esecuzione: avvia l'interprete
            System.out.println("\n--- FASE 2: Esecuzione ---");
            Interpreter interprete = new Interpreter();

            // Proviamo a calcolare il fattoriale di 3
            int valoreInput = 3;
            System.out.println("Eseguo il programma con input = " + valoreInput);

            int risultato = interprete.evalProg(ast, valoreInput);

            // 5. Stampa del risultato
            System.out.println("Risultato finale (output): " + risultato);

            // EXTRA: Puoi testare un altro valore, ad esempio 5 (dovrà restituire 120)
            // System.out.println("Fattoriale di 5: " + interprete.evalProg(ast, 5));

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}