#version 330 core

in vec2 vScreenPos;

out vec4 FragColor;

uniform vec3 uCameraForward;
uniform vec3 uCameraRight;
uniform vec3 uCameraUp;
uniform float uFov;
uniform float uAspect;
uniform vec3 uSunDirection;
uniform float uTime;

// Simple hash-based pseudo noise
float hash(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)),
             dot(p, vec2(269.5, 183.3)));
    return fract(sin(p.x + p.y) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    for (int i = 0; i < 4; ++i) {
        value += amplitude * noise(p * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    float tanHalfFov = tan(radians(uFov) * 0.5);
    vec2 ndc = vScreenPos; // already in [-1, 1]

    vec3 viewDir = normalize(
        uCameraForward +
        uCameraRight * ndc.x * tanHalfFov * uAspect +
        uCameraUp * ndc.y * tanHalfFov
    );

    vec3 horizonColor = vec3(0.65, 0.72, 0.85);
    vec3 zenithColor = vec3(0.10, 0.19, 0.37);

    float skyFactor = clamp(viewDir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 baseSky = mix(horizonColor, zenithColor, pow(skyFactor, 1.5));

    // Sun glow
    vec3 sunDir = normalize(-uSunDirection);
    float sunAmount = max(dot(viewDir, sunDir), 0.0);
    float sunDisc = smoothstep(0.9995, 1.0, sunAmount);
    float sunGlow = smoothstep(0.96, 1.0, sunAmount);

    vec3 sunColor = vec3(1.0, 0.92, 0.76);
    baseSky += sunGlow * sunColor * 0.35;
    baseSky += sunDisc * sunColor * 0.8;

    // Clouds
    vec2 cloudUV = viewDir.xz / max(0.1, viewDir.y + 1.2);
    cloudUV *= 40.0;
    cloudUV += vec2(uTime * 0.015, uTime * 0.01);

    float cloudDensity = fbm(cloudUV) * 0.9;
    cloudDensity = smoothstep(0.55, 0.75, cloudDensity);

    vec3 cloudColor = mix(vec3(0.95, 0.97, 0.99), sunColor, sunGlow * 0.2);
    baseSky = mix(baseSky, cloudColor, cloudDensity * 0.65);

    FragColor = vec4(baseSky, 1.0);
}
