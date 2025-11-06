#version 330 core

in vec2 vTexCoord;

uniform sampler2D uTexture;

out vec4 FragColor;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Discard transparent pixels
    if (texColor.a < 0.1) {
        discard;
    }
    
    // Apply simple ambient lighting
    float ambientStrength = 0.7;
    vec3 ambient = ambientStrength * texColor.rgb;
    
    FragColor = vec4(ambient, texColor.a);
}
