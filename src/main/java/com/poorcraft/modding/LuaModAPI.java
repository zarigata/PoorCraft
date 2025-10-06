package com.poorcraft.modding;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

/**
 * Lua API bridge that exposes Java ModAPI functions to Lua mods.
 * 
 * <p>This class wraps the Java ModAPI and provides Lua-friendly functions
 * that can be called from Lua scripts.
 * 
 * @author PoorCraft Team
 * @version 2.0
 */
public class LuaModAPI {
    
    private final ModAPI modAPI;
    
    public LuaModAPI(ModAPI modAPI) {
        this.modAPI = modAPI;
    }
    
    /**
     * Converts this API to a Lua table that can be exposed to Lua scripts.
     * 
     * @param globals Lua globals
     * @return LuaTable containing all API functions
     */
    public LuaValue toLuaValue(Globals globals) {
        LuaTable api = new LuaTable();
        
        // World access functions
        api.set("get_block", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                return LuaValue.valueOf(modAPI.getBlock(
                    x.checkint(),
                    y.checkint(),
                    z.checkint()
                ));
            }
        });
        
        api.set("set_block", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                modAPI.setBlock(
                    args.checkint(1),
                    args.checkint(2),
                    args.checkint(3),
                    args.checkint(4)
                );
                return LuaValue.NIL;
            }
        });
        
        api.set("get_biome", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue x, LuaValue z) {
                String biome = modAPI.getBiome(x.checkint(), z.checkint());
                return biome != null ? LuaValue.valueOf(biome) : LuaValue.NIL;
            }
        });
        
        api.set("get_height_at", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue x, LuaValue z) {
                return LuaValue.valueOf(modAPI.getHeightAt(x.checkint(), z.checkint()));
            }
        });
        
        // Event registration
        api.set("register_event", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue eventName, LuaValue callback) {
                modAPI.registerEvent(eventName.checkjstring(), new LuaEventCallback(callback));
                return LuaValue.NIL;
            }
        });
        
        // Utility functions
        api.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue message) {
                modAPI.log(message.checkjstring());
                return LuaValue.NIL;
            }
        });
        
        api.set("is_server", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(modAPI.isServer());
            }
        });
        
        api.set("set_shared_data", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue key, LuaValue value) {
                modAPI.setSharedData(key.checkjstring(), luaToJava(value));
                return LuaValue.NIL;
            }
        });
        
        api.set("get_shared_data", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue key) {
                Object value = modAPI.getSharedData(key.checkjstring());
                return javaToLua(value);
            }
        });
        
        // Player position API
        api.set("get_player_position", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                double[] position = modAPI.getPlayerPosition();
                if (position == null) {
                    return LuaValue.NIL;
                }
                LuaTable posTable = new LuaTable();
                posTable.set("x", LuaValue.valueOf(position[0]));
                posTable.set("y", LuaValue.valueOf(position[1]));
                posTable.set("z", LuaValue.valueOf(position[2]));
                return posTable;
            }
        });
        
        // Game time API
        api.set("get_game_time", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(modAPI.getGameTime());
            }
        });
        
        api.set("set_game_time", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue time) {
                modAPI.setGameTime((float) time.checkdouble());
                return LuaValue.NIL;
            }
        });
        
        // Real-world time API
        api.set("get_real_time", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.valueOf(modAPI.getRealTime());
            }
        });
        
        // Weather API
        api.set("get_weather", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                String weather = modAPI.getWeather();
                return weather != null ? LuaValue.valueOf(weather) : LuaValue.NIL;
            }
        });
        
        // Texture API
        api.set("add_procedural_texture", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue name, LuaValue data) {
                // Convert Lua string (bytes) to Java byte array
                String dataStr = data.checkjstring();
                byte[] bytes = dataStr.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                modAPI.addProceduralTexture(name.checkjstring(), bytes);
                return LuaValue.NIL;
            }
        });
        
        // Config API
        api.set("get_mod_config", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue modId) {
                String config = modAPI.getModConfig(modId.checkjstring());
                return config != null ? LuaValue.valueOf(config) : LuaValue.NIL;
            }
        });
        
        // NPC API
        api.set("spawn_npc", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                modAPI.spawnNPC(
                    args.checkint(1),      // npcId
                    args.checkjstring(2),  // name
                    (float) args.checkdouble(3),  // x
                    (float) args.checkdouble(4),  // y
                    (float) args.checkdouble(5),  // z
                    args.checkjstring(6)   // personality
                );
                return LuaValue.NIL;
            }
        });
        
        api.set("despawn_npc", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue npcId) {
                modAPI.despawnNPC(npcId.checkint());
                return LuaValue.NIL;
            }
        });
        
        api.set("npc_say", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue npcId, LuaValue message) {
                modAPI.npcSay(npcId.checkint(), message.checkjstring());
                return LuaValue.NIL;
            }
        });
        
        // Time control API
        api.set("set_time_control_enabled", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue enabled) {
                modAPI.setTimeControlEnabled(enabled.checkboolean());
                return LuaValue.NIL;
            }
        });
        
        // Config table API
        api.set("get_mod_config_table", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue modId) {
                java.util.Map<String, Object> config = modAPI.getModConfigTable(modId.checkjstring());
                return javaMapToLuaTable(config);
            }
        });
        
        return api;
    }
    
    /**
     * Converts a Lua value to a Java object for storage.
     */
    private Object luaToJava(LuaValue value) {
        if (value.isnil()) {
            return null;
        } else if (value.isboolean()) {
            return value.checkboolean();
        } else if (value.isint()) {
            return value.checkint();
        } else if (value.isnumber()) {
            return value.checkdouble();
        } else if (value.isstring()) {
            return value.checkjstring();
        } else if (value.istable()) {
            // For tables, we could convert to Map, but for now just store the LuaValue
            return value;
        } else {
            return value;
        }
    }
    
    /**
     * Converts a Java object to a Lua value.
     */
    private LuaValue javaToLua(Object obj) {
        if (obj == null) {
            return LuaValue.NIL;
        } else if (obj instanceof Boolean) {
            return LuaValue.valueOf((Boolean) obj);
        } else if (obj instanceof Integer) {
            return LuaValue.valueOf((Integer) obj);
        } else if (obj instanceof Double) {
            return LuaValue.valueOf((Double) obj);
        } else if (obj instanceof Float) {
            return LuaValue.valueOf((Float) obj);
        } else if (obj instanceof String) {
            return LuaValue.valueOf((String) obj);
        } else if (obj instanceof LuaValue) {
            return (LuaValue) obj;
        } else if (obj instanceof java.util.Map) {
            return javaMapToLuaTable((java.util.Map<?, ?>) obj);
        } else if (obj instanceof Object[]) {
            return javaArrayToLuaTable((Object[]) obj);
        } else {
            return LuaValue.NIL;
        }
    }
    
    /**
     * Converts a Java Map to a Lua table recursively.
     */
    private LuaTable javaMapToLuaTable(java.util.Map<?, ?> map) {
        LuaTable table = new LuaTable();
        if (map == null) {
            return table;
        }
        
        for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            table.set(key, javaToLua(value));
        }
        
        return table;
    }
    
    /**
     * Converts a Java array to a Lua table (1-indexed).
     */
    private LuaTable javaArrayToLuaTable(Object[] array) {
        LuaTable table = new LuaTable();
        if (array == null) {
            return table;
        }
        
        for (int i = 0; i < array.length; i++) {
            table.set(i + 1, javaToLua(array[i]));  // Lua uses 1-based indexing
        }
        
        return table;
    }
    
    /**
     * Wrapper for Lua callbacks to Java event system.
     */
    private static class LuaEventCallback {
        private final LuaValue callback;
        
        public LuaEventCallback(LuaValue callback) {
            this.callback = callback;
        }
        
        public void invoke(Object event) {
            if (callback.isfunction()) {
                try {
                    // Convert event to Lua table if needed
                    // For now, just pass nil - events will need proper conversion
                    callback.call(LuaValue.NIL);
                } catch (Exception e) {
                    System.err.println("[LuaModAPI] Error invoking Lua callback: " + e.getMessage());
                }
            }
        }
    }
}
