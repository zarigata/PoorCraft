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
    mat4 uProjection;
    mat4 uView;
    vec4 uLightDirection;  // dir.xyz, pad
    vec4 uLightColor;
    vec4 uAmbientColor;
    vec4 uAmbientParams;   // x = ambientStrength
    vec4 uFogColor;
    vec4 uFogParams;       // x = fogStart, y = fogEnd
};
#define U_LIGHT_DIRECTION (uLightDirection.xyz)
#define U_LIGHT_COLOR (uLightColor.rgb)
#define U_AMBIENT_COLOR (uAmbientColor.rgb)
#define U_AMBIENT_STRENGTH (uAmbientParams.x)
#define U_FOG_COLOR (uFogColor.rgb)
#define U_FOG_START (uFogParams.x)
#define U_FOG_END (uFogParams.y)
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
