-- Hi Mod - Simple test mod
-- Displays "Hi!" message for 5 seconds when the game starts

local mod = {}
function mod.init()
    api.log("=======================================")
    api.log("        HI MOD INITIALIZED!           ")
    api.log("=======================================")
    api.log("Hi Mod: This message will appear for 5 seconds")
end

function mod.enable()
    api.log("Hi Mod: Enabled! Get ready to see 'Hi!' for 5 seconds!")
    
    -- Display welcome message
    print("\n")
    print("╔════════════════════════════════════════╗")
    print("║                                        ║")
    print("║         🎉  HI FROM LUA MOD! 🎉        ║")
    print("║                                        ║")
    print("║   This message will display for 5s    ║")
    print("║   Testing the Lua modding system!     ║")
    print("║                                        ║")
    print("╚════════════════════════════════════════╝")
    print("\n")
end

function mod.disable()
    api.log("Hi Mod: Disabled. Goodbye!")
end

return mod
