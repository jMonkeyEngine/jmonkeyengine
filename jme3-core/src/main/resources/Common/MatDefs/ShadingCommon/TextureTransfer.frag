
#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef WRITE_COLOR_MAP
    uniform sampler2D m_ColorMap;
    #ifdef ALPHA_DISCARD
        uniform float m_AlphaDiscard;
    #endif
#endif
#ifdef WRITE_DEPTH
    uniform sampler2D m_DepthMap;
#endif

varying vec2 texCoord;

void main() {
    
    #ifdef WRITE_COLOR_MAP
        gl_FragColor = texture2D(m_ColorMap, texCoord);
        #ifdef ALPHA_DISCARD
            if (gl_FragColor.a <= m_AlphaDiscard) {
                discard;
            }
        #endif
    #else
        gl_FragColor = vec4(0.0);
    #endif
    
    #ifdef WRITE_DEPTH
        gl_FragDepth = texture2D(m_DepthMap, texCoord).r;
    #else
        gl_FragDepth = 1.0;
    #endif
    
}

