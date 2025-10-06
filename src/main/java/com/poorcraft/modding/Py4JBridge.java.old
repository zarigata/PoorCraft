package com.poorcraft.modding;

import py4j.GatewayServer;

/**
 * Manages the Py4J gateway server that enables Python-Java communication.
 * 
 * <p>This bridge creates a GatewayServer that listens on port 25333 (default Py4J port).
 * Python mods connect to this gateway to access the ModAPI and interact with the game.
 * 
 * <p>Architecture:
 * <ul>
 *   <li>Java hosts the GatewayServer (this class)</li>
 *   <li>Python connects as a client using py4j.java_gateway.JavaGateway()</li>
 *   <li>ModAPI is the entry point object accessible from Python</li>
 *   <li>Gateway runs in its own thread (non-blocking)</li>
 * </ul>
 * 
 * <p>Connection flow:
 * <ol>
 *   <li>Java calls start() to create and start GatewayServer</li>
 *   <li>Python connects to localhost:25333</li>
 *   <li>Python accesses ModAPI via gateway.entry_point</li>
 *   <li>Python mods call ModAPI methods to interact with game</li>
 * </ol>
 * 
 * <p><b>Important:</b> This must be started before loading Python mods, otherwise
 * mods will fail to connect to the gateway.
 * 
 * @author PoorCraft Team
 * @version 1.0
 */
public class Py4JBridge {
    
    private GatewayServer gatewayServer;
    private final ModAPI modAPI;
    private volatile boolean running;
    private static final int GATEWAY_PORT = 25333;
    
    /**
     * Creates a new Py4J bridge with the specified ModAPI instance.
     * 
     * @param modAPI The API object to expose to Python mods
     */
    public Py4JBridge(ModAPI modAPI) {
        this.modAPI = modAPI;
        this.running = false;
    }
    
    /**
     * Starts the Py4J gateway server.
     * 
     * <p>Creates a GatewayServer on port 25333 and starts it in a background thread.
     * Waits up to 5 seconds for Python to connect. If the server fails to start,
     * throws a RuntimeException.
     * 
     * @throws RuntimeException if gateway fails to start
     */
    public void start() {
        try {
            System.out.println("[Py4JBridge] Starting gateway server on port " + GATEWAY_PORT + "...");
            
            // Create gateway server with ModAPI as entry point
            gatewayServer = new GatewayServer(modAPI, GATEWAY_PORT);
            
            // Start server in background thread (non-blocking)
            gatewayServer.start();
            
            running = true;
            
            System.out.println("[Py4JBridge] Gateway server started successfully");
            System.out.println("[Py4JBridge] Python mods can now connect to localhost:" + GATEWAY_PORT);
            
            // Wait a bit for Python to connect (optional, helps with timing)
            Thread.sleep(1000);
            
        } catch (Exception e) {
            running = false;
            throw new RuntimeException("[Py4JBridge] Failed to start gateway server: " + e.getMessage(), e);
        }
    }
    
    /**
     * Stops the Py4J gateway server.
     * 
     * <p>Shuts down the gateway server gracefully. Exceptions are caught and logged
     * but not thrown, to ensure cleanup always completes.
     */
    public void stop() {
        if (running && gatewayServer != null) {
            try {
                System.out.println("[Py4JBridge] Shutting down gateway server...");
                gatewayServer.shutdown();
                running = false;
                System.out.println("[Py4JBridge] Gateway server shut down successfully");
            } catch (Exception e) {
                System.err.println("[Py4JBridge] Error shutting down gateway: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if the gateway server is running.
     * 
     * @return true if server is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Gets the gateway server instance.
     * 
     * @return The GatewayServer instance, or null if not started
     */
    public GatewayServer getGatewayServer() {
        return gatewayServer;
    }
    
    /**
     * Gets the port number the gateway is listening on.
     * 
     * @return The gateway port (25333)
     */
    public int getPort() {
        return GATEWAY_PORT;
    }
}
