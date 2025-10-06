-- AI NPC System for PoorCraft
-- Simplified Lua version - conversational AI integration placeholder

local mod = {}
local npcs = {}

function mod.init()
    api.log("[AI_NPC] Initializing AI NPC system...")
    
    -- Get mod config
    local config_json = api.get_mod_config("ai_npc")
    if config_json then
        api.log("[AI_NPC] Config loaded")
    end
    
    api.log("[AI_NPC] Note: Full AI integration requires HTTP libraries")
    api.log("[AI_NPC] This is a placeholder showing mod structure")
end

function mod.enable()
    api.log("[AI_NPC] Enabled - AI NPC system ready")
    
    -- Example: Spawn a simple NPC
    -- In full implementation, this would connect to AI services
    api.spawn_npc(1, "Steve", 100.0, 65.0, 100.0, "friendly villager")
    table.insert(npcs, 1)
end

function mod.disable()
    api.log("[AI_NPC] Disabling AI NPC system...")
    
    -- Despawn all NPCs
    for _, npc_id in ipairs(npcs) do
        api.despawn_npc(npc_id)
    end
    
    npcs = {}
    api.log("[AI_NPC] Disabled")
end

return mod
