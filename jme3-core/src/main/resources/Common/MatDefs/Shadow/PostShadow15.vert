#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;


out vec4 projCoord0;
out vec4 projCoord1;
out vec4 projCoord2;
out vec4 projCoord3;

#ifdef POINTLIGHT
    uniform mat4 m_LightViewProjectionMatrix4;
    uniform mat4 m_LightViewProjectionMatrix5;
    out vec4 projCoord4;
    out vec4 projCoord5;
    out vec4 worldPos;
#else
    #ifndef PSSM
        uniform vec3 m_LightPos; 
        uniform vec3 m_LightDir; 
        out float lightDot;
    #endif
#endif

#ifdef PSSM
out float shadowPosition;
#endif
out vec3 lightVec;

out vec2 texCoord;

in vec3 inPosition;

#ifdef DISCARD_ALPHA
    in vec2 inTexCoord;
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
    gl_Position =  TransformWorldViewProjection(modelSpacePos);

    #ifndef POINTLIGHT
        #ifdef PSSM
             shadowPosition = gl_Position.z;
        #endif        
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
            vec3 lightDir = worldPos.xyz - m_LightPos;
            lightDot = dot(m_LightDir,lightDir);
        #endif
    #endif
}