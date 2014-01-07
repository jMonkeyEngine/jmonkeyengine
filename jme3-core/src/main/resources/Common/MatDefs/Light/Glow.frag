
#if defined(NEED_TEXCOORD1) 
    varying vec2 texCoord1;
#else 
    varying vec2 texCoord;
#endif


#ifdef HAS_GLOWMAP
  uniform sampler2D m_GlowMap;
#endif

#ifdef HAS_GLOWCOLOR
  uniform vec4 m_GlowColor;
#endif


void main(){
    #ifdef HAS_GLOWMAP
        #if defined(NEED_TEXCOORD1) 
           gl_FragColor = texture2D(m_GlowMap, texCoord1);
        #else 
           gl_FragColor = texture2D(m_GlowMap, texCoord);
        #endif
    #else
        #ifdef HAS_GLOWCOLOR
            gl_FragColor =  m_GlowColor;
        #else
            gl_FragColor = vec4(0.0);
        #endif
    #endif
}