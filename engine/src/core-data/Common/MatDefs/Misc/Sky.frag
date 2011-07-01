#import "Common/ShaderLib/Optics.glsllib"

uniform ENVMAP m_Texture;

varying vec3 direction;

void main() {
    vec3 dir = normalize(direction);
    gl_FragColor = Optics_GetEnvColor(m_Texture, direction);
}

