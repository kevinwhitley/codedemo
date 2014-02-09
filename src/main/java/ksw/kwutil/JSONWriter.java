package ksw.kwutil;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Class for writing a JSON string
 */
public class JSONWriter
{
    private StringBuffer _out;
    private boolean _atStart;

    // testing routine
    public static void main(String[] args)
    {
    	TestWrite tw = new TestWrite("hoho", "zebra", 73);
    	
    	try {
	        JSONWriter js = new JSONWriter();
	        js.startObject();
	        js.addItem("rank", "jack");
	        js.addItem("tw", tw);
	        js.addItem("suit", "hearts");
	        js.endObject();
	        System.out.println(js.toString());
    	}
    	catch (JSONWriteException exc) {
    		System.out.println("Exception: " + exc);
    	}
        
    }
    
    public static class TestWrite implements JSONWriteableByGet
    {
    	private String aa;
    	private String bb;
    	private int cc;
    	
    	private static String[] fieldNames = {"aa", "cc", "trouble"};

    	public TestWrite(String ax, String bx, int cx)
    	{
    		aa = ax;
    		bb = bx;
    		cc = cx;
    	}
    	
    	public String getAa()
    	{
    		return aa;
    	}
    	
    	public String getBb()
    	{
    		return bb;
    	}
    	
    	public int getCc()
    	{
    		return cc;
    	}
    	
    	public String[] getTrouble()
    	{
    		return fieldNames;
    	}
    	
		@Override
		public Object getWriteable()
		{
			return this;
		}

		@Override
		public String[] getFieldNames()
		{
			return fieldNames;
		}
    	
    }

    public JSONWriter()
    {
        _out = new StringBuffer();
        _atStart = true;
    }

    // start object at top level or within an array
    public void startObject()
    {
    	startObject(null);
    }
    
    // start object within an object (needs tag)
    public void addObjectToObject(String tag)
    {
    	startObject(tag);
    }

    private void startObject(String tag)
    {
        if (!_atStart) {
            _out.append(',');
        }
        if (tag != null) {
	        addStringData(tag);
	        _out.append(": {");
        }
        else {
            _out.append('{');
        }
        _atStart = true;
    }

    public void endObject()
    {
        _out.append('}');
        _atStart = false;
    }

    // start array at top level or within an array
    public void startArray()
    {
    	startArray(null);
    }

    // start array within an object (needs tag)
    public void addArrayToObject(String tag)
    {
    	startArray(tag);
    }
    
    private void startArray(String tag)
    {
        if (!_atStart) {
            _out.append(',');
        }
        if (tag != null) {
	        addStringData(tag);
	        _out.append(": [");
        }
        else {
            _out.append('[');
        }
        _atStart = true;
    }

    public void endArray()
    {
        _out.append(']');
        _atStart = false;
    }

    public void addItem(String tag, String value)
    {
    	beginItem(tag);
        addStringData(value);
        _atStart = false;
    }

    public void addItem(String tag, Integer value)
    {
    	beginItem(tag);
    	if (value == null) {
            _out.append("null");
    	}
    	else {
	        _out.append(Integer.toString(value));
    	}
        _atStart = false;
    }

    public void addItem(String tag, Double value)
    {
        beginItem(tag);
        if (value == null) {
            _out.append("null");
        }
        else {
            _out.append(Double.toString(value));
        }
        _atStart = false;
    }

    public void addItem(String tag, Short value)
    {
    	beginItem(tag);
    	if (value == null) {
            _out.append("null");
    	}
    	else {
	        _out.append(Short.toString(value));
    	}
        _atStart = false;
    }

    public void addItem(String tag, Long value)
    {
    	beginItem(tag);
    	if (value == null) {
            _out.append("null");
    	}
    	else {
	        _out.append(Long.toString(value));
    	}
        _atStart = false;
    }

    public void addItem(String tag, Boolean value)
    {
    	beginItem(tag);
    	if (value == null) {
            _out.append("null");
    	}
    	else {
	        _out.append(Boolean.toString(value));
    	}
        _atStart = false;
    }
    
    // add another JSON tree to this one
    // NOTE! the passed-in tree must be complete
    public void addItem(String tag, JSONWriter jwriter)
    {
        beginItem(tag);
        _out.append(jwriter.toString());
        _atStart = false;
    }
    
    public void addItem(String tag, JSONWriteable writeable) throws JSONWriteException
    {
    	startObject(tag);
    	writeable.toJSON(this);
    	endObject();
    }

    // add a JSONWriteable object
    // used to serialize an object that exposes its attributes with getters
    // tag should be null if we're adding within array or at top level
    // tag should be non-null if we're adding to an object
    public void addItem(String tag, JSONWriteableByGet writeable) throws JSONWriteException
    {
    	startObject(tag);
    	String[] fieldNames = writeable.getFieldNames();
    	Object writeObject = writeable.getWriteable();
    	Class wClass = writeObject.getClass();
    	if (fieldNames == null) {
    		throw new JSONWriteException("no field names when writing class " + wClass.getSimpleName());
    	}
    	for (String field : fieldNames) {
    		// try to get internal get ("_get") first, otherwise get the usual "get"
    		String methodName = StringUtil.upperFirst("_get", field);
    		Method method = null;
    		try {
	    		method = wClass.getMethod(methodName, (Class[])null);
    		}
        	catch(NoSuchMethodException exc) {
        		methodName = StringUtil.upperFirst("get", field);
        		try {
    	    		method = wClass.getMethod(methodName, (Class[])null);
        		}
        		catch (NoSuchMethodException exc2) {
	        		throw new JSONWriteException("unknown method " + field + " when writing class " + wClass.getSimpleName());
        		}
        	}
        	
        	Object value = null;
        	Exception exc = null;
			try {
				Class fieldType = method.getReturnType();
				value = method.invoke(writeObject);
				addWriteableValue(wClass.getSimpleName(), field, fieldType, value);
			} catch (IllegalArgumentException e) {
				exc = e;
			} catch (IllegalAccessException e) {
				exc = e;
			} catch (InvocationTargetException e) {
				exc = e;
			}
			
			if (exc != null) {
        		throw new JSONWriteException("cannot call get method for " + field + " when writing class " + wClass.getSimpleName() + " " + exc);
			}
    	}
    	endObject();
    }
    
    // add multiple fields of some object
    // generally called from within the implementation of JSONWriteable.toJSON
    // tag should be null if we're adding within array or at top level
    // tag should be non-null if we're adding to an object
    public void addObjectFields(String className, Object... values) throws JSONWriteException
    {
    	if (values == null || values.length < 2) {
    		throw new JSONWriteException("no field names when writing class " + className);
    	}
    	for (int indx=0; indx<values.length; indx+=2) {
    		String field = (String) values[indx];
    		Object value = values[indx+1];
    		Class fieldType = (value != null) ? value.getClass() : null;
			addWriteableValue(className, field, fieldType, value);
    	}
    }
    
    private void addWriteableValue(String className, String field, Class fieldType, Object value) throws JSONWriteException
    {
    	try {
	    	if (value == null) {
	    		// all types can use String - we get the null marker
	    		addItem(field, (String)null);
	    	}
	    	else if (fieldType == java.lang.Boolean.TYPE || fieldType == java.lang.Boolean.class) {
	    		addItem(field, (Boolean)value);
	    	}
	    	else if (fieldType == java.lang.Integer.TYPE || fieldType == java.lang.Integer.class) {
	    		addItem(field, (Integer)value);
	    	}
	    	else if (fieldType == java.lang.Long.TYPE || fieldType == java.lang.Long.class) {
	    		addItem(field, (Long)value);
	    	}
	    	else if (fieldType == java.lang.Short.TYPE || fieldType == java.lang.Short.class) {
	    		addItem(field, (Short)value);
	    	}
	    	else if (fieldType == java.lang.String.class) {
	    		addItem(field, (String)value);
	    	}
	    	else if (fieldType.isArray()) {
	    		startArray(field);
	    		Class arrayType = fieldType.getComponentType();
	    		for (int ai=0; ai<Array.getLength(value); ai++) {
	    			Object aobj = Array.get(value, ai);
	    			addWriteableValue(className, null, arrayType, aobj);
	    		}
	    		endArray();
	    	}
	    	else if (value instanceof List) {
	    		// lists get written out as arrays
	    		List lValue = (List)value;
	    		startArray(field);
	    		if (lValue.size() > 0) {
	    			for (Object val : lValue) {
	    				// nulls are treated as strings to get the "null"
	    				addWriteableValue(className, null, ((val != null) ? val.getClass() : String.class), val);
	    			}
	    		}
	    		endArray();
	    	}
	    	else if (JSONWriteable.class.isAssignableFrom(fieldType)) {
	    		JSONWriteable jw = (JSONWriteable)value;
	    		startObject(field);
	    		jw.toJSON(this);
	    		endObject();
	    	}
	    	else if (JSONWriteableByGet.class.isAssignableFrom(fieldType)) {
	    		addItem(field, (JSONWriteableByGet)value);
	    	}
	    	else {
	    		String fieldName = (field != null) ? field : "[arrayvalue]";
	    		throw new JSONWriteException("unsupported type " + fieldType.getSimpleName() + " for " + fieldName + " when writing class " + className);
	    	}
    	}
    	catch(ClassCastException cce) {
    		throw new JSONWriteException("type mismatch for " + field + " when writing class " + className + " " + cce);
    	}
    }

    private void beginItem(String tag)
    {
        if (!_atStart) {
            _out.append(',');
        }
        if (tag != null) {
            addStringData(tag);
            _out.append(": ");
        }
    }
    
    public void reset()
    {
        _out = new StringBuffer();
        _atStart = false;
    }

    public String toString()
    {
        return _out.toString();
    }

    private void addStringData(String value)
    {
        if (value != null) {
            _out.append('"');
            int slen = value.length();
            for (int ii=0; ii<slen; ii++) {
                char cc = value.charAt(ii);
                int ic = (int)cc;
                if (cc == '"') {
                    _out.append('\\');
                    _out.append(cc);
                }
                else if (cc == '\\') {
                    _out.append("\\\\");
                }
                else if (cc == '/') {
                    _out.append("\\/");
                }
                else if (cc == '\b') {
                    _out.append("\\b");
                }
                else if (cc == '\f') {
                    _out.append("\\f");
                }
                else if (cc == '\n') {
                    _out.append("\\n");
                }
                else if (cc == '\r') {
                    _out.append("\\r");
                }
                else if (cc == '\t') {
                    _out.append("\\t");
                }
                else if (ic > 0x7f) {
                    _out.append("\\u");
                    _out.append(String.format("%04x", ic));
                }
                else {
                    _out.append(cc);
                }
            }
            _out.append('"');
        }
        else {
            _out.append("null");
        }
    }
    
	public static class JSONWriteException extends Exception
	{
		public JSONWriteException(String msg)
		{
			super(msg);
		}
	}

	/*
	 * A JSONWriteable is a class that can be automatically written to JSON.
	 * The normal implementation would be for a class to return "this" from getWriteable, and a list
	 * of field names that can be accessed by standard getters and setters.
	 * However, by returning a different object from getWriteable, the JSONWriteable may act as a facade,
	 * modifying what can be written to the JSON.
	 */
    public interface JSONWriteableByGet
    {
    	public Object getWriteable();
    	public String[] getFieldNames();
    }
    
    public interface JSONWriteable
    {
    	public void toJSON(JSONWriter writer) throws JSONWriteException;
    }

}
