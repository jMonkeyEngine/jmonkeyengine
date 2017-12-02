#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
varying vec2 texCoord;

uniform float m_Value;

void main() {
       vec4 texVal = getColor(m_Texture, texCoord);

       gl_FragColor = texVal * m_Value;

}