package miniimp;
import java.util.*;
import miniimp.MiniImp.*;


public class Optimizer {

    /*
        Optimization Pipeline: executes the optimization passes in a fixed-point loop.
        It runs Constant Propagation (which includes Folding), recalculates Live Variables,
        and then runs Dead Store Elimination. It repeats until the CFG size stabilizes.
    */
    public void optimizingPipeline(CFG cfg, String inputVar, String outputVar) {
        System.out.println("Starting Optimization Pipeline...");

        DefinedVariablesAnalysis defVars = new DefinedVariablesAnalysis();
        defVars.runAnalysis(cfg, inputVar);

        // 1. Initial sanity check
        checkUndefinedVariables(cfg);

        boolean changed = true;
        int pass = 1;

        while (changed) {
            int initialCommandCount = countTotalCommands(cfg);

            // 2. Constant Propagation & Folding
            constantsPropagation(cfg);

            // Recalculate Live Variables because propagation replaced many variable reads with integer constants!
            LiveVariablesAnalysis lva = new LiveVariablesAnalysis();
            lva.runAnalysis(cfg, outputVar);

            // 4. Dead Store Elimination
            deadStoreEliminate(cfg);

            int finalCommandCount = countTotalCommands(cfg);

            // If the number of commands didn't decrease, we reached the fixed point
            if (initialCommandCount == finalCommandCount) {
                changed = false;
            } else {
                System.out.println("Pass " + pass + ": CFG optimized from " + initialCommandCount + " to " + finalCommandCount + " commands.");
                pass++;
            }
        }
        System.out.println("Optimization fixed-point reached in " + pass + " passes.");
    }

    // Auxiliary method to count commands in the CFG
    private int countTotalCommands(CFG cfg) {
        int count = 0;
        for (CFG.Node node : getAllNodes(cfg)) {
            count += node.commands.size();
        }
        return count;
    }

    /*
        Iterate over all cfg blocks. For each block I create a "safe" set equals to the values of the input set.
        Then, I Iterate over all commands of the block and for all commands of type assign, if and while I take all the read variables using my auxiliary function extractVariables.
        For each of those variables, if there aren't in my "safe set", I print an error.
        In the end, if the command is an assignment, I add the variable in the safe set because from this moment on, in that block, the variable is defined.
    */
    public void checkUndefinedVariables(CFG cfg)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        for(CFG.Node block : allNodes)
        {
            Set<String> inSet = new HashSet<>(block.getInputSets(DefinedVariablesAnalysis.DEFINED_VARS));
            for(Cmd cmd : block.commands)
            {
                List<String> variables = new ArrayList<>();
                if(cmd instanceof AssignCmd assign)
                    variables = extractVariables(assign.expr());
                else if(cmd instanceof IfCmd ifCmd)
                    variables = extractVariables(ifCmd.cond());
                else if(cmd instanceof WhileCmd whileCmd)
                    variables = extractVariables(whileCmd.cond());
                for(String variable : variables)
                {
                    if(!inSet.contains(variable))
                        System.err.println("Warning: Possible use of an undefined variable " + variable);
                }
                if(cmd instanceof AssignCmd assign)
                    inSet.add(assign.varName());
            }
        }
    }


    /*
        The idea is:
        Getting all the blocks of the graphs.
        For each block, I save the variables that are surely alive at the end of the block (its output set). I use a new HashSet because I don't want to modify the original data.
        I allocate a list where I put the commands who deserve to survive. I choose Linked List because in this way I will have an efficient inserting at first.
        After this, I cycle on the block commands starting from the last command (I do a reverse for loop). I do this because to know if an assignment is useless, I must first look at what happens after it.
        Assume that in the reverse for loop I meet an assignment x := y + 1 and the variable x is not contained in the current block output set, then nobody cares about x: the command is a dead store.
        Otherwise, x will be useful, so I need to save the command. So:
        1. I push the command on the top of the linked list. I do this because I'm reverse cycling the all commands and pushing on the top guarantee that the list has the original order.
        2. I remove x from the live variables
        3. To compute x, the command needs to read y, so y becomes alive ( java instruction currentLiveVars.addAll(extractVariables(assign.expr())); )

        If i meet an if or a while, I can't eliminate anything because they control the flow of the program. I always save them on the top of the linked list, because the program always needs to read them to determine where to go.
        In the end of the algorithm, I simply substitute the current block list of commands with the optimized new list of commands, where all the useless assignments are deleted.
     */
    public void deadStoreEliminate(CFG cfg)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        for(CFG.Node block : allNodes)
        {
            Set<String> currentLiveVars = new HashSet<>(block.getOutputSets(LiveVariablesAnalysis.LIVE_VARS));
            List<MiniImp.Cmd> optimizedCommands = new LinkedList<>();
            for(int i = block.commands.size() - 1; i >= 0; i--)
            {
                Cmd cmd = block.commands.get(i);
                if(cmd instanceof AssignCmd assign)
                {
                    if(currentLiveVars.contains(assign.varName()))
                    {
                        optimizedCommands.addFirst(assign);
                        currentLiveVars.remove(assign.varName());
                        currentLiveVars.addAll(extractVariables(assign.expr()));
                    }
                }
                else if(cmd instanceof IfCmd ifCmd)
                {
                    optimizedCommands.addFirst(ifCmd);
                    currentLiveVars.addAll(extractVariables(ifCmd.cond()));
                }
                else if (cmd instanceof WhileCmd whileCmd)
                {
                    optimizedCommands.addFirst(whileCmd);
                    currentLiveVars.addAll(extractVariables(whileCmd.cond()));
                }
            }
            block.commands = optimizedCommands;
        }
    }

    public void constantsFolding(CFG cfg)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        for(CFG.Node block : allNodes)
        {
            List<Cmd> foldedCommands = new ArrayList<>(); //new list for optmized commands
            for(Cmd cmd : block.commands)
            {
                if(cmd instanceof AssignCmd assign)
                {
                    Expr foldedExpr = foldExpr(assign.expr()); //I fold the right expr of the assignment
                    foldedCommands.add(new AssignCmd(assign.varName(), foldedExpr)); //I recreate the record with the original variable and the new folded right expression
                }
                else if(cmd instanceof WhileCmd whileCmd)
                {
                    BoolExpr foldedCond = foldBool(whileCmd.cond()); //I fold the while condition
                    foldedCommands.add(new WhileCmd(foldedCond, whileCmd.body())); //I recreate the record with the folded while condition and the original while body
                }
                else if(cmd instanceof IfCmd ifCmd)
                {
                    BoolExpr foldedCond = foldBool(ifCmd.cond()); //I fold the if condition
                    foldedCommands.add(new IfCmd(foldedCond, ifCmd.thenBranch(), ifCmd.elseBranch())); //I recreate the record with the folded if condition and the originals then and else branches
                }
                else
                    foldedCommands.add(cmd); //For any other command like skip, I have nothing to fold
            }
            block.commands = foldedCommands; //I substitute the original block commands list with the optimized one
        }
    }


    /*
        I do folding using pattern matching of java as follows:
        Base cases: integer expression and var expression -> simply return themselves
        Recursive case: binary operation (for example left op right):
        Recursive call on left
        Recursive call on right
        After this, I check if both left and right sides are integer expressions. If yes, I take their values and do the actual operation
        Otherwise, at least one of the two sides is not a number, so I can't do the actual operation and
        I return a new BinOpExpr containing the partially folded branches to preserve the Abstract Syntax Tree structure
     */
    private Expr foldExpr(Expr expr) {
        if (expr == null) return null;

        return switch (expr) {
            case IntExpr intExpr -> intExpr;

            case VarExpr varExpr -> varExpr;

            case BinOpExpr binOpExpr -> {
                Expr foldedLeft = foldExpr(binOpExpr.left());
                Expr foldedRight = foldExpr(binOpExpr.right());

                if (foldedLeft instanceof IntExpr leftInt && foldedRight instanceof IntExpr rightInt) {
                    int leftValue = leftInt.value();
                    int rightValue = rightInt.value();

                    yield switch (binOpExpr.op()) {
                        case "+" -> new IntExpr(leftValue + rightValue);
                        case "-" -> new IntExpr(leftValue - rightValue);
                        case "*" -> new IntExpr(leftValue * rightValue);
                        default -> throw new RuntimeException("Invalid operator: " + binOpExpr.op());
                    };
                } else {
                    yield new BinOpExpr(binOpExpr.op(), foldedLeft, foldedRight);
                }
            }
            default -> expr;
        };
    }
    //similar logic but for boolean expression
    public BoolExpr foldBool(BoolExpr cond) {
        if (cond == null) return null;

        return switch (cond) {

            case TrueBool t -> t;
            case FalseBool f -> f;

            case NotBool notOp -> {
                BoolExpr foldedB = foldBool(notOp.b());

                if (foldedB instanceof TrueBool)
                    yield new FalseBool();
                if (foldedB instanceof FalseBool)
                    yield new TrueBool();
                yield new NotBool(foldedB);
            }

            case AndBool andOp -> {
                BoolExpr foldedLeft = foldBool(andOp.left());

                if (foldedLeft instanceof FalseBool) {
                    yield new FalseBool();
                }
                BoolExpr foldedRight = foldBool(andOp.right());
                if (foldedRight instanceof FalseBool) {
                    yield new FalseBool();
                }
                if (foldedLeft instanceof TrueBool && foldedRight instanceof TrueBool) {
                    yield new TrueBool();
                }
                yield new AndBool(foldedLeft, foldedRight);
            }

            case RelOpBool relOp -> {
                Expr foldedLeft = foldExpr(relOp.left());
                Expr foldedRight = foldExpr(relOp.right());

                if (foldedLeft instanceof IntExpr leftInt && foldedRight instanceof IntExpr rightInt) {
                    boolean result = leftInt.value() < rightInt.value();
                    yield result ? new TrueBool() : new FalseBool();
                }
                yield new RelOpBool(foldedLeft, foldedRight);
            }
            default -> cond;
        };
    }


    public void constantsPropagation(CFG cfg)
    {
        List<CFG.Node> allNodes = getAllNodes(cfg);
        for(CFG.Node block : allNodes)
        {
            List<Cmd> optimizedCommands = new ArrayList<>();
            Map<String, IntExpr> constants = new HashMap<>();
            for(Cmd cmd : block.commands)
            {
                if(cmd instanceof AssignCmd assign)
                {
                    //Substitute the variables with numbers
                    Expr propExpr = propagateExpr(assign.expr(), constants);
                    //Folding: compute the expression (for example: 10 + 5 -> 15)
                    Expr foldedExpr = foldExpr(propExpr);
                    //Map update: if the result is a number, I save it
                    if(foldedExpr instanceof IntExpr intResult)
                        constants.put(assign.varName(), intResult);
                    else //If the assignment is not constant, then I remove the variable, because its value it's no longer predictable
                        constants.remove(assign.varName());
                    optimizedCommands.add(new AssignCmd(assign.varName(), foldedExpr));
                }
                else if (cmd instanceof IfCmd ifCmd)
                {
                    //Propagate and fold the if boolean condition
                    BoolExpr propCondition = propagateBool(ifCmd.cond(), constants);
                    BoolExpr foldedCondition = foldBool(propCondition);
                    optimizedCommands.add(new IfCmd(foldedCondition, ifCmd.thenBranch(), ifCmd.elseBranch()));
                }
                else if(cmd instanceof WhileCmd whileCmd)
                {
                    //Propagate and fold the while boolean condition
                    BoolExpr propCondition = propagateBool(whileCmd.cond(), constants);
                    BoolExpr foldedCondition = foldBool(propCondition);
                    optimizedCommands.add(new WhileCmd(foldedCondition, whileCmd.body()));
                }
                else
                    optimizedCommands.add(cmd);
                block.commands = optimizedCommands;
            }
        }
    }





    //This function takes an expression and substitutes occurrences of known variables with their actual numerical values
    private Expr propagateExpr(Expr expr, Map<String, IntExpr> constants)
    {
        if(expr == null) return null;

        return switch(expr)
        {
            case IntExpr intExpr -> intExpr;

            case VarExpr varExpr -> {
                if(constants.containsKey(varExpr.name()))
                    yield constants.get(varExpr.name());
                else
                    yield varExpr;
            }

            case BinOpExpr binOpExpr -> {
                Expr left = propagateExpr(binOpExpr.left(), constants);
                Expr right = propagateExpr(binOpExpr.right(), constants);
                yield new BinOpExpr(binOpExpr.op(), left, right);
            }

            default -> expr;
        };
    }

    //Similar logic but for boolean expression
    private BoolExpr propagateBool(BoolExpr cond, Map<String, IntExpr> constants) {
        if (cond == null) return null;

        return switch (cond) {
            case TrueBool t -> t;
            case FalseBool f -> f;

            case NotBool notOp -> {
                BoolExpr propB = propagateBool(notOp.b(), constants);
                yield new NotBool(propB);
            }

            case AndBool andOp -> {
                BoolExpr propLeft = propagateBool(andOp.left(), constants);
                BoolExpr propRight = propagateBool(andOp.right(), constants);
                yield new AndBool(propLeft, propRight);
            }

            case RelOpBool relOp -> {
                Expr propLeft = propagateExpr(relOp.left(), constants);
                Expr propRight = propagateExpr(relOp.right(), constants);
                yield new RelOpBool(propLeft, propRight);
            }

            default -> cond;
        };
    }


    //Get all CFG nodes simply doing a BFS visit
    private List<CFG.Node> getAllNodes(CFG cfg) {
        List<CFG.Node> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Queue<CFG.Node> queue = new LinkedList<>();

        queue.add(cfg.entryNode);
        visited.add(cfg.entryNode.id);
        result.add(cfg.entryNode);
        while (!queue.isEmpty()) {
            CFG.Node corrente = queue.poll();
            for (CFG.Edge arco : corrente.outgoingEdges) {
                if (!visited.contains(arco.target().id)) {
                    visited.add(arco.target().id);
                    queue.add(arco.target());
                    result.add(arco.target());
                }
            }
        }
        return result;
    }



    //Navigate the AST of an expression and extract all the variables names that are read
    private List<String> extractVariables(Node node)
    {
        List<String> readVariables = new ArrayList<>();

        if (node == null) return readVariables;

        switch (node) {
            case VarExpr varExpr -> {
                readVariables.add(varExpr.name());
            }
            case IntExpr intExpr -> {}
            case TrueBool t -> {}
            case FalseBool f -> {}


            case BinOpExpr binOp -> {
                // Search for the variables on the left and right of one of the operators: {+, -, *}
                readVariables.addAll(extractVariables(binOp.left()));
                readVariables.addAll(extractVariables(binOp.right()));
            }
            case RelOpBool relOp -> {
                // Search for the variables on the left and right of <
                readVariables.addAll(extractVariables(relOp.left()));
                readVariables.addAll(extractVariables(relOp.right()));
            }
            case AndBool andBool -> {
                // Search for the variables on the left and right of a logical and
                readVariables.addAll(extractVariables(andBool.left()));
                readVariables.addAll(extractVariables(andBool.right()));
            }
            case NotBool notBool -> {
                readVariables.addAll(extractVariables(notBool.b()));
            }

            default -> throw new RuntimeException("Not supported node in extractVariables function: " + node.getClass());
        }

        return readVariables;
    }
}
