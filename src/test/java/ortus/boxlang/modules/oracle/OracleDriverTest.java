package ortus.boxlang.modules.oracle;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.jdbc.drivers.DatabaseDriverType;
import ortus.boxlang.runtime.scopes.Key;

public class OracleDriverTest {

	@Test
	@DisplayName( "Test getName()" )
	public void testGetName() {
		OracleDriver	driver			= new OracleDriver();
		Key				expectedName	= new Key( "Oracle" );
		assertThat( driver.getName() ).isEqualTo( expectedName );
	}

	@Test
	@DisplayName( "Test getType()" )
	public void testGetType() {
		OracleDriver		driver			= new OracleDriver();
		DatabaseDriverType	expectedType	= DatabaseDriverType.ORACLE;
		assertThat( driver.getType() ).isEqualTo( expectedType );
	}

	@Test
	@DisplayName( "Test connection URLs with SID" )
	public void testConnectionURLWithSID() {
		OracleDriver		driver	= new OracleDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "SID", "mySID" );
		config.properties.put( "protocol", "thin" );

		String expectedURL = "jdbc:oracle:thin:@localhost:1521:mySID";
		assertThat( driver.buildConnectionURL( config ) ).isEqualTo( expectedURL );
	}

	@Test
	@DisplayName( "Test connection URLs with service name" )
	public void testConnectionURLWithServiceName() {
		OracleDriver		driver	= new OracleDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "serviceName", "myServiceName" );
		config.properties.put( "protocol", "thin" );

		String expectedURL = "jdbc:oracle:thin:@//localhost:1521/myServiceName";
		assertThat( driver.buildConnectionURL( config ) ).isEqualTo( expectedURL );
	}

	@DisplayName( "Throw an exception if the serviceName or SID is not found" )
	@Test
	public void testBuildConnectionURLNoServiceNameOrSID() {
		OracleDriver		driver	= new OracleDriver();
		DatasourceConfig	config	= new DatasourceConfig();

		assertThrows( IllegalArgumentException.class, () -> {
			driver.buildConnectionURL( config );
		} );
	}

	@DisplayName( "Throw an exception if the serviceName or SID are both blank or empty" )
	@Test
	public void testBuildConnectionURLBlankServiceNameOrSID() {
		OracleDriver		driver	= new OracleDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "serviceName", "" );
		config.properties.put( "sid", "   " );

		assertThrows( IllegalArgumentException.class, () -> {
			driver.buildConnectionURL( config );
		} );
	}

}
