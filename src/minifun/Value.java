package minifun;

public interface Value {
    record IntValue(int value) implements Value {}
    record BoolValue(boolean value) implements Value {}
    record ClosureValue(String param, MiniFun.Expr body, Environment env) implements Value {}
}
