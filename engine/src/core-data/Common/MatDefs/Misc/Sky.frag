#import "Common/ShaderLib/Optics.glsllib"

uniform ENVMAP m_Texture;

varying vec3 direction;

void main() {
    //gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
    
    //gl_FragDepth = 1.0;
    vec3 dir = normalize(direction);
    gl_FragColor = Optics_GetEnvColor(m_Texture, direction);
    //gl_FragColor = vec4(textureCube(m_Texture, dir).xyz, 1.0);
    //gl_FragColor = vec4((dir * vec3(0.5)) + vec3(0.5), 1.0);
}

