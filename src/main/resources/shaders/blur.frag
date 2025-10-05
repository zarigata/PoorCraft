#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform vec2 uDirection; // (1, 0) for horizontal, (0, 1) for vertical
uniform vec2 uTexelSize;  // 1.0 / texture dimensions

// 9-tap box blur kernel
const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec3 result = texture(uTexture, vTexCoord).rgb * weights[0];
    
    for(int i = 1; i < 5; i++) {
        vec2 offset = uDirection * uTexelSize * float(i);
        result += texture(uTexture, vTexCoord + offset).rgb * weights[i];
        result += texture(uTexture, vTexCoord - offset).rgb * weights[i];
    }
    
    FragColor = vec4(result, 1.0);
}
