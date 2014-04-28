uniform mat4 g_WorldViewProjectionMatrix;
uniform vec4 m_Color;

attribute vec3 inPosition;

#ifdef VERTEX_COLOR
    attribute vec4 inColor;
#endif

#ifdef TEXTURE
    attribute vec2 inTexCoord;
    varying vec2 texCoord;
#endif

varying vec4 color;

void main() {
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    #ifdef TEXTURE
        texCoord = inTexCoord;
    #endif
    #ifdef VERTEX_COLOR
        color = m_Color * inColor;
    #else
        color = m_Color;
    #endif
}