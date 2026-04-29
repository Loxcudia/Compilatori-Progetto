package miniimp;

public class Interpreter {

    //Take an expr and the memory, return a number
    public int evalExpr(MiniImp.Expr e, Memory sigma)
    {
        return switch (e)
        {
            //Num rule: return the value n
            case MiniImp.IntExpr(int val) -> val;

            //Var rule: read the memory
            case MiniImp.VarExpr(String name) -> sigma.read(name);

            //Op rule: evaluate at left, evaluate at right, and apply the operator
            case MiniImp.BinOpExpr(String op, MiniImp.Expr left, MiniImp.Expr right) -> {
                int valLeft = evalExpr(left, sigma);
                int valRight = evalExpr(right, sigma);
                yield switch (op) {
                    case "+" -> valLeft + valRight;
                    case "-" -> valLeft - valRight;
                    case "*" -> valLeft * valRight;
                    default -> throw new RuntimeException("Operatore sconosciuto: " + op);
                };
            }
            default -> throw new RuntimeException("Espressione non valida");
        };
    }

    //Take a condition and the memory, return true or false
    public boolean evalBool(MiniImp.BoolExpr b, Memory sigma)
    {
        return switch(b)
        {
            //Base cases:
            case MiniImp.TrueBool() -> true;
            case MiniImp.FalseBool() -> false;

            //Recursive cases:
            case MiniImp.NotBool(MiniImp.BoolExpr innerB) -> !evalBool(innerB, sigma);
            case MiniImp.AndBool(MiniImp.BoolExpr leftExpr, MiniImp.BoolExpr rightExpr) -> {
                boolean boolLeft = evalBool(leftExpr, sigma);
                boolean boolRight = evalBool(rightExpr, sigma);
                yield boolLeft && boolRight;
            }
            case MiniImp.RelOpBool(MiniImp.Expr leftExpr, MiniImp.Expr rightExpr) -> {
                int exprLeft = evalExpr(leftExpr, sigma);
                int exprRight = evalExpr(rightExpr, sigma);
                yield exprLeft < exprRight;
            }
            default -> throw new RuntimeException("Espressione non valida");
        };
    }

    //Take a command and the memory. Since in Java the objects passed as parameter (our Map) are modified by reference, this methods can return nothing. Its goal is to mute the state of sigma.
    public void evalCmd(MiniImp.Cmd c, Memory sigma)
    {
        switch(c)
        {
            //Simply skip and does nothing
            case MiniImp.SkipCmd() -> {}

            //Evaluate the expression and save to memory
            case MiniImp.AssignCmd(String varName, MiniImp.Expr expr) -> {
                int val = evalExpr(expr, sigma);
                sigma.write(varName, val);
            }

            //First execute the left command, then the right
            case MiniImp.SeqCmd(MiniImp.Cmd left, MiniImp.Cmd right) -> {
                evalCmd(left, sigma);
                evalCmd(right, sigma);
            }


            case MiniImp.WhileCmd(MiniImp.BoolExpr condition, MiniImp.Cmd body) -> {
                while(evalBool(condition, sigma))
                    evalCmd(body, sigma);
            }

            case MiniImp.IfCmd(MiniImp.BoolExpr condition, MiniImp.Cmd thenBranch, MiniImp.Cmd elseBranch) -> {
                if(evalBool(condition, sigma))
                    evalCmd(thenBranch, sigma);
                else
                    evalCmd(elseBranch, sigma);
            }

            default -> throw new RuntimeException("Comando non valido o sconosciuto");
        };

    }

    //Take the main AST and the input of the user, return the final output.
    public int evalProg(MiniImp.Prog p, int inputVal)
    {
        Memory sigma = new Memory();
        sigma.write(p.inputVar(), inputVal);
        evalCmd(p.body(), sigma);
        return sigma.read(p.outputVar());
    }
}
