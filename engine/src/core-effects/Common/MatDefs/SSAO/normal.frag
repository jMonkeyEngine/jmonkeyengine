varying vec3 normal;
varying vec2 texCoord;


#ifdef DIFFUSEMAP_ALPHA
    uniform sampler2D m_DiffuseMap;
#endif

void main(void)
{
    float alpha= 1.0;
    #ifdef DIFFUSEMAP_ALPHA
        alpha=texture2D(m_DiffuseMap,texCoord).a;
    #endif
    gl_FragColor = vec4(normal.xy* 0.5 + 0.5,-normal.z* 0.5 + 0.5, alpha);

}

