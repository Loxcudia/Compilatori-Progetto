package minifun;
import minifun.MiniFun.*;
public interface Value {
    record IntValue(int value) implements Value {}
    record BoolValue(boolean value) implements Value {}
    record ClosureValue(String param, Expr body, Environment env) implements Value {}
    record RecClosureValue(String functionName, String parameter, Expr body, Environment closureEnvironment) implements Value {}
}
