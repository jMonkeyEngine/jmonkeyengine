#import "Common/ShaderLib/GLSLCompat.glsllib"

#extension GL_EXT_texture_array : enable
// #extension GL_EXT_gpu_shader4 : enable

uniform vec4 m_Color;

#if defined(HAS_GLOWMAP) || defined(HAS_COLORMAP) || (defined(HAS_LIGHTMAP) && !defined(SEPARATE_TEXCOORD))
    #define NEED_TEXCOORD1
#endif

#ifdef HAS_COLORMAP
    #if !defined(GL_EXT_texture_array) && __VERSION__ < 130
        #error Texture arrays are not supported, but required for this shader.
    #endif

    uniform sampler2DArray m_ColorMap;
#endif

#ifdef NEED_TEXCOORD1
    varying vec3 texCoord1;
#endif

#ifdef HAS_LIGHTMAP
    uniform sampler2D m_LightMap;
    #ifdef SEPERATE_TEXCOORD
        varying vec3 texCoord2;
    #endif
#endif

#ifdef HAS_VERTEXCOLOR
    varying vec4 vertColor;
#endif

void main(){
    vec4 color = vec4(1.0);

    #ifdef HAS_COLORMAP
        color *= texture2DArray(m_ColorMap, texCoord1);
    #endif

    #ifdef HAS_VERTEXCOLOR
        color *= vertColor;
    #endif

    #ifdef HAS_COLOR
        color *= m_Color;
    #endif

    #ifdef HAS_LIGHTMAP
        #ifdef SEPARATE_TEXCOORD
            color.rgb *= texture2D(m_LightMap, texCoord2).rgb;
        #else
            color.rgb *= texture2D(m_LightMap, texCoord1).rgb;
        #endif
    #endif

    gl_FragColor = color;
}
