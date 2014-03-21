uniform sampler3D m_Texture;

varying vec3 texCoord;

void main(){
    gl_FragColor= texture3D(m_Texture,texCoord);
}