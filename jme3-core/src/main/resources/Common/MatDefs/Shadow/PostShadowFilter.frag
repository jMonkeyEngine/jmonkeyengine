#import "Common/ShaderLib/Shadows.glsllib"

uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
uniform mat4 m_ViewProjectionMatrixInverse;
uniform vec4 m_ViewProjectionMatrixRow2;

varying vec2 texCoord;


const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);

uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;

uniform vec2 g_ResolutionInverse;

#ifdef POINTLIGHT
    uniform vec3 m_LightPos;
    uniform mat4 m_LightViewProjectionMatrix4;
    uniform mat4 m_LightViewProjectionMatrix5;
#else
    uniform vec3 m_LightDir;
    #ifndef PSSM    
        uniform vec3 m_LightPos;
    #endif
#endif

#ifdef FADE
uniform vec2 m_FadeInfo;
#endif

vec3 getPosition(in float depth, in vec2 uv){
    vec4 pos = vec4(uv, depth, 1.0) * 2.0 - 1.0;
    pos = m_ViewProjectionMatrixInverse * pos;
    return pos.xyz / pos.w;
}
#ifndef BACKFACE_SHADOWS
    vec3 approximateNormal(in vec4 worldPos,in vec2 texCoord){
        float step = g_ResolutionInverse.x ;
        float stepy = g_ResolutionInverse.y ;
        float depth2 = texture2D(m_DepthTexture,texCoord + vec2(step,-stepy)).r;
        float depth3 = texture2D(m_DepthTexture,texCoord + vec2(-step,-stepy)).r;
        vec4 worldPos2 = vec4(getPosition(depth2,texCoord + vec2(step,-stepy)),1.0);
        vec4 worldPos3 = vec4(getPosition(depth3,texCoord + vec2(-step,-stepy)),1.0);

        vec3 v1 = (worldPos - worldPos2).xyz;
        vec3 v2 = (worldPos3 - worldPos2).xyz;
        return normalize(cross(v1, v2));
    }
#endif

void main(){
    float depth = texture2D(m_DepthTexture,texCoord).r;
    vec4 color = texture2D(m_Texture,texCoord);


    //Discard shadow computation on the sky
    if(depth == 1.0){
        gl_FragColor = color;
        return;
    }

    // get the vertex in world space
    vec4 worldPos = vec4(getPosition(depth,texCoord),1.0);

    vec3 lightDir;
    #ifdef PSSM
        lightDir = m_LightDir;
    #else
        lightDir = worldPos.xyz - m_LightPos;
    #endif

    #ifndef BACKFACE_SHADOWS
        vec3 normal = approximateNormal(worldPos, texCoord);
        float ndotl = dot(normal, lightDir);
        if(ndotl > -0.0){
            gl_FragColor = color;
            return;
        }
    #endif
   
     #if (!defined(POINTLIGHT) && !defined(PSSM))
          if( dot(m_LightDir,lightDir) < 0.0){
            gl_FragColor = color;
            return;
          }         
    #endif

    // populate the light view matrices array and convert vertex to light viewProj space
    vec4 projCoord0 = biasMat * m_LightViewProjectionMatrix0 * worldPos;
    vec4 projCoord1 = biasMat * m_LightViewProjectionMatrix1 * worldPos;
    vec4 projCoord2 = biasMat * m_LightViewProjectionMatrix2 * worldPos;
    vec4 projCoord3 = biasMat * m_LightViewProjectionMatrix3 * worldPos;
    #ifdef POINTLIGHT
       vec4 projCoord4 = biasMat * m_LightViewProjectionMatrix4 * worldPos;
       vec4 projCoord5 = biasMat * m_LightViewProjectionMatrix5 * worldPos;
    #endif

    float shadow = 1.0;
    
    #if defined(PSSM) || defined(FADE)
        float shadowPosition = m_ViewProjectionMatrixRow2.x * worldPos.x +  m_ViewProjectionMatrixRow2.y * worldPos.y +  m_ViewProjectionMatrixRow2.z * worldPos.z +  m_ViewProjectionMatrixRow2.w;
    #endif

    #ifdef POINTLIGHT         
            shadow = getPointLightShadows(worldPos, m_LightPos,
                           m_ShadowMap0,m_ShadowMap1,m_ShadowMap2,m_ShadowMap3,m_ShadowMap4,m_ShadowMap5,
                           projCoord0, projCoord1, projCoord2, projCoord3, projCoord4, projCoord5);
    #else
       #ifdef PSSM
            shadow = getDirectionalLightShadows(m_Splits, shadowPosition,
                           m_ShadowMap0,m_ShadowMap1,m_ShadowMap2,m_ShadowMap3,
                           projCoord0, projCoord1, projCoord2, projCoord3);
       #else 
            //spotlight
            shadow = getSpotLightShadows(m_ShadowMap0,projCoord0);
       #endif
    #endif   

    #ifdef FADE
       shadow = clamp(max(0.0,mix(shadow, 1.0 ,(shadowPosition - m_FadeInfo.x) * m_FadeInfo.y)),0.0,1.0);            
    #endif    
    shadow= shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
    gl_FragColor = color * vec4(shadow, shadow, shadow, 1.0);

}

