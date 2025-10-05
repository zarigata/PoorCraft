package com.poorcraft.launcher

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertTrue

class IntegrationTest {
    
    @Test
    fun `should run engine in headless mode`(@TempDir tempDir: Path) {
        // This test verifies that the engine can start and run in headless mode
        // The engine will auto-stop after 10 seconds (200 ticks)
        
        val launcher = Launcher(arrayOf("--headless"))
        
        // Set up temporary data directory
        System.setProperty("user.dir", tempDir.toString())
        
        try {
            launcher.launch()
            // If we reach here, the engine ran successfully
            assertTrue(true)
        } catch (e: Exception) {
            // Log the exception for debugging
            println("Integration test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
