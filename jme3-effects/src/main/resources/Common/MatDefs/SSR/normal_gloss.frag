#import "Common/ShaderLib/GLSLCompat.glsllib"
varying vec3 wNormal;
varying vec2 texCoord;

#ifdef NORMALMAP
  uniform sampler2D m_NormalMap;   
  varying vec4 wTangent;
#endif

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
    #if defined(NORMALMAP)
        vec3 tan = normalize(wTangent.xyz);
        mat3 tbnMat = mat3(tan, wTangent.w * cross( (wNormal), (tan)), wNormal);
    #endif
    
    #if defined(NORMALMAP)
      vec4 normalHeight = texture2D(m_NormalMap, texCoord);
      //Note the -2.0 and -1.0. We invert the green channel of the normal map, 
      //as it's complient with normal maps generated with blender.
      //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
      //for more explanation.
      vec3 normal = normalize((normalHeight.xyz * vec3(2.0, 2.0, 2.0) - vec3(1.0, 1.0, 1.0)));
      normal = normalize(tbnMat * normal);
      //normal = normalize(normal * inverse(tbnMat));
    #else
      vec3 normal = wNormal;
    #endif
    
    float glossiness = 0.0;
    #ifdef SPECGLOSSPIPELINE
        #ifdef GLOSSINESSMAP
        glossiness = texture2D(m_GlossinessMap, texCoord).r;
        #else 
        glossiness = m_Glossiness;
        #endif
    #else 
        #ifdef ROUGHNESSMAP
        glossiness = 1.0 - texture2D(m_RoughnessMap, texCoord).r;
        #else 
        glossiness = 1.0 - m_Roughness;
        #endif
    #endif
    gl_FragColor = vec4(vec3(normal.xy * 0.5 + 0.5, glossiness), 1.0);

}
