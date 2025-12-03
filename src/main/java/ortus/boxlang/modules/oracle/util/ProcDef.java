/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.modules.oracle.util;

import java.io.Serializable;
import java.util.List;

/**
 * A record to define a stored procedure definition. There can be more than one overridden definition for a procedure,
 * differing by parameter count and types.
 */
public record ProcDef( int paramCount, int nonRefCursorParamCount, List<ProcParameter> params ) implements Serializable {

	/**
	 * Returns a string representation of the Proc definition
	 */
	public String toString() {
		String result = "  - ProcDef paramCount=" + paramCount + " nonRefCursorParamCount=" + nonRefCursorParamCount + "\n    params ("
		    + params.size()
		    + ")\n";
		for ( ProcParameter param : params ) {
			result += param + "\n";
		}
		return result;
	}

}
