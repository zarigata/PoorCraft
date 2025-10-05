# Contributing to PoorCraft Refactor

Thank you for your interest in contributing to PoorCraft Refactor! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Process](#development-process)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors.

### Expected Behavior

- Be respectful and considerate
- Accept constructive criticism gracefully
- Focus on what's best for the project
- Show empathy towards other contributors

### Unacceptable Behavior

- Harassment or discriminatory language
- Trolling or insulting comments
- Publishing others' private information
- Any conduct that would be inappropriate in a professional setting

## Getting Started

### Prerequisites

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```powershell
   git clone https://github.com/YOUR_USERNAME/poorcraft-refactor.git
   cd poorcraft-refactor
   ```
3. **Add upstream remote**:
   ```powershell
   git remote add upstream https://github.com/ORIGINAL_OWNER/poorcraft-refactor.git
   ```
4. **Install dependencies**: See [BUILDING.md](docs/BUILDING.md)

### Setting Up Development Environment

1. **Install JDK 17+**
2. **Install IntelliJ IDEA** (recommended) or your preferred IDE
3. **Import project** as Gradle project
4. **Run tests** to verify setup:
   ```powershell
   .\gradlew.bat test
   ```

## Development Process

### Branching Strategy

- `main` - Stable release branch
- `develop` - Development branch (base for features)
- `feature/*` - Feature branches
- `bugfix/*` - Bug fix branches
- `hotfix/*` - Urgent fixes for production

### Workflow

1. **Create a branch**:
   ```powershell
   git checkout develop
   git pull upstream develop
   git checkout -b feature/my-feature
   ```

2. **Make changes** and commit regularly:
   ```powershell
   git add .
   git commit -m "feat: add new feature"
   ```

3. **Keep branch updated**:
   ```powershell
   git fetch upstream
   git rebase upstream/develop
   ```

4. **Push to your fork**:
   ```powershell
   git push origin feature/my-feature
   ```

5. **Create Pull Request** on GitHub

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples**:
```
feat(engine): add chunk frustum culling
fix(mod-loader): prevent infinite loop in mod callbacks
docs(api): update Lua API documentation
test(world): add chunk generation tests
```

## Coding Standards

### Kotlin Style Guide

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// Good
class ChunkManager(
    private val worldDirectory: Path,
    private val blockRegistry: BlockRegistry
) {
    fun loadChunk(x: Int, z: Int): Chunk? {
        // Implementation
    }
}

// Bad
class ChunkManager(private val worldDirectory:Path,private val blockRegistry:BlockRegistry){
    fun loadChunk(x:Int,z:Int):Chunk?{
        // Implementation
    }
}
```

### Code Organization

- **Package structure**: Follow existing package hierarchy
- **File naming**: PascalCase for classes, camelCase for files
- **One class per file**: Except for small related classes
- **Imports**: Organize and remove unused imports

### Documentation

- **KDoc comments** for public APIs:
  ```kotlin
  /**
   * Loads a chunk from disk or generates it if not found.
   *
   * @param x Chunk X coordinate
   * @param z Chunk Z coordinate
   * @return Loaded or generated chunk, or null on error
   */
  fun loadChunk(x: Int, z: Int): Chunk?
  ```

- **Inline comments** for complex logic
- **README updates** for new features

### Best Practices

1. **Null safety**: Use Kotlin's null safety features
   ```kotlin
   // Good
   val chunk: Chunk? = loadChunk(x, z)
   chunk?.let { processChunk(it) }
   
   // Bad
   val chunk = loadChunk(x, z)!!
   ```

2. **Immutability**: Prefer `val` over `var`
   ```kotlin
   // Good
   val config = EngineConfig.load(path)
   
   // Bad
   var config = EngineConfig.load(path)
   ```

3. **Error handling**: Use proper exception handling
   ```kotlin
   // Good
   try {
       loadChunk(x, z)
   } catch (e: IOException) {
       logger.error("Failed to load chunk", e)
   }
   ```

4. **Resource management**: Use `use` for auto-closing
   ```kotlin
   // Good
   Files.newInputStream(path).use { input ->
       // Read from input
   }
   ```

## Testing

### Writing Tests

- **Unit tests** for individual components
- **Integration tests** for system interactions
- **Test naming**: Descriptive, using backticks for readability

```kotlin
@Test
fun `should load chunk from disk when file exists`() {
    // Arrange
    val manager = ChunkManager(testDir, blockRegistry)
    
    // Act
    val chunk = manager.loadChunk(0, 0)
    
    // Assert
    assertNotNull(chunk)
    assertEquals(0, chunk.x)
}
```

### Running Tests

```powershell
# All tests
.\gradlew.bat test

# Specific module
.\gradlew.bat :engine:test

# Specific test
.\gradlew.bat :engine:test --tests "ChunkTest"

# With coverage
.\gradlew.bat test jacocoTestReport
```

### Test Coverage

- Aim for **80%+ coverage** for new code
- **100% coverage** for critical paths (world saving, mod sandboxing)
- View coverage reports: `build/reports/jacoco/test/html/index.html`

## Submitting Changes

### Pull Request Process

1. **Update documentation** if needed
2. **Add tests** for new functionality
3. **Ensure all tests pass**:
   ```powershell
   .\gradlew.bat test
   ```
4. **Update CHANGELOG.md** (if applicable)
5. **Create Pull Request** with clear description

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
How was this tested?

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] All tests pass
- [ ] No new warnings
```

### Review Process

- **Maintainer review**: At least one maintainer must approve
- **CI checks**: All automated checks must pass
- **Address feedback**: Respond to review comments
- **Squash commits**: May be required before merge

## Reporting Bugs

### Before Submitting

1. **Check existing issues**: Avoid duplicates
2. **Test on latest version**: Ensure bug still exists
3. **Gather information**: Logs, screenshots, steps to reproduce

### Bug Report Template

```markdown
**Describe the bug**
Clear description of the bug

**To Reproduce**
Steps to reproduce:
1. Go to '...'
2. Click on '...'
3. See error

**Expected behavior**
What should happen

**Screenshots**
If applicable

**Environment**
- OS: [e.g., Windows 11]
- Java version: [e.g., 17.0.5]
- PoorCraft version: [e.g., 0.1.2]

**Logs**
Paste relevant log output

**Additional context**
Any other information
```

## Feature Requests

### Before Submitting

1. **Check roadmap**: Feature may already be planned
2. **Search existing requests**: Avoid duplicates
3. **Consider scope**: Is it appropriate for this project?

### Feature Request Template

```markdown
**Is your feature request related to a problem?**
Clear description of the problem

**Describe the solution you'd like**
What you want to happen

**Describe alternatives you've considered**
Other solutions you've thought about

**Additional context**
Mockups, examples, etc.
```

## Areas for Contribution

### High Priority

- Performance optimizations
- Bug fixes
- Documentation improvements
- Test coverage

### Feature Ideas

- Multiplayer support
- Advanced terrain generation
- Entity system
- GUI improvements
- Shader effects

### Good First Issues

Look for issues labeled `good first issue` on GitHub.

## Development Tips

### Debugging

```kotlin
// Use SLF4J logger
private val logger = LoggerFactory.getLogger(MyClass::class.java)

logger.debug("Debug info: {}", variable)
logger.info("Info message")
logger.warn("Warning")
logger.error("Error occurred", exception)
```

### Performance Profiling

```powershell
# Profile build
.\gradlew.bat build --profile

# Run with profiler
.\gradlew.bat :launcher:run -Dgraal.ProfilerEnabled=true
```

### Hot Reload

```powershell
# Terminal 1: Run in dev mode
.\gradlew.bat :launcher:runDev

# Terminal 2: Continuous build
.\gradlew.bat -t classes
```

## Getting Help

- **Discord**: Join our community server
- **GitHub Discussions**: Ask questions
- **Documentation**: Check docs/ directory
- **Stack Overflow**: Tag with `poorcraft`

## Recognition

Contributors are recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project README

Thank you for contributing to PoorCraft Refactor!
