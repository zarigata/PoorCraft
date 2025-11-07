#version 330 core

in vec2 vTexCoord;
in vec3 vNormal;
in vec3 vFragPos;

out vec4 FragColor;

uniform sampler2D uTexture;
uniform vec3 uLightDirection;
uniform vec3 uLightColor;
uniform float uAmbientStrength;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    if (texColor.a < 0.1) {
        discard;
    }

    vec3 norm = normalize(vNormal);
    float diff = max(dot(norm, -normalize(uLightDirection)), 0.0);

    vec3 ambient = vec3(uAmbientStrength);
    vec3 diffuse = diff * uLightColor;
    vec3 lighting = clamp(ambient + diffuse, 0.0, 1.0);

    vec3 result = lighting * texColor.rgb;
    FragColor = vec4(result, texColor.a);
}
