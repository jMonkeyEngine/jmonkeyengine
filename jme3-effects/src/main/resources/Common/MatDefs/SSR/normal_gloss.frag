#import "Common/ShaderLib/GLSLCompat.glsllib"
varying vec3 normal;
varying vec2 texCoord;


#ifdef SPECGLOSSPIPELINE
    #ifdef GLOSSINESSMAP
    uniform sampler2D m_GlossinessMap;
    #else 
    uniform float m_Glossiness;
    #endif
#else 
    #ifdef ROUGHNESSMAP
    uniform sampler2D m_RoughnessMap;
    #else
    uniform float m_Roughness;
    #endif
#endif

void main(void)
{
    float glossiness = 0.0;
    #ifdef SPECGLOSSPIPELINE
        #ifdef GLOSSINESSMAP
        glossiness = texture2D(m_GlossinessMap,texCoord).r;
        #else 
        glossiness = m_Glossiness;
        #endif
    #else 
        #ifdef GLOSSINESSMAP
        glossiness = 1.0 - texture2D(m_RoughnessMap,texCoord).r;
        #else 
        glossiness = 1.0 - m_Roughness;
        #endif
    #endif
    gl_FragColor = vec4(vec3(normal.xy * 0.5 + 0.5, glossiness), 1.0);

}
