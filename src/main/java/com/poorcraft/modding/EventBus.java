package com.poorcraft.modding;

import com.poorcraft.modding.events.Event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event bus that routes events to registered callbacks.
 * 
 * <p>The EventBus is responsible for:
 * <ul>
 *   <li>Managing Lua callback registration</li>
 *   <li>Firing events to all registered callbacks</li>
 *   <li>Handling cancellable events</li>
 *   <li>Isolating callback exceptions (one mod crash doesn't affect others)</li>
 * </ul>
 * 
 * <p>Thread-safe for concurrent event firing.
 * 
 * <p><b>Lua callback invocation:</b>
 * Lua callbacks are invoked via the LuaJ API.
 * 
 * <p><b>Event cancellation:</b>
 * If an event is cancelled by a callback, remaining callbacks are not invoked.
 * This allows mods to prevent actions from occurring.
 * 
 * @author PoorCraft Team
 * @version 2.0
 */
public class EventBus {
    
    private final Map<String, List<Object>> callbacks;
    private final ExecutorService asyncExecutor;
    private boolean verboseLogging = false;  // Can be made configurable
    
    /**
     * Creates a new event bus.
     */
    public EventBus() {
        this.callbacks = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Registers a callback for the specified event.
     * 
     * @param eventName Name of the event (e.g., "block_place")
     * @param callback Callback object (Lua callback)
     */
    public void registerCallback(String eventName, Object callback) {
        callbacks.computeIfAbsent(eventName, k -> new ArrayList<>()).add(callback);
        System.out.println("[EventBus] Registered callback for event: " + eventName);
    }
    
    /**
     * Unregisters a callback for the specified event.
     * 
     * @param eventName Name of the event
     * @param callback Callback object to remove
     */
    public void unregisterCallback(String eventName, Object callback) {
        List<Object> eventCallbacks = callbacks.get(eventName);
        if (eventCallbacks != null) {
            eventCallbacks.remove(callback);
            System.out.println("[EventBus] Unregistered callback for event: " + eventName);
        }
    }
    
    /**
     * Fires an event to all registered callbacks.
     * 
     * <p>Callbacks are invoked in registration order. If the event is cancelled,
     * remaining callbacks are not invoked.
     * 
     * <p>Exceptions in callbacks are caught and logged to prevent one mod from
     * crashing others.
     * 
     * @param event The event to fire
     */
    public void fire(Event event) {
        String eventName = event.getEventName();
        List<Object> eventCallbacks = callbacks.get(eventName);
        
        if (eventCallbacks == null || eventCallbacks.isEmpty()) {
            if (verboseLogging) {
                System.out.println("[EventBus] No callbacks registered for event: " + eventName);
            }
            return;
        }
        
        if (verboseLogging) {
            System.out.println("[EventBus] Firing event: " + eventName + " to " + eventCallbacks.size() + " callbacks");
        }
        
        // Invoke each callback
        for (Object callback : eventCallbacks) {
            try {
                // Invoke callback (Lua or Java)
                invokeCallback(callback, event);
                
                // Check if event was cancelled
                if (event.isCancelled()) {
                    if (verboseLogging) {
                        System.out.println("[EventBus] Event " + eventName + " was cancelled, stopping propagation");
                    }
                    break;  // Stop processing remaining callbacks
                }
                
            } catch (Exception e) {
                // Log error but continue with other callbacks
                // Don't let one mod crash others!
                System.err.println("[EventBus] Error invoking callback for event " + eventName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Fires an event asynchronously in a separate thread.
     * 
     * <p>Useful for events that don't need immediate response and shouldn't
     * block the main thread.
     * 
     * @param event The event to fire
     */
    public void fireAsync(Event event) {
        asyncExecutor.submit(() -> fire(event));
    }
    
    /**
     * Invokes a callback with the event as parameter.
     * 
     * <p>For Lua callbacks, this is handled by the LuaModAPI.
     * For Java callbacks, uses reflection.
     * 
     * @param callback Callback object
     * @param event Event to pass to callback
     * @throws Exception if invocation fails
     */
    private void invokeCallback(Object callback, Event event) throws Exception {
        if (callback == null) {
            return;
        }

        if (callback instanceof java.util.function.Consumer) {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<Event> consumer = (java.util.function.Consumer<Event>) callback;
            consumer.accept(event);
            return;
        }

        try {
            Method invokeMethod = callback.getClass().getMethod("invoke", Object.class);
            invokeMethod.setAccessible(true);
            invokeMethod.invoke(callback, event);
            return;
        } catch (NoSuchMethodException ignored) {
            // Fall through to unknown callback warning
        } catch (ReflectiveOperationException reflectiveException) {
            throw reflectiveException;
        }

        System.err.println("[EventBus] Unknown callback type: " + callback.getClass().getName());
    }
    
    /**
     * Clears all callbacks for the specified event.
     * Useful for mod unloading.
     * 
     * @param eventName Name of the event
     */
    public void clearCallbacks(String eventName) {
        callbacks.remove(eventName);
        System.out.println("[EventBus] Cleared all callbacks for event: " + eventName);
    }
    
    /**
     * Gets the number of registered callbacks for an event.
     * Useful for debugging.
     * 
     * @param eventName Name of the event
     * @return Number of registered callbacks
     */
    public int getCallbackCount(String eventName) {
        List<Object> eventCallbacks = callbacks.get(eventName);
        return eventCallbacks != null ? eventCallbacks.size() : 0;
    }
    
    /**
     * Enables or disables verbose logging.
     * When enabled, logs every event firing.
     * 
     * @param verbose true to enable verbose logging
     */
    public void setVerboseLogging(boolean verbose) {
        this.verboseLogging = verbose;
    }
    
    /**
     * Shuts down the async executor.
     * Should be called when the event bus is no longer needed.
     */
    public void shutdown() {
        asyncExecutor.shutdown();
    }
}
