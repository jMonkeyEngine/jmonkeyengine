#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat3 g_NormalMatrix;
uniform mat4 g_ViewMatrix;

uniform vec4 g_LightColor;
uniform vec4 g_LightPosition;
uniform vec4 g_AmbientLightColor;

uniform float m_Shininess;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;
attribute vec4 inTangent;

varying vec3 vNormal;
varying vec2 texCoord;
varying vec3 vPosition;
varying vec3 vnPosition;
varying vec3 vViewDir;
varying vec3 vnViewDir;
varying vec4 vLightDir;
varying vec4 vnLightDir;

varying vec3 lightVec;

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
    vec3 viewDir = normalize(-wvPosition);

    vec4 wvLightPos = (g_ViewMatrix * vec4(g_LightPosition.xyz,clamp(g_LightColor.w,0.0,1.0)));
    wvLightPos.w = g_LightPosition.w;
    vec4 lightColor = g_LightColor;

    //--------------------------
    // specific to normal maps:
    //--------------------------
    #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
        vec3 wvTangent = normalize(g_NormalMatrix * inTangent.xyz);
        vec3 wvBinormal = cross(wvNormal, wvTangent);

        mat3 tbnMat = mat3(wvTangent, wvBinormal * inTangent.w,wvNormal);

        vPosition = wvPosition * tbnMat;
        vViewDir  = viewDir * tbnMat;

        lightComputeDir(wvPosition, lightColor.w, wvLightPos, vLightDir, lightVec);
        vLightDir.xyz = (vLightDir.xyz * tbnMat).xyz;
    #else
        //-------------------------
        // general to all lighting
        //-------------------------
        vNormal = wvNormal;

        vPosition = wvPosition;
        vViewDir = viewDir;

        lightComputeDir(wvPosition, lightColor.w, wvLightPos, vLightDir, lightVec);

    #endif
   
    AmbientSum  = g_AmbientLightColor; // Default: ambient color is dark gray
    DiffuseSum  = lightColor;
    SpecularSum = lightColor;


#ifdef TRI_PLANAR_MAPPING
    wVertex = vec4(inPosition,0.0);
    wNormal = inNormal;
#endif

}