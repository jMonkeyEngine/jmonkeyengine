#import "Common/ShaderLib/PssmShadows.glsllib"

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

vec3 getPosition(in float depth, in vec2 uv){
    vec4 pos = vec4(uv, depth, 1.0) * 2.0 - 1.0;
    pos = m_ViewProjectionMatrixInverse * pos;
    return pos.xyz / pos.w;
}

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
  
    // populate the light view matrices array and convert vertex to light viewProj space
    vec4 projCoord0 = biasMat * m_LightViewProjectionMatrix0 * worldPos;
    vec4 projCoord1 = biasMat * m_LightViewProjectionMatrix1 * worldPos;
    vec4 projCoord2 = biasMat * m_LightViewProjectionMatrix2 * worldPos;
    vec4 projCoord3 = biasMat * m_LightViewProjectionMatrix3 * worldPos;


    float shadowPosition = m_ViewProjectionMatrixRow2.x * worldPos.x +  m_ViewProjectionMatrixRow2.y * worldPos.y +  m_ViewProjectionMatrixRow2.z * worldPos.z +  m_ViewProjectionMatrixRow2.w;
    
    float shadow = 0.0;
    if(shadowPosition < m_Splits.x){
        shadow = GETSHADOW(m_ShadowMap0, projCoord0);
    }else if( shadowPosition <  m_Splits.y){
        shadowBorderScale = 0.5;
        shadow = GETSHADOW(m_ShadowMap1, projCoord1);
    }else if( shadowPosition <  m_Splits.z){
        shadowBorderScale = 0.25;
        shadow = GETSHADOW(m_ShadowMap2, projCoord2);
    }else if( shadowPosition <  m_Splits.w){
        shadowBorderScale = 0.125;
        shadow = GETSHADOW(m_ShadowMap3, projCoord3);
    }else{
        shadow = 1.0;
    }
    
    shadow= shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
    gl_FragColor = color * vec4(shadow, shadow, shadow, 1.0);

}

