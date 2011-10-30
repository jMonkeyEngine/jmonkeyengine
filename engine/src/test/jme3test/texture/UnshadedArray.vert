uniform mat4 g_WorldViewProjectionMatrix;
attribute vec3 inPosition;

#if defined(HAS_COLORMAP) || (defined(HAS_LIGHTMAP) && !defined(SEPARATE_TEXCOORD))
    #define NEED_TEXCOORD1
#endif

#ifdef NEED_TEXCOORD1
    attribute vec3 inTexCoord;
    varying vec3 texCoord1;
#endif

#ifdef SEPARATE_TEXCOORD
    attribute vec3 inTexCoord2;
    varying vec3 texCoord2;
#endif

#ifdef HAS_VERTEXCOLOR
    attribute vec4 inColor;
    varying vec4 vertColor;
#endif

void main(){
    #ifdef NEED_TEXCOORD1
        texCoord1 = inTexCoord;
    #endif

    #ifdef SEPARATE_TEXCOORD
        texCoord2 = inTexCoord2;
    #endif

    #ifdef HAS_VERTEXCOLOR
        vertColor = inColor;
    #endif

    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}

