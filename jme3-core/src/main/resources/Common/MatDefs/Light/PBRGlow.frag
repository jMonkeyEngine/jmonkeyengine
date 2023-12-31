#import "Common/ShaderLib/GLSLCompat.glsllib"
#if defined(NEED_TEXCOORD1) 
    varying vec2 texCoord1;
#else 
    varying vec2 texCoord;
#endif


#ifdef HAS_EMISSIVEMAP
  uniform sampler2D m_EmissiveMap;
#endif

#ifdef HAS_EMISSIVECOLOR
  uniform vec4 m_Emissive;
#endif


void main(){
    #ifdef HAS_EMISSIVEMAP
        #ifdef HAS_EMISSIVECOLOR
           vec4 color = m_Emissive;
        #else
           vec4 color = vec4(1.0);
        #endif

        #if defined(NEED_TEXCOORD1) 
           gl_FragColor = texture2D(m_EmissiveMap, texCoord1) * color;
        #else 
           gl_FragColor = texture2D(m_EmissiveMap, texCoord) * color;
        #endif
    #else
        #ifdef HAS_EMISSIVECOLOR
            gl_FragColor =  m_Emissive;
        #else
            gl_FragColor = vec4(0.0);
        #endif
    #endif
}
