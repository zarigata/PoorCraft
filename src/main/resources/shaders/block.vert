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

// Transformation matrices
uniform mat4 uModel;      // Model matrix (chunk world position)
uniform mat4 uView;       // View matrix (camera)
uniform mat4 uProjection; // Projection matrix (perspective)

void main() {
    // Transform vertex position to world space
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    
    // Transform to clip space (final position)
    gl_Position = uProjection * uView * worldPos;
    
    // Pass texture coordinates through unchanged
    vTexCoord = aTexCoord;
    
    // Transform normal to world space (use mat3 to ignore translation)
    // This is important for proper lighting - normals are directions, not positions
    vNormal = mat3(uModel) * aNormal;
    
    // Pass world position for lighting calculations in fragment shader
    vFragPos = worldPos.xyz;

    // Distance from camera for fog
    vec3 viewSpacePos = (uView * worldPos).xyz;
    vFogDistance = length(viewSpacePos);
}
