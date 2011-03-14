uniform bool m_UseTex;
uniform sampler2D m_Texture;
uniform vec4 m_Color;

varying vec2 texCoord;
varying vec4 color;

void main() {
    vec4 texVal = texture2D(m_Texture, texCoord);
    texVal = m_UseTex ? texVal : vec4(1.0);
    gl_FragColor = texVal * color * m_Color;
}

