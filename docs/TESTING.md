# PoorCraft Automated Testing Guide

## Overview

PoorCraft ships with a comprehensive automated test suite that validates engine subsystems, resources, Lua mods, networking, and rendering. Tests are written with JUnit 5 and can be executed headlessly on all supported platforms.

## Test Layout

- **`src/test/java/com/poorcraft/test/util/`** – shared testing utilities
  - `TestUtils.java`
  - `HeadlessGameContext.java`
  - `TestReportGenerator.java`
- **`src/test/java/com/poorcraft/test/`** – subsystem test classes
  - `ResourceValidationTest.java`
  - `GameInitializationTest.java`
  - `WorldGenerationTest.java`
  - `ModSystemTest.java`
  - `RenderingSystemTest.java`
  - `NetworkingTest.java`
  - `GameTestRunner.java`

## Running Tests

### Quick Start

- **Windows:**
  ```bat
  scripts\run-tests.bat
  ```
- **Linux/macOS:**
  ```bash
  ./scripts/run-tests.sh
  ```
- **Maven:**
  ```bash
  mvn clean test
  ```

### GameTestRunner

For more control, execute the programmatic runner:
```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.poorcraft.test.GameTestRunner
```

`GameTestRunner` accepts optional arguments:

- `--test <fully.qualified.TestClass>` – run specific test class (repeatable)
- `--filter <regex>` – apply a class name regex filter

## Test Categories

- **ResourceValidationTest** – shader, config, mod, and asset checks
- **GameInitializationTest** – GLFW, OpenGL context, settings, and resource manager
- **WorldGenerationTest** – world, chunk, biome, and feature generation
- **ModSystemTest** – Lua mod loading, event bus, API surface, error handling
- **RenderingSystemTest** – shader compilation, texture atlas, chunk renderer
- **NetworkingTest** – Netty packet serialization, server/client lifecycle, chat, chunk streaming

## Reports

Test executions generate reports under `target/test-reports/`:

- `test-report-<timestamp>.md`
- `test-report-<timestamp>.html`
- `latest-success.txt` or `latest-failure.txt`

The console output also summarizes pass/fail counts. HTML reports include detailed failure stacks and suggested remediation steps.

## Continuous Integration

GitHub Actions workflow `tests.yml` runs the suite on Ubuntu, Windows, and macOS. Artifacts include Maven Surefire logs and generated test reports for inspection.

## Best Practices

- Use fixed seeds in world/mod tests for determinism
- Prefer headless contexts for OpenGL/GLFW validation
- Clean temporary files in `@AfterEach`
- Provide descriptive assertion messages and capture context in `TestReportGenerator`
- When adding new tests, register supporting utilities in `TestUtils`

## Troubleshooting

- **GLFW initialization failure** – ensure display drivers are available; CI runs headless using OSMesa where necessary
- **Shader compilation errors** – check GLSL version directives and brace balancing
- **Networking timeouts** – verify firewall/access permissions; tests use ephemeral ports
- **Gradle/Maven mixed environments** – run `mvn clean` to clear stale build artifacts before retesting

## Related Documentation

- `docs/BUILDING.md`
- `docs/ARCHITECTURE.md`
- `docs/MODDING_GUIDE.md`
