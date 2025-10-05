# Discord Rich Presence Setup Guide

This guide explains how to set up Discord Rich Presence for PoorCraft so your Discord friends can see what you're doing in-game.

## What is Discord Rich Presence?

Discord Rich Presence allows games to display detailed information on your Discord profile, including:
- What biome you're exploring (Desert, Snow, Jungle, or Plains)
- Whether you're in singleplayer or multiplayer
- The game version
- How long you've been playing
- The game's logo

## Prerequisites

1. **Discord Application**: You need Discord installed and running on your computer
2. **Discord Developer Account**: Required to create an application
3. **Maven**: For downloading the Discord RPC library
4. **Internet Connection**: For the initial setup

## Step 1: Create a Discord Application

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **"New Application"** button
3. Name it **"PoorCraft"** (or whatever you want)
4. Click **"Create"**

You should now see your application dashboard with an **Application ID** - this is important!

## Step 2: Upload Game Assets

Discord needs images to display on your profile. We'll upload the PoorCraft logo and optionally biome icons.

### Upload Main Logo

1. In your Discord application, go to **"Rich Presence" → "Art Assets"**
2. Click **"Add Image(s)"**
3. Upload the logo from: `src/main/resources/images/logo.png`
4. Name this asset: **`poorcraft_logo`** (this exact name is important!)
5. Click **"Save Changes"**

### Upload Biome Icons (Optional but Recommended)

To show different biome icons, upload these and name them exactly as shown:

- **desert_biome** - An icon representing desert (sandy/cactus themed)
- **snow_biome** - An icon representing snow (icy/snowy themed)
- **jungle_biome** - An icon representing jungle (green/tropical themed)
- **plains_biome** - An icon representing plains (grassy themed)

You can create simple 512x512 PNG images for these or use generated textures from the game.

## Step 3: Configure PoorCraft

Now we need to tell PoorCraft about your Discord application.

1. Open `src/main/java/com/poorcraft/discord/DiscordRichPresenceManager.java`
2. Find this line near the top:
   ```java
   private static final String APPLICATION_ID = "1234567890123456789";  // TODO: Replace with actual Discord App ID
   ```
3. Replace `"1234567890123456789"` with your **Application ID** from Step 1
4. Optionally update the download URL:
   ```java
   private static final String DOWNLOAD_URL = "https://github.com/yourproject/poorcraft";  // TODO: Update with actual URL
   ```

## Step 4: Build the Project

Make sure Maven downloads the Discord RPC library:

```powershell
mvn clean install
```

If you encounter dependency issues, you may need to:
1. Clear your Maven cache: `Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\com\github\MinnDevelopment"`
2. Try again: `mvn clean install`

## Step 5: Test It Out!

1. **Launch Discord** (must be running BEFORE you start the game)
2. **Run PoorCraft**: Use your build script or `mvn exec:java`
3. **Check your Discord profile** - you should see:
   - "Playing PoorCraft"
   - The PoorCraft logo
   - Your current status (In Main Menu, Exploring Desert, etc.)

## Troubleshooting

### "Discord Rich Presence disabled" message

**Problem**: The game prints "Discord Rich Presence disabled" on startup.

**Solutions**:
- Make sure Discord is running BEFORE you start PoorCraft
- Check that your Application ID is correct in the code
- Verify the Discord RPC library was downloaded correctly

### No Rich Presence showing on profile

**Problem**: Discord is running, game says RPC is enabled, but nothing shows.

**Solutions**:
- In Discord settings, go to **Activity Settings → Activity Privacy**
- Make sure **"Display current activity as a status message"** is enabled
- Restart both Discord and PoorCraft

### Maven dependency errors

**Problem**: Maven can't download the Discord RPC library.

**Solutions**:
- Check your internet connection
- Try using a different repository in `pom.xml`:
  ```xml
  <!-- Alternative: Use JitPack -->
  <dependency>
      <groupId>com.github.MinnDevelopment</groupId>
      <artifactId>java-discord-rpc</artifactId>
      <version>v2.0.2</version>
  </dependency>
  ```
- Or use the native library:
  ```xml
  <!-- Alternative: discord-ipc by jagrosh -->
  <dependency>
      <groupId>com.github.jagrosh</groupId>
      <artifactId>DiscordIPC</artifactId>
      <version>0.5</version>
  </dependency>
  ```

### Assets not showing (broken image icon)

**Problem**: Discord shows a broken image instead of your logo.

**Solutions**:
- Double-check the asset name in Discord Developer Portal matches exactly: `poorcraft_logo`
- Asset names are case-sensitive!
- Wait a few minutes after uploading - Discord caches assets
- Make sure the asset was saved (click "Save Changes" in the portal)

### "javarpc.DiscordLibrary not found" or similar errors

**Problem**: Native library errors on startup.

**Solutions**:
- The Discord RPC library includes native binaries for Windows/Mac/Linux
- Make sure your Maven build completed successfully
- Try cleaning and rebuilding: `mvn clean install`

## What's Displayed

Based on your game state, Discord will show:

### Main Menu
- **Details**: "In Main Menu"
- **State**: "Deciding what to do"
- **Large Image**: PoorCraft logo
- **Elapsed Time**: Yes

### Creating World
- **Details**: "Creating New World"
- **State**: "Generating terrain..."
- **Large Image**: PoorCraft logo
- **Elapsed Time**: Yes

### In-Game (Exploring)
- **Details**: "Exploring [Biome Name]" (e.g., "Exploring Desert")
- **State**: "Playing Singleplayer" or "Playing Multiplayer"
- **Large Image**: PoorCraft logo
- **Small Image**: Biome icon (if uploaded)
- **Elapsed Time**: Yes

### Paused
- **Details**: "Game Paused"
- **State**: "Taking a break"
- **Large Image**: PoorCraft logo
- **Elapsed Time**: Yes (continues from game start)

### Multiplayer Connecting
- **Details**: "Joining Server"
- **State**: "Connecting to [server address]"
- **Large Image**: PoorCraft logo
- **Elapsed Time**: Yes

## Technical Details

### How It Works

1. **Initialization**: When the game starts, it connects to Discord's local RPC server
2. **Updates**: Every 2 seconds, the game checks if your state or biome changed
3. **Callbacks**: Discord callbacks are processed every frame to maintain the connection
4. **Cleanup**: When you quit, the game properly disconnects from Discord

### Biome Detection

The game automatically detects which biome you're in based on your player position:
- Uses `World.getBiome(x, z)` to query the current biome
- Updates Discord when you cross biome boundaries
- Only updates if the biome actually changed (to avoid spam)

### Performance Impact

Discord Rich Presence has minimal performance impact:
- **Initialization**: ~50ms one-time cost on game startup
- **Runtime**: ~1ms per frame for callbacks (negligible)
- **Updates**: Only when state/biome changes, max every 2 seconds
- **Memory**: ~2MB additional memory usage

## Advanced Customization

Want to customize what's displayed? Edit `DiscordRichPresenceManager.java`:

### Change Update Text

Look for methods like `updateInGame()`, `updateMainMenu()`, etc.

Example - change the main menu text:
```java
public void updateMainMenu() {
    if (!initialized) return;
    
    presence.details = "Chilling in Main Menu";  // Changed!
    presence.state = "About to go mining";        // Changed!
    presence.smallImageKey = "";
    presence.smallImageText = "";
    
    lib.Discord_UpdatePresence(presence);
}
```

### Add More Information

You can add buttons (requires Discord RPC v2+):
```java
// In init() method, add:
presence.buttons = new String[]{"Download Game", "Join Discord"};
```

Note: Button URLs must be configured in the Discord Developer Portal.

### Change Update Frequency

In `Game.java`, find this line:
```java
if (discordUpdateTimer >= 2.0f) {  // Update every 2 seconds
```

Change `2.0f` to update more or less frequently (in seconds).

## Disabling Rich Presence

Don't want Discord integration? Just comment out this section in `Game.java`:

```java
// discordRPC = new DiscordRichPresenceManager();
// if (discordRPC.init()) {
//     System.out.println("[Game] Discord Rich Presence enabled");
//     discordRPC.updateMainMenu();
// }
```

Or set a config option to disable it at runtime (recommended for public releases).

## FAQ

**Q: Does this work on all platforms?**  
A: Yes! Works on Windows, macOS, and Linux. The library includes native binaries for all platforms.

**Q: Can other players see my Rich Presence?**  
A: Only your Discord friends can see it, just like any Discord status.

**Q: Does it work offline?**  
A: No, Discord Rich Presence requires Discord to be running and connected to the internet.

**Q: Will this slow down my game?**  
A: No, the performance impact is negligible (< 1ms per frame).

**Q: Can I use custom images for each biome?**  
A: Yes! Upload them to Discord Developer Portal and name them correctly (see Step 2).

**Q: Does Discord need to be running when I compile?**  
A: No, Discord only needs to be running when you PLAY the game.

**Q: I'm getting "Discord not found" errors**  
A: Discord must be running before you launch PoorCraft. Start Discord first, then start the game.

## Resources

- [Discord Developer Portal](https://discord.com/developers/applications)
- [Discord Rich Presence Documentation](https://discord.com/developers/docs/rich-presence/how-to)
- [java-discord-rpc GitHub](https://github.com/MinnDevelopment/java-discord-rpc)

## Support

If you're still having issues:
1. Check the console output for error messages
2. Verify your Application ID is correct
3. Make sure Discord is running
4. Try restarting both Discord and PoorCraft
5. Check that Discord's Activity Privacy settings allow Rich Presence

---

**Made by poor people, for poor people.** Now with Discord bragging rights! 💪
