#if __VERSION__ >= 130
    #extension GL_NV_shadow_samplers_cube : enable
#endif

#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Optics.glsllib"

uniform ENVMAP m_Texture;

varying vec3 direction;

void main() {
    vec3 dir = normalize(direction);
    gl_FragColor = Optics_GetEnvColor(m_Texture, dir);
}
