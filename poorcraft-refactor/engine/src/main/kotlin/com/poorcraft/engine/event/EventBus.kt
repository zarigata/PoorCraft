package com.poorcraft.engine.event

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Event bus for dispatching events to listeners
 */
class EventBus {
    private val logger = LoggerFactory.getLogger(EventBus::class.java)
    private val listeners = ConcurrentHashMap<String, MutableList<EventListener>>()
    
    fun register(eventName: String, listener: EventListener) {
        listeners.computeIfAbsent(eventName) { mutableListOf() }.add(listener)
        logger.debug("Registered listener for event: $eventName")
    }
    
    fun unregister(eventName: String, listener: EventListener) {
        listeners[eventName]?.remove(listener)
    }
    
    fun dispatch(eventName: String, vararg args: Any?) {
        val eventListeners = listeners[eventName] ?: return
        
        for (listener in eventListeners) {
            try {
                listener.onEvent(*args)
            } catch (e: Exception) {
                logger.error("Error dispatching event $eventName to listener", e)
            }
        }
    }
    
    fun clear() {
        listeners.clear()
    }
    
    fun getListenerCount(eventName: String): Int = listeners[eventName]?.size ?: 0
}

/**
 * Event listener interface
 */
fun interface EventListener {
    fun onEvent(vararg args: Any?)
}
