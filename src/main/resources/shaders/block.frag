#version 330 core

// Input variables from vertex shader
in vec2 vTexCoord;  // Texture coordinates
in vec3 vNormal;    // Normal vector (for lighting)
in vec3 vFragPos;   // World position (for lighting)
in float vFogDistance; // Distance for fog calculation

// Output color
out vec4 FragColor;

// Uniforms
uniform sampler2D uTexture;        // Texture atlas sampler

#ifdef USE_UBO
layout(std140, binding = 0) uniform SharedUniforms {
    mat4 uProjectionShared;
    mat4 uViewShared;
    vec4 uLightDirectionShared;
    vec4 uLightColorShared;
    vec4 uAmbientColorShared;
    vec4 uAmbientParamsShared;
    vec4 uFogColorShared;
    vec4 uFogParamsShared;
};
#define U_LIGHT_DIRECTION (uLightDirectionShared.xyz)
#define U_LIGHT_COLOR (uLightColorShared.rgb)
#define U_AMBIENT_COLOR (uAmbientColorShared.rgb)
#define U_AMBIENT_STRENGTH (uAmbientParamsShared.x)
#define U_FOG_COLOR (uFogColorShared.rgb)
#define U_FOG_START (uFogParamsShared.x)
#define U_FOG_END (uFogParamsShared.y)
#else
uniform vec3 uLightDirection;      // Directional light direction (normalized)
uniform vec3 uLightColor;          // Directional light color
uniform vec3 uAmbientColor;        // Ambient light color
uniform float uAmbientStrength;    // Ambient light strength (0.0 to 1.0)
uniform vec3 uFogColor;            // Fog color
uniform float uFogStart;           // Distance where fog starts
uniform float uFogEnd;             // Distance where fog is fully opaque
#define U_LIGHT_DIRECTION uLightDirection
#define U_LIGHT_COLOR uLightColor
#define U_AMBIENT_COLOR uAmbientColor
#define U_AMBIENT_STRENGTH uAmbientStrength
#define U_FOG_COLOR uFogColor
#define U_FOG_START uFogStart
#define U_FOG_END uFogEnd
#endif

void main() {
    // Sample texture from atlas
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Discard fully transparent fragments (for leaves, etc.)
    if (texColor.a < 0.1) {
        discard;
    }

    // Calculate ambient lighting (base illumination)
    vec3 ambient = U_AMBIENT_STRENGTH * U_AMBIENT_COLOR;
    
    // Calculate diffuse lighting (Lambertian reflectance)
    vec3 norm = normalize(vNormal);
    
    // Light direction points FROM the light source, so we negate it
    // This gives us the direction TO the light, which is what we need for the dot product
    float diff = max(dot(norm, -U_LIGHT_DIRECTION), 0.0);
    vec3 diffuse = diff * U_LIGHT_COLOR;
    
    // Combine lighting components
    vec3 lighting = ambient + diffuse;
    
    // Clamp lighting to prevent over-brightening
    // (Though honestly, who doesn't like a little extra glow?)
    lighting = clamp(lighting, 0.0, 1.0);
    
    // Apply lighting to texture color
    vec3 result = lighting * texColor.rgb;
    
    // Apply distance-based fog
    float fogFactor = clamp((vFogDistance - U_FOG_START) / max(U_FOG_END - U_FOG_START, 0.001), 0.0, 1.0);
    vec3 foggedColor = mix(result, U_FOG_COLOR, fogFactor);
    
    // Output final color with original alpha
    FragColor = vec4(foggedColor, texColor.a);
}
