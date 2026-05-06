package miniimp;
import miniimp.MiniImp.*;
public class Interpreter {

    //Take an expr and the memory, return a number
    public int evalExpr(Expr e, Memory sigma)
    {
        return switch (e)
        {
            //Num rule: return the value n
            case IntExpr(int val) -> val;

            //Var rule: read the memory
            case VarExpr(String name) -> sigma.read(name);

            //Op rule: evaluate at left, evaluate at right, and apply the operator
            case BinOpExpr(String op, Expr left, Expr right) -> {
                int valLeft = evalExpr(left, sigma);
                int valRight = evalExpr(right, sigma);
                yield switch (op) {
                    case "+" -> valLeft + valRight;
                    case "-" -> valLeft - valRight;
                    case "*" -> valLeft * valRight;
                    default -> throw new RuntimeException("Unknown operator: " + op);
                };
            }
            default -> throw new RuntimeException("Unvalid expression");
        };
    }

    //Take a condition and the memory, return true or false
    public boolean evalBool(BoolExpr b, Memory sigma)
    {
        return switch(b)
        {
            //Base cases:
            case TrueBool() -> true;
            case FalseBool() -> false;

            //Recursive cases:
            case NotBool(BoolExpr innerB) -> !evalBool(innerB, sigma);
            case AndBool(BoolExpr leftExpr, BoolExpr rightExpr) -> {
                boolean boolLeft = evalBool(leftExpr, sigma);
                boolean boolRight = evalBool(rightExpr, sigma);
                yield boolLeft && boolRight;
            }
            case RelOpBool(Expr leftExpr, Expr rightExpr) -> {
                int exprLeft = evalExpr(leftExpr, sigma);
                int exprRight = evalExpr(rightExpr, sigma);
                yield exprLeft < exprRight;
            }
            default -> throw new RuntimeException("Espressione non valida");
        };
    }

    //Take a command and the memory. Since in Java the objects passed as parameter (our Map) are modified by reference, this methods can return nothing. Its goal is to mute the state of sigma.
    public void evalCmd(Cmd c, Memory sigma)
    {
        switch(c)
        {
            //Simply skip and does nothing
            case SkipCmd() -> {}

            //Evaluate the expression and save to memory
            case AssignCmd(String varName, Expr expr) -> {
                int val = evalExpr(expr, sigma);
                sigma.write(varName, val);
            }

            //First execute the left command, then the right
            case SeqCmd(Cmd left, Cmd right) -> {
                evalCmd(left, sigma);
                evalCmd(right, sigma);
            }


            case WhileCmd(BoolExpr condition, Cmd body) -> {
                while(evalBool(condition, sigma))
                    evalCmd(body, sigma);
            }

            case IfCmd(BoolExpr condition, Cmd thenBranch, Cmd elseBranch) -> {
                if(evalBool(condition, sigma))
                    evalCmd(thenBranch, sigma);
                else
                    evalCmd(elseBranch, sigma);
            }

            default -> throw new RuntimeException("Unvalid or unknown command");
        };

    }

    //Take the main AST and the input of the user, return the final output.
    public int evalProg(Prog p, int inputVal)
    {
        Memory sigma = new Memory();
        sigma.write(p.inputVar(), inputVal);
        evalCmd(p.body(), sigma);
        return sigma.read(p.outputVar());
    }
}
