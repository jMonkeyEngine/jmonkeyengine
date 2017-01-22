#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform sampler2D m_BloomTex;
uniform float m_BloomIntensity;

varying vec2 texCoord;

void main(){
   vec4 colorRes = getColor(m_Texture, texCoord);
   vec4 bloom = texture2D(m_BloomTex, texCoord);
   gl_FragColor = bloom * m_BloomIntensity + colorRes;
}

