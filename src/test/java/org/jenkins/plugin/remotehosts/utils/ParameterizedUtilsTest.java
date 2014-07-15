package org.jenkins.plugin.remotehosts.utils;

import java.util.HashMap;
import java.util.Map;

import org.jenkins.plugins.remotehosts.utils.ParameterizedUtils;

import junit.framework.TestCase;

public class ParameterizedUtilsTest extends TestCase {

	/**
	 * Test parameterized script filtering and replacement
	 */
	public void testReplaceParameterizedScript() {
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("VAR1", "value1");
		vars.put("VAR2", "value2");
		
		String script 			= "/path/example/with/parameterized/$VAR1/ /another/example/with/parameterized/$VAR2\n\r";
		String expectedResult 	= "/path/example/with/parameterized/value1/ /another/example/with/parameterized/value2\n\r";

		assertEquals(expectedResult, ParameterizedUtils.replaceParametrizedScript(script, vars));
	}
}
