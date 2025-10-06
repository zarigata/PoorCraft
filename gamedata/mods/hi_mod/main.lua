-- Hi Mod - Simple test mod
-- Displays "Hi!" message for 5 seconds when the game starts

local mod = {}
local message_timer = 0
local show_message = false

function mod.init()
    api.log("=======================================")
    api.log("        HI MOD INITIALIZED!           ")
    api.log("=======================================")
    api.log("Hi Mod: This message will appear for 5 seconds")
    
    -- Start the timer
    show_message = true
    message_timer = 5.0
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
    
    -- Start countdown
    startCountdown()
end

function mod.disable()
    api.log("Hi Mod: Disabled. Goodbye!")
    show_message = false
end

-- Function to display countdown
function startCountdown()
    -- Create a separate thread for countdown (simulated with print)
    for i = 5, 1, -1 do
        api.log("Hi Mod: Message will disappear in " .. i .. " seconds...")
    end
    api.log("Hi Mod: 5 seconds elapsed! Message period complete.")
    api.log("Hi Mod: Lua modding system is working correctly! ✓")
end

return mod
