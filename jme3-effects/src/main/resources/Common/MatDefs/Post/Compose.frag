#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform COLORTEXTURE m_CompositeTexture;
varying vec2 texCoord;

void main() {
      vec4 texVal = getColor(m_Texture, texCoord);
      vec4 compositeVal = getColor(m_CompositeTexture, texCoord);
      gl_FragColor = mix(compositeVal,texVal,texVal.a);
}

