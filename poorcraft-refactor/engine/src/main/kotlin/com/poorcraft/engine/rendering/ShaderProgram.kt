package com.poorcraft.engine.rendering

import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack
import org.slf4j.LoggerFactory

/**
 * Shader program wrapper
 */
class ShaderProgram(private val programId: Int) {
    private val logger = LoggerFactory.getLogger(ShaderProgram::class.java)
    private val uniformLocations = mutableMapOf<String, Int>()
    
    companion object {
        fun createDefault(): ShaderProgram {
            val vertexShader = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec2 aTexCoord;
                layout (location = 2) in float aShade;
                
                out vec2 TexCoord;
                out float Shade;
                
                uniform mat4 projection;
                uniform mat4 view;
                uniform mat4 model;
                
                void main() {
                    gl_Position = projection * view * model * vec4(aPos, 1.0);
                    TexCoord = aTexCoord;
                    Shade = aShade;
                }
            """.trimIndent()
            
            val fragmentShader = """
                #version 330 core
                in vec2 TexCoord;
                in float Shade;
                
                out vec4 FragColor;
                
                uniform sampler2D texture1;
                
                void main() {
                    vec4 texColor = texture(texture1, TexCoord);
                    if (texColor.a < 0.1)
                        discard;
                    FragColor = texColor * vec4(vec3(Shade), 1.0);
                }
            """.trimIndent()
            
            return create(vertexShader, fragmentShader)
        }
        
        fun create(vertexSource: String, fragmentSource: String): ShaderProgram {
            val vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER)
            val fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER)
            
            val program = glCreateProgram()
            glAttachShader(program, vertexShader)
            glAttachShader(program, fragmentShader)
            glLinkProgram(program)
            
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                throw RuntimeException("Failed to link shader program: ${glGetProgramInfoLog(program)}")
            }
            
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)
            
            return ShaderProgram(program)
        }
        
        private fun compileShader(source: String, type: Int): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, source)
            glCompileShader(shader)
            
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                throw RuntimeException("Failed to compile shader: ${glGetShaderInfoLog(shader)}")
            }
            
            return shader
        }
    }
    
    fun use() {
        glUseProgram(programId)
    }
    
    fun setUniform(name: String, value: Matrix4f) {
        val location = getUniformLocation(name)
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(16)
            value.get(buffer)
            glUniformMatrix4fv(location, false, buffer)
        }
    }
    
    fun setUniform(name: String, value: Float) {
        val location = getUniformLocation(name)
        glUniform1f(location, value)
    }
    
    fun setUniform(name: String, value: Int) {
        val location = getUniformLocation(name)
        glUniform1i(location, value)
    }
    
    private fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            glGetUniformLocation(programId, name)
        }
    }
    
    fun cleanup() {
        glDeleteProgram(programId)
    }
}
