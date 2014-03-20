#import "Common/ShaderLib/Optics.glsllib"

uniform float m_Shininess;

varying vec2 texCoord;
varying vec4 AmbientSum;
varying vec4 DiffuseSum;
varying vec4 SpecularSum;

varying float vDepth;
varying vec3 vNormal;

#ifdef DIFFUSEMAP
  uniform sampler2D m_DiffuseMap;
#endif

#ifdef SPECULARMAP
  uniform sampler2D m_SpecularMap;
#endif

#ifdef PARALLAXMAP
  uniform sampler2D m_ParallaxMap;
#endif

#ifdef NORMALMAP
  uniform sampler2D m_NormalMap;
  varying mat3 tbnMat;
#endif

vec2 encodeNormal(in vec3 n){
    vec2 enc = normalize(n.xy) * (sqrt(-n.z*0.5+0.5));
    enc = enc*vec2(0.5)+vec2(0.5);
    return enc;
}

void main(){
    vec2 newTexCoord = texCoord;
    float height = 0.0;
    #if defined(PARALLAXMAP) || defined(NORMALMAP_PARALLAX)
       #ifdef PARALLAXMAP
          height = texture2D(m_ParallaxMap, texCoord).r;
       #else
          height = texture2D(m_NormalMap, texCoord).a;
       #endif
       float heightScale = 0.05;
       float heightBias = heightScale * -0.5;
       height = (height * heightScale + heightBias);
    #endif


    // ***********************
    // Read from textures
    // ***********************
    #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
      vec4 normalHeight = texture2D(m_NormalMap, newTexCoord);
      vec3 normal = (normalHeight.xyz * vec3(2.0) - vec3(1.0));
      normal.y = -normal.y;

      normal = tbnMat * normal;
    #else
      vec3 normal = vNormal;
      #if !defined(LOW_QUALITY) && !defined(V_TANGENT)
         normal = normalize(normal);
      #endif
    #endif

    #ifdef DIFFUSEMAP
      vec4 diffuseColor = texture2D(m_DiffuseMap, newTexCoord);
    #else
      vec4 diffuseColor = vec4(1.0);
    #endif

    #ifdef SPECULARMAP
      vec4 specularColor = texture2D(m_SpecularMap, newTexCoord);
    #else
      vec4 specularColor = vec4(1.0);
    #endif

    diffuseColor.rgb  *= DiffuseSum.rgb;
    specularColor.rgb *= SpecularSum.rgb;

    gl_FragData[0] = vec4(diffuseColor.rgb, 1.0);
    gl_FragData[1] = vec4(encodeNormal(normal), 0.0, 0.0);
                          /*encodeNormal(vNormal));*/
    gl_FragData[2] = vec4(specularColor.rgb, m_Shininess / 128.0);
}
