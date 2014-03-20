#import "Common/ShaderLib/Hdr.glsllib"

varying vec2 texCoord;

uniform sampler2D m_Texture;
uniform sampler2D m_Lum;
uniform sampler2D m_Lum2;

uniform float m_A;
uniform float m_White;
uniform float m_BlendFactor;
uniform float m_Gamma;

void main() {
    float avgLumA = HDR_DecodeLum( texture2D(m_Lum, vec2(0.0)) );
    float avgLumB = HDR_DecodeLum( texture2D(m_Lum2, vec2(0.0)) );
    float lerpedLum = mix(avgLumA, avgLumB, m_BlendFactor);

    vec4 color = texture2D(m_Texture, texCoord);
    vec3 c1 = HDR_ToneMap(color.rgb, lerpedLum, m_A, m_White);
    //vec3 c2 = HDR_ToneMap2(color.rgb, lerpedLum, m_A * vec2(0.25), m_White);

    //float l1 = HDR_GetLuminance(c1);
    //float l2 = HDR_GetLuminance(c2);

    //vec3 final = mix(c2, c1, clamp(l1, 0.0, 1.0));

    //tonedColor = pow(tonedColor, vec3(m_Gamma));
    gl_FragColor = vec4(c1, color.a);
}

