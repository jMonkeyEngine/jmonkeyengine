#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"

uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;


varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

#ifdef POINTLIGHT
    uniform mat4 m_LightViewProjectionMatrix4;
    uniform mat4 m_LightViewProjectionMatrix5;
    uniform vec3 m_LightPos;
    varying vec4 projCoord4;
    varying vec4 projCoord5;
    varying vec4 worldPos;
#else
    uniform vec3 m_LightDir;
    #ifndef PSSM
        uniform vec3 m_LightPos;
        varying float lightDot;
    #endif
#endif

#if defined(PSSM) || defined(FADE)
varying float shadowPosition;
#endif

varying vec2 texCoord;
attribute vec3 inPosition;

#ifndef BACKFACE_SHADOWS
    attribute vec3 inNormal;
    varying float nDotL;
#endif

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
    gl_Position = TransformWorldViewProjection(modelSpacePos);
    vec3 lightDir;

    #if defined(PSSM) || defined(FADE)
        shadowPosition = gl_Position.z;
    #endif  

    #ifndef POINTLIGHT
        vec4 worldPos=vec4(0.0);
    #endif
    // get the vertex in world space
    worldPos = TransformWorld(modelSpacePos);

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
        #ifndef PSSM
            //Spot light
            lightDir = worldPos.xyz - m_LightPos;
            lightDot = dot(m_LightDir,lightDir);
        #endif
    #endif

    #ifndef BACKFACE_SHADOWS
        vec3 normal = normalize(TransformWorld(vec4(inNormal,0.0))).xyz;
        #ifdef POINTLIGHT
            lightDir = worldPos.xyz - m_LightPos;
        #else
            #ifdef PSSM
               lightDir = m_LightDir;
            #endif
        #endif
        nDotL = dot(normal, lightDir);
    #endif
}