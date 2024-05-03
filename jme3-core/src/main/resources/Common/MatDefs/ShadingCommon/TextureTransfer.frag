
#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef WRITE_COLOR
    uniform sampler2D m_ColorMap;
#endif
#ifdef WRITE_DEPTH
    uniform sampler2D m_DepthMap;
#endif
#ifdef WRITE_ALPHA
    uniform float m_Alpha;
#endif

varying vec2 texCoord;

void main() {
    
    #ifdef WRITE_COLOR
        gl_FragColor = texture2D(m_ColorMap, texCoord);
    #else
        gl_FragColor = vec4(0.0);
    #endif
    
    #ifdef WRITE_DEPTH
        gl_FragDepth = texture2D(m_DepthMap, texCoord).r;
    #else
        gl_FragDepth = 1.0;
    #endif
    
    #ifdef WRITE_ALPHA
        gl_FragColor.a = m_Alpha;
    #endif
    
}

