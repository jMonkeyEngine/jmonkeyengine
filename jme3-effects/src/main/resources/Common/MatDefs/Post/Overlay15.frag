#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform vec4 m_Color;
in vec2 texCoord;

void main() {
      vec4 texVal = getColor(m_Texture, texCoord);
      gl_FragColor = texVal * m_Color;
}

