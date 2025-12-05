# Oracle JDBC Driver for BoxLang

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://www.boxlang.io">www.boxlang.io</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

This module provides a BoxLang JDBC driver for Oracle databases, enabling seamless integration between BoxLang applications and Oracle Database for enterprise-grade data operations with support for both Service Name and SID-based connections.

## Features

- 🚀 **Enterprise Ready**: Built on Oracle's official JDBC driver with full ACID compliance
- 🔌 **Flexible Connectivity**: Support for both Service Name and SID connection formats
- 🌐 **Multiple Protocols**: Support for `thin` (default), `oci`, and `kprb` protocols
- 🔄 **BoxLang Integration**: Native support for BoxLang's `queryExecute()` and datasource management
- ⚡ **Connection Pooling**: Leverages HikariCP for high-performance connection management
- 🛠️ **Easy Configuration**: Simple datasource setup with sensible defaults

## Installation

### Via CommandBox (Recommended)

```bash
box install bx-oracle
```

### Via BoxLang Module Installer

```bash
# Into the BoxLang HOME
install-bx-module bx-oracle

# Or a local folder
install-bx-module bx-oracle --local
```

## Quick Start

Once installed, you can immediately start using Oracle databases in your BoxLang applications:

```javascript
// Define a datasource using Service Name
this.datasources[ "oracleDB" ] = {
    "driver": "oracle",
    "serviceName": "XEPDB1",
    "host": "localhost",
    "port": 1521,
    "username": "system",
    "password": "mypassword"
};

// Use it in your code
result = queryExecute("SELECT * FROM employees WHERE department_id = ?", [10], {"datasource": "oracleDB"});
```

## Configuration Examples

See [BoxLang's Defining Datasources](https://boxlang.ortusbooks.com/boxlang-language/syntax/queries#defining-datasources) documentation for full examples on where and how to construct a datasource connection pool.

### Service Name Connection (Recommended)

This is the modern and recommended approach for Oracle connections:

```javascript
this.datasources["oracleDB"] = {
    "driver": "oracle",
    "serviceName": "XEPDB1",              // Oracle Service Name
    "host": "localhost",                  // Default: localhost
    "port": 1521,                         // Default: 1521
    "username": "system",
    "password": "mypassword"
};
```

**Generated JDBC URL**: `jdbc:oracle:thin:@//localhost:1521/XEPDB1`

### SID-Based Connection (Legacy)

For older Oracle installations or legacy systems:

```javascript
this.datasources["legacyDB"] = {
    "driver": "oracle",
    "SID": "ORCL",                        // Oracle System ID
    "host": "oracle-server.company.com",
    "port": 1521,
    "username": "appuser",
    "password": "securepass"
};
```

**Generated JDBC URL**: `jdbc:oracle:thin:@oracle-server.company.com:1521:ORCL`

### Oracle Cloud (OCI) Connection

For Oracle Autonomous Database or Oracle Cloud Infrastructure:

```javascript
this.datasources["cloudDB"] = {
    "driver": "oracle",
    "serviceName": "mydb_high",
    "host": "adb.us-ashburn-1.oraclecloud.com",
    "port": 1522,
    "protocol": "thin",                   // Options: thin, oci, kprb
    "username": "ADMIN",
    "password": "CloudPassword123"
};
```

### Advanced Configuration with Custom Properties

You can specify additional JDBC properties and connection pool settings:

```javascript
this.datasources["advancedDB"] = {
    "driver": "oracle",
    "serviceName": "PRODDB",
    "host": "prod-oracle.example.com",
    "port": 1521,
    "username": "produser",
    "password": "prodpassword",

    // HikariCP Connection Pool Settings
    "properties": {
        "maximumPoolSize": 20,
        "minimumIdle": 5,
        "connectionTimeout": 30000,
        "idleTimeout": 600000,
        "maxLifetime": 1800000
    },

    // Oracle-specific JDBC parameters
    "custom": {
        "oracle.net.CONNECT_TIMEOUT": "10000",
        "oracle.jdbc.ReadTimeout": "30000",
        "v$session.program": "BoxLangApp"
    }
};
```

## Configuration Reference

### Required Properties

| Property | Description | Example |
|----------|-------------|---------|
| `driver` | Must be `"oracle"` | `"oracle"` |
| `username` | Database username | `"system"` |
| `password` | Database password | `"mypassword"` |
| **Either:** | | |
| `serviceName` | Oracle Service Name (recommended) | `"XEPDB1"` |
| **Or:** | | |
| `SID` | Oracle System Identifier (legacy) | `"ORCL"` |

### Optional Properties

| Property | Default | Description |
|----------|---------|-------------|
| `host` | `localhost` | Database server hostname or IP |
| `port` | `1521` | Oracle listener port |
| `protocol` | `thin` | Connection protocol: `thin`, `oci`, or `kprb` |

**Note**: You must provide **either** `serviceName` **or** `SID`, but not both.

### Protocol Options

- **`thin`** (Default): Pure Java JDBC driver, no Oracle Client required
- **`oci`**: Oracle Call Interface, requires Oracle Client installation
- **`kprb`**: Server-side internal driver for stored procedures

## Usage Examples

### Basic Database Operations

```javascript
// Create a table
queryExecute("
    CREATE TABLE employees (
        employee_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        first_name VARCHAR2(50) NOT NULL,
        last_name VARCHAR2(50) NOT NULL,
        email VARCHAR2(100) UNIQUE,
        hire_date DATE DEFAULT SYSDATE,
        salary NUMBER(10,2)
    )
", [], {"datasource": "oracleDB"});

// Insert data
queryExecute("
    INSERT INTO employees (first_name, last_name, email, salary)
    VALUES (?, ?, ?, ?)
", ["John", "Doe", "john.doe@example.com", 75000.00], {"datasource": "oracleDB"});

// Query data with bind parameters
employees = queryExecute("
    SELECT employee_id, first_name, last_name, email, hire_date, salary
    FROM employees
    WHERE salary > ?
    ORDER BY last_name
", [50000], {"datasource": "oracleDB"});

// Update data
queryExecute("
    UPDATE employees
    SET salary = salary * 1.10
    WHERE employee_id = ?
", [1], {"datasource": "oracleDB"});
```

### Working with Transactions

```javascript
try {
    // Begin transaction
    transaction action="begin" {
        // Multiple operations
        queryExecute("
            INSERT INTO employees (first_name, last_name, email, salary)
            VALUES (?, ?, ?, ?)
        ", ["Jane", "Smith", "jane.smith@example.com", 80000], {"datasource": "oracleDB"});

        queryExecute("
            INSERT INTO audit_log (action, performed_by, performed_at)
            VALUES (?, ?, SYSDATE)
        ", ["NEW_EMPLOYEE", "admin"], {"datasource": "oracleDB"});

        // Commit transaction
        transaction action="commit";
    }
} catch (any e) {
    // Rollback on error
    transaction action="rollback";
    rethrow;
}
```

### Using Oracle-Specific Features

```javascript
// Sequences
newId = queryExecute("
    SELECT employee_seq.NEXTVAL as next_id FROM dual
", [], {"datasource": "oracleDB"});

// PL/SQL Blocks
queryExecute("
    BEGIN
        update_employee_salary(?, ?);
    END;
", [employeeId, newSalary], {"datasource": "oracleDB"});

// Oracle Date Functions
recentEmployees = queryExecute("
    SELECT first_name, last_name, hire_date
    FROM employees
    WHERE hire_date >= ADD_MONTHS(SYSDATE, -6)
    ORDER BY hire_date DESC
", [], {"datasource": "oracleDB"});

// Using ROWNUM for pagination
pagedResults = queryExecute("
    SELECT * FROM (
        SELECT e.*, ROWNUM rnum FROM (
            SELECT * FROM employees ORDER BY hire_date DESC
        ) e WHERE ROWNUM <= ?
    ) WHERE rnum > ?
", [20, 10], {"datasource": "oracleDB"});  // Get rows 11-20
```

### Testing with Oracle XE

Perfect for development and testing with Oracle Express Edition:

```javascript
// Application.bx for testing
component {
    this.name = "MyTestApp";

    this.datasources["testDB"] = {
        "driver": "oracle",
        "serviceName": "XEPDB1",
        "host": "localhost",
        "port": 1521,
        "username": "system",
        "password": "testpassword"
    };

    function onApplicationStart() {
        // Create test schema
        queryExecute("
            CREATE TABLE IF NOT EXISTS test_data (
                id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                test_name VARCHAR2(100),
                test_value NUMBER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ", [], {"datasource": "testDB"});

        return true;
    }
}
```

## Development

### Prerequisites

- Java 21+
- BoxLang Runtime 1.3.0+
- Gradle (wrapper included)
- Oracle Database (XE, Standard, or Enterprise)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/ortus-boxlang/bx-oracle.git
cd bx-oracle

# Build the module
./gradlew build

# Run tests (requires Oracle XE running)
./gradlew test

# Create module structure for local testing
./gradlew createModuleStructure
```

### Running Oracle XE with Docker

For local development and testing, use the included Docker Compose configuration:

```bash
# Start Oracle XE container
docker-compose up -d

# Check logs
docker-compose logs -f

# Stop the container
docker-compose down
```

**Default Connection Details:**

- Host: `localhost`
- Port: `1521`
- Service Name: `XEPDB1`
- Username: `system`
- Password: `boxlangrocks`

### Project Structure

```
bx-oracle/
├── src/
│   ├── main/
│   │   ├── bx/
│   │   │   └── ModuleConfig.bx          # Module configuration
│   │   ├── java/
│   │   │   └── ortus/boxlang/modules/
│   │   │       └── oracle/
│   │   │           ├── OracleDriver.java # JDBC driver implementation
│   │   │           └── util/
│   │   │               └── KeyDictionary.java
│   │   └── resources/
│   └── test/
│       ├── java/                        # Unit and integration tests
│       └── resources/
│           ├── boxlang.json            # Test runtime config
│           └── libs/                   # BoxLang runtime JAR
├── build.gradle                         # Build configuration
├── box.json                            # ForgeBox module manifest
├── docker-compose.yaml                 # Oracle XE for testing
└── readme.md                           # This file
```

### Testing

The module includes comprehensive tests:

- **Unit Tests**: Test the Oracle driver implementation directly
- **Integration Tests**: Test the module within the full BoxLang runtime
- **End-to-End Tests**: Verify database operations work correctly

```bash
# Run all tests
./gradlew test

# Run with verbose output
./gradlew test --info

# Run specific test class
./gradlew test --tests "OracleDriverTest"

# Format code
./gradlew spotlessApply
```

### Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for your changes
5. Ensure all tests pass (`./gradlew test`)
6. Format your code (`./gradlew spotlessApply`)
7. Commit your changes (`git commit -m 'Add amazing feature'`)
8. Push to the branch (`git push origin feature/amazing-feature`)
9. Open a Pull Request

## Compatibility

| Module Version | BoxLang Version | Oracle JDBC Version |
|----------------|-----------------|---------------------|
| 1.6.x          | 1.3.0+          | 23.5.0.24.07        |
| 1.5.x          | 1.2.0+          | 21.x                |

## Troubleshooting

### Common Issues

#### Connection refused or timeout

**Solution**: Ensure Oracle listener is running and accessible:
```bash
# Check listener status
lsnrctl status

# Check if port is open
telnet oracle-host 1521

# Verify firewall rules allow connections to port 1521
```

#### Service Name vs SID confusion

**Error**: `Either the serviceName or SID property is required for the Oracle JDBC Driver.`

**Solution**: Provide exactly one connection identifier:
```javascript
// CORRECT - Service Name
"serviceName": "XEPDB1"

// CORRECT - SID
"SID": "ORCL"

// INCORRECT - Missing both
// (no serviceName or SID property)

// INCORRECT - Both specified (SID takes precedence)
"serviceName": "XEPDB1",
"SID": "ORCL"
```

#### Invalid protocol error

**Error**: `The protocol 'xyz' is not valid for the Oracle Driver.`

**Solution**: Use only supported protocols:
```javascript
"protocol": "thin"  // Default, pure Java
"protocol": "oci"   // Requires Oracle Client
"protocol": "kprb"  // Server-side only
```

#### Oracle client libraries not found (OCI)

**Error**: `UnsatisfiedLinkError: no ocijdbc in java.library.path`

**Solution**: The `oci` protocol requires Oracle Instant Client:
1. Download Oracle Instant Client from Oracle's website
2. Set `LD_LIBRARY_PATH` (Linux) or `PATH` (Windows) to include the client location
3. Or use `thin` protocol which doesn't require client libraries

#### Module not loading

**Error**: Module fails to load or driver not registered

**Solution**: Ensure module is properly installed:
```bash
# Rebuild module
./gradlew clean build

# Check module structure
ls -la build/module/libs/

# Verify BoxLang can find the module
install-bx-module bx-oracle --force
```

### Debug Mode

Enable debug logging in your BoxLang application:

```javascript
// In your Application.bx
this.datasources["debugDB"] = {
    "driver": "oracle",
    "serviceName": "XEPDB1",
    "host": "localhost",
    "port": 1521,
    "username": "system",
    "password": "password",
    "logSql": true,
    "logLevel": "DEBUG"
};
```

## Resources

- **Documentation**: [BoxLang Database Guide](https://boxlang.ortusbooks.com/boxlang-language/syntax/queries)
- **Oracle JDBC Documentation**: [Oracle Database JDBC Developer's Guide](https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/)
- **Issues & Support**: [GitHub Issues](https://github.com/ortus-boxlang/bx-oracle/issues)
- **ForgeBox**: [bx-oracle Package](https://forgebox.io/view/bx-oracle)

## Changelog

See [changelog.md](changelog.md) for a complete list of changes and version history.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) for details.

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
