#ifdef TEXTURE
uniform sampler2D m_Texture;
varying vec2 texCoord;
#endif

varying vec4 color;

void main() {
    #ifdef TEXTURE
      vec4 texVal = texture2D(m_Texture, texCoord);
      gl_FragColor = texVal * color;
    #else
      gl_FragColor = color;
    #endif
}

