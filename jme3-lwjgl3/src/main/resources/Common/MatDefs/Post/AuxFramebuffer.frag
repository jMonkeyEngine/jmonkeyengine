#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
varying vec2 texCoord;

vec3 linearToSrgb(vec3 color) {
    vec3 linear = max(color, vec3(0.0));
    vec3 encodedLow = linear * 12.92;
    vec3 encodedHigh = 1.055 * pow(linear, vec3(1.0 / 2.4)) - 0.055;
    return mix(encodedLow, encodedHigh, step(vec3(0.0031308), linear));
}

void main() {
    vec4 color = getColor(m_Texture, texCoord);
    gl_FragColor = vec4(linearToSrgb(color.rgb), color.a);
}
