# Changelog

All notable changes to PoorCraft Refactor will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Multiplayer support
- Advanced terrain generation (Perlin noise)
- Entity system
- Inventory and crafting
- GUI mod manager
- Shader mod support
- Resource pack system
- Sound system
- Particle effects

## [0.1.2] - 2024-01-XX

### Added
- Complete Kotlin-based game engine with LWJGL rendering
- Lua mod system with Luaj integration
- Sandboxed mod execution environment
- Event bus for inter-system communication
- Block registry with texture atlas system
- Chunk management with async loading/saving
- Region file storage for world persistence
- First-run bootstrap with asset extraction
- Texture atlas packer tool
- Comprehensive API documentation
- Example mod with event listeners
- Unit tests for core components
- Integration tests for headless mode
- GitHub Actions CI/CD pipeline
- GraalVM native-image build support
- Portable mode (--portable flag)
- Development mode (--dev-mode flag)
- Headless mode for testing (--headless flag)

### Engine Features
- OpenGL 3.3 core profile rendering
- Chunk-based world with 16×256×16 chunks
- Greedy meshing for efficient rendering
- Face culling (only visible faces rendered)
- Automatic shading (top/side/bottom faces)
- Frustum culling (planned)
- VBO-based mesh rendering
- Texture atlas with UV mapping
- Configurable render distance
- Configurable FOV and window size
- VSync support

### Mod System Features
- Pure JVM Lua interpreter (Luaj)
- Per-mod sandboxed globals
- Restricted file system access
- CPU time limits per tick
- Memory limits per mod
- Automatic mod disabling on errors
- Event registration system
- Scheduled callbacks
- Mod lifecycle hooks (onLoad, onEnable, onDisable, onTick)

### API
- Engine API: event registration, block manipulation, scheduling
- Logger API: info, warn, error logging
- FileAPI: restricted file read/write within mod directory
- Events: onTick, onChunkLoad, onChunkUnload, onBlockPlace, onBlockBreak, onPlayerJoin

### Documentation
- README.md with quick start guide
- API.md with complete Lua API reference
- SKINS.md with texture creation guide
- BUILDING.md with build instructions
- CONTRIBUTING.md with contribution guidelines

### Testing
- Unit tests for BlockRegistry
- Unit tests for Chunk
- Unit tests for EventBus
- Integration test for headless mode
- CI pipeline with automated testing

### Build System
- Gradle Kotlin DSL build configuration
- Multi-module project structure
- GraalVM native-image integration
- Automated testing in CI
- Artifact generation

## [0.1.1] - 2024-01-XX (Hypothetical)

### Added
- Initial project structure
- Basic rendering pipeline
- Simple chunk generation

### Fixed
- Memory leaks in chunk loading
- Texture loading issues

## [0.1.0] - 2024-01-XX (Hypothetical)

### Added
- Initial release
- Basic voxel rendering
- Simple world generation

---

## Version History

### Version Numbering

PoorCraft Refactor follows [Semantic Versioning](https://semver.org/):

- **MAJOR** version: Incompatible API changes
- **MINOR** version: New functionality (backwards-compatible)
- **PATCH** version: Bug fixes (backwards-compatible)

### Release Schedule

- **Major releases**: When significant breaking changes occur
- **Minor releases**: Every 2-3 months with new features
- **Patch releases**: As needed for bug fixes

### Support Policy

- **Current version**: Full support
- **Previous minor version**: Security fixes only
- **Older versions**: No support

---

## Migration Guides

### Migrating to 0.2.0 (Future)

When 0.2.0 is released, migration guides will be provided here.

---

## Deprecation Notices

No deprecations in current version.

---

## Security

### Reporting Security Issues

Please report security vulnerabilities to: security@poorcraft.example.com

Do not open public issues for security vulnerabilities.

### Security Fixes

Security fixes will be released as patch versions and backported to supported versions.

---

## Credits

### Contributors

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for a list of contributors.

### Dependencies

- **LWJGL**: OpenGL bindings for Java
- **Luaj**: Pure Java Lua interpreter
- **Kotlin**: Modern JVM language
- **GraalVM**: Native image compilation
- **JOML**: Java OpenGL Math Library
- **Gson**: JSON serialization
- **SLF4J/Logback**: Logging framework

### Special Thanks

- Minecraft for inspiration
- LWJGL community for excellent documentation
- Kotlin community for language support
- GraalVM team for native-image tooling

---

[Unreleased]: https://github.com/yourrepo/poorcraft-refactor/compare/v0.1.2...HEAD
[0.1.2]: https://github.com/yourrepo/poorcraft-refactor/releases/tag/v0.1.2
