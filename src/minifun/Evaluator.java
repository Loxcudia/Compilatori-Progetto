package minifun;
import minifun.MiniFun.*;
public class Evaluator {

    public Value eval(Expr expr, Environment rho)
    {
        return switch(expr)
        {
            //Basic values and variables:
            case IntExpr(int n) -> new Value.IntValue(n);
            case BoolExpr(boolean b) -> new Value.BoolValue(b);
            case VarExpr(String name) -> rho.read(name);

            //Binary and logic operations:
            case BinOpExpr(String op, Expr left, Expr right) -> {
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
            case IfExpr(Expr condition, Expr thenBranch, Expr elseBranch) -> {
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
            case LetExpr(String varName, Expr valueExpr, Expr body) -> {
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
            case FunExpr(String parameter, Type parameterType, Expr body) -> {
                yield new Value.ClosureValue(parameter, body, rho);
            }

            //Function application
            case AppExpr(Expr function, Expr argument) ->
            {
                Value closure = eval(function, rho);
                Value argumentValue = eval(argument, rho);
                //Default closure
                if(closure instanceof Value.ClosureValue(String parameter, Expr body, Environment closureEnvironment))
                {
                    Environment executionEnvironment = closureEnvironment.extend(parameter, argumentValue);
                    yield eval(body, executionEnvironment);
                }
                //Recursive closure
                else if (closure instanceof Value.RecClosureValue(String funName, String parameter, Expr body, Environment closureEnvironment))
                {
                    /*
                        1. I extend the environment by adding the function itself (so there can be recursive calls)
                        2. I extend the environment by adding the input parameter
                        3. Evaluate the body with this new environment
                     */
                    Environment executionEnvironment = closureEnvironment.extend(funName, closure);
                    executionEnvironment = executionEnvironment.extend(parameter, argumentValue);
                    yield eval(body, executionEnvironment);
                }
                else
                    throw new RuntimeException("Error! You are trying to applicate something that is not a function!");
            }

            //Function definition with name
            case LetFunExpr(String functionName, Type functionType, Expr functionBody, Expr body) -> {
                if (functionBody instanceof FunExpr(String parameter, Type paramType, Expr fBody)) {
                    Value closure = new Value.RecClosureValue(functionName, parameter, fBody, rho);
                    Environment newRho = rho.extend(functionName, closure);
                    yield eval(body, newRho);
                } else {
                    throw new RuntimeException("Type Error: letfun requires a function definition (fun => ...)");
                }
            }
            default -> throw new RuntimeException("AST Node not supported by the Evaluator: " + expr);
        };
    }
}
