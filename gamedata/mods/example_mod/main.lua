-- Example Lua Mod for PoorCraft
-- This demonstrates the basic structure of a Lua mod

local mod = {}

function mod.init()
    api.log("Example Mod: Initializing...")
    
    -- Get mod config
    local config_json = api.get_mod_config("example_mod")
    if config_json then
        api.log("Example Mod: Config loaded")
    end
end

function mod.enable()
    api.log("Example Mod: Enabled!")
    api.log("Example Mod: Lua modding is now active in PoorCraft")
end

function mod.disable()
    api.log("Example Mod: Disabled")
end

-- Return the mod table so functions can be called
return mod
