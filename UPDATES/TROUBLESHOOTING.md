# PoorCraft Troubleshooting Guide

## Build Issues

### "Java not found" or "JAVA_HOME not set"
1. Install JDK 17 or higher from https://adoptium.net/
2. Set the `JAVA_HOME` environment variable
3. Add the Java `bin` directory to `PATH`
4. Verify installation with `java -version`

### "Maven not found"
1. Install Maven from https://maven.apache.org/
2. Add Maven to `PATH`
3. Verify with `mvn -version`

### "Python not found"
1. Install Python 3.8+ from https://python.org/
2. Add Python to `PATH`
3. Verify with `python --version` (or `python3` on Unix)

### "Failed to install Python dependencies"
```bash
cd python
pip install --upgrade pip
pip install -r requirements.txt
```
If specific packages fail:
- `pip install Pillow numpy requests py4j`
- Check the Python version (3.8+ required)
- Use `pip3` instead on Linux/Mac

## Runtime Issues

### "Failed to start Py4J bridge"
1. Ensure Python dependencies are installed (`pip install py4j==0.10.9.7`)
2. Confirm port 25333 is free (firewall rules)
3. Temporarily disable mods (`"enabled": false` in `mods/*/mod.json`)

### "Failed to load font"
- The engine falls back to no-text rendering
- Place a TTF file at `src/main/resources/fonts/default.ttf`
- Recommended: Roboto, Press Start 2P, or any TrueType font

### "Failed to load textures"
- Engine uses solid color placeholders
- Enable `mods/block_texture_generator/` for procedural textures
- Or add 16x16 PNGs under `src/main/resources/textures/blocks/`

### "OpenGL error" or "Failed to create window"
1. Update GPU drivers
2. Ensure OpenGL 3.3+ support
3. Reduce window size in settings
4. Disable vsync (`"vsync": false`)

### "Game crashes on startup"
1. Read the console log for stack traces
2. Disable mods temporarily
3. Delete `config/settings.json` to reset to defaults
4. Run with logging: `java -jar target/poorcraft-*.jar 2>&1 | tee error.log`

### "Black screen / Nothing renders"
- Check console for shader compilation errors
- Verify OpenGL 3.3+ support
- Ensure a world is generated (create one from main menu)
- Confirm chunks are loading (F3 debug overlay)

### "Mods not loading"
1. Check console for Python errors
2. Verify dependencies: `pip list | findstr py4j`
3. Validate `mod.json` files with a JSON linter
4. Ensure `"enabled": true` in each mod

## Mod-Specific Issues

### Procedural Block Texture Generator
- Install Pillow: `pip install Pillow>=10.0.0`
- Install NumPy: `pip install numpy>=1.24.0`
- Validate `mods/block_texture_generator/config.json`
- Lower `max_textures` to 64 if memory is low (creeper-safe!)

### AI NPC
- Use Ollama (`ollama serve`) or provide an API key
- Confirm provider is reachable (`curl http://localhost:11434` for Ollama)
- Ensure `"enabled": true` in the mod config
- Adjust `response_timeout` if requests time out

## Performance Issues

### Low FPS
1. Reduce render distance in settings
2. Disable vsync on high refresh monitors
3. Lower `maxFps`
4. Disable the skin generator mod
5. Close other applications (Minecraft flashbacks!)

### High memory usage
1. Reduce chunk load distance
2. Lower skin generator `max_textures`
3. Increase Java heap: `java -Xmx2G -jar poorcraft-*.jar`

### Stuttering / Lag spikes
1. Increase chunk unload distance
2. Disable structure generation temporarily
3. Disable AI NPC mod if latency spikes

## Shutdown Problems
- Ensure the console shows graceful mod shutdown messages
- Confirm Py4J bridge closes (no leftover Python processes)

## Debug Mode
Press **F3** to view:
- FPS
- Position & chunk coordinates
- Loaded chunk count
- Biome info

## Getting Help
1. Check `logs/` for fresh crash reports
2. Enable verbose logging flags if available
3. Submit issues with:
   - Full console output
   - System specs
   - Steps to reproduce

Stay calm, take breaks, and remember: even villagers sometimes panic. 😉
