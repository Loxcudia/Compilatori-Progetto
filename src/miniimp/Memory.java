package miniimp;

import java.util.HashMap;
import java.util.Map;

public class Memory {
    private final Map<String, Integer> sigma;

    public Memory()
    {
        this.sigma = new HashMap<>();
    }

    public int read(String varname)
    {
        Integer value = sigma.get(varname);
        if(value == null)
            throw new RuntimeException("DEADLOCK! The variable '" + varname + "' is not defined.");
        else
            return value;
    }

    public void write(String varname, int value)
    {
        sigma.put(varname, value);
    }

    @Override
    public String toString()
    {
        return sigma.toString();
    }
}
