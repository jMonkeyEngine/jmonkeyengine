#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform float m_ShadowMapSlice;
uniform sampler2DArray m_ShadowMapArray;
varying vec2 texCoord1;

void main() {
    float shadow = texture2D(m_ShadowMapArray, vec3(texCoord1, m_ShadowMapSlice)).r;

    shadow = sqrt(shadow);

    // TODO: make it betterer
    gl_FragColor.rgb = vec3(shadow);
    gl_FragColor.a = 1.0;
}
