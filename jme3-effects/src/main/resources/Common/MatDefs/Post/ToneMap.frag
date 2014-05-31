#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform vec3 m_WhitePoint;

#if __VERSION__ >= 150
in vec2 texCoord;
#else
varying vec2 texCoord;
#endif

vec3 FilmicCurve(in vec3 x)
{
    const float A = 0.22;
    const float B = 0.30;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.01;
    const float F = 0.30;

    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}

// whitePoint should be 11.2

vec3 ToneMap_Filmic(vec3 color, vec3 whitePoint)
{
    return FilmicCurve(color) / FilmicCurve(whitePoint);
}

void main() {
    // TODO: This is incorrect if multi-sampling is used.
    // The tone-mapping should be performed for each sample independently.

    vec4 texVal = getColor(m_Texture, texCoord);
    vec3 toneMapped = ToneMap_Filmic(texVal.rgb, m_WhitePoint);
    gl_FragColor = vec4(toneMapped, texVal.a);
}