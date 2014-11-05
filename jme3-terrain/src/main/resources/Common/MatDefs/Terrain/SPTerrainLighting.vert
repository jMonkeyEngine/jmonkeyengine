uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat3 g_NormalMatrix;
uniform mat4 g_ViewMatrix;

uniform vec4 g_AmbientLightColor;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;
attribute vec4 inTangent;

varying vec3 vNormal;
varying vec2 texCoord;
varying vec3 vPos;
varying vec3 vTangent;
varying vec3 vBinormal;

varying vec4 AmbientSum;
varying vec4 DiffuseSum;
varying vec4 SpecularSum;

#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
  varying vec3 wNormal;
#endif



void main(){
    vec4 pos = vec4(inPosition, 1.0);
    gl_Position = g_WorldViewProjectionMatrix * pos;
    #ifdef TERRAIN_GRID
    texCoord = inTexCoord * 2.0;
    #else
    texCoord = inTexCoord;
    #endif

    vec3 wvPosition = (g_WorldViewMatrix * pos).xyz;
    vec3 wvNormal  = normalize(g_NormalMatrix * inNormal);

    //--------------------------
    // specific to normal maps:
    //--------------------------
    #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
      vTangent = g_NormalMatrix * inTangent.xyz;
      vBinormal = cross(wvNormal, vTangent)* inTangent.w;      
    #endif 

    //-------------------------
    // general to all lighting
    //-------------------------
    vNormal = wvNormal;
    vPos = wvPosition; 

    AmbientSum  = g_AmbientLightColor; 
    DiffuseSum  = vec4(1.0);
    SpecularSum = vec4(0.0);


#ifdef TRI_PLANAR_MAPPING
    wVertex = vec4(inPosition,0.0);
    wNormal = inNormal;
#endif

}