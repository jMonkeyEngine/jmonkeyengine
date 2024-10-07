layout (location = 0) out vec4 Red;
layout (location = 1) out vec4 Green;
layout (location = 2) out vec4 Blue;
layout (location = 3) out vec4 Merged;
uniform vec4 m_Albedo;

void main() {
    Red.r = m_Albedo.r;
    Green.r = m_Albedo.g;
    Blue.r = m_Albedo.b;

    // This is cheating, typically you would use the red, green and blue as a texture and render/combine them with another shader.
    Merged = m_Albedo;
}
