# AI Companion Phase 2 Verification Checklist

## Status: ✅ Ready for Test Execution

Use this checklist to validate the native AI Companion system after the Lua mod deprecation.

---

## Phase 0: Unified Testing & Resize Fix

- [ ] `scripts/unified-test-and-run.bat` and `./scripts/unified-test-and-run.sh` present, documented, and honour `--mode`, `--quick-tests`, `--skip-tests`, `--test-only`, `--skip-build`.
- [ ] Windows and Unix scripts both invoke `mvn -Pquick-tests test` for the pre-flight step and write reports to `target/test-reports/`.
- [ ] `PreFlightTestSuite` included in repository, produces HTML/Markdown reports, and runnable via `java com.poorcraft.test.PreFlightTestSuite`.
- [ ] `WindowResizeTest` executes under Maven Failsafe (integration-test phase) and covers single resize, rapid resize bursts, maximize/minimize transitions, and in-game state changes within performance budgets.
- [ ] UIManager/Game blur debounce interval enforced (~150 ms); resize events are coalesced and no crashes occur during rapid resize.
- [ ] Maven profiles `quick-tests`, `dev-build`, and `prod-build` defined and referenced by unified scripts.
- [ ] README, `docs/TESTING.md`, `scripts/README.md`, and `docs/MANUAL_TESTING_GUIDE.md` updated with unified workflow and resize procedures.

---

## Phase 2 Focus Areas

### 1. Provider Setup & Connectivity
- [ ] Configure Ollama (local) and confirm health check succeeds
- [ ] Configure OpenRouter with API key and verify chat completion response
- [ ] Configure Gemini with API key and confirm payload is accepted
- [ ] Validate fallback behavior when preferred provider is unavailable

### 2. Prompt Construction & Reasoning Filters
- [ ] Ensure system prompt is sent as a `system` role (inspect OpenRouter request payload)
- [ ] Confirm reasoning tags (`<reasoning>`, `[reasoning]`, `Reasoning:` lines) are removed from player-facing replies
- [ ] Verify sanitized content remains multiline when needed and does not trim legitimate text

### 3. Action Parsing & Execution
- [ ] Send "Companion follow" and confirm NPC begins following without false triggers from words like "unfollowable"
- [ ] Send "Please stop now" and ensure NPC halts following
- [ ] Request a gather action (e.g., "Gather 12 stone") and observe task assignment with quantity clamped <= 64
- [ ] Use new ModAPI helpers (`npcGatherResource`, `npcMoveToPosition`, `npcBreakBlock`) from a test mod and confirm tasks execute or fail gracefully

### 4. Companion Lifecycle & Autospawn
- [ ] Enable `spawnOnStart` and ensure companion appears at world start near the player
- [ ] Disable `spawnOnStart` and confirm companion remains absent until manually spawned
- [ ] Validate world reload re-establishes companion state without duplicate spawns

### 5. Error Handling & Resilience
- [ ] Disconnect provider network and confirm user-facing error log without crashes
- [ ] Trigger provider JSON parsing failure and ensure system degrades gracefully
- [ ] Confirm chat overlay renders AI replies in standard (non-system) color
- [ ] Validate settings screen note directs players to the AI Companion configuration panel

---

## Regression Sweep
- [ ] Chat overlay still emits ModAPI events for non-system messages
- [ ] Existing Lua mods continue to receive chat callbacks without companion interference
- [ ] Multiplayer chat flow preserves server-provided system flags

---

## Critical Bug Fix Verification

### Implementation
- [ ] Scroll event routing updated to respect overlays and non-game states
- [ ] BlockPreviewRenderer restores scissor, blend, and depth write state after previews
- [ ] Removed duplicate scissor enable call in BlockPreviewRenderer
- [ ] Tree felling logic counts logs above and below the broken segment

### Test
1. **Hotbar Scroll**
   - Enter gameplay with no overlays, scroll mouse wheel → hotbar selection changes each notch
   - Open settings menu, scroll → settings list moves while hotbar remains stable
2. **Pause Menu Visibility**
   - Open inventory, hover preview, close
   - Open pause menu → all buttons visible, no clipping/blue screen
3. **Inventory Rendering**
   - Open inventory after previews
   - Confirm slots, previews, background render normally
4. **Tree Felling Any Log**
   - For a tall tree, break bottom, middle, and top logs across separate trees
   - Verify entire trunk falls and drops match trunk height each time

### Success Criteria
- ✅ Mouse wheel scroll behaves contextually (gameplay vs. UI)
- ✅ UI overlays remain visible after using inventory previews
- ✅ Tree felling removes the full trunk regardless of which segment was broken
- ✅ Automated and manual tests updated/executed as per plan

---

## Documentation & Support Checks
- [ ] README troubleshooting section references AI Companion guide rather than deprecated mods
- [ ] AI companion guide reflects latest provider setup steps and ModAPI helpers

---

## Exit Criteria
- All Phase 2 focus area items checked ✅
- No critical errors logged during verification run
- Test artifacts (logs/screenshots) archived with build output
