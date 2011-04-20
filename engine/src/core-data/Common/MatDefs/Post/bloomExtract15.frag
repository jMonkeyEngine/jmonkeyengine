#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;

uniform float m_ExposurePow;
uniform float m_ExposureCutoff;

in vec2 texCoord;

#ifdef HAS_GLOWMAP
  uniform sampler2D m_GlowMap;
#endif

void main(){
   vec4 color;
   #ifdef DO_EXTRACT
     color = getColor(m_Texture, texCoord);
     if ( (color.r + color.g + color.b) / 3.0 < m_ExposureCutoff ) {
         color = vec4(0.0);
      }else{
         color = pow(color, vec4(m_ExposurePow));
      }
   #endif

   #ifdef HAS_GLOWMAP
        vec4 glowColor = texture2D( m_GlowMap, texCoord );
        glowColor = pow(glowColor, vec4(m_ExposurePow));
        color += glowColor;
   #endif
   
   gl_FragColor = color;
}
