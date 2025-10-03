#version 330 core

// Vertex attributes
layout(location = 0) in vec2 aPosition;  // Vertex position (0-1 range for unit quad)
layout(location = 1) in vec2 aTexCoord;  // Texture coordinates

// Output to fragment shader
out vec2 vTexCoord;

// Uniforms
uniform mat4 uProjection;  // Orthographic projection matrix
uniform mat4 uModel;       // Model matrix (position/scale)

void main() {
    // Transform position from model space to screen space
    // No view matrix needed - UI is always in screen space
    gl_Position = uProjection * uModel * vec4(aPosition, 0.0, 1.0);
    
    // Pass texture coordinates to fragment shader
    vTexCoord = aTexCoord;
}
