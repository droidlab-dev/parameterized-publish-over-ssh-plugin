package org.jenkins.plugins.remotehosts.utils;

import hudson.Util;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * 
 */
public class ParameterizedUtils {
	
	/**
	 * Replace parameterized value on the String attributes of a given object
	 * 
	 * @param object
	 * @param vars
	 * @param printStream
	 */
	public static void reflectiveParameterizedValueReplacer(Object object, Map<String, String> vars, PrintStream printStream) {
		
		try { // Try to replace parameterized value using reflection on the given object
			
			for (Field field : object.getClass().getDeclaredFields()) {
				
				if(field.getType().equals(String.class) && field.getModifiers() != Modifier.TRANSIENT){

					field.setAccessible(true);
					
				    field.set(object, replaceParameterizedValue((String)field.get(object), vars));
	            }
			}
	        
		} catch (IllegalArgumentException e) {
			printStream.print("[WARN] Parametrized value unreplaced for " +object.getClass().getName() + ": " + e);
		} catch (IllegalAccessException e) {
			printStream.print("[WARN] Parametrized value unreplaced for " +object.getClass().getName() + ": " + e);
		} catch (SecurityException e) {
			printStream.print("[WARN] Parametrized value unreplaced for " +object.getClass().getName() + ": " + e);
		}

	}
	
	
	/**
	 * Replace parameterized value on a given String
	 * 
	 * @param value
	 * @param vars
	 * @return
	 */
	public static String replaceParameterizedValue(String value, Map<String, String> vars) {

		if (value != null && value.startsWith("$")) {
			value = vars.get(value.replace("$", ""));
		}

		return value;
	}
	

	/**
	 * Replace parameterized value on a given script
	 * 
	 * @param script
	 * @param vars
	 * @return
	 */
	public static String replaceParametrizedScript(String script, Map<String, String> vars) {

		while (script != null && script.contains("$")) {

			String parameterized = script.substring(script.indexOf('$') + 1);
			
			if(parameterized.contains("\n")){ parameterized = parameterized.substring(0, parameterized.indexOf('\n')); }
			if(parameterized.contains("\r")){ parameterized = parameterized.substring(0, parameterized.indexOf('\r')); }
			if(parameterized.contains(" ")) { parameterized = parameterized.substring(0, parameterized.indexOf(' '));  }
			if(parameterized.contains("/")) { parameterized = parameterized.substring(0, parameterized.indexOf('/'));  }
			if(parameterized.contains(";")) { parameterized = parameterized.substring(0, parameterized.indexOf(';'));  }
			if(parameterized.contains("\\")){ parameterized = parameterized.substring(0, parameterized.indexOf('\\')); }
		
			script = script.replaceFirst("\\$" + parameterized, Util.fixNull(vars.get(parameterized)));
		}
		
		return script;
	}
	

	/**
	 * 
	 * @param inputString
	 * @return
	 */
	public static boolean containsParameterizedValue(String inputString) {
		
		return Util.fixNull(inputString).contains("$");
	}
	
	
	/**
	 * Replace parameterized value on a given script
	 * 
	 * @author Edmund Wagner (this plugin was initialy forked from Jenkins SSH plugin :
	 * 						  https://wiki.jenkins-ci.org/display/JENKINS/SSH+plugin)
	 * 
	 * @param originalCommand
	 * @param vars
	 * @return
	 */
	public static String replace(String originalCommand, Map<String, String> vars) {
		
		if(originalCommand == null){
			return null;
		}
		vars.remove("_"); //why _ as key for build tool?
		StringBuilder sb = new StringBuilder();
		for (String variable : vars.keySet()) {
			if (originalCommand.contains(variable) ) {
				sb.append(variable).append("=\"").append(vars.get(variable)).append("\"\n");
			}
		}
		sb.append("\n");
		sb.append(originalCommand);
		return sb.toString();
	}
}
