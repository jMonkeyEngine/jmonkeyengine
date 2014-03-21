#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform COLORTEXTURE m_CompositeTexture;
in vec2 texCoord;

out vec4 finalColor;

void main() {
      vec4 texVal = getColor(m_Texture, texCoord);
      vec4 compositeVal = getColor(m_CompositeTexture, texCoord);
      finalColor = mix(compositeVal,texVal,texVal.a);      
}

