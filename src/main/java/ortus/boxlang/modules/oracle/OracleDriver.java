/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.modules.oracle;

import java.sql.Types;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.drivers.DatabaseDriverType;
import ortus.boxlang.runtime.jdbc.drivers.GenericJDBCDriver;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;

/**
 * The Oracle JDBC Driver
 * https://www.baeldung.com/java-jdbc-url-format
 * jdbc:oracle:thin:@HOSTNAME:1521/DATABASENAME
 * https://docs.oracle.com/en/database/oracle/oracle-database/21/jajdb/oracle/jdbc/OracleDriver.html
 */
public class OracleDriver extends GenericJDBCDriver {

	protected static final String	DEFAULT_CLASSNAME			= "oracle.jdbc.OracleDriver";
	protected static final String	DEFAULT_PORT				= "1521";
	protected static final String	DEFAULT_HOST				= "localhost";
	protected static final String	DEFAULT_PROTOCOL			= "thin";
	protected static final String	DEFAULT_DELIMITER			= "&";
	protected static final IStruct	DEFAULT_HIKARI_PROPERTIES	= Struct.of();
	protected static final IStruct	DEFAULT_CUSTOM_PARAMS		= Struct.of();
	protected static final IStruct	AVAILABLE_PROTOCOLS			= Struct.of(
	    "thin", "Default protocol",
	    "oci", "Oracle Call Interface",
	    "kprb", "Kerberos"
	);

	/**
	 * Constructor
	 */
	public OracleDriver() {
		super();
		this.name					= new Key( "Oracle" );
		this.type					= DatabaseDriverType.ORACLE;
		this.driverClassName		= DEFAULT_CLASSNAME;
		this.defaultDelimiter		= DEFAULT_DELIMITER;
		this.defaultCustomParams	= DEFAULT_CUSTOM_PARAMS;
		this.defaultProperties		= DEFAULT_HIKARI_PROPERTIES;
	}

	@Override
	public String buildConnectionURL( DatasourceConfig config ) {
		// Validate the host
		String host = ( String ) config.properties.getOrDefault( "host", DEFAULT_HOST );
		if ( host.isEmpty() ) {
			host = DEFAULT_HOST;
		}

		// Port
		String port = StringCaster.cast( config.properties.getOrDefault( "port", DEFAULT_PORT ) );
		if ( port.isEmpty() || port.equals( "0" ) ) {
			port = DEFAULT_PORT;
		}

		// Protocol
		String protocol = ( String ) config.properties.getOrDefault( "protocol", DEFAULT_PROTOCOL );
		if ( protocol.isEmpty() ) {
			protocol = DEFAULT_PROTOCOL;
		}
		// Validate the protocol
		if ( !AVAILABLE_PROTOCOLS.containsKey( protocol ) ) {
			throw new IllegalArgumentException(
			    String.format(
			        "The protocol '%s' is not valid for the Oracle Driver. Available protocols are %s",
			        protocol,
			        AVAILABLE_PROTOCOLS.keySet().toString()
			    )
			);
		}

		// Validate the database
		String sid = ( String ) config.properties.getOrDefault( "SID", "" );
		if ( !sid.isBlank() ) {
			// Build the connection URL with the SID
			return String.format(
			    "jdbc:oracle:%s:@%s:%s:%s",
			    protocol,
			    host,
			    port,
			    sid
			);
		}

		String serviceName = ( String ) config.properties.getOrDefault( "serviceName", "" );
		if ( serviceName.isBlank() ) {
			throw new IllegalArgumentException( "Either the serviceName or SID property is required for the Oracle JDBC Driver." );
		}

		// Build the connection URL
		return String.format(
		    "jdbc:oracle:%s:@//%s:%s/%s",
		    protocol,
		    host,
		    port,
		    serviceName
		);
	}

	/**
	 * Map a SQL type to a QueryColumnType. The default implementation will use the mappings in the QueryColumnType enum.
	 * Override this method if the driver has specific mappings. Example, mapping RowId in Oracle to a String type.
	 * 
	 * @param sqlType The SQL type to map, from java.sql.Types
	 * 
	 * @return The QueryColumnType
	 */
	// @Override
	public QueryColumnType mapSQLTypeToQueryColumnType( int sqlType ) {
		// We map RowID to VARCHAR
		if ( sqlType == Types.ROWID ) {
			return QueryColumnType.VARCHAR;
		}
		// Everything else uses the default mapping
		return QueryColumnType.fromSQLType( sqlType );
	}

	/**
	 * Transform a value according to the driver's specific needs. This allows drivers to map custom Java classes to native BL types.
	 * The default implementation will return the value as-is.
	 * 
	 * @param sqlType The SQL type of the value, from java.sql.Types
	 * @param value   The value to transform
	 * 
	 * @return The transformed value
	 */
	// @Override
	public Object transformValue( int sqlType, Object value ) {
		if ( sqlType == Types.ROWID && value != null ) {
			// Convert Oracle RowId to String
			return value.toString();
		}
		return value;
	}

}
