#version 450

layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec2 inTexCoord;
layout (location = 2) in vec3 inNormal;

layout (location = 0) out vec3 fragColor;
layout (location = 1) out vec2 texCoord;

layout (set = 0, binding = 0) uniform CameraBuffer {
    mat4 worldViewProjectionMatrix;
} cam;

void main() {
    gl_Position = cam.worldViewProjectionMatrix * vec4(inPosition, 1.0);
    texCoord = inTexCoord;
}