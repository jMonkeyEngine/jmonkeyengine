#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform sampler2D m_ColorRamp;
varying vec2 texCoord;

uniform float m_Value;

void main() {

    vec4 texVal = getColor(m_Texture, texCoord);
    gl_FragColor = texVal * m_Value;

}
