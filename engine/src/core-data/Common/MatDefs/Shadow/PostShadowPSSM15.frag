#import "Common/ShaderLib/PssmShadows15.glsllib"

out vec4 outFragColor;

#ifdef PSSM
in float shadowPosition;
#endif

in vec4 projCoord0;
in vec4 projCoord1;
in vec4 projCoord2;
in vec4 projCoord3;

#ifdef POINTLIGHT
    in vec4 projCoord4;
    in vec4 projCoord5;
    uniform vec3 m_LightPos;
    in vec4 worldPos;
#endif

#ifdef DISCARD_ALPHA
    #ifdef COLOR_MAP
        uniform sampler2D m_ColorMap;
    #else    
        uniform sampler2D m_DiffuseMap;
    #endif
    uniform float m_AlphaDiscardThreshold;
    varying vec2 texCoord;
#endif

#ifdef FADE
uniform vec2 m_FadeInfo;
#endif

void main(){
    
    #ifdef DISCARD_ALPHA
        #ifdef COLOR_MAP
             float alpha = texture2D(m_ColorMap,texCoord).a;
        #else    
             float alpha = texture2D(m_DiffuseMap,texCoord).a;
        #endif
      
        if(alpha < m_AlphaDiscardThreshold){
            discard;
        }
    #endif

    float shadow = 1.0;
    #ifdef PSSM
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
        }
    #endif


    #ifdef POINTLIGHT         
         vec3 vect = worldPos.xyz - m_LightPos;
         vec3 absv= abs(vect);
         float maxComp = max(absv.x,max(absv.y,absv.z));
         if(maxComp == absv.y){
            if(vect.y < 0.0){
                shadow = GETSHADOW(m_ShadowMap0, projCoord0);
outFragColor = vec4(projCoord0.z);             
            }else{
                shadow = GETSHADOW(m_ShadowMap1, projCoord1);
outFragColor = vec4(projCoord1.z);
            }
         }else if(maxComp == absv.z){
            if(vect.z < 0.0){
                shadow = GETSHADOW(m_ShadowMap2, projCoord2);
outFragColor =vec4(projCoord2.z);
            }else{
                shadow = GETSHADOW(m_ShadowMap3, projCoord3);
outFragColor = vec4(projCoord3.z);
            }
         }else if(maxComp == absv.x){
            if(vect.x < 0.0){
                shadow = GETSHADOW(m_ShadowMap4, projCoord4);
outFragColor = vec4(projCoord4.z);
            }else{
                shadow = GETSHADOW(m_ShadowMap5, projCoord5);
outFragColor = vec4(projCoord5.z);
            }
         }                  
    #endif   

    #ifdef FADE
      shadow = max(0.0,mix(shadow,1.0,(shadowPosition - m_FadeInfo.x) * m_FadeInfo.y));    
    #endif
  
    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);   
    outFragColor =  vec4(shadow, shadow, shadow, 1.0);

}

