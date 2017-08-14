#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Optics.glsllib"

uniform textureCube m_Texture;
uniform sampler2D m_SimpleTexture;

varying vec3 direction;

void main() {

    vec3 dir = normalize(direction);

    #if defined(CUBE_MAP)
        gl_FragColor = Optics_GetEnvColor(m_Texture, dir);
    #else
        gl_FragColor = Optics_GetEnvColor(m_SimpleTexture, dir);
    #endif
}

