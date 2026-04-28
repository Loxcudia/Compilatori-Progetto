import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        String code = "x := 10 + 5 ; y := x";
        System.out.println("Codice: " + code + "\n");

        // 1. Lexing
        List<MiniImp.Token> tokens = MiniImp.Lexer.tokenize(code);
        System.out.println("Tokens: " + tokens);

        // 2. Parsing
        MiniImp.Parser parser = new MiniImp.Parser(tokens);
        MiniImp.Cmd ast = parser.parseCmd();

        System.out.println("\nAST Generato:\n" + ast);
    }
}