#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aLocalPos;

out vec3 vLocalPos;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

void main() {
    vLocalPos = aLocalPos;
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
}
