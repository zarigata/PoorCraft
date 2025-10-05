#version 330 core

in vec3 vLocalPos;

out vec4 FragColor;

uniform float uProgress;
uniform bool uIsOutline;

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}

void main() {
    if (uIsOutline) {
        FragColor = vec4(0.0, 0.0, 0.0, 0.75);
        return;
    }

    float baseAlpha = mix(0.18, 0.45, clamp(uProgress, 0.0, 1.0));

    vec3 cell = floor(vLocalPos * (3.0 + uProgress * 10.0));
    float crack = smoothstep(0.55, 0.95, hash(cell) + uProgress * 0.8);

    float alpha = clamp(baseAlpha + crack * 0.25, 0.05, 0.8);
    FragColor = vec4(vec3(0.0), alpha);
}
