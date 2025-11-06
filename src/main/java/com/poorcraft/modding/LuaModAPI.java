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
                int npcId = args.checkint(1);      // npcId (ignored)
                String name = args.checkjstring(2);  // name
                float x = (float) args.checkdouble(3);  // x
                float y = (float) args.checkdouble(4);  // y
                float z = (float) args.checkdouble(5);  // z
                String personality = args.checkjstring(6);   // personality
                String skinName = args.optjstring(7, "steve");  // skinName (optional)
                
                int actualId = modAPI.spawnNPC(npcId, name, x, y, z, personality, skinName);
                return LuaValue.valueOf(actualId);
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
        
        // Chat API
        api.set("send_chat_message", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue message) {
                modAPI.sendChatMessage(message.checkjstring());
                return LuaValue.NIL;
            }
        });
        
        api.set("register_chat_listener", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue callback) {
                if (!callback.isfunction()) {
                    return LuaValue.NIL;
                }
                
                // Wrap Lua callback in Java Consumer
                modAPI.registerChatListener(data -> {
                    try {
                        LuaTable msgTable = new LuaTable();
                        msgTable.set("sender_id", LuaValue.valueOf(data.senderId));
                        msgTable.set("sender_name", LuaValue.valueOf(data.senderName));
                        msgTable.set("message", LuaValue.valueOf(data.message));
                        msgTable.set("timestamp", LuaValue.valueOf(data.timestamp));
                        msgTable.set("is_system", LuaValue.valueOf(data.isSystemMessage));
                        callback.call(msgTable);
                    } catch (Exception e) {
                        System.err.println("[LuaModAPI] Error in Lua chat listener: " + e.getMessage());
                    }
                });
                return LuaValue.NIL;
            }
        });
        
        api.set("get_current_biome", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                String biome = modAPI.getCurrentBiome();
                return biome != null ? LuaValue.valueOf(biome) : LuaValue.NIL;
            }
        });
        
        // HTTP API
        api.set("http_request", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                try {
                    String url = args.checkjstring(1);
                    String method = args.checkjstring(2);
                    String jsonBody = args.isnil(3) ? null : args.checkjstring(3);
                    
                    // Check if arg 4 is headers table or callback
                    java.util.Map<String, String> headers = null;
                    LuaValue callback;
                    
                    if (args.narg() >= 5) {
                        // 5 args: url, method, body, headers, callback
                        if (args.istable(4)) {
                            headers = luaTableToStringMap(args.checktable(4));
                        }
                        callback = args.checkfunction(5);
                    } else {
                        // 4 args: url, method, body, callback
                        callback = args.checkfunction(4);
                    }
                    
                    modAPI.makeHttpRequest(url, method, jsonBody, headers, responseBody -> {
                        try {
                            if (responseBody != null) {
                                callback.call(LuaValue.valueOf(responseBody));
                            } else {
                                callback.call(LuaValue.NIL);
                            }
                        } catch (Exception e) {
                            System.err.println("[LuaModAPI] Error in HTTP callback: " + e.getMessage());
                        }
                    });
                    
                    return LuaValue.NIL;
                } catch (Exception e) {
                    System.err.println("[LuaModAPI] Error in http_request: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        api.set("http_request_sync", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                try {
                    String url = args.checkjstring(1);
                    String method = args.checkjstring(2);
                    String jsonBody = args.isnil(3) ? null : args.checkjstring(3);
                    
                    String response = modAPI.makeHttpRequestSync(url, method, jsonBody);
                    return response != null ? LuaValue.valueOf(response) : LuaValue.NIL;
                } catch (Exception e) {
                    System.err.println("[LuaModAPI] Error in http_request_sync: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // HTTP POST JSON API
        api.set("http_post_json", new VarArgFunction() {
            @Override
            public LuaValue invoke(org.luaj.vm2.Varargs args) {
                try {
                    String url = args.checkjstring(1);
                    LuaTable bodyTable = args.checktable(2);
                    LuaTable headersTable = args.isnil(3) ? null : args.checktable(3);
                    LuaValue callback = args.checkfunction(4);
                    
                    // Convert Lua tables to Java Maps
                    java.util.Map<String, Object> body = luaTableToMap(bodyTable);
                    java.util.Map<String, String> headers = headersTable != null ? luaTableToStringMap(headersTable) : null;
                    
                    modAPI.httpPostJson(url, body, headers, responseBody -> {
                        try {
                            if (responseBody != null) {
                                callback.call(LuaValue.valueOf(responseBody));
                            } else {
                                callback.call(LuaValue.NIL);
                            }
                        } catch (Exception e) {
                            System.err.println("[LuaModAPI] Error in HTTP POST JSON callback: " + e.getMessage());
                        }
                    });
                    
                    return LuaValue.NIL;
                } catch (Exception e) {
                    System.err.println("[LuaModAPI] Error in http_post_json: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // JSON parsing API
        api.set("parse_json", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue jsonString) {
                try {
                    String json = jsonString.checkjstring();
                    java.util.Map<String, Object> parsed = modAPI.parseJson(json);
                    return javaMapToLuaTable(parsed);
                } catch (Exception e) {
                    System.err.println("[LuaModAPI] Error in parse_json: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // NPC follow distance API
        api.set("set_npc_follow_distance", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue npcId, LuaValue distance) {
                modAPI.setNPCFollowDistance(npcId.checkint(), (float) distance.checkdouble());
                return LuaValue.NIL;
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
     * Converts a Lua table to a Java Map<String, String> for HTTP headers.
     */
    private java.util.Map<String, String> luaTableToStringMap(LuaTable table) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (table == null) {
            return map;
        }
        
        LuaValue key = LuaValue.NIL;
        while (true) {
            org.luaj.vm2.Varargs next = table.next(key);
            if ((key = next.arg1()).isnil()) {
                break;
            }
            LuaValue value = next.arg(2);
            map.put(key.tojstring(), value.tojstring());
        }
        
        return map;
    }
    
    /**
     * Converts a Lua table to a Java Map<String, Object> for JSON encoding.
     */
    private java.util.Map<String, Object> luaTableToMap(LuaTable table) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (table == null) {
            return map;
        }
        
        LuaValue key = LuaValue.NIL;
        while (true) {
            org.luaj.vm2.Varargs next = table.next(key);
            if ((key = next.arg1()).isnil()) {
                break;
            }
            LuaValue value = next.arg(2);
            
            // Convert value to appropriate Java type
            if (value.isnil()) {
                map.put(key.tojstring(), null);
            } else if (value.isboolean()) {
                map.put(key.tojstring(), value.toboolean());
            } else if (value.isint()) {
                map.put(key.tojstring(), value.toint());
            } else if (value.isnumber()) {
                map.put(key.tojstring(), value.todouble());
            } else if (value.isstring()) {
                map.put(key.tojstring(), value.tojstring());
            } else if (value.istable()) {
                // Recursively convert nested tables
                map.put(key.tojstring(), luaTableToMap((LuaTable) value));
            } else {
                map.put(key.tojstring(), value.tojstring());
            }
        }
        
        return map;
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
                    // Convert event to Lua table based on event type
                    LuaValue eventData = convertEventToLua(event);
                    callback.call(eventData);
                } catch (Exception e) {
                    System.err.println("[LuaModAPI] Error invoking Lua callback: " + e.getMessage());
                }
            }
        }
        
        private LuaValue convertEventToLua(Object event) {
            if (event instanceof com.poorcraft.modding.events.BiomeChangeEvent) {
                com.poorcraft.modding.events.BiomeChangeEvent biomeEvent = 
                    (com.poorcraft.modding.events.BiomeChangeEvent) event;
                
                LuaTable table = new LuaTable();
                table.set("player_id", LuaValue.valueOf(biomeEvent.getPlayerId()));
                table.set("old_biome", LuaValue.valueOf(biomeEvent.getPreviousBiome()));
                table.set("new_biome", LuaValue.valueOf(biomeEvent.getNewBiome()));
                table.set("x", LuaValue.valueOf(biomeEvent.getWorldX()));
                table.set("z", LuaValue.valueOf(biomeEvent.getWorldZ()));
                return table;
            }
            
            // For other events, return nil for now
            return LuaValue.NIL;
        }
    }
}
