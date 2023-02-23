#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_ColorMap;

varying vec2 texCoord1;

void main(){
    gl_FragColor = texture2D(m_ColorMap, texCoord1);     
    gl_FragColor.a *= 12.0 / (1.0 + gl_FragColor.a * 11.0 );
}