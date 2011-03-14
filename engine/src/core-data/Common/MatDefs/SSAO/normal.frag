varying vec3 normal;
varying vec2 texCoord;


#ifdef DIFFUSEMAP_ALPHA
    uniform sampler2D m_DiffuseMap;
    uniform float m_AlphaDiscardThreshold;
#endif

void main(void)
{

    #ifdef DIFFUSEMAP_ALPHA
        if(texture2D(m_DiffuseMap,texCoord).a<m_AlphaDiscardThreshold){
            discard;
        }
    #endif
    gl_FragColor = vec4(normal.xy* 0.5 + 0.5,-normal.z* 0.5 + 0.5, 1.0);

}

