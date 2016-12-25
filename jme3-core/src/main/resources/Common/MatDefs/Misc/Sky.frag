#import "Common/ShaderLib/Optics.glsllib"
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform ENVMAP m_Texture;

varying vec3 direction;

void main() {
    vec3 dir = normalize(direction);
    gl_FragColor = Optics_GetEnvColor(m_Texture, dir);
}

