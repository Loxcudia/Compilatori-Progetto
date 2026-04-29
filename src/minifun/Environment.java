package minifun;
import java.util.HashMap;
import java.util.Map;
public class Environment {
    private final Map<String, Value> rho; //environment must be final because when we add a variable. we don't modify the current environment, but we create a total new environment

    //Constructor to create an empty environment
    public Environment()
    {
        this.rho = new HashMap<>();
    }

    //Private Constructor to "clone" the environment
    private Environment(Map<String, Value> baseMap)
    {
        this.rho = new HashMap<>(baseMap);
    }

    //This is rho(x)
    public Value read(String name)
    {
        Value val = rho.get(name);
        if(val == null)
                throw new RuntimeException("Error: the variable '" + name + "is not defined in the current environment");
        return val;
    }

    //This is rho[x -> y]. Return a new environment, not modify the old.
    public Environment extend(String name, Value val)
    {
        //Clone the current map:
        Environment newEnv = new Environment(this.rho);
        //Add or update the new variable in the clone:
        newEnv.rho.put(name, val);
        return newEnv;
    }

    @Override
    public String toString()
    {
        return rho.toString();
    }
}
