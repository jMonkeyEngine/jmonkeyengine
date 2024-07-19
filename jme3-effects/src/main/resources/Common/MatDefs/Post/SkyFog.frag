#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_ColorMap;
uniform sampler2D m_DepthMap;
uniform float m_Density;
uniform float m_Distance;
#ifdef FOG_MAP
    uniform sampler2D m_FogMap;
#else
    uniform vec4 m_FogColor;
#endif

varying vec2 texCoord;

const float LOG2 = 1.442695;

void main() {

    vec2 frustum = vec2(1.0, m_Distance);
    vec4 color = texture2D(m_ColorMap, texCoord);
    float depth = texture2D(m_DepthMap, texCoord).r;
    depth = (2.0 * frustum.x) / (frustum.y + frustum.x - depth * (frustum.y - frustum.x));
    
    #ifdef FOG_MAP
        vec4 fogClr = texture2D(m_FogMap, texCoord);
    #else
        vec4 fogClr = m_FogColor;
    #endif
    
    float fogFactor = exp2(-m_Density * m_Density * depth * depth * LOG2);
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    gl_FragColor = mix(fogClr, color, fogFactor);
    
}
