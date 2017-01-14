#import "Common/ShaderLib/GLSLCompat.glsllib"
/*
GLSL conversion of Michael Horsch water demo
http://www.bonzaisoftware.com/wfs.html
Converted by Mars_999
8/20/2005
*/
uniform vec3 m_lightPos;
uniform float m_time;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_ViewMatrix;
uniform vec3 g_CameraPosition;
uniform mat3 g_NormalMatrix;

attribute vec4 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inTangent;
attribute vec3 inNormal;

varying vec4 lightDir;
varying vec2 waterTex1;
varying vec2 waterTex2;
varying vec4 position;
varying vec4 viewDir;
varying vec4 viewpos;
varying vec4 viewLightDir;
varying vec4 viewCamDir;


//unit 0 = water_reflection
//unit 1 = water_refraction
//unit 2 = water_normalmap
//unit 3 = water_dudvmap
//unit 4 = water_depthmap

void main(void)
{
    viewpos.x = g_CameraPosition.x;
    viewpos.y = g_CameraPosition.y;
    viewpos.z = g_CameraPosition.z;
    viewpos.w = 1.0;

    vec4  temp;
    vec4 tangent = vec4(1.0, 0.0, 0.0, 0.0);
    vec4 norm = vec4(0.0, 1.0, 0.0, 0.0);
    vec4 binormal = vec4(0.0, 0.0, 1.0, 0.0);


    temp = viewpos - inPosition;

    viewDir.x = dot(temp, tangent);
    viewDir.y = dot(temp, binormal);
    viewDir.z = dot(temp, norm);
    viewDir.w = 0.0;

    temp = vec4(m_lightPos,1.0)- inPosition;
    lightDir.x = dot(temp, tangent);
    lightDir.y = dot(temp, binormal);
    lightDir.z = dot(temp, norm);
    lightDir.w = 0.0;

   vec4 viewSpaceLightPos=g_ViewMatrix*vec4(m_lightPos,1.0);
   vec4 viewSpacePos=g_WorldViewMatrix*inPosition;
   vec3 wvNormal  = normalize(g_NormalMatrix * inNormal);
   vec3 wvTangent = normalize(g_NormalMatrix * inTangent);
   vec3 wvBinormal = cross(wvNormal, wvTangent);
   mat3 tbnMat = mat3(wvTangent, wvBinormal, wvNormal);

    temp = viewSpaceLightPos - viewSpacePos;
    viewLightDir.xyz=temp.xyz*tbnMat;
    viewLightDir.w = 0.0;

    temp = -viewSpacePos;
    viewCamDir.xyz =temp.xyz*tbnMat;
    viewCamDir.w = 0.0;


    vec2 t1 = vec2(0.0, -m_time);
    vec2 t2 = vec2(0.0, m_time);

    waterTex1 = inTexCoord + t1;
    waterTex2 = inTexCoord + t2;

    position = g_WorldViewProjectionMatrix * inPosition;
    gl_Position = position;
}
