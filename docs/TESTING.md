# PoorCraft Automated Testing Guide

## Unified Test & Run Workflow

The unified scripts `scripts\unified-test-and-run.bat` (Windows) and `./scripts/unified-test-and-run.sh` (Linux/macOS) provide the primary development and release flow. They coordinate a consistent pipeline:

1. Execute the pre-flight test suite via the `quick-tests` Maven profile.
2. Optionally run the full regression suite (`mvn clean verify`) covering Surefire and Failsafe phases.
3. Build development (`dev-build`) or production (`prod-build`) artifacts.
4. Launch the game unless `--test-only` is specified.

### Key Options

- `--mode <dev|prod>` – choose development (JAR) or production (Launch4j) build outputs.
- `--quick-tests` – stop after the pre-flight suite completes.
- `--skip-tests` – reuse existing reports and proceed directly to build/launch steps.
- `--test-only` – run tests/build without launching the game client.
- `--skip-build` – keep existing artifacts when you only need validation.

The scripts emit reports to `target/test-reports/`, log Maven output for each stage, and exit early if any verification step fails.

### Typical Flows

- **Daily development:**
  - Windows: `scripts\unified-test-and-run.bat --mode dev --quick-tests`
  - Linux/macOS: `./scripts/unified-test-and-run.sh --mode dev --quick-tests`
- **Pre-release regression:** run without `--quick-tests` to cover the full Surefire + Failsafe matrix and verify builds for both `--mode dev` and `--mode prod`.
- **CI-style validation:** add `--test-only` to collect test artifacts without launching the game.

## Pre-Flight Test Suite

`PreFlightTestSuite` validates fast-start health checks before a full regression run. It covers UI initialization, render pipeline smoke tests, resource validation, scripting hooks, and window resize debounce logic. The suite executes quickly and feeds reports to `target/test-reports/` using `TestReportGenerator`.

### Running the Suite

- **Via unified script:** add `--quick-tests` to either unified script to run only the pre-flight checks.
- **Direct Maven invocation:**
  ```bat
  mvn -Pquick-tests test
  ```
  ```bash
  mvn -Pquick-tests test
  ```

### Standalone Entry Point

After compiling test classes (e.g., `mvn -q -DskipTests test-compile`), the suite can also be launched directly:

```bash
java com.poorcraft.test.PreFlightTestSuite
```

This path is useful for IDE debugging or scripted integrations that manage the classpath explicitly.

## Test Layout

- **`src/test/java/com/poorcraft/test/util/`** – shared testing utilities
  - `TestUtils.java`
  - `HeadlessGameContext.java`
  - `TestReportGenerator.java`
- **`src/test/java/com/poorcraft/test/`** – subsystem test classes
  - `PreFlightTestSuite.java`
  - `ResourceValidationTest.java`
  - `GameInitializationTest.java`
  - `WorldGenerationTest.java`
  - `ModSystemTest.java`
  - `RenderingSystemTest.java`
  - `NetworkingTest.java`
  - `WindowResizeTest.java` (Failsafe integration test)
  - `GameTestRunner.java`

## Running Tests

### Quick Iteration

Use the unified script with `--quick-tests` for the fastest checks or invoke the Maven profile directly:

- Windows: `scripts\unified-test-and-run.bat --mode dev --quick-tests`
- Linux/macOS: `./scripts/unified-test-and-run.sh --mode dev --quick-tests`
- Maven only: `mvn -Pquick-tests test`

### Full Regression

Remove `--quick-tests` (or run `mvn clean verify`) to execute the entire regression pack. `WindowResizeTest` now runs under the Maven Failsafe plugin during the integration-test phase and is excluded from Surefire, so ensure the verify goal completes before considering the resize workflow signed off.

### Advanced: GameTestRunner

For bespoke test selection or report customization, execute the programmatic runner:

```bash
mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.poorcraft.test.GameTestRunner
```

`GameTestRunner` accepts optional arguments:

- `--test <fully.qualified.TestClass>` – run specific test class (repeatable)
- `--filter <regex>` – apply a class name regex filter

## Test Categories

- **PreFlightTestSuite** – smoke tests for engine boot, assets, scripting, and UI resize debounce
- **ResourceValidationTest** – shader, config, mod, and asset checks
- **GameInitializationTest** – GLFW, OpenGL context, settings, and resource manager
- **WorldGenerationTest** – world, chunk, biome, and feature generation
- **ModSystemTest** – Lua mod loading, event bus, API surface, error handling
- **RenderingSystemTest** – shader compilation, texture atlas, chunk renderer
- **NetworkingTest** – Netty packet serialization, server/client lifecycle, chat, chunk streaming
- **WindowResizeTest** – integration coverage of resize debounce, maximize/restore, and in-game transitions

## Reports

Test executions generate reports under `target/test-reports/`:

- `test-report-<timestamp>.md`
- `test-report-<timestamp>.html`
- `latest-success.txt` or `latest-failure.txt`

The console output also summarizes pass/fail counts. HTML reports include detailed failure stacks and suggested remediation steps.

## Continuous Integration

GitHub Actions workflow `tests.yml` runs the unified pipeline on Ubuntu, Windows, and macOS. Artifacts include Maven Surefire/Failsafe logs and generated test reports for inspection.

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
