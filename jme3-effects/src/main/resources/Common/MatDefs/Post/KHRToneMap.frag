#extension GL_ARB_texture_multisample : enable
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Hdr.glsllib"


uniform vec3 m_Exposure;
uniform vec3 m_Gamma;
varying vec2 texCoord;
 
vec3 applyCurve(in vec3 x) {    
    return HDR_KHRToneMap(x, m_Exposure, m_Gamma);
}


#ifdef NUM_SAMPLES

uniform sampler2DMS m_Texture;

vec4 applyToneMap() {
    ivec2 iTexC = ivec2(texCoord * vec2(textureSize(m_Texture)));
    vec4 color = vec4(0.0);
    for (int i = 0; i < NUM_SAMPLES; i++) {
        vec4 hdrColor = texelFetch(m_Texture, iTexC, i);
        vec3 ldrColor = applyCurve(hdrColor.rgb);
        color += vec4(ldrColor, hdrColor.a);
    }
    return color / float(NUM_SAMPLES);
}

#else
 
uniform sampler2D m_Texture;
 
vec4 applyToneMap() {
    vec4 texVal = texture2D(m_Texture, texCoord);
    return vec4(applyCurve(texVal.rgb) , texVal.a);
}
 
#endif

void main() {
    gl_FragColor = applyToneMap();
}
