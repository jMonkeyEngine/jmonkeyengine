//#define FRAGMENT_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform vec4 m_Color;
uniform sampler2D m_ColorMap;

varying vec2 texCoord1;

void main(){
    vec4 color = vec4(1.0);

    #ifdef HAS_COLORMAP
        color *= texture2D(m_ColorMap, texCoord1);     
    #endif

    #ifdef HAS_COLOR
        color *= m_Color;
    #endif

    gl_FragColor = color;
}