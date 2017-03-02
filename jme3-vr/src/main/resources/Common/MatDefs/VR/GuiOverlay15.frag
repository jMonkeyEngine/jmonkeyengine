//#define FRAGMENT_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_ColorMap;

in vec2 texCoord1;

out vec4 outColor;

void main(){
    outColor = texture2D(m_ColorMap, texCoord1);     
    outColor.a *= 12.0 / (1.0 + outColor.a * 11.0 );
}