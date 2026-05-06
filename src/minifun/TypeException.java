package minifun;

public class TypeException extends RuntimeException {
    public TypeException(String message)
    {
        super("\n[TYPE ERROR] " + message);
    }
}
