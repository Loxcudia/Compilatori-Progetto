package minifun;

import java.util.*;
import java.util.regex.*;

public class MiniFun {
    //AST NODES
    interface Node {}
    interface Expr extends Node {}

    public interface Type {}
    public record IntType() implements Type {
        @Override public String toString() {return "int";}
    }
    public record BoolType() implements Type {
        @Override public String toString() {return "bool";}
    }
    public record FunType(Type left, Type right) implements Type {
        @Override public String toString() {return "(" + left + "->" + right + ")";}
    }

    // Basic values
    record IntExpr(int value) implements Expr {}
    record BoolExpr(boolean value) implements Expr {} // Uniamo True e False per comodità
    record VarExpr(String name) implements Expr {}

    // Operations
    record BinOpExpr(String op, Expr left, Expr right) implements Expr {}
    record IfExpr(Expr condition, Expr thenBranch, Expr elseBranch) implements Expr {}

    // Scope and functions
    record FunExpr(String parameter, Type parameterType, Expr body) implements Expr {} // fun x => t
    record AppExpr(Expr function, Expr arg) implements Expr {} // t t (es. f 3)
    record LetExpr(String varName, Expr value, Expr body) implements Expr {} // let x = t in t'
    record LetFunExpr(String funName, Type parameterType, Expr funBody, Expr body) implements Expr {} // letfun f x = t in t'


    enum TokenType {
        //KEYWORDS (must be before VAR)
        FUN("fun"), IF("if"), THEN("then"), ELSE("else"),
        LETFUN("letfun"), LET("let"), INTTYPE("int"), IN("in"), TRUE("true"), FALSE("false"),
         BOOLTYPE("bool"),

        //COMPOSED SYMBOLS (must be before the singles)
        ARROW("=>"), FUNTYPEARROW("->"), EQCMP("=="),

        //SINGLE SYMBOLS
        EQASSIGN("="), PLUS("\\+"), MINUS("-"), MULT("\\*"), LT("<"),
        TYPEOF(":"), LPAREN("\\("), RPAREN("\\)"),

        //GENERIC IDENTIFIERS AND EOF (always on bottom)
        VAR("[a-zA-Z][a-zA-Z0-9]*"), NUM("[0-9]+"), EOF("");

        public final String pattern;
        TokenType(String pattern) { this.pattern = pattern; }
    }

    record Token(TokenType type, String value) {}

    static class Lexer {
        static List<Token> tokenize(String input) {
            List<Token> tokens = new ArrayList<>();
            StringBuilder regex = new StringBuilder();
            for (TokenType t : TokenType.values()) {
                if (t != TokenType.EOF) regex.append(String.format("|(?<%s>%s)", t.name(), t.pattern));
            }
            Matcher m = Pattern.compile(regex.substring(1)).matcher(input);

            while (m.find()) {
                if (m.group().trim().isEmpty()) continue;
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

    //Recursive descending parser, written with the help of the AI
    static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        public Parser(List<Token> tokens) { this.tokens = tokens; }

        private Token current() { return tokens.get(pos); }
        private void advance() { if (pos < tokens.size() - 1) pos++; }

        private boolean match(TokenType expected) {
            if (current().type() == expected) { advance(); return true; }
            return false;
        }

        private Token consume(TokenType expected, String errorMsg) {
            Token tokenToReturn = current();
            if (!match(expected)) {
                throw new RuntimeException(errorMsg + " Got: " + current().value());
            }
            return tokenToReturn;
        }

        // Entry point: Read a term <t>
        public Expr parseExpr() {
            // 1. letfun f x = t in t'
            if (match(TokenType.LETFUN)) {
                String fName = consume(TokenType.VAR, "Expected function name after 'letfun'").value();

                //Consume the : and read the function type
                consume(TokenType.TYPEOF, "Expected ':' after function name " + fName);
                Type fType = parseType();

                consume(TokenType.EQASSIGN, "Expected '=' after function type");
                Expr fBody = parseExpr();

                consume(TokenType.IN, "Expected 'in' after function body");
                Expr body = parseExpr();

                return new LetFunExpr(fName, fType, fBody, body);
            }
            // 2. let x = t in t'
            else if (match(TokenType.LET)) {
                String varName = current().value();
                consume(TokenType.VAR, "Expected variable name");
                consume(TokenType.EQASSIGN, "Expected '='");
                Expr value = parseExpr();
                consume(TokenType.IN, "Expected 'in'");
                Expr body = parseExpr();
                return new LetExpr(varName, value, body);
            }
            // 3. fun x => t
            else if (match(TokenType.FUN)) {
                String param = consume(TokenType.VAR, "Expected parameter name after 'fun'").value();

                // Consume : and read the type
                consume(TokenType.TYPEOF, "Expected ':' after the parameter " + param);
                Type paramType = parseType();

                consume(TokenType.ARROW, "Expected row '=>' after the type");
                Expr body = parseExpr();

                return new FunExpr(param, paramType, body);
            }
            // 4. if t then t else t
            else if (match(TokenType.IF)) {
                Expr cond = parseExpr();
                consume(TokenType.THEN, "Expected 'then'");
                Expr thenBranch = parseExpr();
                consume(TokenType.ELSE, "Expected 'else'");
                Expr elseBranch = parseExpr();
                return new IfExpr(cond, thenBranch, elseBranch);
            }

            // If that's not one of the keyword, go down in the operators
            return parseRelOp();
        }

        // Relational operators: == , <
        private Expr parseRelOp() {
            Expr left = parseAddOp();
            while (current().type() == TokenType.EQCMP || current().type() == TokenType.LT) {
                String op = current().value();
                advance();
                left = new BinOpExpr(op, left, parseAddOp());
            }
            return left;
        }

        // Additional operators: + , -
        private Expr parseAddOp() {
            Expr left = parseMultOp();
            while (current().type() == TokenType.PLUS || current().type() == TokenType.MINUS) {
                String op = current().value();
                advance();
                left = new BinOpExpr(op, left, parseMultOp());
            }
            return left;
        }

        // Multiplicative operator: *
        private Expr parseMultOp() {
            Expr left = parseApp();
            while (match(TokenType.MULT)) {
                left = new BinOpExpr("*", left, parseApp());
            }
            return left;
        }

        // Function application
        private Expr parseApp() {
            Expr left = parseAtom();
            // Until the next token can start with an atomic expression, we are passing arguments
            while (isAtomStart()) {
                Expr arg = parseAtom();
                left = new AppExpr(left, arg);
            }
            return left;
        }

        //An atomic value is a base value, a variable, an expression in brackets
        private boolean isAtomStart() {
            TokenType t = current().type();
            return t == TokenType.NUM || t == TokenType.TRUE || t == TokenType.FALSE ||
                    t == TokenType.VAR || t == TokenType.LPAREN;
        }

        private Expr parseAtom() {
            if (current().type() == TokenType.NUM) {
                int val = Integer.parseInt(current().value());
                advance();
                return new IntExpr(val);
            } else if (match(TokenType.TRUE)) {
                return new BoolExpr(true);
            } else if (match(TokenType.FALSE)) {
                return new BoolExpr(false);
            } else if (current().type() == TokenType.VAR) {
                String name = current().value();
                advance();
                return new VarExpr(name);
            } else if (match(TokenType.LPAREN)) {
                Expr expr = parseExpr();
                consume(TokenType.RPAREN, "Expected closing brackets ')'");
                return expr;
            }
            throw new RuntimeException("Unexpected term: " + current().value());
        }

        // Method to parsing types (left associative for the row ->)
        private Type parseType() {
            Type type = parseBaseType();

            // If we got a '->', continue to concatenate at left
            while (match(TokenType.FUNTYPEARROW)) {
                Type rightType = parseBaseType();
                type = new FunType(type, rightType);
            }

            return type;
        }

        // Method for base types or types in brackets
        private Type parseBaseType() {
            if (match(TokenType.INTTYPE)) {
                return new IntType();
            }
            if (match(TokenType.BOOLTYPE)) {
                return new BoolType();
            }
            if (match(TokenType.LPAREN)) {
                Type type = parseType();
                consume(TokenType.RPAREN, "Expected ')' after the type");
                return type;
            }
            throw new RuntimeException("Parsing error: Invalid or missing type. Got: " + current().type() + " ('" + current().value() + "')");
        }
    }
}
