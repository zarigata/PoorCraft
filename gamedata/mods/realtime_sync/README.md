# Real-Time Synchronization Mod

This mod synchronizes in-game time with real-world time based on your system clock.

## Features

- **Real-time synchronization**: Syncs in-game time with your system clock
- **Configurable time scale**: Speed up or slow down time with a multiplier
- **Adjustable sync interval**: Control how often the sync happens for performance tuning
- **Debug logging**: Optional verbose logging for troubleshooting
- **Future location-based sync**: Placeholder for syncing based on real-world location
- **Future weather sync**: Placeholder for weather synchronization

## Configuration

Edit `mod.json` to customize the mod's behavior:

### `sync_enabled` (boolean, default: `true`)
Master toggle for time synchronization. Set to `false` to disable the mod without unloading it.

**Example**: `"sync_enabled": true`

### `time_scale` (number, default: `1.0`)
Speed multiplier for time progression.
- `1.0` = real-time (24 hours real-world = 24 hours in-game)
- `0.5` = half speed (48 hours real-world = 24 hours in-game)
- `2.0` = double speed (12 hours real-world = 24 hours in-game)

**Example**: `"time_scale": 1.0`

### `sync_interval` (number, default: `60.0`)
How often to update the in-game time, in seconds. Lower values provide more accurate sync but may impact performance.

**Example**: `"sync_interval": 60.0`

### `use_player_location` (boolean, default: `false`)
**[Future Feature]** When implemented, this will sync time based on the player's real-world location (timezone detection).

**Example**: `"use_player_location": false`

### `weather_sync_enabled` (boolean, default: `false`)
**[Future Feature]** When implemented, this will sync weather with real-world weather data from weather APIs.

**Example**: `"weather_sync_enabled": false`

### `debug_logging` (boolean, default: `false`)
Enable verbose logging for debugging purposes. Shows sync events and position information.

**Example**: `"debug_logging": false`

## How It Works

1. Every `sync_interval` seconds, the mod reads the current system time
2. The system time is converted to a 0.0-1.0 range representing time of day:
   - `0.0` = midnight
   - `0.25` = 6:00 AM (sunrise)
   - `0.5` = noon
   - `0.75` = 6:00 PM (sunset)
   - `1.0` = midnight
3. The `time_scale` multiplier is applied if configured
4. The game time is updated via the ModAPI

## Future Enhancements

### Location-Based Time Zones
Using the player's real-world location (via IP geolocation or manual configuration), the mod could sync time based on the player's timezone.

### Weather Synchronization
Integrate with weather APIs (OpenWeatherMap, WeatherAPI, etc.) to sync in-game weather with real-world conditions at the player's location.

### Seasonal Adjustments
Adjust day/night cycle length based on real-world seasons and latitude.

### Custom Time Offset
Allow players to configure a time offset (e.g., "+2 hours") to shift the sync without changing timezone.

## Technical Notes

- This mod uses the new `update(deltaTime)` lifecycle function introduced in PoorCraft v2.0
- The mod requires the extended ModAPI with time and position functions
- Time synchronization only affects the visual day/night cycle and lighting
- The sync interval should be balanced between accuracy and performance (60 seconds is recommended)

## Troubleshooting

**Time not syncing?**
- Check that `sync_enabled` is set to `true` in `mod.json`
- Enable `debug_logging` to see sync events in the console
- Verify the mod is loaded by checking the console for "Real-Time Sync: Enabled!" message

**Performance issues?**
- Increase `sync_interval` to reduce update frequency (try 120 or 180 seconds)
- Disable `debug_logging` if enabled

**Want to disable temporarily?**
- Set `sync_enabled` to `false` and reload the world
- Or set `enabled` to `false` in `mod.json` to completely unload the mod
