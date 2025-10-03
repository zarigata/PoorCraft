#version 330 core

// Input from vertex shader
in vec2 vTexCoord;

// Output color
out vec4 FragColor;

// Uniforms
uniform vec4 uColor;              // Color tint (RGBA)
uniform sampler2D uTexture;       // Texture sampler
uniform bool uUseTexture;         // Whether to sample texture or use solid color

void main() {
    if (uUseTexture) {
        // Sample texture and multiply by color tint
        vec4 texColor = texture(uTexture, vTexCoord);
        FragColor = texColor * uColor;
    } else {
        // Use solid color
        FragColor = uColor;
    }
}
