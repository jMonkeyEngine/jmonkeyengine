uniform sampler2D m_Texture;
varying vec2 texCoord;
 
uniform int m_NumColors;
uniform float m_Gamma;
uniform float m_Strength;
 
void main() {
    vec4 texVal = texture2D(m_Texture, texCoord);
 
    texVal = pow(texVal, vec4(m_Gamma));
    texVal = texVal * vec4(m_NumColors);
    texVal = floor(texVal);
    texVal = texVal / vec4(m_NumColors);
    texVal = pow(texVal, vec4(1.0/m_Gamma));
 
    gl_FragColor = mix(texture2D(m_Texture, texCoord), texVal, m_Strength);
}