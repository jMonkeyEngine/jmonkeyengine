#import "Common/ShaderLib/Hdr.glsllib"

uniform sampler2D m_Texture;
varying vec2 texCoord;

#ifdef BLOCKS
 uniform vec2 m_PixelSize;
 uniform vec2 m_BlockSize;
 uniform float m_NumPixels;
#endif

vec4 blocks(vec2 halfBlockSize, vec2 pixelSize, float numPixels){
    vec2 startUV = texCoord - halfBlockSize;
    vec2 endUV = texCoord + halfBlockSize;

    vec4 sum = vec4(0.0);
    float numPix = 0.0;
    //float maxLum = 0.0;

    for (float x = startUV.x; x < endUV.x; x += pixelSize.x){
        for (float y = startUV.y; y < endUV.y; y += pixelSize.y){
            numPix += 1.0;
            vec4 color = texture2D(m_Texture, vec2(x,y));

            #ifdef ENCODE_LUM
            color = HDR_EncodeLum(HDR_GetLum(color.rgb));
            #endif
            //#ifdef COMPUTE_MAX
            //maxLum = max(color.r, maxLum);
            //#endif
            sum += color;
        }
    }
    sum /= numPix;

    #ifdef DECODE_LUM
    sum = vec4(HDR_DecodeLum(sum));
       //#ifdef COMPUTE_MAX
       //maxLum = HDR_GetExpLum(maxLum);
       //#endif
    #endif

    return sum;
}

vec4 fetch(){
    vec4 color = texture2D(m_Texture, texCoord);
    #ifdef ENCODE_LUM
       return HDR_EncodeLum(HDR_GetLum(color.rgb));
    #elif defined DECODE_LUM
       return vec4(HDR_DecodeLum(color));
    #else
       return color;
    #endif
}

void main() {
    #ifdef BLOCKS
    gl_FragColor = blocks(m_BlockSize * vec2(0.5), m_PixelSize, m_NumPixels);
    #else
    gl_FragColor = vec4(fetch());
    #endif
}


