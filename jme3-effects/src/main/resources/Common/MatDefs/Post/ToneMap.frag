#extension GL_ARB_texture_multisample : enable
#import "Common/ShaderLib/GLSLCompat.glsllib"

vec3 FilmicCurve(in vec3 x) {
    const float A = 0.22;
    const float B = 0.30;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.01;
    const float F = 0.30;

    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}

// whitePoint should be 11.2

vec3 ToneMap_Filmic(vec3 color, vec3 whitePoint) {
    return FilmicCurve(color) / FilmicCurve(whitePoint);
}

uniform vec3 m_WhitePoint;
varying vec2 texCoord;
 
#ifdef NUM_SAMPLES

uniform sampler2DMS m_Texture;

vec4 ToneMap_TextureFilmic() {
    ivec2 iTexC = ivec2(texCoord * vec2(textureSize(m_Texture)));
    vec4 color = vec4(0.0);
    for (int i = 0; i < NUM_SAMPLES; i++) {
        vec4 hdrColor = texelFetch(m_Texture, iTexC, i);
        vec3 ldrColor = FilmicCurve(hdrColor.rgb);
        color += vec4(ldrColor, hdrColor.a);
    }
    color.rgb /= FilmicCurve(m_WhitePoint);
    return color / float(NUM_SAMPLES);
}

#else
 
uniform sampler2D m_Texture;
 
vec4 ToneMap_TextureFilmic() {
    vec4 texVal = texture2D(m_Texture, texCoord);
    return vec4(ToneMap_Filmic(texVal.rgb, m_WhitePoint), texVal.a);
}
 
#endif

void main() {
    gl_FragColor = ToneMap_TextureFilmic();
}
