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

import ortus.boxlang.runtime.scopes.Key;

/**
 * A record to define a stored procedure parameter
 */
public record ProcParameter( int position, String name, String typeName, Key inOut ) implements Serializable {

	/**
	 * Returns a string representation of the Proc parameter
	 */
	public String toString() {
		String result = "      - Param #" + position + " name=" + name + " type=" + typeName + " inOut=" + inOut;
		return result;
	}

	/**
	 * Indicates if the parameter is an OUT parameter
	 * 
	 * @return True if the parameter is an OUT parameter, false otherwise
	 */
	public boolean isOut() {
		return inOut.equals( OracleUtil.outKey );
	}

	/**
	 * Indicates if the parameter is a REF CURSOR
	 * 
	 * @return True if the parameter is a REF CURSOR, false otherwise
	 */
	public boolean isRefCursor() {
		return typeName.equals( "REF CURSOR" );
	}
}
