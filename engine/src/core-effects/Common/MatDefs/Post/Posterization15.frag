#import "Common/ShaderLib/MultiSample.glsllib"
 
uniform COLORTEXTURE m_Texture;
in vec2 texCoord;
 
uniform int m_NumColors;
uniform float m_Gamma;
uniform float m_Strength;
 
void main() {
    vec4 texVal = getColor(m_Texture, texCoord);
 
    texVal = pow(texVal, vec4(m_Gamma));
    texVal = texVal * m_NumColors;
    texVal = floor(texVal);
    texVal = texVal / m_NumColors;
    texVal = pow(texVal, vec4(1.0/m_Gamma));
 
    gl_FragColor = mix(getColor(m_Texture, texCoord), texVal, m_Strength);
}