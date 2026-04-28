import java.util.*;
import java.util.regex.*;
public class MiniImp {
    // ==========================================
    // 1. I NODI DELL'AST (Abstract Syntax Tree)
    // ==========================================
    interface Node {}
    interface Cmd extends Node {}
    interface Expr extends Node {}

    // Esempio di nodi specifici
    record AssignCmd(String varName, Expr expr) implements Cmd {}
    record SeqCmd(Cmd left, Cmd right) implements Cmd {}
    record NumberExpr(int value) implements Expr {}
    record VarExpr(String name) implements Expr {}
    record PlusExpr(Expr left, Expr right) implements Expr {}

    // ==========================================
    // 2. IL LEXER (Lo Scanner)
    // ==========================================
    enum TokenType {
        DEF("def"), MAIN("main"), WITH("with"), INPUT("input"), OUTPUT("output"), AS("as"),
        ASSIGN(":="), SEMI(";"), PLUS("\\+"), MULT("\\*"), LPAREN("\\("), RPAREN("\\)"),
        VAR("[a-zA-Z][a-zA-Z0-9]*"), NUM("[0-9]+"), EOF("");

        public final String pattern;
        TokenType(String pattern) { this.pattern = pattern; }
    }

    record Token(TokenType type, String value) {}

    static class Lexer {
        static List<Token> tokenize(String input) {
            List<Token> tokens = new ArrayList<>();
            // Costruiamo una super-regex unendo tutti i pattern
            StringBuilder regex = new StringBuilder();
            for (TokenType t : TokenType.values()) {
                if (t != TokenType.EOF) regex.append(String.format("|(?<%s>%s)", t.name(), t.pattern));
            }
            Matcher m = Pattern.compile(regex.substring(1)).matcher(input);

            while (m.find()) {
                if (m.group().trim().isEmpty()) continue; // Ignora spazi
                for (TokenType t : TokenType.values()) {
                    if (t != TokenType.EOF && m.group(t.name()) != null) {
                        tokens.add(new Token(t, m.group(t.name())));
                        break;
                    }
                }
            }
            tokens.add(new Token(TokenType.EOF, ""));
            return tokens;
        }
    }

    // ==========================================
    // 3. IL PARSER (A discesa ricorsiva)
    // ==========================================
    static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        public Parser(List<Token> tokens) { this.tokens = tokens; }

        private Token current() { return tokens.get(pos); }
        private void advance() { if (pos < tokens.size() - 1) pos++; }

        // Controlla se il token corrente è quello atteso e va avanti
        private boolean match(TokenType expected) {
            if (current().type() == expected) {
                advance();
                return true;
            }
            return false;
        }

        // --- Regole Sintattiche ---

        // <cmd> ::= <var> := <e> | <cmd> ; <cmd>
        public Cmd parseCmd() {
            Cmd cmd = parseSingleCmd();
            // Se c'è un punto e virgola, leggiamo un altro comando e li uniamo in una Sequenza
            while (match(TokenType.SEMI)) {
                Cmd nextCmd = parseSingleCmd();
                cmd = new SeqCmd(cmd, nextCmd);
            }
            return cmd;
        }

        private Cmd parseSingleCmd() {
            if (current().type() == TokenType.VAR) {
                String varName = current().value();
                advance(); // Consuma la variabile
                if (!match(TokenType.ASSIGN)) throw new RuntimeException("Atteso ':=' dopo la variabile");
                Expr expr = parseExpr();
                return new AssignCmd(varName, expr);
            }
            throw new RuntimeException("Comando non riconosciuto: " + current().value());
        }

        // <e> ::= <e> + <e> | <var> | <int>
        public Expr parseExpr() {
            // Gestione ultra-base delle espressioni (Termine + Termine)
            Expr left = parseTerm();
            while (match(TokenType.PLUS)) {
                Expr right = parseTerm();
                left = new PlusExpr(left, right);
            }
            return left;
        }

        private Expr parseTerm() {
            if (current().type() == TokenType.NUM) {
                int val = Integer.parseInt(current().value());
                advance();
                return new NumberExpr(val);
            } else if (current().type() == TokenType.VAR) {
                String name = current().value();
                advance();
                return new VarExpr(name);
            }
            throw new RuntimeException("Espressione non valida: " + current().value());
        }
    }

}
