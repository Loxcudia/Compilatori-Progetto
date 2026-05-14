package miniimp;
import java.util.*;
import miniimp.MiniImp.*;
import java.io.PrintWriter;
import java.io.IOException;

public class LLVMGenerator {
    private int registerCounter = 1; // Counter for fresh names
    //I use StringBuilder class because it gives mutably strings and efficient functions to manipulate them
    private StringBuilder body = new StringBuilder();
    private StringBuilder allocas = new StringBuilder();
    private Set<String> allocatedVars = new HashSet<>();

    // Generates a name with unique register
    private String nextReg() {return "%" + (registerCounter++);}

    // Ensures that a variable has its alloca in the entry block
    private void allocateVar(String varName)
    {
        if (!allocatedVars.contains(varName))
        {
            allocatedVars.add(varName);
            allocas.append("    %").append(varName).append("_ptr = alloca i64\n");
        }
    }

    public void generateToFile(CFG cfg, String inputVar, String outputVar, String filename) throws IOException
    {
        String ir = generateIR(cfg, inputVar, outputVar);
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println(ir);
        }
        System.out.println("LLVM IR file generated: " + filename);
    }

    /*
        For every variable of the miniimp program, I generate an instruction "alloca i64" in the start block "entry". This creates a pointer in the stack memory.
        Every read of a variable becomes a load in a fresh register and every write becomes a store in the allocated pointer.
        There are no phi-nodes. I will give the code to the command "opt -passes=mem2reg", which transform these memory operations in SSA register and phi-nodes.
     */
    private String generateIR(CFG cfg, String inputVar, String outputVar) {
        StringBuilder llvm = new StringBuilder();

        // Module and function header
        llvm.append("; ========================================\n");
        llvm.append("; LLVM header\n");
        llvm.append("; ========================================\n\n");

        //The function get an i64 input and return an i64 output
        llvm.append("define i64 @func(i64 %in) {\n");
        llvm.append("entry:\n");

        // Verifying if input and output are allocated
        allocateVar(inputVar);
        allocateVar(outputVar);

        //I save the input parameter in the input variable
        allocas.append("    store i64 %in, ptr %").append(inputVar).append("_ptr\n");

        //I start the cfg visit. Every block of the cfg is translated in a LLVM basic block with a label (for example: block_0, block_1, ...)
        List<CFG.Node> allNodes = getAllNodes(cfg);
        for (CFG.Node block : allNodes)
        {
            body.append("\nblock_").append(block.id).append(":\n");

            String lastConditionRegister = null; // I save the result of the last if/while

            //I translate the block commands
            for (Cmd cmd : block.commands)
            {
                if (cmd instanceof AssignCmd assign) {
                    allocateVar(assign.varName());
                    String valReg = evalExpr(assign.expr());
                    body.append("    store i64 ").append(valReg).append(", ptr %").append(assign.varName()).append("_ptr\n");
                }
                else if (cmd instanceof IfCmd ifCmd) {
                    lastConditionRegister = evalBool(ifCmd.cond());
                }
                else if (cmd instanceof WhileCmd whileCmd) {
                    lastConditionRegister = evalBool(whileCmd.cond());
                }
            }

            /*
                    At the end of every block, I generate a jump instruction:
                        - br label %target for unconditional jumps;
                        - br i1 %cond, label %true, label %false for if/while;
                        - ret i64 %val for terminate the function and return the output.
             */
            if (block.outgoingEdges.isEmpty())
            {
                //Exit node: load the output variable and then do the return
                String outReg = nextReg();
                body.append("    ").append(outReg).append(" = load i64, ptr %").append(outputVar).append("_ptr\n");
                body.append("    ret i64 ").append(outReg).append("\n");
            }
            else if (block.outgoingEdges.size() == 1) {
                //Unconditional jump
                CFG.Node target = block.outgoingEdges.get(0).target();
                body.append("    br label %block_").append(target.id).append("\n");
            }
            else if (block.outgoingEdges.size() == 2) {
                //Conditional jump (branch)
                CFG.Node trueTarget = null;
                CFG.Node falseTarget = null;

                for (CFG.Edge edge : block.outgoingEdges) {
                    if (edge.type() == CFG.EdgeType.TRUE) trueTarget = edge.target();
                    if (edge.type() == CFG.EdgeType.FALSE) falseTarget = edge.target();
                }

                body.append("    br i1 ").append(lastConditionRegister)
                        .append(", label %block_").append(trueTarget.id)
                        .append(", label %block_").append(falseTarget.id).append("\n");
            }
        }

        llvm.append(allocas.toString());
        llvm.append("    br label %block_").append(cfg.entryNode.id).append("\n");
        llvm.append(body.toString());
        llvm.append("}\n");

        return llvm.toString();
    }


    //Functions that translates expressions and boolean expressions
    private String evalExpr(Expr expr) {
        return switch (expr) {
            case IntExpr i -> String.valueOf(i.value());
            case VarExpr v -> {
                String reg = nextReg();
                body.append("    ").append(reg).append(" = load i64, ptr %").append(v.name()).append("_ptr\n");
                yield reg;
            }
            case BinOpExpr b -> {
                String leftVal = evalExpr(b.left());
                String rightVal = evalExpr(b.right());
                String reg = nextReg();
                String llvmOp = switch (b.op()) {
                    case "+" -> "add";
                    case "-" -> "sub";
                    case "*" -> "mul";
                    default -> throw new RuntimeException("Unsupported operator");
                };
                body.append("    ").append(reg).append(" = ").append(llvmOp).append(" i64 ").append(leftVal).append(", ").append(rightVal).append("\n");
                yield reg;
            }
            default -> throw new RuntimeException("Unsupported expression" + expr);
        };
    }

    private String evalBool(BoolExpr expr) {
        return switch (expr) {
            case TrueBool t -> "true";
            case FalseBool f -> "false";
            case RelOpBool r -> {
                String leftVal = evalExpr(r.left());
                String rightVal = evalExpr(r.right());
                String reg = nextReg();
                body.append("    ").append(reg).append(" = icmp slt i64 ").append(leftVal).append(", ").append(rightVal).append("\n");
                yield reg;
            }
            case NotBool n -> {
                String innerVal = evalBool(n.b());
                String reg = nextReg();
                body.append("    ").append(reg).append(" = xor i1 ").append(innerVal).append(", true\n");
                yield reg;
            }
            case AndBool a -> {
                String leftVal = evalBool(a.left());
                String rightVal = evalBool(a.right());
                String reg = nextReg();
                body.append("    ").append(reg).append(" = and i1 ").append(leftVal).append(", ").append(rightVal).append("\n");
                yield reg;
            }
            default -> throw new RuntimeException("Unsupported boolean expression" + expr);
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
            CFG.Node current = queue.poll();
            for (CFG.Edge edge : current.outgoingEdges) {
                if (!visited.contains(edge.target().id)) {
                    visited.add(edge.target().id);
                    queue.add(edge.target());
                    result.add(edge.target());
                }
            }
        }
        return result;
    }
}