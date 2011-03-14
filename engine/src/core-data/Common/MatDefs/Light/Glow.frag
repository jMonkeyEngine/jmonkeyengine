varying vec2 texCoord;

#ifdef HAS_GLOWMAP
  uniform sampler2D m_GlowMap;
#endif

#ifdef HAS_GLOWCOLOR
  uniform vec4 m_GlowColor;
#endif


void main(){
   
    #ifdef HAS_GLOWMAP
        gl_FragColor = texture2D(m_GlowMap, texCoord);
    #else
        #ifdef HAS_GLOWCOLOR
            gl_FragColor =  m_GlowColor;
        #else
            gl_FragColor = vec4(0.0);
        #endif
    #endif
}