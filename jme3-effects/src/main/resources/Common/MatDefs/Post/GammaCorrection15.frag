#import "Common/ShaderLib/MultiSample.glsllib"
 
uniform COLORTEXTURE m_Texture;
in vec2 texCoord;
 
uniform float m_gamma;

vec3 gamma(vec3 L,float gamma)
{
	return pow(L, vec3(1.0 / gamma));
}
 
void main() {
    vec4 texVal = texture2D(m_Texture, texCoord);
 
 	if(m_gamma > 0.0)
 	{
    	texVal.rgb = gamma(texVal.rgb , m_gamma);
 	}
 	
 	#ifdef COMPUTE_LUMA
 		texVal.a = dot(texVal.rgb, vec3(0.299, 0.587, 0.114));
 	#endif
 
    gl_FragColor = texVal;
}