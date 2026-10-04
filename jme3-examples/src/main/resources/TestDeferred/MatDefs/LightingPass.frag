out vec4 fragColor;
in vec2 texCoord;
uniform sampler2D m_Albedo;
uniform sampler2D m_Normal;
uniform sampler2D m_WorldPosition;
uniform vec3 m_ViewPosition; // TODO: Can we do better and get that from the projection matrix?

// Migrate this to UBO/SSBO once available
const int NR_POINTLIGHTS = 8;
uniform vec4 m_PointLight_Position[NR_POINTLIGHTS];
uniform vec4 m_PointLight_Color[NR_POINTLIGHTS];

float lightRadiusMultiplier(float radius, float maxRadius) {
    return (1.0 - step(maxRadius, radius));
}

void main() {
    vec3 Albedo = texture2D(m_Albedo, texCoord).rgb;
    vec3 FragPos = texture2D(m_WorldPosition, texCoord).rgb;
    vec3 Normal = texture2D(m_Normal, texCoord).rgb;

    vec3 lighting = Albedo * 0.1; // Hard-Coded Ambient Component
    vec3 viewDir = normalize(m_ViewPosition - FragPos);

    // See https://learnopengl.com/Advanced-Lighting/Deferred-Shading, licensed CC-BY-NC
    for(int i = 0; i < NR_POINTLIGHTS; ++i)
    {
        vec3 lightDir = m_PointLight_Position[i].rgb - FragPos;
        vec3 lightDirNormal = normalize(lightDir);
        float lightDist = length(lightDir);
        vec3 diffuse = max(dot(Normal, lightDirNormal), 0.0) * Albedo * m_PointLight_Color[i].rgb;
        lighting += diffuse * lightRadiusMultiplier(lightDist, m_PointLight_Position[i].a); // Handle the radius culling basically.
    }

    fragColor = vec4(lighting, 1.0);
}
