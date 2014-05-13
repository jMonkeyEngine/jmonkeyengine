uniform sampler2D m_Texture;
varying vec2 texCoord;

uniform float m_InvGamma;

vec3 gamma(vec3 L,float invGamma){
	return pow(L, vec3(invGamma));
}

void main() {
    vec4 texVal = texture2D(m_Texture, texCoord);

    texVal.rgb = gamma(texVal.rgb , m_InvGamma);
 	
    gl_FragColor = texVal;
}