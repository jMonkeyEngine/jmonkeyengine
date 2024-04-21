
#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef MAP
    uniform sampler2D m_ColorMap;
#else
    uniform vec4 m_Color;
#endif

varying vec2 texCoord;

void main(){
    if (texCoord.y > 0.5) {
        discard;
    }
    #ifdef MAP
        gl_FragColor = texture2D(m_ColorMap, texCoord);
    #else
        gl_FragColor = m_Color;
    #endif
}
