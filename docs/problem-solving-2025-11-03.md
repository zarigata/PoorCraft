# Problem Solving Report — 2025-11-03

## Summary
- `mvn clean package` currently fails because several Surefire tests fail (world generation, mod system, font renderer).
- Building with `-DskipTests` succeeds and produces both `target/PoorCraft.jar` and `target/PoorCraft.exe`.
- Launching `java -jar target\PoorCraft.jar` after the skipped build starts the game successfully (window opens; process remains running until the user closes it).

## Detailed Findings

### 1. World generation feature validation failure (`WorldGenerationTest.testFeatureGeneration`)
- Assertion message: Biome feature thresholds violated for numerous Plains chunks within the 5×5 scan area around the origin.
- Observed chunk sampling: majority Plains with only fringe Snow chunks (see `com.poorcraft.test.WorldGenerationTest.txt`).
- The test expects each encountered biome (Plains/Jungle/Desert/Snow) to expose surface features (trees, cactus, snow layers) within ±4 blocks of surface height.
- Possible causes to investigate:
  - `FeatureGenerator.generateFeatures` may under-sample or skip placements for Plains/Jungle biomes (low probability thresholds, missing RNG seeding, or execution order relative to terrain generation).
  - Terrain height sampling (`World.getHeightAt`) could be returning values that cause surface scans to miss placed features (e.g. vegetation generated above/below scan window).
  - Biome distribution near spawn might rarely produce Jungle/Desert results for the deterministic seed `12345L`, causing expectations to fail; test records that numerous Plains chunks lacked foliage.

### 2. Mod lifecycle regressions (`ModSystemTest`)
- Failing assertions:
  1. `testModLoading`: Expected all discovered mods to end in `ENABLED` or `ERROR`, but at least one remained in another state.
  2. `testModErrorHandling`: `faulty_test_mod` reached `ENABLED` instead of staying in `ERROR/LOADED` after simulated failure.
  3. `testModIsolation`: `api.getSharedData("isolation_alpha_written")` returned `null` instead of `"alpha"`.
- Implications:
  - Lua mod loader may not be running initialization scripts (mod state remains `LOADED`/`DISABLED`).
  - Faulty mod sandboxing or error propagation changed—the loader no longer keeps failing mods from enabling.
  - Shared data bridge (`ModAPI.setSharedData`) might not persist values between mod contexts.
- Next debugging steps:
  - Inspect recent changes to `LuaModLoader`, `LuaModContainer`, and mod bootstrap scripts inside `gamedata/mods/*`.
  - Re-run tests with focused logging (e.g. enable mod loader debug output) to see actual state transitions.

### 3. Font fallback expectation mismatch (`FontRendererTest.testFontRendererFallback`)
- Assertion expected `getTextHeight()` to equal requested font size (16) when an invalid font path is supplied.
- Actual value was `24`, indicating the renderer found a fallback system font and baked a 24px atlas.
- Current implementation attempts multiple fallback candidates (`src/main/resources/fonts/default.ttf`, system fonts). When one succeeds, `useFallback` remains `false`, so metrics reflect the baked atlas (20/24/32 px) rather than the raw input size.
- Decision point: either adjust the test to accept successfully-loaded fallback fonts or change `FontRenderer.init` to mark the renderer as “fallback mode” whenever the requested font is missing, even if another font loads.

### 4. Runtime launch status
- After skipping tests, `mvn clean package -DskipTests` reported `BUILD SUCCESS` and produced:
  - `target/PoorCraft.jar`
  - `target/PoorCraft.exe` (via Launch4j)
- `java -jar target\PoorCraft.jar` logs (truncated):
  - `PoorCraft starting...`
  - `[Main] Initializing game directories...`
  - `[World] Created world with seed: ...`
  - `[UIManager] UIScaleManager initialized ...`
  - `[MenuButton] Button texture ...` (indicates assets loading; truncated but no fatal errors observed)
- Process remains active; to stop the running game, close the game window or interrupt the Java process.

## Recommended Follow-up
1. **World features**: Add instrumentation inside `FeatureGenerator` to confirm whether Plains tree generation is skipped; verify scan window alignment in the test. Consider adjusting spawn seed or increasing iteration radius if distribution rarely produces trees.
2. **Mod loader**: Review initialization order in `HeadlessGameContext.initializeSubsystem("game")`; ensure mods execute their `init.lua` and shared data storage is populated before tests assert state.
3. **Font fallback**: Decide desired behaviour—either relax the test expectation to allow alternate fonts (documented fallback path) or modify `FontRenderer` to flag fallback mode whenever the requested font path is missing.
4. **Continuous builds**: Until failures are fixed, use `mvn clean package -DskipTests` for manual playtests. Restore full test execution before release.
