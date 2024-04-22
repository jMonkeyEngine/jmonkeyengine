#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;
varying vec2 texCoord;
 
void main() {
    vec3 val = getDepth(m_DepthTexture, texCoord).rrr;
    if (val.r <= 1.0 && val.r >= 0.0) {
        gl_FragColor.rgb = vec3(1.0) - val;
    } else {
        gl_FragColor.rgb = vec3(1.0, 0.0, 0.0);
    }
}
