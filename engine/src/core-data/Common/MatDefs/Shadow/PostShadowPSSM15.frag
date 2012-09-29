#import "Common/ShaderLib/PssmShadows15.glsllib"

out vec4 outFragColor;
in float shadowPosition;
in vec4 projCoord0;
in vec4 projCoord1;
in vec4 projCoord2;
in vec4 projCoord3;

#ifdef DISCARD_ALPHA
    #ifdef COLOR_MAP
        uniform sampler2D m_ColorMap;
    #else    
        uniform sampler2D m_DiffuseMap;
    #endif
    uniform float m_AlphaDiscardThreshold;
    varying vec2 texCoord;
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
    }
    
    shadow= shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
 
    outFragColor =  vec4(shadow, shadow, shadow, 1.0);

}

