# Deploying PoorCraft

This guide covers packaging, distribution, and deployment of PoorCraft for various platforms and distribution channels.

## Table of Contents

- [Overview](#overview)
- [Distribution Formats](#distribution-formats)
- [Building for Distribution](#building-for-distribution)
- [Platform-Specific Packaging](#platform-specific-packaging)
- [Steam Integration](#steam-integration)
- [Version Management](#version-management)
- [Release Checklist](#release-checklist)
- [Continuous Deployment](#continuous-deployment)
- [Update Distribution](#update-distribution)
- [Mod Distribution](#mod-distribution)
- [Licensing](#licensing)
- [Support](#support)
- [Analytics](#analytics)
- [Security](#security)
- [Post-Release](#post-release)
- [Steam-Specific Requirements](#steam-specific-requirements)
- [Distribution Channels](#distribution-channels)
- [File Sizes](#file-sizes)
- [System Requirements](#system-requirements)
- [Next Steps](#next-steps)

## Overview

PoorCraft can be distributed in multiple formats for different platforms and audiences. This guide covers best practices for packaging and releasing the game.

**Distribution Options:**
- Fat JAR (universal)
- Windows executable
- Linux packages (.deb, AppImage)
- macOS application bundle
- Steam release

## Distribution Formats

### Fat JAR (Universal)

**File:** `target/PoorCraft.jar`  
**Size:** ~15-20MB  
**Platform:** All (Windows, Linux, macOS)  
**Requirements:** Java 17+ installed  
**Best For:** Developers, Linux users, macOS users

**Run Command:**
```bash
java -jar PoorCraft.jar
```

**Advantages:**
- Cross-platform
- Single file
- No platform-specific builds

**Disadvantages:**
- Requires Java installation
- Less integrated with OS

### Windows Executable

**File:** `target/PoorCraft.exe`  
**Size:** ~15-20MB  
**Platform:** Windows only  
**Requirements:** Java 17+ (exe checks and prompts if missing)  
**Best For:** Windows end-users

**Run Command:**
```bash
PoorCraft.exe
```
Or double-click in Windows Explorer.

**Advantages:**
- Native Windows integration
- Checks for Java automatically
- Professional appearance
- Can be code-signed

**Disadvantages:**
- Windows only
- Still requires Java runtime

**Creation:**
Built automatically by Launch4j Maven plugin during `mvn package`.

### Steam Release

**Package:** JAR/EXE + gamedata/ + assets/  
**Platform:** Separate builds for Windows, Linux, macOS  
**Requirements:** Steamworks SDK integration  
**Best For:** Official Steam distribution

## Building for Distribution

### Step 1: Clean Build

Ensure a clean build environment:

```bash
mvn clean package
```

### Step 2: Test the Build

Before distribution, thoroughly test:

1. **Run the game:**
   ```bash
   java -jar target/PoorCraft.jar
   ```

2. **Test all features:**
   - World creation with different seeds
   - Single-player gameplay
   - Multiplayer (host and join)
   - Settings menu
   - Mod loading (check console for "Hi Mod" messages)

3. **Test on target platforms:**
   - Windows 10, Windows 11
   - Ubuntu 22.04, Fedora, Arch
   - macOS 12+, Apple Silicon

### Step 3: Prepare Distribution Package

Create the distribution structure:

```
PoorCraft-2.0.0/
├── PoorCraft.jar (or PoorCraft.exe for Windows)
├── README.md
├── LICENSE.txt
├── CHANGELOG.md
├── gamedata/
│   └── mods/
│       ├── example_mod/
│       ├── hi_mod/
│       └── realtime_sync/
└── assets/
    └── ui/
```

**Include:**
- Executable (JAR or EXE)
- Documentation (README, LICENSE, CHANGELOG)
- Example mods
- UI assets

**Exclude:**
- Source code (unless open-source release)
- Build artifacts (pom.xml, src/)
- Development tools

## Platform-Specific Packaging

### Windows

**Recommended Format:** Executable + Installer

#### Using Inno Setup (Installer)

1. **Install Inno Setup:** [https://jrsoftware.org/isinfo.php](https://jrsoftware.org/isinfo.php)

2. **Create installer script** (`installer.iss`):
   ```innosetup
   [Setup]
   AppName=PoorCraft
   AppVersion=2.0.0
   DefaultDirName={autopf}\PoorCraft
   DefaultGroupName=PoorCraft
   OutputDir=dist
   OutputBaseFilename=PoorCraft-2.0.0-Setup
   Compression=lzma2
   SolidCompression=yes
   
   [Files]
   Source: "target\PoorCraft.exe"; DestDir: "{app}"
   Source: "gamedata\*"; DestDir: "{app}\gamedata"; Flags: recursesubdirs
   Source: "assets\*"; DestDir: "{app}\assets"; Flags: recursesubdirs
   Source: "README.md"; DestDir: "{app}"
   Source: "LICENSE.txt"; DestDir: "{app}"
   
   [Icons]
   Name: "{group}\PoorCraft"; Filename: "{app}\PoorCraft.exe"
   Name: "{autodesktop}\PoorCraft"; Filename: "{app}\PoorCraft.exe"
   ```

3. **Compile installer:**
   ```bash
   iscc installer.iss
   ```

#### Code Signing (Recommended)

Purchase code signing certificate and sign the executable:

```bash
signtool sign /f certificate.pfx /p password /t http://timestamp.digicert.com target\PoorCraft.exe
```

**Benefits:**
- Windows SmartScreen won't warn users
- Professional appearance
- Required for Steam

#### JRE Bundling (Optional)

Bundle JRE for users without Java:

1. Download JRE 17 from [Adoptium](https://adoptium.net/)
2. Include in distribution:
   ```
   PoorCraft-2.0.0/
   ├── PoorCraft.exe
   ├── jre/
   │   └── (JRE files)
   └── ...
   ```
3. Modify Launch4j to use bundled JRE

**Pros:** Users don't need to install Java  
**Cons:** Distribution size increases to ~100-150MB

### Linux

**Recommended Formats:** .deb package, AppImage

#### Debian Package (.deb)

1. **Create package structure:**
   ```bash
   mkdir -p poorcraft-2.0.0/DEBIAN
   mkdir -p poorcraft-2.0.0/usr/share/poorcraft
   mkdir -p poorcraft-2.0.0/usr/share/applications
   mkdir -p poorcraft-2.0.0/usr/share/pixmaps
   ```

2. **Copy files:**
   ```bash
   cp target/PoorCraft.jar poorcraft-2.0.0/usr/share/poorcraft/
   cp -r gamedata poorcraft-2.0.0/usr/share/poorcraft/
   cp -r assets poorcraft-2.0.0/usr/share/poorcraft/
   ```

3. **Create control file** (`poorcraft-2.0.0/DEBIAN/control`):
   ```
   Package: poorcraft
   Version: 2.0.0
   Section: games
   Priority: optional
   Architecture: amd64
   Depends: openjdk-17-jre
   Maintainer: Zarigata
   Description: Open-source voxel sandbox game
    PoorCraft is a Minecraft-inspired game with Lua modding and multiplayer.
   ```

4. **Create .desktop file** (`poorcraft-2.0.0/usr/share/applications/poorcraft.desktop`):
   ```ini
   [Desktop Entry]
   Type=Application
   Name=PoorCraft
   Comment=Open-source voxel sandbox game
   Exec=/usr/share/poorcraft/poorcraft.sh
   Icon=poorcraft
   Terminal=false
   Categories=Game;
   ```

5. **Create launcher script** (`poorcraft-2.0.0/usr/share/poorcraft/poorcraft.sh`):
   ```bash
   #!/bin/bash
   cd /usr/share/poorcraft
   java -jar PoorCraft.jar
   ```

6. **Build package:**
   ```bash
   dpkg-deb --build poorcraft-2.0.0
   ```

#### AppImage (Portable)

AppImage is a portable format that works on any Linux distribution.

Use [appimagetool](https://github.com/AppImage/AppImageKit) to create:

```bash
# Create AppDir structure
mkdir -p PoorCraft.AppDir/usr/bin
mkdir -p PoorCraft.AppDir/usr/share

# Copy files
cp target/PoorCraft.jar PoorCraft.AppDir/usr/bin/
cp -r gamedata PoorCraft.AppDir/usr/share/
cp -r assets PoorCraft.AppDir/usr/share/

# Create AppRun script
# ... (launcher script)

# Build AppImage
appimagetool PoorCraft.AppDir
```

### macOS

**Recommended Format:** .app bundle

#### Create Application Bundle

1. **Create bundle structure:**
   ```bash
   mkdir -p PoorCraft.app/Contents/MacOS
   mkdir -p PoorCraft.app/Contents/Resources
   mkdir -p PoorCraft.app/Contents/Resources/gamedata
   mkdir -p PoorCraft.app/Contents/Resources/assets
   ```

2. **Copy files:**
   ```bash
   cp target/PoorCraft.jar PoorCraft.app/Contents/Resources/
   cp -r gamedata/* PoorCraft.app/Contents/Resources/gamedata/
   cp -r assets/* PoorCraft.app/Contents/Resources/assets/
   ```

3. **Create Info.plist** (`PoorCraft.app/Contents/Info.plist`):
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
   <plist version="1.0">
   <dict>
       <key>CFBundleName</key>
       <string>PoorCraft</string>
       <key>CFBundleDisplayName</key>
       <string>PoorCraft</string>
       <key>CFBundleIdentifier</key>
       <string>com.poorcraft.game</string>
       <key>CFBundleVersion</key>
       <string>2.0.0</string>
       <key>CFBundlePackageType</key>
       <string>APPL</string>
       <key>CFBundleExecutable</key>
       <string>poorcraft</string>
   </dict>
   </plist>
   ```

4. **Create launcher script** (`PoorCraft.app/Contents/MacOS/poorcraft`):
   ```bash
   #!/bin/bash
   cd "$(dirname "$0")/../Resources"
   java -jar PoorCraft.jar
   ```

5. **Make executable:**
   ```bash
   chmod +x PoorCraft.app/Contents/MacOS/poorcraft
   ```

#### Code Signing & Notarization

For distribution, macOS requires code signing and notarization:

1. **Sign with Apple Developer certificate:**
   ```bash
   codesign --force --deep --sign "Developer ID Application: Your Name" PoorCraft.app
   ```

2. **Notarize with Apple:**
   ```bash
   xcrun altool --notarize-app --file PoorCraft.zip --primary-bundle-id com.poorcraft.game --username your@email.com --password @keychain:AC_PASSWORD
   ```

3. **Staple notarization ticket:**
   ```bash
   xcrun stapler staple PoorCraft.app
   ```

**Required:** Apple Developer account ($99/year)

## Steam Integration

### Prerequisites

- Steam Partner account
- Steamworks SDK
- App ID from Steam
- Completed store page

### Integration Steps

1. **Add Steamworks4j dependency** to `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.code-disaster.steamworks4j</groupId>
       <artifactId>steamworks4j</artifactId>
       <version>1.9.0</version>
   </dependency>
   ```

2. **Initialize Steam API** in `Main.java`:
   ```java
   if (!SteamAPI.init()) {
       System.err.println("Failed to initialize Steam API");
   }
   ```

3. **Add Steam features** (optional):
   - Achievements
   - Cloud saves
   - Workshop (for mods)
   - Rich presence

4. **Create `steam_appid.txt`** with your App ID:
   ```
   123456
   ```

5. **Test with Steam client** in development mode

### Steam Build Configuration

#### Depot Structure

Create separate depots for each platform:

**Windows Depot:**
```
depot_windows/
├── PoorCraft.exe
├── steam_api64.dll
├── gamedata/
└── assets/
```

**Linux Depot:**
```
depot_linux/
├── PoorCraft.jar
├── launch.sh
├── libsteam_api.so
├── gamedata/
└── assets/
```

**macOS Depot:**
```
depot_macos/
├── PoorCraft.app/
├── libsteam_api.dylib
├── gamedata/
└── assets/
```

#### Build Script

Create build scripts for Steam using `steamcmd`:

```bash
steamcmd +login username +run_app_build app_build.vdf +quit
```

See [Steamworks Documentation](https://partner.steamgames.com/doc/sdk/uploading) for details.

## Version Management

### Semantic Versioning

PoorCraft follows [Semantic Versioning](https://semver.org/):

**Format:** `MAJOR.MINOR.PATCH`

- **MAJOR:** Breaking changes, major refactors (2.0.0)
- **MINOR:** New features, backwards compatible (2.1.0)
- **PATCH:** Bug fixes, small improvements (2.0.1)

### Version Update Checklist

When releasing a new version:

- [ ] Update `<version>` in `pom.xml` (line 9)
- [ ] Update Launch4j version info (pom.xml lines 254-263)
- [ ] Update version in `README.md`
- [ ] Add entry to `CHANGELOG.md`
- [ ] Update version in build scripts
- [ ] Tag release in Git: `git tag v2.0.0`
- [ ] Update Steam build version (if applicable)

## Release Checklist

### Pre-Release

- [ ] All tests pass
- [ ] No critical bugs
- [ ] No compiler warnings (or documented)
- [ ] Documentation updated (README, CHANGELOG, guides)
- [ ] Version numbers consistent across all files
- [ ] Example mods work correctly
- [ ] Multiplayer tested (host and join)
- [ ] Performance acceptable (60+ FPS on target hardware)
- [ ] Code signed (Windows, macOS)

### Build

- [ ] Clean build: `mvn clean package`
- [ ] Verify JAR size (~15-20MB)
- [ ] Test JAR on all platforms
- [ ] Test EXE on Windows
- [ ] Verify mods load (check console for "Hi Mod")
- [ ] No runtime errors or exceptions

### Distribution

- [ ] Create GitHub release
- [ ] Upload JAR and EXE as assets
- [ ] Upload platform-specific packages (.deb, .app)
- [ ] Upload source code archive
- [ ] Write comprehensive release notes
- [ ] Update website/documentation
- [ ] Announce on social media
- [ ] Submit to Steam (if applicable)
- [ ] Update itch.io page (if applicable)

## Continuous Deployment

### GitHub Actions Example

Automate releases with GitHub Actions:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean package
      
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            target/PoorCraft.jar
            target/PoorCraft.exe
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Update Distribution

### Auto-Update System (Future)

Plan for implementing auto-updates:

1. **Check for updates** on startup
2. **Download new version** in background
3. **Prompt user** to restart and update
4. **Preserve user data** (worlds, mods, config)
5. **Apply update** on restart

### Manual Update

Current update process for users:

1. Download new version
2. Replace old JAR/EXE
3. Keep `gamedata/` and `assets/` folders
4. Restart game
5. Migrations handled automatically on startup

## Mod Distribution

### Mod Packaging

Package mods as ZIP files:

```
my_mod.zip
├── mod.json
├── main.lua
└── README.md (optional)
```

**Distribution:**
- Users extract to `gamedata/mods/`
- Restart game to load mod

### Mod Repository (Future)

Consider creating:
- Central mod repository website
- In-game mod browser
- One-click mod installation
- Automatic mod updates
- Steam Workshop integration

## Licensing

### MIT License

PoorCraft is licensed under the MIT License.

**Include in distribution:**
- `LICENSE.txt` file
- Copyright notice in README
- Attribution in About screen (future)

**Key Terms:**
- Free to use, modify, distribute
- Attribution required
- No warranty provided
- Commercial use allowed

## Support

### User Support Channels

- **GitHub Issues:** Bug reports and feature requests
- **GitHub Discussions:** General questions
- **Discord:** Real-time community support (optional)
- **Email:** Support email (optional)

### Documentation

Ensure these are accessible:
- README.md for overview
- docs/ for detailed guides
- In-game help (future)
- Video tutorials (optional)

## Analytics (Optional)

If implementing analytics:

- **Crash reporting:** Sentry, Bugsnag
- **Usage analytics:** Respect privacy, aggregate only
- **Performance metrics:** FPS, memory usage
- **Opt-in only:** Clearly disclosed, user consent required
- **GDPR compliance:** If distributing in EU

## Security

### Code Signing

Sign executables to prevent security warnings:

- **Windows:** Authenticode certificate
- **macOS:** Apple Developer certificate

### Distribution Security

- **HTTPS only** for downloads
- **Checksum verification:** Provide SHA-256 checksums
- **No telemetry** without consent
- **Secure multiplayer:** Encryption (future)

### Example Checksum File

```
# PoorCraft 2.0.0 Checksums (SHA-256)
abc123...  PoorCraft.jar
def456...  PoorCraft.exe
```

Generate with:
```bash
sha256sum PoorCraft.jar > checksums.txt
```

## Post-Release

### Monitoring

After release:

- Monitor GitHub Issues for bug reports
- Check community feedback
- Track download statistics
- Respond to support requests

### Maintenance

Plan for:

- Bug fix releases (2.0.1, 2.0.2)
- Feature updates (2.1.0, 2.2.0)
- Documentation updates
- Community engagement
- Mod ecosystem support

## Steam-Specific Requirements

### Store Page

Required for Steam release:

- **Screenshots:** At least 5 (1920x1080 recommended)
- **Trailer video:** 30-60 seconds
- **Description:** Short (160 char) and long (detailed)
- **System requirements:** Minimum and recommended
- **Tags:** Relevant categories (Sandbox, Voxel, Open World, Multiplayer)
- **Pricing:** Set price and regional pricing

### Technical Requirements

- Steamworks SDK integration
- Steam DRM (optional)
- Steam achievements (optional)
- Steam cloud saves (optional)
- Steam workshop (optional, for mods)
- Controller support (optional)

### Submission Process

1. Complete store page
2. Upload builds to Steam (via steamcmd)
3. Set release date
4. Submit for review
5. Address any feedback from Valve
6. Release or schedule release

## Distribution Channels

### Primary Channels

- **GitHub Releases:** Free, open-source distribution
- **Steam:** Primary commercial distribution (if approved)
- **itch.io:** Indie-friendly platform, easy setup

### Secondary Channels

- **Direct download:** From project website
- **Package managers:** apt, brew, chocolatey (future)

## File Sizes

Approximate sizes:

- **JAR:** 15-20MB
- **EXE:** 15-20MB
- **Full package with mods:** 25-30MB
- **With bundled JRE:** 100-150MB
- **Installer (Windows):** 20-30MB

## System Requirements

Include in store pages and README:

### Minimum

- **OS:** Windows 10, Ubuntu 20.04, macOS 11
- **CPU:** Dual-core 2.0 GHz
- **RAM:** 2GB
- **GPU:** OpenGL 3.3 compatible
- **Storage:** 500MB
- **Additional:** Java 17+ (if not bundled)

### Recommended

- **OS:** Windows 11, Ubuntu 22.04, macOS 12
- **CPU:** Quad-core 3.0 GHz
- **RAM:** 4GB
- **GPU:** Dedicated GPU with 1GB VRAM
- **Storage:** 1GB
- **Additional:** Java 17+ (if not bundled)

## Next Steps

After deploying:

1. **Monitor feedback** from users
2. **Triage bug reports** and prioritize fixes
3. **Plan next version** features
4. **Update documentation** as needed
5. **Engage community** for feedback
6. **Maintain CHANGELOG.md** for all releases

---

**Ready to deploy? Follow this guide step-by-step for a successful release!**

For more information:
- [Building Guide](BUILDING.md)
- [Architecture Guide](ARCHITECTURE.md)
- [Contributing Guide](../CONTRIBUTING.md)
