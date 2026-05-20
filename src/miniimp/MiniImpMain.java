package miniimp;
import miniimp.MiniImp.*;
import java.util.List;

public class MiniImpMain {
    //Generated Main by AI to test miniimp
    public static void main(String[] args) {

        String code = """
                def main with input x output y as
                a := 2 * (x + 3);
                if not (a < 10) then
                    y := a + (5 * (2 + 1))
                else
                    y := (a * 2) + 1
                """;


        try {
            List<Token> tokens = Lexer.tokenize(code);
            Parser parser = new Parser(tokens);
            Prog ast = parser.parseProg();

            System.out.println("Parsing completato con successo!");

            System.out.println("\n--- FASE 2: Esecuzione ---");
            Interpreter interprete = new Interpreter();
            int valoreInput = 1;
            System.out.println("Eseguo il programma con input = " + valoreInput);

            int risultato = interprete.evalProg(ast, valoreInput);

            System.out.println("Risultato finale (output): " + risultato);


        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}