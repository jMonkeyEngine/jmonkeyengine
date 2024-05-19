
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform sampler2D m_GlowMap;
uniform float m_GlowFactor;
varying vec2 texCoord;

void main() {

    gl_FragColor = mix(getColor(m_Texture, texCoord), texture2D(m_GlowMap, texCoord), m_GlowFactor);

}

