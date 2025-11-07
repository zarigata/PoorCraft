-- Real-Time Synchronization Mod for PoorCraft
-- Synchronizes in-game time with real-world time

local mod = {}

-- State variables
local config = nil
local sync_timer = 0.0
local last_sync_time = 0
local eventQueue = {}

-- Default config values
local default_config = {
    sync_enabled = true,
    time_scale = 1.0,
    sync_interval = 60.0,
    use_player_location = false,
    weather_sync_enabled = false,
    debug_logging = false
}

-- Load config with defaults
local function load_config()
    local cfg = api.get_mod_config_table("realtime_sync")
    
    -- Apply defaults for missing values
    if not cfg or not next(cfg) then
        return default_config
    end
    
    local result = {}
    for key, default_value in pairs(default_config) do
        if cfg[key] ~= nil then
            result[key] = cfg[key]
        else
            result[key] = default_value
        end
    end
    
    return result
end

-- Convert real-world time to game time (0.0-1.0)
local function real_time_to_game_time(real_millis)
    -- Extract hours and minutes from milliseconds
    local seconds_in_day = 86400
    local seconds = math.floor(real_millis / 1000)
    local time_of_day = seconds % seconds_in_day
    
    -- Convert to 0.0-1.0 range (0.0 = midnight, 0.5 = noon, 1.0 = midnight)
    local game_time = time_of_day / seconds_in_day
    
    -- Apply time scale multiplier if configured
    if config and config.time_scale ~= 1.0 then
        game_time = (game_time * config.time_scale) % 1.0
    end
    
    return game_time
end

-- Log message only if debug logging is enabled
local function log_debug(message)
    if config and config.debug_logging then
        api.log(message)
    end
end

-- Initialize the mod
function mod.init()
    api.log("Real-Time Sync: Initializing...")
    
    -- Load config using new structured API
    config = load_config()
    
    -- Initialize state
    sync_timer = 0.0
    last_sync_time = api.get_real_time()
    
    api.log("Real-Time Sync: Initialized with sync_interval=" .. config.sync_interval .. "s")
    if config.debug_logging then
        api.log("Real-Time Sync: Debug logging enabled")
    end

    -- Initialize shared state for cross-mod communication
    local sharedState = api.get_shared_data("realtime_sync_state")
    if sharedState == nil then
        sharedState = {}
    end
    sharedState.lastSync = api.get_real_time()
    api.set_shared_data("realtime_sync_state", sharedState)

    api.register_event("on_block_change", function(event)
        if event.mod_origin == "realtime_sync" then
            return
        end
        eventQueue[#eventQueue + 1] = {
            type = "block_change",
            x = event.x,
            y = event.y,
            z = event.z,
            block = event.block
        }
    end)

    api.register_event("on_tick", function(_)
        if #eventQueue == 0 then
            return
        end
        local pending = eventQueue
        eventQueue = {}
        for _, queued in ipairs(pending) do
            api.broadcast_network_event("realtime_sync", queued)
        end
    end)
end

-- Enable the mod
function mod.enable()
    api.log("Real-Time Sync: Enabled!")
    
    -- Enable external time control to prevent game's auto-progression
    api.set_time_control_enabled(true)
    
    -- Perform initial sync if enabled
    if config and config.sync_enabled then
        local real_time = api.get_real_time()
        local game_time = real_time_to_game_time(real_time)
        api.set_game_time(game_time)
        log_debug("Real-Time Sync: Initial sync - game time set to " .. game_time)
    end

    eventQueue = {}

    api.register_event("network_event", function(event)
        if event.channel ~= "realtime_sync" then
            return
        end
        if event.payload.type == "block_change" then
            api.set_block(event.payload.x, event.payload.y, event.payload.z, event.payload.block)
        end
    end)
end

-- Disable the mod
function mod.disable()
    api.log("Real-Time Sync: Disabled")
    
    -- Restore automatic time progression
    api.set_time_control_enabled(false)
    
    sync_timer = 0.0
end

-- Update function - called every frame
function mod.update(deltaTime)
    -- Only update if config is loaded and sync is enabled
    if not config or not config.sync_enabled then
        return
    end
    
    -- Increment sync timer
    sync_timer = sync_timer + deltaTime
    
    -- Check if it's time to sync
    if sync_timer >= config.sync_interval then
        -- Get current real-world time
        local real_time = api.get_real_time()
        
        -- Convert to game time
        local game_time = real_time_to_game_time(real_time)
        
        -- Set game time
        api.set_game_time(game_time)
        
        -- Get player position for logging (if available)
        if config.debug_logging then
            local pos = api.get_player_position()
            if pos then
                log_debug(string.format(
                    "Real-Time Sync: Synced at position (%.1f, %.1f, %.1f) - game time: %.3f",
                    pos.x, pos.y, pos.z, game_time
                ))
            else
                log_debug("Real-Time Sync: Synced - game time: " .. game_time)
            end
        end
        
        -- Reset sync timer
        sync_timer = 0.0
        last_sync_time = real_time
    end
end

-- Return the mod table
return mod
