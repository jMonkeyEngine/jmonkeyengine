#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform vec4 m_Color;
in vec2 texCoord;
out vec4 fragColor;

void main() {
      vec4 texVal = getColor(m_Texture, texCoord);
      fragColor = texVal * m_Color;
}

