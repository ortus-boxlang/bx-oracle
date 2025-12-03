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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.jdbc.BoxConnection;
import ortus.boxlang.runtime.scopes.Key;

/**
 * A helper for Oracle JDBC Driver related utilities.
 */
public class OracleUtil {

	public static final Key			inKey			= Key.of( "in" );
	public static final Key			outKey			= Key.of( "out" );
	public static final Key			inoutKey		= Key.of( "inout" );

	// TODO: switch to cache
	private static Map<Key, Proc>	procMetaCache	= new ConcurrentHashMap<>();
	private static final Object		cacheLock		= new Object();

	/**
	 * Get stored procedure metadata, using caching
	 * 
	 * @param connection The BoxConnection instance
	 * @param procName   The name of the stored procedure
	 * 
	 * @return The Proc metadata
	 * 
	 * @throws SQLException If a database access error occurs
	 */
	public static Proc getProcMeta( BoxConnection connection, String procName ) throws SQLException {
		Key		cacheKey	= Key.of( connection.getDataSource().getUniqueName().getName() + procName );
		Proc	proc		= procMetaCache.get( cacheKey );
		if ( proc == null ) {
			synchronized ( cacheLock ) {
				proc = procMetaCache.get( cacheKey );
				if ( proc == null ) {
					proc = buildProcMeta( connection, procName );
					procMetaCache.put( cacheKey, proc );
				}
			}
		}
		return proc;
	}

	/**
	 * Build stored procedure metadata
	 * 
	 * @param connection The BoxConnection instance
	 * @param procName   The name of the stored procedure
	 * 
	 * @return The Proc metadata
	 * 
	 * @throws SQLException If a database access error occurs
	 */
	public static Proc buildProcMeta( BoxConnection connection, String procName ) throws SQLException {
		String	resolvedPart2;
		int		resolvedObjectNumber;

		// Resolve the object number using DBMS_UTILITY.NAME_RESOLVE
		// If the proc doesn't exist, we error here.
		try ( BoxConnection conn = connection.getDataSource().getBoxConnection();
		    CallableStatement stmt = conn.prepareCall( "{ call dbms_utility.name_resolve(?, ?, ?, ?, ?, ?, ?, ?) }" ) ) {

			// IN parameters
			stmt.setString( 1, procName );  // name
			stmt.setInt( 2, 1 );            // context (1 for procedure/function)

			// OUT parameters (all must be registered for Oracle)
			stmt.registerOutParameter( 3, Types.VARCHAR );  // schema
			stmt.registerOutParameter( 4, Types.VARCHAR );  // part1
			stmt.registerOutParameter( 5, Types.VARCHAR );  // part2
			stmt.registerOutParameter( 6, Types.VARCHAR );  // dblink
			stmt.registerOutParameter( 7, Types.INTEGER );  // part1_type
			stmt.registerOutParameter( 8, Types.INTEGER );  // object_number

			stmt.execute();

			// Retrieve OUT values
			resolvedPart2			= stmt.getString( 5 );
			resolvedObjectNumber	= stmt.getInt( 8 );
		}

		// Query out the procedure parameters from ALL_ARGUMENTS
		String			sql			= """
		                              SELECT UNIQUE POSITION,
		                              	ARGUMENT_NAME,
		                              	DATA_TYPE,
		                              	IN_OUT,
		                              	OVERLOAD
		                              FROM SYS.ALL_ARGUMENTS
		                              WHERE OBJECT_ID = ?
		                              	AND OBJECT_NAME = ?
		                              	AND DATA_LEVEL = 0
		                              ORDER BY OVERLOAD, POSITION
		                              """;

		List<ProcDef>	definitions	= new ArrayList<>();
		try ( BoxConnection conn = connection.getDataSource().getBoxConnection();
		    PreparedStatement stmt = conn.prepareStatement( sql ) ) {

			stmt.setInt( 1, resolvedObjectNumber );
			stmt.setString( 2, resolvedPart2 );

			List<ProcParameter>	params			= new ArrayList<>();
			Integer				lastOverload	= null;
			try ( ResultSet rs = stmt.executeQuery() ) {

				while ( rs.next() ) {
					Integer overload = rs.getInt( "OVERLOAD" );
					if ( overload != null && lastOverload != null && !overload.equals( lastOverload ) ) {
						definitions
						    .add( new ProcDef( params.size(), ( int ) params.stream().filter( p -> !p.typeName().equals( "REF CURSOR" ) ).count(), params ) );
						params = new ArrayList<>();
					}
					lastOverload = overload;

					Key		inOutKey;
					String	inOut	= rs.getString( "IN_OUT" );
					if ( "IN".equals( inOut ) ) {
						inOutKey = inKey;
					} else if ( "OUT".equals( inOut ) ) {
						inOutKey = outKey;
					} else {
						inOutKey = inoutKey;
					}

					params.add(
					    new ProcParameter(
					        rs.getInt( "POSITION" ),
					        rs.getString( "ARGUMENT_NAME" ),
					        rs.getString( "DATA_TYPE" ),
					        inOutKey
					    )
					);
				}
			}
			definitions.add( new ProcDef( params.size(), ( int ) params.stream().filter( p -> !p.typeName().equals( "REF CURSOR" ) ).count(), params ) );
		}

		Proc proc = new Proc( connection.getDataSource().getUniqueName(), procName, definitions );

		return proc;

	}
}
