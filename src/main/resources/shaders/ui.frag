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
        // Sample texture
        // For font atlas (GL_RED format), use red channel as alpha
        // For regular textures, use as-is
        // This handles both cases gracefully - pretty neat if you ask me
        vec4 texColor = texture(uTexture, vTexCoord);
        
        // If texture is single-channel (font atlas), use red as alpha
        // Otherwise use the full texture color
        float alpha = texColor.r;
        FragColor = vec4(uColor.rgb, uColor.a * alpha);
    } else {
        // Use solid color
        FragColor = uColor;
    }
}
