varying vec2 texCoord;

#ifdef DIFFUSEMAP_ALPHA
uniform sampler2D m_DiffuseMap;
#endif


void main(){
   #ifdef DIFFUSEMAP_ALPHA
      if (texture2D(m_DiffuseMap, texCoord).a <= 0.50)
          discard;
   #endif

   gl_FragColor = vec4(1.0);
}