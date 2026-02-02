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

import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Types;

import ortus.boxlang.modules.oracle.util.OracleUtil;
import ortus.boxlang.modules.oracle.util.Proc;
import ortus.boxlang.modules.oracle.util.ProcDef;
import ortus.boxlang.modules.oracle.util.ProcParameter;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.BoxConnection;
import ortus.boxlang.runtime.jdbc.BoxStatement;
import ortus.boxlang.runtime.jdbc.drivers.DatabaseDriverType;
import ortus.boxlang.runtime.jdbc.drivers.GenericJDBCDriver;
import ortus.boxlang.runtime.jdbc.drivers.JDBCDriverFeature;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
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
	    "kprb", "Kerberos" );

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

		setFeatures( JDBCDriverFeature.TRIM_TRAILING_SEMICOLONS );
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
			        AVAILABLE_PROTOCOLS.keySet().toString() ) );
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
			    sid );
		}

		String serviceName = ( String ) config.properties.getOrDefault( "serviceName", "" );
		if ( serviceName.isBlank() ) {
			throw new IllegalArgumentException(
			    "Either the serviceName or SID property is required for the Oracle JDBC Driver." );
		}

		// Build the connection URL
		return String.format(
		    "jdbc:oracle:%s:@//%s:%s/%s",
		    protocol,
		    host,
		    port,
		    serviceName );
	}

	/**
	 * Map a SQL type to a QueryColumnType. The default implementation will use the
	 * mappings in the QueryColumnType enum.
	 * Override this method if the driver has specific mappings. Example, mapping
	 * RowId in Oracle to a String type.
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
		return super.mapSQLTypeToQueryColumnType( sqlType );
	}

	/**
	 * Transform a value according to the driver's specific needs. This allows
	 * drivers to map custom Java classes to native BL types.
	 * The default implementation will return the value as-is.
	 * 
	 * @param sqlType   The SQL type of the value, from java.sql.Types
	 * @param value     The value to transform
	 * @param statement The BoxStatement instance
	 * 
	 * @return The transformed value
	 */
	// @Override
	public Object transformValue( int sqlType, Object value, BoxStatement statement ) {
		if ( value == null ) {
			return null;
		}
		if ( sqlType == Types.ROWID ) {
			// Convert Oracle RowId to String
			return value.toString();
		}
		if ( value instanceof RowId ) {
			return value.toString();
		}
		return super.transformValue( sqlType, value, statement );
	}

	/**
	 * Map param type to SQL type. For the most part, these mappings are defined by
	 * the QueryColumnType enum,
	 * but some drivers may have specific needs. Oracle, or example, uses CHAR even
	 * when you ask for VARCHAR which allows
	 * char columns to match without trailing space.
	 * 
	 * @param type  The QueryColumnType of the parameter
	 * @param value The value of the parameter (in case the mapping needs to
	 *              consider the value)
	 * 
	 * @return The SQL type as defined in java.sql.Types
	 */
	// @Override
	public int mapParamTypeToSQLType( QueryColumnType type, Object value ) {
		// This allows a char column to match without trailing spaces or trimming.
		// From my testing, it doesn't appear to have any negative side effects, but if
		// neccessary, we can limit when this swap occurs based on the value.
		if ( type == QueryColumnType.VARCHAR ) {
			return Types.CHAR;
		}
		return super.mapParamTypeToSQLType( type, value );
	}

	/**
	 * Emit stored proc named parameter syntax according to the driver's specific
	 * needs.
	 * 
	 * @param callSQL   The StringBuilder to append the parameter syntax to
	 * @param paramName The name of the parameter
	 */
	public void emitStoredProcNamedParam( StringBuilder callSQL, String paramName ) {
		// remove any leading :
		if ( paramName.startsWith( ":" ) ) {
			paramName = paramName.substring( 1 );
		}
		// output paraName => ?
		callSQL.append( paramName ).append( " => " ).append( "?" );
	}

	/**
	 * Pre-process a stored procedure call. This allows the driver to do any specific pre-processing
	 * before the procedure is called. This can include registering output parameters, etc.
	 * 
	 * @param conn          The BoxConnection instance
	 * @param procedureName The name of the stored procedure
	 * @param params        The parameters array
	 * @param procResults   The procedure results array
	 * @param context       The BoxLang context
	 * @param debug         Whether debug mode is enabled
	 */
	public void preProcessProcCall( BoxConnection conn, String procedureName, Array params, Array procResults, IBoxContext context, boolean debug )
	    throws SQLException {

		Proc proc = OracleUtil.getProcMeta( conn, procedureName );
		if ( debug ) {
			System.out.println( proc.toString() );
		}

		// TODO: Handle overloads properly and find matching defitnition based on param count and types
		ProcDef	def		= proc.definitions().get( 0 );
		boolean	isNamed	= false;
		// If there is at least one param, check and see if it has a name
		if ( params.size() > 0 ) {
			isNamed = ( ( IStruct ) params.get( 0 ) ).containsKey( Key.DBVarName );
		} else {
			// No params provided - force named mode since we'll need to add ref cursor params by name
			isNamed = true;
		}
		for ( int i = 0; i < def.params().size(); i++ ) {
			ProcParameter paramDef = def.params().get( i );
			if ( paramDef.isOut() && paramDef.isRefCursor() ) {
				// Validate that we have enough params for the preceding parameters (only for positional params)
				// Named params can skip optional params with default values
				if ( !isNamed && params.size() < i ) {
					throw new SQLException(
					    String.format(
					        "Missing parameters for stored procedure '%s'. Expected at least %d parameters before the ref cursor parameter '%s' at position %d, but only %d were provided.",
					        procedureName,
					        i,
					        paramDef.name(),
					        i + 1,
					        params.size() ) );
				}
				IStruct newParam = Struct.of( Key.type, "out", Key.sqltype, "refcursor" );
				// add out param for ref cursor
				if ( !procResults.isEmpty() ) {
					IStruct nextProcResult = ( IStruct ) procResults.get( 0 );
					// TODO: Look at resultSet to see if they are skipping results
					newParam.put( Key.variable, nextProcResult.get( Key._NAME ) );
					procResults.removeAt( 0 );
				}
				if ( isNamed ) {
					newParam.put( Key.DBVarName, ":" + paramDef.name() );
					// For named params, just append - order doesn't matter since names handle mapping
					params.add( newParam );
				} else {
					// For positional params, insert at the correct position based on the definition
					params.insertAt( i + 1, newParam );
				}
			}
		}
		if ( !procResults.isEmpty() ) {
			// To be compat just ignore extra proc results, which actually matches non-oracle behavior
			procResults.clear();
			// throw new SQLException( "More proc results were specified than are present in the procedure definition. " + procResults.toString() );
		}
	}

}
