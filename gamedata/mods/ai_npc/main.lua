-- AI NPC Companion System for PoorCraft
-- Responds to chat messages and biome changes

local mod = {}
local last_biome = nil
local response_cooldown = 0
local COOLDOWN_TIME = 5.0  -- Seconds between responses

-- Simple response patterns (no external AI for now)
local chat_responses = {
    ["hello"] = "Hello there! How can I help you today?",
    ["hi"] = "Hi! Nice to see you!",
    ["help"] = "I'm here to keep you company! Try exploring different biomes.",
    ["bye"] = "Goodbye! Safe travels!",
    ["weather"] = "The weather is clear today. Perfect for exploring!",
    ["time"] = "Time flies when you're having fun!",
}

local biome_responses = {
    ["Desert"] = "Wow, it's getting hot here! Watch out for cacti.",
    ["Snow"] = "Brrr! It's cold in this snowy biome. Stay warm!",
    ["Jungle"] = "Welcome to the jungle! So much greenery around.",
    ["Plains"] = "Ah, the peaceful plains. A great place to build.",
}

function mod.init()
    api.log("[AI_NPC] Initializing AI NPC companion system...")
    
    -- Get initial biome
    last_biome = api.get_current_biome()
    if last_biome then
        api.log("[AI_NPC] Starting biome: " .. last_biome)
    end
    
    api.log("[AI_NPC] Companion ready!")
end

function mod.enable()
    api.log("[AI_NPC] Enabled - AI companion is now active")
    
    -- Register chat listener
    api.register_chat_listener(function(msg)
        -- Don't respond to system messages or our own messages
        if msg.is_system or msg.sender_name == "AI Companion" then
            return
        end
        
        -- Check cooldown
        if response_cooldown > 0 then
            return
        end
        
        -- Check for matching patterns
        local message_lower = string.lower(msg.message)
        for pattern, response in pairs(chat_responses) do
            if string.find(message_lower, pattern) then
                api.send_chat_message(response)
                response_cooldown = COOLDOWN_TIME
                return
            end
        end
    end)
    
    -- Register biome change event listener
    api.register_event('biome_change', function(event)
        api.log("[AI_NPC] Biome changed: " .. event.old_biome .. " -> " .. event.new_biome)
        
        -- Send biome change message if we have a response and not on cooldown
        local response = biome_responses[event.new_biome]
        if response and response_cooldown <= 0 then
            api.send_chat_message(response)
            response_cooldown = COOLDOWN_TIME
        end
    end)
    
    api.log("[AI_NPC] Chat listener and biome_change event registered")
end

function mod.update(delta_time)
    -- Update cooldown
    if response_cooldown > 0 then
        response_cooldown = response_cooldown - delta_time
    end
    
    -- Biome change detection now handled by event system
    -- No need to poll every frame
end

function mod.disable()
    api.log("[AI_NPC] Disabling AI companion...")
    api.log("[AI_NPC] Disabled")
end

return mod
