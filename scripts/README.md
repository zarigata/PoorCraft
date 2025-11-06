# Build Scripts

This directory contains build, testing, and launch helpers for PoorCraft.

## Recommended: Unified Test & Run

`scripts\unified-test-and-run.bat` and `./scripts/unified-test-and-run.sh` are the primary workflows for day-to-day development and release prep. They combine automated testing, Maven builds, and optional launch steps with consistent reporting.

### Quick Usage

- **Dev + quick tests:**
  - Windows: `scripts\unified-test-and-run.bat --mode dev --quick-tests`
  - Linux/macOS: `./scripts/unified-test-and-run.sh --mode dev --quick-tests`
- **Full regression (pre-release):**
  - Windows: `scripts\unified-test-and-run.bat --mode prod`
  - Linux/macOS: `./scripts/unified-test-and-run.sh --mode prod`
- **Test-only CI flow:** add `--test-only` and optionally `--skip-build` when artifacts already exist.

### Key Options

- `--mode <dev|prod>` – select dev JAR or prod EXE packaging profiles.
- `--quick-tests` – run only the pre-flight (`quick-tests` profile) suite.
- `--skip-tests` – skip automated validation (not recommended for releases).
- `--test-only` – exit after tests/builds without launching the game.
- `--skip-build` – reuse existing artifacts after tests succeed.

The scripts write consolidated artifacts under `target/test-reports/` and enforce Java/Maven prerequisite checks.

## Legacy / Alternative Scripts

These remain for backwards compatibility or niche scenarios but should be considered secondary to the unified workflow.

### build-and-run.bat / build-and-run.sh
Builds the project with Maven and automatically runs the game without pre-flight checks.

```bat
scripts\build-and-run.bat
```
```bash
chmod +x scripts/build-and-run.sh
scripts/build-and-run.sh
```

### build-exe.bat / build-exe.sh
Creates the Windows executable using the production profile.

```bat
scripts\build-exe.bat
```
```bash
chmod +x scripts/build-exe.sh
scripts/build-exe.sh
```

### quick-play.*
Fast iteration scripts that skip tests and launch immediately (`quick-play.bat`, `.ps1`, `.sh`).

### test-and-play.*
Predecessor to the unified flow. Still supports `--skip-tests` / `-SkipTests` and `--test-only` / `-TestOnly` but does not integrate the new Maven profiles.

### run-tests.*
Runs Maven test phases only; use when validating in CI environments without launching the game.

### run-poorcraft.*
Launch-only helpers for players or QA who already have artifacts built.

## Troubleshooting

- Verify Java (JDK 17+) and Maven (3.6+) are on `PATH` before running any script.
- Review `target/test-reports/` for HTML/Markdown summaries, plus `latest-success.txt` or `latest-failure.txt` markers.
- On Unix systems ensure scripts are executable: `chmod +x scripts/*.sh`.
- If Maven reports profile errors, confirm `pom.xml` includes `quick-tests`, `dev-build`, and `prod-build` definitions and rerun with `mvn -Pquick-tests test` manually.

## Notes

- All scripts automatically change to the project root directory.
- Batch (`.bat`) files run on Command Prompt or PowerShell; `.ps1` versions require the appropriate execution policy.
- Shell (`.sh`) scripts support Linux, macOS, and Git Bash on Windows.

## Requirements

- Java JDK 17 or higher
- Maven 3.6 or higher
- Bash-compatible shell for `.sh` scripts

## See Also

- [BUILDING.md](../docs/BUILDING.md) – Detailed build instructions
- [DEPLOYMENT.md](../docs/DEPLOYMENT.md) – Distribution guide
