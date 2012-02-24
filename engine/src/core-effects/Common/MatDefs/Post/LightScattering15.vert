uniform mat4 g_WorldViewProjectionMatrix;
uniform vec3 m_LightPosition;

in vec4 inPosition;
in vec2 inTexCoord;
out vec2 texCoord;
out vec2 lightPos;

void main() {
    vec2 pos = (g_WorldViewProjectionMatrix * inPosition).xy;
    gl_Position = vec4(pos, 0.0, 1.0);
    lightPos=m_LightPosition.xy;
    texCoord = inTexCoord;
}