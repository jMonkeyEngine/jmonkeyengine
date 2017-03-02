#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
varying vec2 texCoord;
 
uniform int m_NumColors;
uniform float m_Gamma;
uniform float m_Strength;
 
void main() {
    vec4 color = getColor(m_Texture, texCoord);
    vec4 texVal = vec4(color);
 
    texVal = pow(texVal, vec4(m_Gamma));
    texVal = texVal * vec4(m_NumColors);
    texVal = floor(texVal);
    texVal = texVal / vec4(m_NumColors);
    texVal = pow(texVal, vec4(1.0/m_Gamma));
 
    gl_FragColor = mix(color, texVal, m_Strength);
}