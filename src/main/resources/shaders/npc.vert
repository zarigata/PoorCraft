#version 330 core

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoord;

uniform mat4 uView;
uniform mat4 uProjection;
uniform vec3 uPosition;
uniform vec3 uCameraRight;
uniform vec3 uCameraUp;
uniform vec4 uUVRect;

out vec2 vTexCoord;

void main() {
    // Create billboard that always faces camera
    vec3 billboardOffset = (uCameraRight * aPosition.x) + (uCameraUp * aPosition.y);
    vec4 worldPos = vec4(uPosition + billboardOffset, 1.0);
    gl_Position = uProjection * uView * worldPos;
    
    // Map texture coordinates using UV rect
    vTexCoord = vec2(
        mix(uUVRect.x, uUVRect.z, aTexCoord.x),
        mix(uUVRect.y, uUVRect.w, aTexCoord.y)
    );
}
