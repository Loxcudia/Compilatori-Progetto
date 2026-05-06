package minifun;
import minifun.MiniFun.*;
import java.util.HashMap;
import java.util.Map;

public class TypeEnvironment {
    private final Map<String, Type> gamma; //It's final for the same reeason of the Environment

    public TypeEnvironment()
    {
        this.gamma = new HashMap<>();
    }

    private TypeEnvironment(Map<String, Type> oldGamma) //private constructor for cloning of the type environment. Same behaviour of the Environment's private constructor
    {
        this.gamma = new HashMap<>(oldGamma);
    }

    //Same behaviour of the Environment's extend
    public TypeEnvironment extend(String varName, Type type)
    {
        TypeEnvironment newTypeEnvironment = new TypeEnvironment(this.gamma);
        newTypeEnvironment.gamma.put(varName, type);
        return newTypeEnvironment;
    }

    //Search for the type of a variable (var rule of the deduction system)
    public Type lookup(String varName)
    {
        if(gamma.containsKey(varName))
        {
            return gamma.get(varName);
        }
        else
            throw new TypeException("Variable not defined: '" + varName + "' is not present in the context.");
    }
}
