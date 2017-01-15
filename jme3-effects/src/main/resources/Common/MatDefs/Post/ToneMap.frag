#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform vec3 m_WhitePoint;

varying vec2 texCoord;

vec3 FilmicCurve(in vec3 x){
    const float A = 0.22;
    const float B = 0.30;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.01;
    const float F = 0.30;

    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}

// whitePoint should be 11.2

vec3 ToneMap_Filmic(vec3 color, vec3 whitePoint){
    return FilmicCurve(color) / FilmicCurve(whitePoint);
}

vec4 tonemap(int i) {

    vec4 texVal = fetchTextureSample(m_Texture, texCoord, i);
    vec3 toneMapped = ToneMap_Filmic(texVal.rgb, m_WhitePoint);

    return vec4(toneMapped, texVal.a);
}


void main() {
    #ifdef RESOLVE_MS
        vec4 color = vec4(0.0);
        for (int i = 0; i < m_NumSamples; i++){
            color += tonemap(i);
        }
        gl_FragColor = color / m_NumSamples;
    #else
        gl_FragColor = tonemap(0);
    #endif
}
