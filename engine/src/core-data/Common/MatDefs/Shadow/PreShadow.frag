varying vec2 texCoord;


#ifdef COLOR_MAP
  uniform sampler2D m_ColorMap;
#endif 
#ifdef DIFFUSEMAP
  uniform sampler2D m_DiffuseMap;
#endif
   


void main(){
    float a = 1.0;
    
    #ifdef COLOR_MAP
        a = texture2D(m_ColorMap, texCoord).a;
    #endif    
    #ifdef DIFFUSEMAP
        a = texture2D(m_DiffuseMap, texCoord).a;
    #endif

   gl_FragColor = vec4(a);
}