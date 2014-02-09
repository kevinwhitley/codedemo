package ksw.kwutil;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// straightforward parser of JSON input strings
// see http://www.json.org/ for a description of the language

public class JSONReader
{
    private JSONLex _lexer;
    
    private Map<Class, Map<String, Method>> _methodMap;
    
    // stacks of the current type being constructed
    private List<Class> _typeStack;
    private List<Map<String, Object>> _valuesStack;

	public JSONReader()
	{
		_lexer = new JSONLex();
		_methodMap = new HashMap<Class, Map<String, Method>>(5);
	}
	
    // parse a JSON string
    // we'll hand back a value which may be of varying type - a null, or Integer, or Boolean or String
    // or List or Map (the latter two being nested arbitrarily)
    public Object parse(CharSequence input) throws JSONParseException
    {
        if (input == null) {
            throw new JSONParseException("null input", null);
        }
        Object result = null;
        _lexer.setInput(input.toString());
        // consume the input
        Object topValue = parseValue();
        // make sure we're at the end
        if (_lexer.getNextToken() == JSONLex.JSONToken.JsEnd) {
            result = topValue;
        }
        
        return result;
    }

	public JSONReadableBySetters parseUsingSetters(String input, Class readableClass) throws JSONParseException
	{
		Object mapAndLists = parse(input);
		JSONReadableBySetters result = buildObjectUsingSetters(readableClass, (Map<String, Object>)mapAndLists);
		
		return result;
	}
	
	// parse a json tree into a single object (which may involve sub-objects getting parsed)
	// the whole tree must be simple types or JSONReadable classes
	public JSONReadable parseInputAsObject(String input, Class readableClass) throws JSONParseException
	{
		Map<String, Object> mapAndLists = (Map<String, Object>)parse(input);
		_typeStack = new ArrayList<Class>(10);
		_valuesStack = new ArrayList<Map<String, Object>>(10);
		
		JSONReadable result = buildObject(readableClass, mapAndLists);
		
		return result;
	}
	
	private JSONReadable buildObject(Class objectClass, Map<String, Object> parsedValues) throws JSONParseException
	{
		if (parsedValues == null) {
			return null;
		}
		
		_typeStack.add(objectClass);
		_valuesStack.add(parsedValues);
		
		JSONReadable result = null;
		Exception exc = null;
		try {
			Constructor cons = objectClass.getConstructor(JSONReader.class);
			result = (JSONReadable) cons.newInstance(this);
		}
		catch (Exception e) {
			exc = e;
		}
		if (exc != null) {
			throw new JSONParseException("buildObject cannot instantiate " + objectClass.getSimpleName() + " ", exc);
		}
		
		_typeStack.remove(_typeStack.size()-1);
		_valuesStack.remove(_valuesStack.size()-1);
		
		return result;
	}
	
	public Object getValue(String fieldName) throws JSONParseException
	{
		// we're using the type currently at the top of the stack
		Class objectType = _typeStack.get(_typeStack.size()-1);

		// see if there is a field with the expected name
		Class fieldType;
		Class subType = null;
		try {
			Field field = getField(objectType, fieldName);
			if (field == null) {
				field = getField(objectType, "_"+fieldName);
			}
		    fieldType = field.getType();
			// calculate a possible generic subtype (for Lists)
			Type ft = field.getGenericType();
			if (ft instanceof ParameterizedType) {
				subType = (Class) ((ParameterizedType)ft).getActualTypeArguments()[0];
			}
		}
		catch(Exception exc) {
			fieldType = null;
		}
		if (fieldType == null) {
			// try finding a setter
			Map<String, Method> methods = getSetterMethods(objectType);
			Method setterMethod = methods.get(fieldName);
			if (setterMethod != null) {
				fieldType = setterMethod.getParameterTypes()[0];
				// calculate a possible generic subtype (for Lists)
				Type ft = setterMethod.getGenericParameterTypes()[0];
				if (ft instanceof ParameterizedType) {
					subType = (Class) ((ParameterizedType)ft).getActualTypeArguments()[0];
				}
			}
		}
		
		if (fieldType == null) {
    		throw new JSONParseException("no type discoverable for " + fieldName + " when reading class " + objectType.getSimpleName(), null);
		}
		
		return getValue(fieldName, fieldType, subType);
	}
	
	private Field getField(Class objectClass, String fieldName)
	{
		Field result;
		try {
		    result = objectClass.getDeclaredField(fieldName);
		}
		catch(Exception exc) {
			result = null;
		}
		return result;
	}
	
	public Object getValue(String fieldName, Class fieldType, Class fieldSubType) throws JSONParseException
	{
		Class objectClass = _typeStack.get(_typeStack.size()-1);
		Map<String, Object> valueMap = _valuesStack.get(_valuesStack.size()-1);
		Object parsedValue = valueMap.get(fieldName);
		Object value = calculateReadableValue(objectClass.getSimpleName(), fieldName, fieldType, fieldSubType, parsedValue);
        return value;
	}

	public JSONReadableBySetters buildObjectUsingSetters(Class oClass, Map<String, Object> values) throws JSONParseException
	{
		Map<String, Method> methods = getSetterMethods(oClass);
		
		// construct an instance of the class, using the JSONReader constructor
		JSONReadableBySetters result = null;
		Exception exc = null;
		try {
			Constructor cons = oClass.getConstructor();
			result = (JSONReadableBySetters) cons.newInstance();
		}
		catch (InstantiationException e) {
			exc = e;
		}
		catch (IllegalAccessException e) {
			exc = e;
		} catch (SecurityException e) {
			exc = e;
		} catch (NoSuchMethodException e) {
			exc = e;
		} catch (IllegalArgumentException e) {
			exc = e;
		} catch (InvocationTargetException e) {
			exc = e;
		}
		if (exc != null) {
			throw new JSONParseException("Cannot instantiate " + oClass.getSimpleName() + " ", exc);
		}
		
		for (String fieldName : values.keySet()) {
			Object parsedValue = values.get(fieldName);
			Method setterMethod = methods.get(fieldName);
			if (setterMethod == null) {
	    		throw new JSONParseException("no setter for " + fieldName + " when reading class " + oClass.getSimpleName(), null);
			}
			setReadableValue(result, fieldName, setterMethod, parsedValue);
		}
		
		// call the object and let it get a chance to clean up
		result.postJSONRead();
		
		return result;
	}
	
	// fetch (possibly initializing) a map of setter methods for a class
	private Map<String, Method> getSetterMethods(Class<JSONReadableBySetters> rClass)
	{
		Map<String, Method> methods = _methodMap.get(rClass);
		if (methods == null) {
			// find all the setters
			methods = new HashMap<String, Method>(30);
			_methodMap.put(rClass, methods);
			Method[] ms = rClass.getMethods();
			if (ms != null) {
				for (Method meth : ms) {
					String methodName = meth.getName();
					String fieldName = null;
					if (meth.getParameterTypes().length != 1) {
						continue;
					}
					if (methodName.startsWith("_set")) {
						fieldName = methodName.substring(4);
					}
					else if (methodName.startsWith("set")) {
						fieldName = methodName.substring(3);
					}
					if (fieldName == null) {
						// not a setter
						continue;
					}
					fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
					Method old = methods.put(fieldName, meth);
					if (old != null && methodName.startsWith("set")) {
						// whoops, we overrode the _set with the set
						// but we want to prefer the _set, so...
						methods.put(fieldName, old);
					}
				}
			}
		}
		
		return methods;
	}
	
	private Object calculateReadableValue(String readClassName, String fieldName, Class fieldType, Class fieldSubType, Object parsedValue) throws JSONParseException
	{
		// default, works for simple types
		// note that value may be null
		Object setValue = parsedValue;
		
		Class valueType = (parsedValue != null) ? parsedValue.getClass() : null;
		
		if (JSONReadableBySetters.class.isAssignableFrom(fieldType)) {
			// we're setting an object
			// we need to instantiate it with the map in value
			if (parsedValue != null && !Map.class.isAssignableFrom(valueType)) {
				throw new JSONParseException("did not have map value for " + fieldName + " when reading class " + readClassName, null);
			}
			JSONReadableBySetters subObject = null;
			if (parsedValue != null) {
				subObject = buildObjectUsingSetters(fieldType, (Map<String,Object>)parsedValue);
            }
			setValue = subObject;
		}
		else if (JSONReadable.class.isAssignableFrom(fieldType)) {
			// setting an object that does callbacks in constructor
			if (parsedValue != null && !Map.class.isAssignableFrom(valueType)) {
				throw new JSONParseException("did not have map values for " + fieldName + " when reading class " + readClassName, null);
			}
			setValue = buildObject(fieldType, (Map<String, Object>)parsedValue);
		}
		else if (fieldType.isArray() || List.class.isAssignableFrom(fieldType)) {
			// we're setting either a list or array
			// instantiate items from the list in value and put them into a list or array
			// and then use that as the setter value
			if (parsedValue != null && !List.class.isAssignableFrom(valueType)) {
				throw new JSONParseException("did not have list value for " + fieldName + " when reading class " + readClassName, null);
			}
			Class subType = null;
			
			if (fieldType.isArray()) {
				subType = fieldType.getComponentType();
				setValue = buildArray(subType, (List)parsedValue);
			}
			else {
				// we were passed the field subtype
				setValue = buildList(fieldSubType, (List)parsedValue);
			}
		}
		
		return setValue;
	}
	
	private void setReadableValue(JSONReadableBySetters readObject, String fieldName, Method setterMethod, Object parsedValue) throws JSONParseException
	{
		Class objectType = readObject.getClass();
		
		// the type that the setter expects
		Class fieldType = setterMethod.getParameterTypes()[0];
		
		Class fieldSubType = null;
		Type tt = setterMethod.getGenericParameterTypes()[0];
		if (tt instanceof ParameterizedType) {
			fieldSubType = (Class) ((ParameterizedType)tt).getActualTypeArguments()[0];
			if (fieldSubType.isInterface()) {
				throw new JSONParseException("cannot instantiate interface elements - field " + fieldName + " when reading class " + objectType.getSimpleName(), null);
			}
		}
		Object setValue = calculateReadableValue(objectType.getSimpleName(), fieldName, fieldType, fieldSubType, parsedValue);
		
		/*
		Class valueType = (parsedValue != null) ? parsedValue.getClass() : null;
		
		// default, works for simple types
		// note that value may be null
		Object setValue = parsedValue;
		
		if (JSONReadableBySetters.class.isAssignableFrom(fieldType)) {
			// we're setting an object
			// we need to instantiate it with the map in value
			if (parsedValue != null && !Map.class.isAssignableFrom(valueType)) {
				throw new JSONParseException("did not have map value for " + fieldName + " when reading class " + objectType.getSimpleName());
			}
			JSONReadableBySetters subObject = null;
			if (parsedValue != null) {
				subObject = buildObjectUsingSetters(fieldType, (Map<String,Object>)parsedValue);
            }
			setValue = subObject;
		}
		else if (fieldType.isArray() || List.class.isAssignableFrom(fieldType)) {
			// we're setting either a list or array
			// instantiate items from the list in value and put them into a list or array
			// and then use that as the setter value
			if (parsedValue != null && !List.class.isAssignableFrom(valueType)) {
				throw new JSONParseException("did not have list value for " + fieldName + " when reading class " + objectType.getSimpleName());
			}
			Class subType = null;
			
			if (fieldType.isArray()) {
				subType = fieldType.getComponentType();
				setValue = buildArray(subType, (List)parsedValue);
			}
			else {
				// figure out the parameterized type, if any, of the list
				Type tt = setterMethod.getGenericParameterTypes()[0];
				if (tt instanceof ParameterizedType) {
					subType = (Class) ((ParameterizedType)tt).getActualTypeArguments()[0];
					if (subType.isInterface()) {
						throw new JSONParseException("cannot instantiate interface elements - field " + fieldName + " when reading class " + objectType.getSimpleName());
					}
				}
				setValue = buildList(subType, (List)parsedValue);
			}
		}
		*/
		
		Exception exc = null;
		try {
			/*
			if (fieldType == Integer.TYPE) {
				Integer iValue = ((Integer)setValue);
				setterMethod.invoke(readObject, iValue);
			}
			else {
				setterMethod.invoke(readObject, fieldType.cast(setValue));
			}
			*/
			setterMethod.invoke(readObject, setValue);
		}
		catch (ClassCastException e) {
			exc = e;
		}
		catch (IllegalArgumentException e) {
			exc = e;
		}
		catch (IllegalAccessException e) {
			exc = e;
		}
		catch (InvocationTargetException e) {
			exc = e;
		}
		if (exc != null) {
			throw new JSONParseException("failure executing setter for " + fieldName + " when reading class " + objectType.getSimpleName() + ": ", exc);
		}
	}
	
	private Object[] buildArray(Class elementType, List values) throws JSONParseException
	{
		if (values == null) {
			return null;
		}
		Object[] result = (Object[])Array.newInstance(elementType, values.size());
        int index = 0;
		for (Object value : values) {
			if (value == null) {
				// do nothing - we'll put the null in the array
			}
			else if (JSONReadableBySetters.class.isAssignableFrom(elementType)) {
				// build an object
				result[index++] = buildObjectUsingSetters(elementType, (Map<String,Object>)value);
			}
			else if (JSONReadable.class.isAssignableFrom(elementType)) {
				// build an object
				result[index++] = buildObject(elementType, (Map<String,Object>)value);
			}
			else if (elementType.isArray()) {
				// array of arrays
				Class arrayType = elementType.getComponentType();
				result[index++] = buildArray(arrayType, (List)value);
			}
			else if (List.class.isAssignableFrom(elementType)) {
				// array of lists...
				throw new JSONParseException("array of lists not supported", null);
			}
			else {
				result[index++] = value;
			}
		}
		
		return result;
	}
	
	private List buildList(Class elementType, List values) throws JSONParseException
	{
		if (values == null) {
			return null;
		}
		List result = new ArrayList(values.size());
		for (Object value : values) {
			if (JSONReadableBySetters.class.isAssignableFrom(elementType)) {
				// build an object
				result.add(buildObjectUsingSetters(elementType, (Map<String,Object>)value));
			}
			else if (JSONReadable.class.isAssignableFrom(elementType)) {
				// build an object
				result.add(buildObject(elementType, (Map<String,Object>)value));
			}
			else if (elementType.isArray()) {
				// array of arrays
				Class arrayType = elementType.getComponentType();
				result.add(buildArray(arrayType, (List)value));
			}
			else if (List.class.isAssignableFrom(elementType)) {
				// list of lists...
				throw new JSONParseException("list of lists not supported", null);
			}
			else {
				result.add(value);
			}
		}
		
		return result;
	}

	private Object parseValue() throws JSONParseException
	{
		JSONLex.JSONToken token = _lexer.getNextToken();
		if (token == JSONLex.JSONToken.JsArrayStart) {
			return parseArray();
		}
		else if (token == JSONLex.JSONToken.JsObjectStart) {
			return parseObject();
		}
		else if (token == JSONLex.JSONToken.JsString || token == JSONLex.JSONToken.JsInt || token == JSONLex.JSONToken.JsDouble ||
				 token == JSONLex.JSONToken.JsBoolean || token == JSONLex.JSONToken.JsNull) {
			return _lexer.getValue();
		}
		else {
			throw new JSONParseException("Unexpected token parsing element", null);
		}
	}
	
	private List parseArray() throws JSONParseException
	{
		// we're here because there was an array start
		// we should see elements separated with separators and ending with an array end
		List result = new ArrayList();
		boolean isFirst = true;
        JSONLex.JSONToken token;
		while (true) {
		    if (isFirst) {
		        // check for an empty array
		        token = _lexer.getNextToken();
		        if (token == JSONLex.JSONToken.JsArrayEnd) {
		            // we're done
		            return result;
		        }
		        else {
		            // that was the start of a value - reuse it
		            _lexer.push();
		        }
		        isFirst = false;
		    }
			Object value = parseValue();
			result.add(value);
			token = _lexer.getNextToken();
			if (token == JSONLex.JSONToken.JsArrayEnd) {
				return result;
			}
			if (token != JSONLex.JSONToken.JsSeparator) {
				throw new JSONParseException("Array elements not separated by ,", null);
			}
		}
	}
	
	private Map parseObject() throws JSONParseException
	{
		// we're here because there was an object start
		// we should see identifer element separator... until the end
		Map result = new HashMap();
		boolean isFirst = true;
		while (true) {
			// get the identifier
			JSONLex.JSONToken token = _lexer.getNextToken();
			if (token == JSONLex.JSONToken.JsObjectEnd) {
				return result;
			}
			
			if (!isFirst) {
				// we should see a separator
				if (token != JSONLex.JSONToken.JsSeparator) {
				    String before = _lexer.getPreviousContent();
					throw new JSONParseException("Object values not separated properly: "+before, null);
				}
				token = _lexer.getNextToken();
			}
			else {
				isFirst = false;
			}

			// we should then see an identifier (string followed by identifier char) then the value
			if (token != JSONLex.JSONToken.JsString) {
				throw new JSONParseException("Did not see identifying string when parsing object, saw " + token + " at " + _lexer.getIndex(), null);
			}
			String key = (String)_lexer.getValue();
			token = _lexer.getNextToken();
			if (token != JSONLex.JSONToken.JsIdentifierChar) {
				throw new JSONParseException("Did not get expected identifier char when parsing object", null);
			}
			
			// get the value
			Object value = parseValue();
			result.put(key, value);
		}
	}
	
	public static void main(String[] args)
	{
        test("[6, 7, true, \"my cat\\nisblack\", [\"embedded\", false, \"abc\\u002dxyz\"], 222]");
        //test("{\"forframe\": [\"/thumbnail/photos03/nk102603/DSCN3533.JPG\", \"/thumbnail/photos03/nk102603/DSCN3534.JPG\", \"/thumbnail/photos03/nk102603/DSCN3535.JPG\", \"/thumbnail/photos03/nk102603/DSCN3537.JPG\", \"/thumbnail/photos03/nk102603/DSCN3539.JPG\", \"/thumbnail/photos03/nk102603/DSCN3549.JPG\"], \"hoai_thu\": [\"/thumbnail/photos03/nk102603/DSCN3534.JPG\", \"/thumbnail/photos03/nk102603/DSCN3536.JPG\", \"/thumbnail/photos03/nk102603/DSCN3550.JPG\"], \"kfamily\": [\"/thumbnail/photos03/nk102603/DSCN3533.JPG\", \"/thumbnail/photos03/nk102603/DSCN3534.JPG\", \"/thumbnail/photos03/nk102603/DSCN3535.JPG\", \"/thumbnail/photos03/nk102603/DSCN3537.JPG\", \"/thumbnail/photos03/nk102603/DSCN3539.JPG\", \"/thumbnail/photos03/nk102603/DSCN3543.JPG\", \"/thumbnail/photos03/nk102603/DSCN3549.JPG\", \"/thumbnail/photos03/nk102603/DSCN3550.JPG\"], \"peter\": [\"/thumbnail/photos03/nk102603/DSCN3558.JPG\", \"/thumbnail/photos03/nk102603/DSCN3560.JPG\"]}");
        //test("{}");
	    //test("{\"kevin\": {\"editType\": null,\"tags\": [\"hoai-thu\",\"kevin\",\"david\",\"peter\",\"htfamily\",\"kfamily\",\"&testtest\",\"abstract\",\"fountain\"]}}");
	    //testObject("{\"aa\": \"fish\", \"bb\": 33, \"cc\": true, \"names\": [\"joe\", \"mary\"]}");
	    
		//parameterTest();
	    // fieldTest(TestRead.class);
	}
	
	public void setBlah(List<JSONReadableBySetters> value)
	{
	}
	
	public static void fieldTest(Class testC)
	{
		Field[] fields = testC.getDeclaredFields();
		for (Field fd : fields) {
			System.out.println(fd.getName() + " has type " + fd.getType().getCanonicalName());
			Type ft = fd.getGenericType();
			if (ft instanceof ParameterizedType) {
				Class subType = (Class) ((ParameterizedType)ft).getActualTypeArguments()[0];
				System.out.println("   subtype " + subType.getCanonicalName());
			}
		}
	}
	
	public static class TestRead implements JSONReadable
	{
		private String aa;
		private int bb;
		private Boolean cc;
		private List<String> names;
		
		public TestRead(JSONReader reader) throws JSONParseException
		{
			aa = (String)reader.getValue("aa");
			bb = (Integer)reader.getValue("bb");
			cc = (Boolean)reader.getValue("cc");
			names = (List<String>)reader.getValue("names");
		}
		
		public void setAa(String val)
		{
			aa = val;
		}
		
		public void setCc(Boolean val)
		{
			cc = val;
		}
		
		public void setNames(List<String> val)
		{
			names = val;
		}
		
		public void print()
		{
			System.out.println("aa: " + aa + ", bb: " + bb + ", cc: " + cc);
			for (String nm : names) {
				System.out.println("  name: " + nm);
			}
		}

		public void postJSONRead()
		{
		}
	}
	
	public static void test(String testString)
	{
		System.out.println("Testing JSONReader with input:");
		System.out.println(testString);
		System.out.println();
		JSONReader reader = new JSONReader();
		try {
			Object value = reader.parse(testString);
			printValue(0, value);
		}
		catch(JSONParseException exc) {
			System.out.println("Parse exception: " + exc.getMessage());
		}
	}
	
	public static void testObject(String testString)
	{
		System.out.println("Testing JSONReader with input:");
		System.out.println(testString);
		System.out.println();
		JSONReader reader = new JSONReader();
		try {
			Object value = reader.parseInputAsObject(testString, TestRead.class);
			((TestRead)value).print();
		}
		catch(JSONParseException exc) {
			System.out.println("Parse exception: " + exc.getMessage());
		}
		
	}
	
	public static void printValue(int depth, Object value)
	{
		printIndent(depth);
		if (value == null) {
			System.out.println("null");
		}
		else if (value instanceof List) {
			printArray(depth, (List)value);
		}
		else if (value instanceof Map) {
			printObject(depth, (Map<String,Object>)value);
		}
		else {
			System.out.println(value);
		}
	}
	
	private static void printArray(int depth, List value)
	{
		printIndent(depth);
		System.out.println("Array:");
		for (Object val : value) {
			printValue(depth+1, val);
		}
	}
	
	private static void printObject(int depth, Map<String, Object> value)
	{
        printIndent(depth);
        System.out.println("Object:");
        for (String key: value.keySet()) {
        	printIndent(depth+1);
        	System.out.print(key + ":");
        	printValue(depth+1, value.get(key));
        }
	}
	
	private static void printIndent(int depth)
	{
		for (int ii=0; ii<depth; ii++) {
			System.out.print("    ");
		}
	}
	
	public static class JSONParseException extends Exception
	{
		public JSONParseException(String msg, Throwable cause)
		{
			super(msg, cause);
		}
		
		public String toString()
		{
			String msg = super.toString();
			if (getCause() != null) {
				msg += "\n  caused by " + getCause().toString();
			}
			return msg;
		}
	}

	private static class JSONLex
	{
		private String _json;
		private int _index;
		private Object _value;
		
		// to support pushing a token back on the stream
		private int _oldIndex;

		enum JSONToken
		{
			JsString, // "xxx"
			JsInt, // 3
			JsDouble, // 26.7
			JsBoolean, // true
			JsNull, // null
			JsArrayStart, // [
			JsArrayEnd, // ]
			JsObjectStart, // {
			JsObjectEnd, // }
			JsIdentifierChar, // :
			JsSeparator, // ,
			JsUnknown, JsEnd
			// end of input
		}

		public JSONLex()
		{
			_json = null;
			_index = 0;
			_oldIndex = 0;
		}
		
		public void setInput(String json)
		{
			_json = json;
			_index = 0;
		}

		public int getIndex()
		{
			return _index;
		}

		public JSONToken getNextToken()
		{
			_value = null;
			if (_json == null) {
				return JSONToken.JsEnd;
			}
			
			// remember the start index so that we can back up to it
			_oldIndex = _index;
			
			// there are many single-character tokens
			// and we skip white space
			char cc = 0;
			while (_index < _json.length()) {
				cc = _json.charAt(_index++);
				if (!Character.isWhitespace(cc)) {
					break;
				}
			}
			if (cc == 0 || Character.isWhitespace(cc)) {
				// we hit the end of the input
				return JSONToken.JsEnd;
			}

			// decide based on that character
			switch (cc) {
			case '[':
				return JSONToken.JsArrayStart;
			case ']':
				return JSONToken.JsArrayEnd;
			case '{':
				return JSONToken.JsObjectStart;
			case '}':
				return JSONToken.JsObjectEnd;
			case '"':
				return consumeString();
			case ',':
				return JSONToken.JsSeparator;
			case ':':
				return JSONToken.JsIdentifierChar;
			case 't':
			case 'f':
				return consumeBoolean();
			case 'n':
				return consumeNull();
			case '-':
				return consumeNumber();
			default:
				// should be a number
				if (cc >= '0' && cc <= '9') {
					return consumeNumber();
				}
				else {
					return JSONToken.JsUnknown;
				}
			}
		}
		
		// get the previous chunk of input, for debugging
		public String getPreviousContent()
		{
		    int start = _index - 80;
		    if (start < 0) {
		        start = 0;
		    }
		    return _json.substring(start, _index);
		}
		
		public Object getValue()
		{
			return _value;
		}
		
		// back up so that we'll re-read the same token
		public void push()
		{
		    _index = _oldIndex;
		}

		private JSONToken consumeString()
		{
			// we will be called after the beginning double-quote has been passed
			char cc = 0;
			int start = _index;
			boolean escaping = false;
			StringBuilder composedValue = null;  // in case we have to put together the value from escape sequences
			while (_index < _json.length()) {
				cc = _json.charAt(_index++);
				if (!escaping) {
					if (cc == '"') {
						break;
					}
					else if (cc == '\\') {
						// start of escape sequence
						escaping = true;
						if (composedValue == null) {
							composedValue = new StringBuilder();
							if (_index > start) {
								composedValue.append(_json.substring(start, _index-1));
							}
						}
					}
					else if (composedValue != null) {
						composedValue.append(cc);
					}
				}
				else {
					// handle escape sequence
					switch(cc) {
					case '"':
					case '\\':
					case '/':
						composedValue.append(cc);
						break;
					case 'b':
						composedValue.append('\b');
						break;
					case 'f':
						composedValue.append('\f');
						break;
					case 'n':
						composedValue.append('\n');
						break;
					case 'r':
						composedValue.append('\r');
						break;
					case 't':
						composedValue.append('\t');
						break;
					case 'u':
						// should have 4 hex char unicode sequence here
						if (_json.length() < _index+4) {
							return JSONToken.JsUnknown;
						}
						String hexValue = _json.substring(_index, _index+4);
						int value = 0;
						try {
							value = Integer.parseInt(hexValue, 16);
						}
						catch(NumberFormatException exc) {
							return JSONToken.JsUnknown;
						}
						composedValue.append((char)value);
						_index += 4;
					}
					escaping = false;
				}
				
			}
			if (cc != '"') {
				// we didn't see the end delimiter
				return JSONToken.JsUnknown;
			}
			
			// save the value
			if (composedValue != null) {
				_value = composedValue.toString();
			}
			else {
				_value = _json.substring(start, _index-1);
			}
			return JSONToken.JsString;
		}
		
		private JSONToken consumeBoolean()
		{
			// we've already consumed the first char, look at it again
			char first = _json.charAt(_index-1);
			if (first == 'f') {
				if (_json.length() >= _index+4 && _json.substring(_index-1, _index+4).equals("false")) {
					_value = Boolean.FALSE;
					_index += 4;
					return JSONToken.JsBoolean;
				}
			}
			else if (first == 't') {
				if (_json.length() >= _index+3 && _json.substring(_index-1, _index+3).equals("true")) {
					_value = Boolean.TRUE;
					_index += 3;
					return JSONToken.JsBoolean;
				}
			}
			
			return JSONToken.JsUnknown;
		}
		
		private JSONToken consumeNull()
		{
			// we've already consumed the first char and it is an n
			if (_json.length() >= _index+3 && _json.substring(_index-1, _index+3).equals("null")) {
				_value = null;
				_index += 3;
				return JSONToken.JsNull;
			}
			
			return JSONToken.JsUnknown;
		}
		
		private JSONToken consumeNumber()
		{
			// we will be called after the beginning character has been passed (and checked legal)
			// just supporting integers and non-exponent doubles now
			int sign = 1;
			boolean haveDecimal = false;
			int start = _index;
			if (_json.charAt(_index-1) == '-') {
				sign = -1;
			}
			else {
				// back up to get first numeric
				start = _index - 1;
			}
			while (_index < _json.length()) {
				char cc = _json.charAt(_index++);
				if (cc == '.' && !haveDecimal) {
				    // we can have one decimal point, no more
				    haveDecimal = true;
				}
				else if (cc < '0' || cc > '9') {
					_index--;
					break;
				}
			}
			if (start == _index) {
				// bare negative sign
				return JSONToken.JsUnknown;
			}
			String numberString = _json.substring(start, _index);
			JSONToken type;
			if (!haveDecimal) {
			    _value = sign * Integer.parseInt(numberString);
			    type = JSONToken.JsInt;
			}
			else {
			    _value = sign * Double.parseDouble(numberString);
			    type = JSONToken.JsDouble;
			}
			
			return type;
		}
	}
	
	// JSONReadable needs a post-read method to handle version upgrade
	// can optionally provide fields (so can skip some present in the file
	public static interface JSONReadableBySetters
	{
		public void postJSONRead();
	}
	
	// marker class indicating that class can be read
	// the class *must* implement a constructor that has an argument of JSONReader
	// that constructor is then responsible for setting up the object via calls to reader.getValue
	public static interface JSONReadable
	{
		
	}
	
}
