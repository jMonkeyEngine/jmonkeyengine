uniform sampler2D m_Texture; // this should hold the texture rendered by the horizontal blur pass
uniform float m_Size;
uniform float m_Scale;

varying vec2 texCoord;

void main(){ 
   float blurSize = m_Scale/m_Size;
   vec4 sum = vec4(0.0);

   // blur in x (vertical)
   // take nine samples, with the distance blurSize between them
   sum += texture2D(m_Texture, vec2(texCoord.x- 4.0*blurSize, texCoord.y )) * 0.05;
   sum += texture2D(m_Texture, vec2(texCoord.x- 3.0*blurSize, texCoord.y )) * 0.09;
   sum += texture2D(m_Texture, vec2(texCoord.x - 2.0*blurSize, texCoord.y)) * 0.12;
   sum += texture2D(m_Texture, vec2(texCoord.x- blurSize, texCoord.y )) * 0.15;
   sum += texture2D(m_Texture, vec2(texCoord.x, texCoord.y)) * 0.16;
   sum += texture2D(m_Texture, vec2(texCoord.x+ blurSize, texCoord.y )) * 0.15;
   sum += texture2D(m_Texture, vec2(texCoord.x+ 2.0*blurSize, texCoord.y )) * 0.12;
   sum += texture2D(m_Texture, vec2(texCoord.x+ 3.0*blurSize, texCoord.y )) * 0.09;
   sum += texture2D(m_Texture, vec2(texCoord.x+ 4.0*blurSize, texCoord.y )) * 0.05;

   gl_FragColor = sum;
}