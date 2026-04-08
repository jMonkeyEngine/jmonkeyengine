#version 450

layout (location = 0) in vec2 texCoord;
layout (location = 0) out vec4 outColor;
layout(set = 0, binding = 0) uniform sampler2D m_ColorTexture;
layout(std140) readonly buffer ssbo1 {
    vec4 lights[];
};

void main() {
    outColor = texture(colorTexture, texCoord);
}