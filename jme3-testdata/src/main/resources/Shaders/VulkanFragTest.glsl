#version 450

layout(location = 0) in vec3 fragColor;
layout(location = 1) in vec2 texCoord;

layout(location = 0) out vec4 outColor;

layout(binding = 1) uniform sampler2D colorTexture;

void main() {
    outColor = texture(colorTexture, texCoord) * vec4(fragColor, 1.0);
}