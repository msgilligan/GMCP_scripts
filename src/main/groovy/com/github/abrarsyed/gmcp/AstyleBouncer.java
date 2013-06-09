package com.github.abrarsyed.gmcp;

import java.lang.reflect.Method;

import com.google.common.base.Throwables;

@SuppressWarnings("rawtypes")
public class AstyleBouncer
{
	private Object				instance;
	private Class				clazz;
	private static final String	CLASS			= "AStyleInterface";
	private static final String	METHOD_VERSION	= "getVersion";
	private static final String	METHOD_FORMAT	= "formatSources";

	public AstyleBouncer()
	{
		try
		{
			clazz = Class.forName(CLASS);
			instance = clazz.newInstance();
		}
		catch (Throwable t)
		{
			Throwables.propagate(t);
		}
	}

	/**
	 * Call the AStyleGetVersion function in Artistic Style.
	 * @return A String containing the version number from Artistic Style.
	 */
	@SuppressWarnings("unchecked")
	public String getVersion()
	{
		try
		{
			Method m = clazz.getMethod(METHOD_VERSION, new Class[0]);
			m.setAccessible(true);
			String out = (String) m.invoke(instance, new Object[0]);
			return out;
		}
		catch (Throwable t)
		{
			Throwables.propagate(t);
		}

		// stupid compiler
		return null;
	}

	/**
	 * Call the AStyleMain function in Artistic Style.
	 * @param textIn A string containing the source code to be formatted.
	 * @return A String containing the formatted source from Artistic Style.
	 */
	@SuppressWarnings("unchecked")
	public String formatSource(String textIn, String options)
	{
		try
		{
			Method m = clazz.getMethod(METHOD_FORMAT, new Class[] { String.class, String.class });
			m.setAccessible(true);
			String out = (String) m.invoke(instance, textIn, options);
			return out;
		}
		catch (Throwable t)
		{
			Throwables.propagate(t);
		}

		// stupid compiler
		return null;
	}
}
