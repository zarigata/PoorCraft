# Build Scripts

This directory contains build and execution scripts for PoorCraft.

## Scripts

### build-and-run.bat / build-and-run.sh
Builds the project with Maven and automatically runs the game.

**Usage:**
```bash
# Windows
scripts\build-and-run.bat

# Linux/macOS
chmod +x scripts/build-and-run.sh
scripts/build-and-run.sh
```

### build-exe.bat / build-exe.sh
Builds the project and creates the Windows executable. Shows detailed output.

**Usage:**
```bash
# Windows
scripts\build-exe.bat

# Linux/macOS
chmod +x scripts/build-exe.sh
scripts/build-exe.sh
```

### run-poorcraft.ps1
PowerShell script for building and running PoorCraft on Windows.

**Usage:**
```powershell
scripts\run-poorcraft.ps1
```

## Testing & Playing Scripts

### Quick Play (Development)
For rapid iteration without running tests.

**Windows:**
```bat
scripts\quick-play.bat
```

**PowerShell:**
```powershell
scripts\quick-play.ps1
```

These scripts build the project with tests skipped and immediately launch the game.

### Test & Play (Full Testing)
Run automated tests before launching the game.

**Windows:**
```bat
scripts\test-and-play.bat [options]
```

**PowerShell:**
```powershell
scripts\test-and-play.ps1 [options]
```

**Options:**
- `--skip-tests` / `-SkipTests` – Skip automated tests.
- `--test-only` / `-TestOnly` – Run tests without launching the game.
- `-Verbose` (PowerShell) – Enable detailed Maven output.
- `-OpenReports` (PowerShell) – Open Surefire reports after testing.

**Examples:**
```bat
scripts\test-and-play.bat
scripts\test-and-play.bat --skip-tests
scripts\test-and-play.bat --test-only
```

```powershell
scripts\test-and-play.ps1 -Verbose
scripts\test-and-play.ps1 -TestOnly -OpenReports
```

### Manual Testing
See `docs/MANUAL_TESTING_GUIDE.md` for a full checklist of features to validate manually.

## Script Comparison

| Script | Purpose | Tests | Speed | Use Case |
|--------|---------|-------|-------|----------|
| `quick-play.*` | Fast iteration | ❌ | ⚡ | Development |
| `test-and-play.*` | Full validation | ✅ | 🐢 | Pre-commit, releases |
| `run-tests.*` | Tests only | ✅ | 🐢 | CI/CD |
| `run-poorcraft.*` | Play only | ❌ | ⚡ | End users |

## Notes

- All scripts automatically change to the project root directory
- Scripts check for Java installation before building
- The .bat files are for Windows Command Prompt/PowerShell
- The .sh files are for Linux/macOS/Git Bash on Windows
- Make sure shell scripts are executable: `chmod +x scripts/*.sh`

## Requirements

- Java JDK 17 or higher
- Maven 3.6 or higher
- For shell scripts: Bash-compatible shell

## See Also

- [BUILDING.md](../docs/BUILDING.md) - Detailed build instructions
- [DEPLOYMENT.md](../docs/DEPLOYMENT.md) - Distribution guide
