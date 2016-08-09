#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;

uniform sampler2D m_BloomTex;
uniform float m_BloomIntensity;

in vec2 texCoord;
out vec4 fragColor;

void main(){
  vec4 colorRes = getColor(m_Texture,texCoord);
  vec4 bloom = texture(m_BloomTex, texCoord);
  fragColor = bloom * m_BloomIntensity + colorRes;
}

