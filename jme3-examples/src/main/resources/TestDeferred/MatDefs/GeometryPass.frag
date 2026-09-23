layout (location = 0) out vec4 Position;
layout (location = 1) out vec4 Normals;
layout (location = 2) out vec4 Albedo;
uniform vec4 m_Albedo;
in vec3 worldPosition;
in vec3 worldNormal;

void main() {
    Position = vec4(worldPosition, 1.0);
    Normals = vec4(worldNormal, 1.0);
    Albedo.rgb = m_Albedo.rgb;
    Albedo.a = 1.0; // Specular
}
