/**
 * Multiplayer networking with client-server architecture.
 * 
 * <p>This package implements a complete multiplayer system using Netty:</p>
 * <ul>
 *   <li>{@link com.poorcraft.network.server} - Authoritative game server</li>
 *   <li>{@link com.poorcraft.network.client} - Thin game client</li>
 *   <li>{@link com.poorcraft.network.packet} - Network packet definitions</li>
 *   <li>{@link com.poorcraft.network.codec} - Packet encoding/decoding</li>
 * </ul>
 * 
 * <h2>Architecture:</h2>
 * <p>PoorCraft uses an authoritative server model where the server owns all
 * world state and validates player actions. Clients are thin views that receive
 * updates from the server and send player input.</p>
 * 
 * <h2>Protocol:</h2>
 * <ul>
 *   <li>Transport: TCP with Netty</li>
 *   <li>Framing: Length-prefixed (4-byte header)</li>
 *   <li>Serialization: Custom ByteBuf-based</li>
 *   <li>Keep-alive: 15-second interval, 30-second timeout</li>
 * </ul>
 * 
 * <h2>Packet Types:</h2>
 * <p>14 packet types handle handshake, login, chunk streaming, player movement,
 * block updates, and connection management. See {@link com.poorcraft.network.packet}
 * for all packet definitions.</p>
 * 
 * <h2>Server Tick Rate:</h2>
 * <p>The server runs at 20 TPS (50ms per tick), similar to Minecraft. Clients
 * render at uncapped FPS with interpolation for smooth remote player movement.</p>
 * 
 * @see com.poorcraft.network.server.GameServer
 * @see com.poorcraft.network.client.GameClient
 * @since 0.1.1
 */
package com.poorcraft.network;
