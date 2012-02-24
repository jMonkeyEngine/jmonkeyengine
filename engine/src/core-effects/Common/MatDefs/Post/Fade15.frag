#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform float m_Value;

in vec2 texCoord;

void main() {
    vec4 texVal = getColor(m_Texture, texCoord);
    gl_FragColor = texVal * m_Value;
}