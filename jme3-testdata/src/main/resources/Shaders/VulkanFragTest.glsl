#version 450

#in(location = 0) vec2 texCoord;
#out(location = 0) vec4 outColor;
layout(set = 0, binding = 0) uniform sampler2D m_ColorTexture;
layout(std140) readonly buffer ssbo1 {
    vec4 lights[];
};

void main() {
    outColor = texture(colorTexture, texCoord);
}