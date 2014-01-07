uniform sampler2D m_Texture;
uniform sampler2D m_BloomTex;
uniform float m_BloomIntensity;

varying vec2 texCoord;

void main(){
   vec4 colorRes = texture2D(m_Texture, texCoord);
   vec4 bloom = texture2D(m_BloomTex, texCoord);
   gl_FragColor = bloom * m_BloomIntensity + colorRes;
}

