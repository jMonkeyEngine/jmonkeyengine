#version 450

layout(location = 1) in vec2 texCoord;

layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 1) uniform sampler2D colorTexture;

void main() {
    outColor = texture(colorTexture, texCoord);
}