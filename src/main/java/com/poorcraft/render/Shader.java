package com.poorcraft.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Manages OpenGL shader programs for rendering.
 * 
 * This class handles compiling GLSL vertex and fragment shaders, linking them into
 * a shader program, and providing convenient methods for setting uniform variables.
 * 
 * Uniform locations are cached to avoid redundant glGetUniformLocation calls.
 * 
 * @author PoorCraft Team
 */
public class Shader {
    
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private Map<String, Integer> uniformLocations;
    
    /**
     * Creates a shader program from vertex and fragment shader source code.
     * 
     * @param vertexSource GLSL vertex shader source code
     * @param fragmentSource GLSL fragment shader source code
     * @throws RuntimeException if shader compilation or linking fails
     */
    public Shader(String vertexSource, String fragmentSource) {
        this.uniformLocations = new HashMap<>();
        
        // Compile shaders - this is where the magic happens... or errors, lots of errors
        vertexShaderId = compileShader(vertexSource, GL_VERTEX_SHADER);
        fragmentShaderId = compileShader(fragmentSource, GL_FRAGMENT_SHADER);
        
        // Link them together into a program
        linkProgram();
    }
    
    /**
     * Compiles a shader from source code.
     * 
     * @param source GLSL shader source code
     * @param type Shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @return OpenGL shader ID
     * @throws RuntimeException if compilation fails
     */
    private int compileShader(String source, int type) {
        int shaderId = glCreateShader(type);
        if (shaderId == 0) {
            throw new RuntimeException("Failed to create shader object");
        }
        
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);
        
        // Check if compilation succeeded
        int compileStatus = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        if (compileStatus == GL_FALSE) {
            String errorLog = glGetShaderInfoLog(shaderId);
            glDeleteShader(shaderId);
            throw new RuntimeException("Shader compilation failed:\n" + errorLog);
        }
        
        return shaderId;
    }
    
    /**
     * Links vertex and fragment shaders into a shader program.
     * 
     * @throws RuntimeException if linking fails
     */
    private void linkProgram() {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Failed to create shader program");
        }
        
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        
        // Check if linking succeeded
        int linkStatus = glGetProgrami(programId, GL_LINK_STATUS);
        if (linkStatus == GL_FALSE) {
            String errorLog = glGetProgramInfoLog(programId);
            throw new RuntimeException("Shader linking failed:\n" + errorLog);
        }
        
        // Shaders are linked into the program now, we can detach and delete them
        // They're like training wheels - needed for building but not for running
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }
    
    /**
     * Binds this shader program for use in rendering.
     */
    public void bind() {
        glUseProgram(programId);
    }
    
    /**
     * Unbinds the current shader program.
     */
    public void unbind() {
        glUseProgram(0);
    }
    
    /**
     * Gets the location of a uniform variable, caching the result.
     * 
     * @param name Uniform variable name
     * @return Uniform location, or -1 if not found
     */
    private int getUniformLocation(String name) {
        // Check cache first - no need to ask OpenGL every time
        if (uniformLocations.containsKey(name)) {
            return uniformLocations.get(name);
        }
        
        int location = glGetUniformLocation(programId, name);
        if (location == -1) {
            System.err.println("[Shader] Warning: Uniform '" + name + "' not found or was optimized out");
        }
        
        uniformLocations.put(name, location);
        return location;
    }
    
    /**
     * Sets an integer uniform variable.
     * 
     * @param name Uniform name
     * @param value Integer value
     */
    public void setUniform(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }
    
    /**
     * Sets a float uniform variable.
     * 
     * @param name Uniform name
     * @param value Float value
     */
    public void setUniform(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }
    
    /**
     * Sets a vec3 uniform variable.
     * 
     * @param name Uniform name
     * @param value Vector3f value
     */
    public void setUniform(String name, Vector3f value) {
        glUniform3f(getUniformLocation(name), value.x, value.y, value.z);
    }
    
    /**
     * Sets a mat4 uniform variable.
     * 
     * @param name Uniform name
     * @param value Matrix4f value
     */
    public void setUniform(String name, Matrix4f value) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        value.get(buffer);
        glUniformMatrix4fv(getUniformLocation(name), false, buffer);
    }
    
    /**
     * Deletes the shader program and frees GPU resources.
     */
    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }
    
    /**
     * Loads a shader program from resource files.
     * 
     * @param vertexPath Path to vertex shader resource
     * @param fragmentPath Path to fragment shader resource
     * @return Compiled and linked Shader instance
     * @throws RuntimeException if resources cannot be loaded or shader compilation fails
     */
    public static Shader loadFromResources(String vertexPath, String fragmentPath) {
        String vertexSource = com.poorcraft.resources.ResourceManager.getInstance()
                .loadTextResource(vertexPath);
        String fragmentSource = com.poorcraft.resources.ResourceManager.getInstance()
                .loadTextResource(fragmentPath);
        
        return new Shader(vertexSource, fragmentSource);
    }
}
