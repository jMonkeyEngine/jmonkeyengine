#import "Common/ShaderLib/GLSLCompat.glsllib"
varying vec3 normal;
varying vec2 texCoord;


#ifdef DIFFUSEMAP_ALPHA
    uniform sampler2D m_DiffuseMap;
#endif

#ifdef COLORMAP_ALPHA
    uniform sampler2D m_ColorMap;
#endif

#ifdef BASECOLORMAP_ALPHA
    uniform sampler2D m_BaseColorMap;
#endif

#if defined DIFFUSEMAP_ALPHA || defined COLORMAP_ALPHA || defined BASECOLORMAP_ALPHA
    uniform float m_AlphaDiscardThreshold;
#endif

void main(void)
{

    #ifdef DIFFUSEMAP_ALPHA
        if(texture2D(m_DiffuseMap,texCoord).a<m_AlphaDiscardThreshold){
            discard;
        }
    #endif
    #ifdef COLORMAP_ALPHA
        if(texture2D(m_ColorMap,texCoord).a<m_AlphaDiscardThreshold){
            discard;
        }
    #endif
    #ifdef BASECOLORMAP_ALPHA
        if(texture2D(m_BaseColorMap,texCoord).a<m_AlphaDiscardThreshold){
            discard;
        }
    #endif
    gl_FragColor = vec4(normal.xy* 0.5 + 0.5,-normal.z* 0.5 + 0.5, 1.0);

}
