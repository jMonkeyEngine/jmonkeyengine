
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform vec2 m_TexelSize;
varying vec2 texCoord;

void main() {
    
    // upsampling code: https://learnopengl.com/Guest-Articles/2022/Phys.-Based-Bloom

    // The filter kernel is applied with a radius, specified in texture
    // coordinates, so that the radius will vary across mip resolutions.
    float x = m_TexelSize.x;
    float y = m_TexelSize.y;

    // Take 9 samples around current texel:
    // a - b - c
    // d - e - f
    // g - h - i
    // === ('e' is the current texel) ===
    vec3 a = getColor(m_Texture, vec2(texCoord.x - x, texCoord.y + y)).rgb;
    vec3 b = getColor(m_Texture, vec2(texCoord.x,     texCoord.y + y)).rgb;
    vec3 c = getColor(m_Texture, vec2(texCoord.x + x, texCoord.y + y)).rgb;

    vec3 d = getColor(m_Texture, vec2(texCoord.x - x, texCoord.y)).rgb;
    vec3 e = getColor(m_Texture, vec2(texCoord.x,     texCoord.y)).rgb;
    vec3 f = getColor(m_Texture, vec2(texCoord.x + x, texCoord.y)).rgb;

    vec3 g = getColor(m_Texture, vec2(texCoord.x - x, texCoord.y - y)).rgb;
    vec3 h = getColor(m_Texture, vec2(texCoord.x,     texCoord.y - y)).rgb;
    vec3 i = getColor(m_Texture, vec2(texCoord.x + x, texCoord.y - y)).rgb;

    // Apply weighted distribution, by using a 3x3 tent filter:
    //        | 1 2 1 |
    // 1/16 * | 2 4 2 |
    //        | 1 2 1 |
    vec3 upsample = e*4.0;
    upsample += (b+d+f+h)*2.0;
    upsample += (a+c+g+i);
    upsample /= 16.0;
    
    gl_FragColor = vec4(upsample, 1.0);
    
}
