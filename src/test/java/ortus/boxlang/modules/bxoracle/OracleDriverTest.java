package ortus.boxlang.modules.bxoracle;

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
	@DisplayName( "Test buildConnectionURL()" )
	public void testBuildConnectionURL() {
		OracleDriver		driver	= new OracleDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "driver", "oracle" );
		config.properties.put( "database", "mydb" );

		String expectedURL = "jdbc:oracle:thin:@//localhost:1521/mydb";
		assertThat( driver.buildConnectionURL( config ) ).isEqualTo( expectedURL );
	}

	@DisplayName( "Throw an exception if the database is not found" )
	@Test
	public void testBuildConnectionURLNoDatabase() {
		OracleDriver		driver	= new OracleDriver();
		DatasourceConfig	config	= new DatasourceConfig();

		assertThrows( IllegalArgumentException.class, () -> {
			driver.buildConnectionURL( config );
		} );
	}

}
