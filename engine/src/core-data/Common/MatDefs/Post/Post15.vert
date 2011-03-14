uniform mat4 g_WorldViewProjectionMatrix;

in vec4 inPosition;
in vec2 inTexCoord;

out vec2 texCoord;

void main() {
    vec2 pos = (g_WorldViewProjectionMatrix * inPosition).xy;
    gl_Position = vec4(pos, 0.0, 1.0);
    texCoord = inTexCoord;
}