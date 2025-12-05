# BoxLang Oracle Module - AI Agent Instructions

## Project Overview

This is a **BoxLang module** that provides Oracle JDBC driver support. BoxLang modules extend the BoxLang runtime with custom JDBC drivers, BIFs, components, interceptors, and other runtime services through Java ServiceLoader patterns.

**Key Architecture:**
- Hybrid Java + BoxLang (.bx) codebase
- Java implements the JDBC driver (`OracleDriver` extends `GenericJDBCDriver`)
- BoxLang provides module configuration and lifecycle hooks (`ModuleConfig.bx`)
- Gradle-based build with ServiceLoader auto-generation and shadowing
- Module packages as a structured directory with JAR in `libs/` folder

## Critical Development Workflows

### Building & Testing
```bash
# Build the module (creates build/module structure)
./gradlew build

# Run tests (requires BoxLang runtime JAR)
./gradlew test

# Download BoxLang runtime dependency
./gradlew downloadBoxLang

# Format Java code (uses .ortus-java-style.xml)
./gradlew spotlessApply

# Create distribution ZIP
./gradlew zipModuleStructure
```

**Integration tests require Oracle XE running:**
```bash
docker-compose up -d  # Starts Oracle XE on port 1521
# Connection: username=system, password=boxlangrocks, serviceName=XEPDB1
```

### Version Management
```bash
./gradlew bumpMajorVersion  # 1.6.0 -> 2.0.0
./gradlew bumpMinorVersion  # 1.6.0 -> 1.7.0
./gradlew bumpPatchVersion  # 1.6.0 -> 1.6.1
```

Versions update `gradle.properties` and flow to `box.json` and `ModuleConfig.bx` via token replacements during build.

## BoxLang Module Conventions

### Module Structure
```
src/main/
  java/ortus/boxlang/modules/oracle/  # Java implementations
    OracleDriver.java                 # Main JDBC driver
    util/KeyDictionary.java          # Centralized Key definitions
  bx/ModuleConfig.bx                 # Module descriptor & lifecycle
  resources/                          # Packaged resources
build/module/                         # Final module structure
  libs/*.jar                          # Shadow JAR with dependencies
  ModuleConfig.bx                     # Token-replaced config
  box.json                            # Token-replaced metadata
```

### ServiceLoader Registration
The module uses Java ServiceLoader for runtime discovery. Services are auto-registered in `build.gradle`:
```gradle
serviceLoader {
    serviceInterface 'ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver'
    serviceInterface 'ortus.boxlang.runtime.bifs.BIF'
    serviceInterface 'ortus.boxlang.runtime.components.Component'
    // ... other interfaces
}
```
When creating new JDBC drivers, BIFs, or components, they're automatically registered via `META-INF/services` files during compilation.

### BoxLang Runtime Integration
- **Keys**: Use `ortus.boxlang.runtime.scopes.Key` for all string constants (see `KeyDictionary.java`)
- **Structs**: Use `ortus.boxlang.runtime.types.Struct` and `IStruct` for maps
- **Type Casting**: Use `ortus.boxlang.runtime.dynamic.casters.*` classes (e.g., `StringCaster.cast()`)
- **JDBC Drivers**: Extend `GenericJDBCDriver`, implement `buildConnectionURL()`, set `DatabaseDriverType`

### Module Lifecycle (ModuleConfig.bx)
```boxlang
function configure() {
    // Define module settings, objectMappings, datasources, interceptors
}

function onLoad() {
    // Called on module activation
}

function onUnload() {
    // Called on module deactivation
}
```

## Testing Patterns

### BaseIntegrationTest Setup
All integration tests extend `BaseIntegrationTest` which:
1. Initializes `BoxRuntime` with `src/test/resources/boxlang.json` config
2. Loads the module from `build/module/` directory
3. Provides shared context (`ScriptingRequestBoxContext`) and variables scope
4. Reuses runtime instance across tests (@BeforeAll)

**Example test structure:**
```java
public class MyTest extends BaseIntegrationTest {
    @Test
    public void testFeature() {
        runtime.executeSource("/* BoxLang code */", context);
        assertThat(variables.get(result)).isNotNull();
    }
}
```

### Unit vs Integration Tests
- **Unit tests** (e.g., `OracleDriverTest.java`): Test Java classes directly without BoxLang runtime
- **Integration tests** (e.g., `IntegrationTest.java`): Load full module and execute BoxLang scripts

## Project-Specific Patterns

### JDBC Driver Implementation
Oracle driver supports two connection patterns:
1. **SID-based**: `jdbc:oracle:thin:@host:port:SID`
2. **Service Name**: `jdbc:oracle:thin:@//host:port/serviceName`

Both are validated in `buildConnectionURL()` - one must be provided or `IllegalArgumentException` is thrown.

**Protocols**: `thin` (default), `oci`, `kprb` - validated against `AVAILABLE_PROTOCOLS` Struct.

### Token Replacement Strategy
During module build, tokens in `.bx` and `box.json` files are replaced:
- `@build.version@` → `gradle.properties` version
- `@build.number@` → `BUILD_ID` env var (or removed in development branch)

### Branch-Specific Versioning
Development branch automatically gets `-snapshot` suffix in version strings:
```gradle
if (branch == 'development') {
    version = version.replaceAll(/-.*/, '-snapshot')
}
```

### Code Formatting Standards
- **Java**: Eclipse formatter with `.ortus-java-style.xml` (run `./gradlew spotlessApply`)
- **BoxLang**: CFFormat with `.cfformat.json` (CommandBox: `box cfformat run`)
- **Indentation**: 4 spaces (both Java and BoxLang)
- **Max columns**: 115 characters

## Common Gotchas

1. **BoxLang JAR dependency**: Tests require BoxLang runtime JAR in `src/test/resources/libs/` or parallel `../boxlang/build/libs/` directory. Run `./gradlew downloadBoxLang` if missing.

2. **Module loading in tests**: Use `BaseIntegrationTest.loadModule()` to load from `build/module/` - must run `./gradlew build` first.

3. **ServiceLoader updates**: After adding new service implementations, run `./gradlew clean build` to regenerate `META-INF/services` files.

4. **Key usage**: Never use raw strings for BoxLang keys - define in `KeyDictionary.java` or create inline with `new Key("name")`.

5. **Shadow JAR**: Final module uses shadow JAR to bundle dependencies - check `build/module/libs/` for the merged artifact.

## External Dependencies

- **BoxLang Runtime**: Core runtime (compileOnly, dynamically loaded)
- **Oracle JDBC Driver**: Bundled in shadow JAR (check Oracle licensing)
- **JUnit 5**: Testing framework with Truth assertions
- **Spotless**: Code formatting enforcement
- **Shadow Plugin**: JAR merging for module distribution
