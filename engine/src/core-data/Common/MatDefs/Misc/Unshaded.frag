uniform vec4 m_Color;

#if defined(HAS_GLOWMAP) || defined(HAS_COLORMAP) || (defined(HAS_LIGHTMAP) && !defined(SEPARATE_TEXCOORD))
    #define NEED_TEXCOORD1
#endif

#ifdef HAS_COLORMAP
    uniform sampler2D m_ColorMap;
#endif

#ifdef NEED_TEXCOORD1
    varying vec2 texCoord1;
#endif

#ifdef HAS_LIGHTMAP
    uniform sampler2D m_LightMap;
    #ifdef SEPARATE_TEXCOORD
        varying vec2 texCoord2;
    #endif
#endif

#ifdef HAS_VERTEXCOLOR
    varying vec4 vertColor;
#endif

void main(){
    vec4 color = vec4(1.0);

    #ifdef HAS_COLORMAP
        color *= texture2D(m_ColorMap, texCoord1);
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