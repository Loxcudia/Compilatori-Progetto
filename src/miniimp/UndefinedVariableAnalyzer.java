package miniimp;
import java.util.*;
import miniimp.MiniImp.*;

public class UndefinedVariableAnalyzer {

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
