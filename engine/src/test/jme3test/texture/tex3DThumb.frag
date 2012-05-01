uniform sampler3D m_Texture;
uniform int m_Rows;
uniform float m_InvDepth;

varying vec2 texCoord;

void main(){
    float rows = float(m_Rows);
    float depthx = floor(texCoord.x);
    float depthy = (rows - 1.0) - floor(texCoord.y);    
    //vec3 texC=vec3(texCoord.x,texCoord.y ,0.7);//
  
    vec3 texC = vec3(fract(texCoord.x),fract(texCoord.y),(depthy * rows + depthx) * m_InvDepth);
    gl_FragColor = texture3D(m_Texture, texC);
}