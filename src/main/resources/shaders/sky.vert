#version 330 core

layout(location = 0) in vec2 aPosition;

out vec2 vScreenPos;

void main() {
    vec4 clipPos = vec4(aPosition, 0.0, 1.0);
    gl_Position = clipPos;
    vScreenPos = aPosition;
}
