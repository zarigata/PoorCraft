# Contributing to PoorCraft

Thank you for your interest in contributing to PoorCraft! This guide will help you get started with contributing to our open-source voxel sandbox game.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Style Guidelines](#code-style-guidelines)
- [JavaDoc Standards](#javadoc-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Modding Contributions](#modding-contributions)
- [Documentation Contributions](#documentation-contributions)
- [Bug Reports](#bug-reports)
- [Feature Requests](#feature-requests)
- [Questions](#questions)
- [License](#license)

## Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors:

- **Be respectful and inclusive** - Treat everyone with respect and consideration
- **Constructive feedback only** - Focus on helping others improve
- **No harassment or discrimination** - Zero tolerance for abusive behavior
- **Follow the MIT License terms** - Respect the open-source license

By contributing, you agree to abide by these principles and the MIT License terms.

## Getting Started

### Prerequisites

- **JDK 17 or higher** - Required for compilation
- **Maven 3.6+** - Build automation tool
- **Git** - Version control

### Initial Setup

1. **Fork the repository** on GitHub
2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/poorcraft.git
   cd poorcraft
   ```
3. **Build the project:**
   ```bash
   mvn clean package
   ```
4. **Run the game:**
   ```bash
   java -jar target/PoorCraft.jar
   ```

For detailed build instructions, see [docs/BUILDING.md](docs/BUILDING.md).

## Development Workflow

1. **Create a feature branch** from main:
   ```bash
   git checkout -b feature/your-feature-name
   ```
   Or for bug fixes:
   ```bash
   git checkout -b fix/bug-description
   ```

2. **Make your changes** and test thoroughly

3. **Commit with clear messages:**
   ```bash
   git commit -m "Add feature X"
   git commit -m "Fix bug Y in Z component"
   ```

4. **Push to your fork:**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create a Pull Request** on GitHub

6. **Wait for review** and address any feedback

## Code Style Guidelines

### Java Code Style

Follow the existing code style demonstrated in the codebase:

- **Indentation:** 4 spaces (no tabs)
- **Braces:** Opening brace on same line
  ```java
  public void method() {
      // code here
  }
  ```
- **Variable names:** Descriptive and camelCase
  ```java
  int chunkLoadDistance = 8;  // Good
  int cld = 8;                 // Bad (except loop counters)
  ```
- **Class names:** PascalCase
  ```java
  public class ChunkManager { }
  ```
- **Constants:** UPPER_SNAKE_CASE
  ```java
  private static final int MAX_RENDER_DISTANCE = 16;
  ```

### Lua Code Style

For Lua mods, follow the style in `gamedata/mods/example_mod/main.lua`:

- **Indentation:** 4 spaces
- **Local variables preferred:**
  ```lua
  local myVariable = 10  -- Good
  myVariable = 10        -- Bad (creates global)
  ```
- **Function names:** snake_case
  ```lua
  function my_helper_function()
      -- code here
  end
  ```
- **Comments:** Explain complex logic
  ```lua
  -- Calculate chunk offset for biome generation
  local offset = (x * 16) + z
  ```

### Personality in Comments

Comments with personality and humor are encouraged! They make the code more enjoyable to read. See examples in `Camera.java`, `World.java`, and `ChunkManager.java`.

```java
// Like a photographer, but for voxels
public class Camera {
    // The camera's position in world space (where we're floating in the void)
    private Vector3f position;
}
```

## JavaDoc Standards

All public classes and methods **must** have JavaDoc documentation:

### Class-Level JavaDoc

```java
/**
 * Manages dynamic chunk loading and unloading based on player position.
 * 
 * <p>This class is responsible for keeping the world around the player loaded
 * while unloading distant chunks to manage memory usage. It uses a multi-threaded
 * approach with 4 worker threads for terrain generation.</p>
 * 
 * @see World
 * @see Chunk
 * @since 1.0.0
 */
public class ChunkManager {
    // ...
}
```

### Method-Level JavaDoc

```java
/**
 * Requests a chunk to be loaded at the specified coordinates.
 * 
 * @param chunkX the X coordinate of the chunk
 * @param chunkZ the Z coordinate of the chunk
 * @return true if the chunk was queued for loading, false if already loaded
 * @throws IllegalStateException if the chunk manager is not initialized
 */
public boolean loadChunk(int chunkX, int chunkZ) {
    // ...
}
```

### Required Tags

- `@param` for all parameters
- `@return` for non-void methods
- `@throws` for checked exceptions
- `@see` for related classes/methods (optional but helpful)
- `@since` for new APIs (optional)

## Testing Requirements

Before submitting your pull request:

1. **Build successfully:**
   ```bash
   mvn clean package
   ```

2. **Test gameplay features:**
   - **Single-player:** World creation, chunk loading, block place/break
   - **Multiplayer:** Connect to server, player synchronization, block updates
   - **UI:** Test at multiple resolutions (800x600, 1920x1080, 3840x2160)

3. **Test mods (if modding API changed):**
   - Verify example mods in `gamedata/mods/` load correctly
   - Check console for "Hi Mod" or similar test messages
   - Ensure no Lua errors

4. **Include testing steps in PR description:**
   ```markdown
   ## Testing
   - Tested world generation with 3 different seeds
   - Verified chunk loading at render distances 8, 12, 16
   - Confirmed no memory leaks over 30 minutes of gameplay
   ```

## Pull Request Guidelines

### PR Title

Use clear, descriptive titles:
- ✅ "Add chunk compression for network packets"
- ✅ "Fix player position desync in multiplayer"
- ❌ "Update stuff"
- ❌ "Changes"

### PR Description

Include:
1. **What** - What does this PR do?
2. **Why** - Why is this change needed?
3. **How** - How did you implement it?
4. **Testing** - How was it tested?
5. **Screenshots/Videos** - For visual changes

Example:
```markdown
## Summary
Adds chunk compression to reduce network bandwidth by ~60%.

## Motivation
Chunk packets are currently ~64KB uncompressed, causing lag on slow connections.

## Implementation
- Added GZIPOutputStream compression in ChunkDataPacket
- Updated PacketDecoder to decompress chunk data
- Maintained backwards compatibility with version flag

## Testing
- Tested with 5 players on LAN
- Measured bandwidth: 64KB → 25KB per chunk
- No performance impact on client/server

## Related Issues
Closes #123
```

### Before Submitting

- [ ] Code compiles without errors
- [ ] No new warnings introduced
- [ ] All tests pass
- [ ] JavaDoc added for new public APIs
- [ ] CHANGELOG.md updated under "Unreleased" section
- [ ] Code follows style guidelines
- [ ] Documentation updated (if adding features)

## Modding Contributions

### New Mods

1. **Create mod directory:** `gamedata/mods/your_mod_name/`
2. **Add mod.json:**
   ```json
   {
       "id": "your_mod_name",
       "name": "Your Mod Name",
       "version": "1.0.0",
       "description": "What your mod does",
       "author": "Your Name",
       "main": "main.lua",
       "enabled": true
   }
   ```
3. **Create main.lua:**
   ```lua
   local mod = {}
   
   function mod.init()
       api.log("Your Mod Name loaded!")
   end
   
   return mod
   ```
4. **Document your mod** in a README.md within the mod folder
5. **Test with multiple world seeds and biomes**

See [docs/MODDING_GUIDE.md](docs/MODDING_GUIDE.md) for complete modding documentation.

## Documentation Contributions

Documentation improvements are always welcome!

- **Location:** `docs/` directory
- **Format:** Markdown
- **Style:** Clear and concise
- **Code examples:** Include when appropriate
- **Table of contents:** Update if adding new sections

### Documentation Types

- **Guides:** Step-by-step tutorials (MODDING_GUIDE.md, BUILDING.md)
- **Reference:** API documentation (API_REFERENCE.md, EVENT_CATALOG.md)
- **Architecture:** System design (ARCHITECTURE.md)
- **How-to:** Specific tasks (DEPLOYMENT.md)

## Bug Reports

Found a bug? Help us fix it!

### Use GitHub Issues

1. **Search existing issues** - Check if already reported
2. **Create new issue** with:
   - **OS:** Windows 10, Ubuntu 22.04, macOS 12, etc.
   - **Java version:** Output of `java -version`
   - **PoorCraft version:** 2.0.0, 0.1.1, etc.
   - **Steps to reproduce:**
     1. Start game
     2. Create world
     3. Walk to coordinates X, Y, Z
     4. Bug occurs
   - **Expected behavior:** What should happen
   - **Actual behavior:** What actually happens
   - **Screenshots/logs:** If applicable

### Example Bug Report

```markdown
**OS:** Windows 11
**Java:** OpenJDK 17.0.2
**Version:** 2.0.0

**Steps to reproduce:**
1. Host multiplayer game
2. Have second player join
3. Break a block
4. Block appears broken on server but not on client

**Expected:** Block breaks on both client and server
**Actual:** Block only breaks on server

**Logs:**
```
[ERROR] BlockBreakPacket deserialization failed: ...
```
```

## Feature Requests

Have an idea for PoorCraft?

### Use GitHub Issues with "enhancement" label

- **Explain the use case** - Why is this feature useful?
- **Describe the benefits** - Who does it help?
- **Consider scope** - Does it fit a voxel sandbox with modding?
- **Reference Plans.md** - Check if already planned

### Example Feature Request

```markdown
**Feature:** Add water physics with flow simulation

**Use Case:** 
Players want to build working canals and waterfalls.

**Benefits:**
- More realistic world interactions
- Creative building possibilities
- Aligns with Minecraft-like gameplay

**Scope:**
Could be complex but highly requested. Maybe start with static water and add flow in v2.1?
```

## Questions

Have questions about PoorCraft development?

1. **Check documentation first:**
   - [README.md](README.md) - Project overview
   - [docs/](docs/) - Comprehensive guides
   - [API_REFERENCE.md](docs/API_REFERENCE.md) - Lua API docs

2. **Search existing GitHub Issues and Discussions**

3. **Ask in GitHub Discussions** - For general questions

4. **Open an Issue** - For specific technical questions

## License

By contributing to PoorCraft, you agree to license your contributions under the **MIT License**.

All contributions will be available under the same MIT License terms as the rest of the project. You retain copyright to your contributions but grant everyone the rights specified in the MIT License.

See the full license in [README.md](README.md).

---

**Thank you for contributing to PoorCraft! Happy coding! ⛏️**

For more information:
- [Building Guide](docs/BUILDING.md)
- [Architecture Documentation](docs/ARCHITECTURE.md)
- [Modding Guide](docs/MODDING_GUIDE.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
