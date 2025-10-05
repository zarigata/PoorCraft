package com.poorcraft.engine.event

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EventBusTest {
    
    @Test
    fun `should register and dispatch events`() {
        val eventBus = EventBus()
        var called = false
        var receivedValue: Int? = null
        
        eventBus.register("test_event") { args ->
            called = true
            receivedValue = args[0] as Int
        }
        
        eventBus.dispatch("test_event", 42)
        
        assertEquals(true, called)
        assertEquals(42, receivedValue)
    }
    
    @Test
    fun `should support multiple listeners for same event`() {
        val eventBus = EventBus()
        var count = 0
        
        eventBus.register("test_event") { count++ }
        eventBus.register("test_event") { count++ }
        eventBus.register("test_event") { count++ }
        
        eventBus.dispatch("test_event")
        
        assertEquals(3, count)
    }
    
    @Test
    fun `should unregister listeners`() {
        val eventBus = EventBus()
        var count = 0
        
        val listener = EventListener { count++ }
        eventBus.register("test_event", listener)
        eventBus.dispatch("test_event")
        
        assertEquals(1, count)
        
        eventBus.unregister("test_event", listener)
        eventBus.dispatch("test_event")
        
        assertEquals(1, count) // Should not increment
    }
    
    @Test
    fun `should get listener count`() {
        val eventBus = EventBus()
        
        assertEquals(0, eventBus.getListenerCount("test_event"))
        
        eventBus.register("test_event") {}
        assertEquals(1, eventBus.getListenerCount("test_event"))
        
        eventBus.register("test_event") {}
        assertEquals(2, eventBus.getListenerCount("test_event"))
    }
}
