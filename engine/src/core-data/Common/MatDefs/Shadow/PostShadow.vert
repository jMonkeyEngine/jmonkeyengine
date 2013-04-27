#import "Common/ShaderLib/Skinning.glsllib"
uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform mat4 g_ViewMatrix;
uniform vec3 m_LightPos; 

varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

#ifdef POINTLIGHT
uniform mat4 m_LightViewProjectionMatrix4;
uniform mat4 m_LightViewProjectionMatrix5;
varying vec4 projCoord4;
varying vec4 projCoord5;
varying vec4 worldPos;
#endif

#ifdef PSSM
varying float shadowPosition;
#endif
varying vec3 lightVec;

varying vec2 texCoord;

attribute vec3 inPosition;

#ifdef DISCARD_ALPHA
    attribute vec2 inTexCoord;
#endif

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);


void main(){
   vec4 modelSpacePos = vec4(inPosition, 1.0);
  
   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos);
   #endif
    gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;

    #ifndef POINTLIGHT
        #ifdef PSSM
             shadowPosition = gl_Position.z;
        #endif        
        vec4 worldPos=vec4(0.0);
    #endif
    // get the vertex in world space
    worldPos = g_WorldMatrix * modelSpacePos;

    #ifdef DISCARD_ALPHA
       texCoord = inTexCoord;
    #endif
    // populate the light view matrices array and convert vertex to light viewProj space
    projCoord0 = biasMat * m_LightViewProjectionMatrix0 * worldPos;
    projCoord1 = biasMat * m_LightViewProjectionMatrix1 * worldPos;
    projCoord2 = biasMat * m_LightViewProjectionMatrix2 * worldPos;
    projCoord3 = biasMat * m_LightViewProjectionMatrix3 * worldPos;
    #ifdef POINTLIGHT
        projCoord4 = biasMat * m_LightViewProjectionMatrix4 * worldPos;
        projCoord5 = biasMat * m_LightViewProjectionMatrix5 * worldPos;
    #else
        
        vec4 vLightPos = g_ViewMatrix * vec4(m_LightPos,1.0);
        vec4 vPos = g_ViewMatrix * worldPos;        
        lightVec = vLightPos.xyz - vPos.xyz;
    #endif
}