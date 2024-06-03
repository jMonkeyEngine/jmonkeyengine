
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
                //gl_FragColor = vec4(1.0);
                discard;
            }
        #endif
        if (gl_FragColor.rgb == vec3(0.0)) {
            //gl_FragColor.rgb = vec3(1.0, 0.0, 0.0);
        }
        #ifdef DEBUG
            gl_FragColor.rgba = vec4(gl_FragColor.a, 0.0, 0.0, 1.0);
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

