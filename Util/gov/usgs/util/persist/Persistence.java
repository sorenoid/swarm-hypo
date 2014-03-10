package gov.usgs.util.persist;

import gov.usgs.util.ConfigFile;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Set of procedures to store objects in configuration files, 
 * as 'object_classname.fieldname:persistenceID'='serialized_field_value' pairs
 *
 * @author Dan Cervelli
 */
public class Persistence
{
	private static final String NULL_VALUE = "@@null@@";
	
	private static ConfigFile config;
	private static ConfigFile outConfig;
	
	private static Map<Class<?>, Translator> translators;
	
	private Persistence() {}
	
	static
	{
		translators = new HashMap<Class<?>, Translator>();
		translators.put(boolean.class, new BooleanTranslator());
		translators.put(byte.class, new ByteTranslator());
		translators.put(short.class, new ShortTranslator());
		translators.put(int.class, new IntTranslator());
		translators.put(long.class, new LongTranslator());
		translators.put(char.class, new CharTranslator());
		translators.put(float.class, new FloatTranslator());
		translators.put(double.class, new DoubleTranslator());
		translators.put(String.class, new StringTranslator());
	}

	/**
	 * Abstract base class for translators, i.e classes performs serialization
	 * of specified type to and from string. 
	 * Question: why it's impossible to use standard serialization feature, via Serializable? 
	 */
	public static abstract class Translator
	{
		public abstract Object translateFromString(String str);
		public String translateToString(Object obj)
		{
			return obj.toString();
		}
	}
	
	/**
	 * Loads content of configuration file into <code>config</code>
	 * @param cf disk file name
	 */
	public static void loadConfig(String cf)
	{
		config = new ConfigFile(cf);
	}
	
	/**
	 * Computes persistentID, unique id for object instance
	 * @param fields array of object's fields
	 * @param object object to compute persistent ID
	 * @return string - persistent ID for object, string representation of first found in array field which is annotated	 */
	private static String getPersistentID(Field[] fields, Object object)
	{
		try
		{
			for (Field field : fields) 
			{
				if (field.isAnnotationPresent(PersistentID.class))
				{
					boolean acc = field.isAccessible();
					field.setAccessible(true);
					Object obj = field.get(object);
					String s = null;
					if (obj != null)
						s = obj.toString();
					field.setAccessible(acc);
					return s;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Getter for Translator for specified Class
	 * @param type Class
	 * @return Translator for given class 
	 * @see Translator
	 */
	private static Translator getTranslator(Class<?> type)
	{
		Translator translator = translators.get(type);
		if (translator == null)
		{
			for (Class<?> t : translators.keySet())
			{
				if (t.isAssignableFrom(type))
				{
					translator = translators.get(t);
					break;
				}
			}
		}
		return translator;
	}
	
	/**
	 * Loads stored in ConfigFile object, in it's own type
	 * @param object 
	 */
	public static void load(Object object)
	{
		load(object, object.getClass());
	}

	/**
	 * Loads stored in ConfigFile object and cast it to given super class
	 * Question: if we need to load only subset of object's fields 
	 * why it's impossible to use standard transient modificator, 
	 * and fixed class serial ID?
	 * Question: why we don't use standard object's writeObject/readObject methods
	 * to set serialization mode and data location?
	 * @param object 
	 * @param superClass 
	 */
	public static void load(Object object, Class<?> superClass)
	{
		ConfigFile cf = config.getSubConfig(object.getClass().getName());
		final Field fields[] = superClass.getDeclaredFields();
		String id = getPersistentID(fields, object);
		for (Field field : fields) 
		{
			if (field.isAnnotationPresent(Persistent.class))
			{
				try
				{
					String key = field.getName();
					if (id != null)
						key += ":" + id;
					
					String value = cf.getString(key);
					String fieldName = object.getClass().getName() + "." + key;
					if (value == null || value.length() == 0)
					{	
						System.err.println("no data for persisent field: " + fieldName);
						continue;
					}
					
					boolean acc = field.isAccessible();
					field.setAccessible(true);
					Class<?> type = field.getType();
					Translator xlat = getTranslator(type);
					if (xlat != null)
					{
						Object v;
						if (value.equals(NULL_VALUE))
							v = null;
						else
							v = xlat.translateFromString(value);
						field.set(object, v);
					}
					else
						System.err.println("could not load persistent field: " + fieldName);
					
					field.setAccessible(acc);
				}
				catch (IllegalAccessException ex)
				{
					ex.printStackTrace();
				}
			}
	    }
	}
	
	/**
	 * Assign <code>outConfig</code> value to <code>config</code>
	 */
	public static void useOriginalConfig()
	{
		outConfig = config;
	}
	
	/**
	 * Serialize object as instance of it's own class, and store it in the <code>ConfigFile outConfig</code>
	 * @param object to be serialized
	 */
	public static void save(Object object)
	{
		save(object, object.getClass());
	}
	
	/**
	 * Serialize object as instance of given superclass, and store it in the <code>ConfigFile outConfig</code>
	 * The same question as in load(Object object, Class superClass)
	 * @param object to be serialized
	 * @param superClass class to be serialized as
	 */
	public static void save(Object object, Class<?> superClass)
	{
		if (outConfig == null)
			outConfig = new ConfigFile();
		
		final Field fields[] = superClass.getDeclaredFields();
		String id = getPersistentID(fields, object);
		for (Field field : fields) 
		{
			if (field.isAnnotationPresent(Persistent.class))
			{
				try
				{
					boolean acc = field.isAccessible();
					field.setAccessible(true);
					String key = object.getClass().getName() + "." + field.getName();
					if (id != null)
						key += ":" + id;
					Translator xlat = getTranslator(field.getType());
					if (xlat != null)
					{
						Object v = field.get(object);
						String value;
						if (v == null)
							value = NULL_VALUE;
						else
							value = xlat.translateToString(field.get(object));
						outConfig.put(key, value, false);
					}
					else
						System.err.println("could not save persistent field: " + object.getClass().getName() + "." + field.getName());
					
					field.setAccessible(acc);
				}
				catch (IllegalAccessException ex)
				{
					ex.printStackTrace();
				}
			}
	    }
	}
	
	/**
	 * Saves <code>ConfigFile outConfig</code> to file
	 * @param fn disk file name
	 */
	public static void flush(String fn)
	{
		outConfig.writeToFile(fn);
	}
	
	/**
	 * Translator for boolean
	 * @see Translator
	 */
	protected static class BooleanTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return Boolean.parseBoolean(str);
		}
	}

	/**
	 * Translator for byte
	 * @see Translator
	 */
	protected static class ByteTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return Byte.parseByte(str);
		}
	}
	
	/**
	 * Translator for int
	 * @see Translator
	 */
	protected static class IntTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return Integer.parseInt(str);
		}
	}
	
	/**
	 * Translator for long
	 * @see Translator
	 */
	protected static class LongTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return Long.parseLong(str);
		}
	}
	
	/**
	 * Translator for char
	 * @see Translator
	 */
	protected static class CharTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return str.charAt(0);
		}
	}
	
	/**
	 * Translator for short
	 * @see Translator
	 */
	protected static class ShortTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return Short.parseShort(str);
		}
	}
	
	/**
	 * Translator for float
	 * @see Translator
	 */
	protected static class FloatTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return Float.parseFloat(str);
		}
	}
	
	/**
	 * Translator for double
	 * @see Translator
	 */
	protected static class DoubleTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return Double.parseDouble(str);
		}
	}

	/**
	 * Translator for String
	 * @see Translator
	 */
	protected static class StringTranslator extends Translator
	{
		public Object translateFromString(String str)
		{
			return str.replace("\\n", "\n");
		}
		
		public String translateToString(Object obj)
		{
			return ((String)obj).replace("\n", "\\n");
		}
	}
}
