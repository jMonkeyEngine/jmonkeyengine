uniform mat4 g_WorldViewProjectionMatrix;
uniform vec4 m_Color;

attribute vec4 inPosition;
attribute vec4 inColor;

varying vec2 texCoord;
varying vec4 color;

void main() {
    vec2 pos = (g_WorldViewProjectionMatrix * inPosition).xy;
    gl_Position = vec4(pos, 0.0, 1.0);

    color = inColor * m_Color;
}