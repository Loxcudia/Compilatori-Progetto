package minifun;

public class Evaluator {

    public Value eval(MiniFun.Expr expr, Environment rho)
    {
        return switch(expr)
        {
            //Basic values and variables:
            case MiniFun.IntExpr(int n) -> new Value.IntValue(n);
            case MiniFun.BoolExpr(boolean b) -> new Value.BoolValue(b);
            case MiniFun.VarExpr(String name) -> rho.read(name);

            //Binary and logic operations:
            case MiniFun.BinOpExpr(String op, MiniFun.Expr left, MiniFun.Expr right) -> {
                Value leftValue = eval(left, rho);
                Value rightValue = eval(right, rho);
                //I control if the left and the right exprs are actually integers before doing math
                if(leftValue instanceof Value.IntValue(int l) && rightValue instanceof Value.IntValue(int r))
                {
                    yield switch(op)
                    {
                        case "+" -> new Value.IntValue(l + r);
                        case "-" -> new Value.IntValue(l - r);
                        case "*" -> new Value.IntValue(l * r);
                        case "==" -> new Value.BoolValue(l == r);
                        case "<" -> new Value.BoolValue(l < r);
                        default -> throw new RuntimeException("Unknown Operator " + op);
                    };
                }
                else
                    throw new RuntimeException("Type Error: The operator " + op + "requires two integers");
            }

            //If-then else
            case MiniFun.IfExpr(MiniFun.Expr condition, MiniFun.Expr thenBranch, MiniFun.Expr elseBranch) -> {
                Value conditionValue = eval(condition, rho);
                if(conditionValue instanceof Value.BoolValue(boolean b))
                {
                    if(b)
                        yield eval(thenBranch, rho);
                    else
                        yield eval(elseBranch, rho);
                }
                else
                    throw new RuntimeException("Type Error: if condition must be boolean");
            }

            //Variables declaration with let
            case MiniFun.LetExpr(String varName, MiniFun.Expr valueExpr, MiniFun.Expr body) -> {
                /*
                        1) I calculate the value of the expr
                        2) I create a total new Environment with the variable added
                        3) I evaluate the body using the new Environment
                 */
                Value val = eval(valueExpr, rho);
                Environment newRho = rho.extend(varName, val);
                yield eval(body, newRho);
            }

            //Function definition
            case MiniFun.FunExpr(String parameter, MiniFun.Expr body) -> {
                yield new Value.ClosureValue(parameter, body, rho);
            }

            //Function application
            case MiniFun.AppExpr(MiniFun.Expr function, MiniFun.Expr argument) ->
            {
                /*
                    1) I evaluate the function (expecting a closure)
                    2) I evaluate the argument to pass it at the function
                    3) I extract the pieces of the closure
                 */
                Value closure = eval(function, rho);
                Value argumentValue = eval(argument, rho);
                if(closure instanceof Value.ClosureValue(String parameter, MiniFun.Expr body, Environment closureEnvironment))
                {
                    Environment executionEnvironment = closureEnvironment.extend(parameter, argumentValue); // I extend the closure environment, not the current environment
                    yield eval(body, executionEnvironment);
                }
                else
                    throw new RuntimeException("Error! You are trying to applicate something that is not a function!");
            }

            //Function definition with name
            case MiniFun.LetFunExpr(String functionName, String parameter, MiniFun.Expr functionBody, MiniFun.Expr body) -> { // equals to let fName = fun param => fBody in body
                Value closure = new Value.ClosureValue(parameter, functionBody, rho);
                Environment newRho = rho.extend(functionName, closure);
                yield eval(body, newRho);
            }
            default -> throw new RuntimeException("AST Node not supported by the Evaluator: " + expr);
        };
    }
}
