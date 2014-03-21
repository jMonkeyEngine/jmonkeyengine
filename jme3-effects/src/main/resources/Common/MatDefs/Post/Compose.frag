uniform sampler2D m_Texture;
uniform sampler2D m_CompositeTexture;
varying vec2 texCoord;

void main() {
      vec4 texVal = texture2D(m_Texture, texCoord);
      vec4 compositeVal = texture2D(m_CompositeTexture, texCoord);
      gl_FragColor = mix(compositeVal,texVal,texVal.a);
}

