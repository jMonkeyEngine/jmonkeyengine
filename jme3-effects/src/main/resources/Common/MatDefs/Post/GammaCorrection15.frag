#import "Common/ShaderLib/MultiSample.glsllib"
 
uniform COLORTEXTURE m_Texture;
in vec2 texCoord;
 
uniform float m_InvGamma;

vec3 gamma(vec3 L,float invGamma){
	return pow(L, vec3(invGamma));
}

out vec4 fragColor;
 
void main() {
    vec4 texVal = getColor(m_Texture, texCoord);
    
    texVal.rgb = gamma(texVal.rgb , m_InvGamma);
 
    fragColor = texVal;
}