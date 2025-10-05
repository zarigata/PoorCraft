# PoorCraft Quick Start Guide

## Prerequisites

- **Java 17+** (https://adoptium.net/)
- **Maven 3.6+** (https://maven.apache.org/)
- **Python 3.8+** (https://python.org/)
- **OpenGL 3.3+ GPU**

## Installation

### Windows
1. Clone or download the repository.
2. Run the script:
   ```cmd
   build-and-run.bat
   ```
3. Wait for Maven to finish; the game launches automatically.

### Linux / macOS
1. Clone or download the repository.
2. Make the script executable:
   ```bash
   chmod +x build-and-run.sh
   ```
3. Execute it:
   ```bash
   ./build-and-run.sh
   ```
4. Wait for the build; the game launches automatically.

### Manual Build
```bash
cd python
pip install -r requirements.txt
cd ..
mvn clean package
java -jar target/poorcraft-0.1.0-SNAPSHOT-jar-with-dependencies.jar
```

## First Launch
1. The main menu should appear.
2. Open **Singleplayer**.
3. Create a new world (seed optional).
4. Wait for world generation (5–10 seconds).
5. Explore!

## Controls
- **Movement**: `WASD`
- **Jump / Fly up**: `Space`
- **Sneak / Fly down**: `Shift`
- **Sprint**: `Ctrl`
- **Mouse Look**: Move mouse
- **Pause**: `Esc`
- **Debug Overlay**: `F3`

## Mods to Try
- **Procedural Block Texture Generator**: Enable in `mods/block_texture_generator/mod.json` (`"enabled": true`). Generates procedural block textures.
- **AI NPC**: Requires Ollama or an API key. Configure `mods/ai_npc/config.json`.

## Multiplayer (Experimental)
- **Host**: Main Menu → Multiplayer → Host Game.
- **Join**: Main Menu → Multiplayer → Direct Connect → enter `IP:port`.

## Configuration
Runtime settings stored in `config/settings.json` (created on first launch).
- `graphics.renderDistance`: 4–16 chunks.
- `graphics.maxFps`: `0` for uncapped, or a specific limit.
- `controls.mouseSensitivity`: 0.01–0.5.
- `world.seed`: 0 for random or set a value.

## Troubleshooting
Refer to `TROUBLESHOOTING.md` for solutions to build, runtime, and performance issues. Bring snacks; debugging is hungry work.

## Next Steps
- Read `docs/MODDING_GUIDE.md` to build custom mods.
- Tweak settings for performance.
- Share worlds with friends (remember to open ports!).

Have fun, and watch out for creepers even if we're not exactly Minecraft. Yet.
