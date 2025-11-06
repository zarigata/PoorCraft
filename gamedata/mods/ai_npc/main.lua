-- AI Companion Mod (Lua)
-- Provides a lightweight placeholder NPC that periodically chats with the player.

local mod = {}

local config = nil
local speak_timer = 0.0
local last_phrase_index = 1

local phrases = {
    "I'm keeping an eye out for creepers... even imaginary ones!",
    "Need supplies? I can remind you what's in the chest (mentally).",
    "If you get lost, just follow the sound of my witty commentary.",
    "Another beautiful blocky day, don't you think?"
}

local function load_config()
    local cfg = api.get_mod_config_table("ai_npc")
    if type(cfg) ~= "table" then
        cfg = {}
    end
    return {
        enabled = cfg.enabled ~= false,
        companion_name = cfg.companion_name or "Sparky",
        speak_interval = math.max(10, tonumber(cfg.speak_interval) or 45),
        debug_logging = cfg.debug_logging == true
    }
end

local function log_debug(message)
    if config and config.debug_logging then
        api.log("[AI NPC] " .. message)
    end
end

local function broadcast_status(active)
    api.set_shared_data("ai_npc.status", {
        name = config and config.companion_name or "Sparky",
        active = active == true,
        interval = config and config.speak_interval or 45
    })
end

local function next_phrase()
    if #phrases == 0 then
        return "Hello from your AI companion!"
    end
    local phrase = phrases[last_phrase_index]
    last_phrase_index = last_phrase_index + 1
    if last_phrase_index > #phrases then
        last_phrase_index = 1
    end
    return phrase
end

function mod.init()
    api.log("[AI NPC] Initializing companion controller...")
    config = load_config()
    speak_timer = 0.0
    last_phrase_index = 1
    broadcast_status(false)

    api.register_event("chat_message", function(event)
        if not event or type(event.message) ~= "string" then
            return
        end
        if event.message:lower():find("thanks", 1, true) then
            api.send_chat_message(config.companion_name .. " says: You're welcome!")
        end
    end)

    log_debug("Loaded config for companion '" .. config.companion_name .. "'")
end

function mod.enable()
    if not config or not config.enabled then
        api.log("[AI NPC] Disabled via configuration; skipping activation.")
        broadcast_status(false)
        return
    end

    api.log("[AI NPC] Companion '" .. config.companion_name .. "' is now following you.")
    broadcast_status(true)

    api.register_event("on_tick", function(event)
        if not config or not config.enabled then
            return
        end
        local delta = 0.0
        if type(event) == "table" and type(event.delta_time) == "number" then
            delta = event.delta_time
        end
        speak_timer = speak_timer + delta
        if speak_timer >= config.speak_interval then
            speak_timer = 0.0
            local phrase = next_phrase()
            api.send_chat_message(config.companion_name .. ": " .. phrase)
            log_debug("Spoke phrase: " .. phrase)
        end
    end)
end

function mod.disable()
    api.log("[AI NPC] Companion resting until next enable.")
    broadcast_status(false)
    speak_timer = 0.0
end

return mod
