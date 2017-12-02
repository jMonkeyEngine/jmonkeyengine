#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;
varying vec2 texCoord;

uniform vec4 m_FogColor;
uniform float m_FogDensity;
uniform float m_FogDistance;

const float LOG2 = 1.442695;

void main() {
       vec2 m_FrustumNearFar=vec2(1.0,m_FogDistance);
       vec4 texVal = getColor(m_Texture, texCoord);
       float fogVal = getDepth(m_DepthTexture,texCoord).r;
       float depth= (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - fogVal* (m_FrustumNearFar.y-m_FrustumNearFar.x));

       float fogFactor = exp2( -m_FogDensity * m_FogDensity * depth *  depth * LOG2 );
       fogFactor = clamp(fogFactor, 0.0, 1.0);
       gl_FragColor = mix(m_FogColor,texVal,fogFactor);

}