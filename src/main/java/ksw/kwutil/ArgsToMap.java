package ksw.kwutil;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 */
public class ArgsToMap
{
    public static final String StringArg = "S";
    public static final String ListArg = "L"; // multiple string values for an argument
    public static final String DoubleListArg = "DL"; // multiple 2-string values
    public static final String FlagArgFalse = "F"; // argument can be set or not (set with -arg, cleared with +arg)
    public static final String FlagArgTrue = "T";  // like FlagArgFalse, but default value of true
    public static final String IntegerArg = "I";

    private Map<String, String> _meta;
    private Map<String, Object> _result;

    // the constructor arguments are pairs of tokens & types
    public ArgsToMap(String... argParams)
    {
        _meta = new HashMap<String, String>();
        _result = new HashMap<String, Object>();
        for (int ii=0; ii<argParams.length; ii++) {
            String token = argParams[ii];
            String type = argParams[ii+1];
            ii++;
            _meta.put(token, type);
            if (FlagArgFalse.equals(type)) {
                _result.put(token, Boolean.FALSE);
            }
            else if (FlagArgTrue.equals(type)) {
                _result.put(token, Boolean.TRUE);
            }
        }
    }

    // turn the program's arguments into a map
    // a null return indicates an error of some sort
    public Map<String, Object> processArgs(String[] args)
    {
        for (int ii=0; ii<args.length; ii++) {
            String token = args[ii];
            char firstC = token.charAt(0);
            if (firstC != '-' && firstC != '+') {
                return null;
            }
            // look up the token
            token = token.substring(1);
            String type = _meta.get(token);
            if (type == null) {
                return null;
            }

            // most types expect a following token - check
            Object argument = null;
            if (StringArg.equals(type) || ListArg.equals(type) || IntegerArg.equals(type)) {
                if (args.length < ii+2) {
                    return null;
                }
                if (firstC != '-') {
                    // these types all expect a - in front of the token
                    return null;
                }
                argument = args[ii+1];
                ii++;
            }
            if (DoubleListArg.equals(type)) {
                if (args.length < ii+3) {
                    return null;
                }
                if (firstC != '-') {
                    return null;
                }
                String[] a2 = new String[2];
                a2[0] = args[ii+1];
                a2[1] = args[ii+2];
                argument = a2;
                ii += 2;
            }

            Object old;
            if (StringArg.equals(type)) {
                old = _result.put(token, argument);
                if (old != null) {
                    return null;
                }
            }
            else if (ListArg.equals(type)) {
                List<String> oldList = (List<String>)_result.get(token);
                if (oldList == null) {
                    oldList = new ArrayList<String>();
                    _result.put(token, oldList);
                }
                oldList.add((String)argument);
            }
            else if (DoubleListArg.equals(type)) {
                List<String[]> oldList = (List<String[]>)_result.get(token);
                if (oldList == null) {
                    oldList = new ArrayList<String[]>();
                    _result.put(token, oldList);
                }
                oldList.add((String[])argument);
            }
            else if (IntegerArg.equals(type)) {
                Integer val = Integer.parseInt((String)argument);
                old = _result.put(token, val);
                if (old != null) {
                    return null;
                }
            }
            else if (FlagArgFalse.equals(type) || FlagArgTrue.equals(type)) {
                // note that we always have a default value for flag args
                Boolean val = (firstC == '-');
                _result.put(token, val);
            }
        }

        return _result;
    }

    // add another value to the processed arguments programmatically
    // returns true if the add was successful
    public boolean add(String token, Object value)
    {
        // look up the token
        String type = _meta.get(token);
        if (type == null) {
            return false;
        }

        Object oldVal = _result.get(token);

        if (DoubleListArg.equals(type)) {
            if (!(value instanceof String[])) {
                return false;
            }
            List<String[]> valueList = (List<String[]>)oldVal;
            if (valueList == null) {
                valueList = new ArrayList<String[]>(1);
                _result.put(token, valueList);
            }
            valueList.add((String[])value);
        }
        else if (StringArg.equals(type) || oldVal != null) {
            if (!(value instanceof String)) {
                return false;
            }
            _result.put(token, value);
        }
        else if (ListArg.equals(type)) {
            if (!(value instanceof String)) {
                return false;
            }
            List<String> valueList = (List<String>)oldVal;
            if (valueList == null) {
                valueList = new ArrayList<String>(1);
                _result.put(token, valueList);
            }
            valueList.add((String)value);
        }
        else if (IntegerArg.equals(type) || oldVal != null) {
            if (!(value instanceof Integer)) {
                return false;
            }
            _result.put(token, value);
        }
        else if (FlagArgFalse.equals(type) || FlagArgTrue.equals(type)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            _result.put(token, value);
        }

        return true;
    }
}
