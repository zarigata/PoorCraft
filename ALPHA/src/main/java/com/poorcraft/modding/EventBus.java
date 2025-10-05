package com.poorcraft.modding;

import com.poorcraft.modding.events.Event;
import py4j.reflection.ReflectionEngine;

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
 *   <li>Managing Python callback registration</li>
 *   <li>Firing events to all registered callbacks</li>
 *   <li>Handling cancellable events</li>
 *   <li>Isolating callback exceptions (one mod crash doesn't affect others)</li>
 * </ul>
 * 
 * <p>Thread-safe for concurrent event firing.
 * 
 * <p><b>Python callback invocation:</b>
 * Python callbacks are Py4J proxy objects. They're invoked using reflection
 * to call the __call__ method (Python callables).
 * 
 * <p><b>Event cancellation:</b>
 * If an event is cancelled by a callback, remaining callbacks are not invoked.
 * This allows mods to prevent actions from occurring.
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class EventBus {
    
    private final Map<String, List<Object>> pythonCallbacks;
    private final ExecutorService asyncExecutor;
    private boolean verboseLogging = false;  // Can be made configurable
    
    /**
     * Creates a new event bus.
     */
    public EventBus() {
        this.pythonCallbacks = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Registers a Python callback for the specified event.
     * 
     * @param eventName Name of the event (e.g., "block_place")
     * @param callback Python callback object (Py4J proxy)
     */
    public void registerPythonCallback(String eventName, Object callback) {
        pythonCallbacks.computeIfAbsent(eventName, k -> new ArrayList<>()).add(callback);
        System.out.println("[EventBus] Registered Python callback for event: " + eventName);
    }
    
    /**
     * Unregisters a Python callback for the specified event.
     * 
     * @param eventName Name of the event
     * @param callback Python callback object to remove
     */
    public void unregisterPythonCallback(String eventName, Object callback) {
        List<Object> callbacks = pythonCallbacks.get(eventName);
        if (callbacks != null) {
            callbacks.remove(callback);
            System.out.println("[EventBus] Unregistered Python callback for event: " + eventName);
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
        List<Object> callbacks = pythonCallbacks.get(eventName);
        
        if (callbacks == null || callbacks.isEmpty()) {
            if (verboseLogging) {
                System.out.println("[EventBus] No callbacks registered for event: " + eventName);
            }
            return;
        }
        
        if (verboseLogging) {
            System.out.println("[EventBus] Firing event: " + eventName + " to " + callbacks.size() + " callbacks");
        }
        
        // Invoke each callback
        for (Object callback : callbacks) {
            try {
                // Invoke Python callable via Py4J
                // Python callables have a __call__ method
                invokePythonCallback(callback, event);
                
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
     * Invokes a Python callback with the event as parameter.
     * 
     * <p>Uses reflection to call the Python callable's __call__ method.
     * This is how Py4J proxy objects are invoked from Java.
     * 
     * @param callback Python callback object (Py4J proxy)
     * @param event Event to pass to callback
     * @throws Exception if invocation fails
     */
    private void invokePythonCallback(Object callback, Event event) throws Exception {
        // Python callables can be invoked directly if they have a __call__ method
        // Py4J handles this automatically when we call methods on the proxy
        
        try {
            // Try to find and invoke the __call__ method
            Method callMethod = callback.getClass().getMethod("__call__", Object.class);
            callMethod.invoke(callback, event);
        } catch (NoSuchMethodException e) {
            // Fallback: try calling the callback directly as a method
            // Some Python objects might be wrapped differently
            try {
                Method[] methods = callback.getClass().getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("__call__")) {
                        method.invoke(callback, event);
                        return;
                    }
                }
                // If no __call__ found, log warning
                System.err.println("[EventBus] Python callback doesn't have __call__ method: " + callback.getClass());
            } catch (Exception ex) {
                throw new Exception("Failed to invoke Python callback: " + ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Clears all callbacks for the specified event.
     * Useful for mod unloading.
     * 
     * @param eventName Name of the event
     */
    public void clearCallbacks(String eventName) {
        pythonCallbacks.remove(eventName);
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
        List<Object> callbacks = pythonCallbacks.get(eventName);
        return callbacks != null ? callbacks.size() : 0;
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
