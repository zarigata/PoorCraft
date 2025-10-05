#version 330 core

// Input vertex attributes
layout(location = 0) in vec3 aPosition;  // Vertex position in local chunk space
layout(location = 1) in vec2 aTexCoord;  // Texture coordinates
layout(location = 2) in vec3 aNormal;    // Vertex normal for lighting

// Output variables to fragment shader
out vec2 vTexCoord;   // Pass texture coordinates
out vec3 vNormal;     // Pass normal for lighting calculations
out vec3 vFragPos;    // Pass world position for lighting
out float vFogDistance; // Distance for fog calculation

uniform mat4 uModel;      // Model matrix (chunk world position)

#ifdef USE_UBO
layout(std140, binding = 0) uniform SharedUniforms {
    mat4 uProjectionBlock;
    mat4 uViewBlock;
    vec4 uLightDirectionBlock;
    vec4 uLightColorBlock;
    vec4 uAmbientColorBlock;
    vec4 uAmbientParamsBlock;
    vec4 uFogColorBlock;
    vec4 uFogStartBlock;
    vec4 uFogEndBlock;
};
#else
uniform mat4 uView;       // View matrix (camera)
uniform mat4 uProjection; // Projection matrix (perspective)
#endif

#ifdef USE_UBO
#define VIEW_MATRIX   uViewBlock
#define PROJ_MATRIX   uProjectionBlock
#else
#define VIEW_MATRIX   uView
#define PROJ_MATRIX   uProjection
#endif

void main() {
    // Transform vertex position to world space
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    
    // Transform to clip space (final position)
    gl_Position = PROJ_MATRIX * VIEW_MATRIX * worldPos;
    
    // Pass texture coordinates through unchanged
    vTexCoord = aTexCoord;
    
    // Transform normal to world space (use mat3 to ignore translation)
    // This is important for proper lighting - normals are directions, not positions
    vNormal = mat3(uModel) * aNormal;

    // Pass world position for lighting calculations in fragment shader
    vFragPos = worldPos.xyz;

    // Distance from camera for fog
    vec3 viewSpacePos = (VIEW_MATRIX * worldPos).xyz;
    vFogDistance = length(viewSpacePos);
}
#undef VIEW_MATRIX
#undef PROJ_MATRIX
