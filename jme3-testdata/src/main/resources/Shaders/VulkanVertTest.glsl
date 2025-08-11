#version 450

layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec3 inColor;
layout (location = 2) in vec2 inTexCoord;

layout (location = 0) out vec3 fragColor;
layout (location = 1) out vec2 texCoord;

layout (binding = 0) uniform UniformBufferObject {
    mat4 worldViewProjectionMatrix;
} ubo;

void main() {
    gl_Position = ubo.worldViewProjectionMatrix * vec4(inPosition, 1.0);
    fragColor = inColor;
    texCoord = inTexCoord;
}