#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
#ifndef VERTEX_LIGHTING
    #import "Common/ShaderLib/BlinnPhongLighting.glsllib"
    #import "Common/ShaderLib/Lighting.glsllib"
#endif
// shading model
#import "Common/ShaderLib/ShadingModel.glsllib"

// fog - jayfella
#ifdef USE_FOG
#import "Common/ShaderLib/MaterialFog.glsllib"
varying float fog_distance;
uniform vec4 m_FogColor;

#ifdef FOG_LINEAR
uniform vec2 m_LinearFog;
#endif

#ifdef FOG_EXP
uniform float m_ExpFog;
#endif

#ifdef FOG_EXPSQ
uniform float m_ExpSqFog;
#endif

#endif // end fog

varying vec2 texCoord;
#ifdef SEPARATE_TEXCOORD
  varying vec2 texCoord2;
#endif

varying vec3 AmbientSum;
varying vec4 DiffuseSum;
varying vec3 SpecularSum;

#ifdef DIFFUSEMAP
  uniform sampler2D m_DiffuseMap;
#endif

#ifdef SPECULARMAP
  uniform sampler2D m_SpecularMap;
#endif

#ifdef PARALLAXMAP
  uniform sampler2D m_ParallaxMap;  
#endif
#if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP))) && !defined(VERTEX_LIGHTING)
    uniform float m_ParallaxHeight;
    varying vec3 vPos;
    uniform vec3 g_CameraPosition;
#endif

#ifdef LIGHTMAP
  uniform sampler2D m_LightMap;
#endif
  
#ifdef NORMALMAP
  uniform sampler2D m_NormalMap;   
  varying vec4 vTangent;
#endif
varying vec3 vNormal;

#ifdef ALPHAMAP
  uniform sampler2D m_AlphaMap;
#endif

#ifdef COLORRAMP
  uniform sampler2D m_ColorRamp;
#endif

uniform float m_AlphaDiscardThreshold;

#ifndef VERTEX_LIGHTING
uniform float m_Shininess;

    #ifdef USE_REFLECTION 
        uniform float m_ReflectionPower;
        uniform float m_ReflectionIntensity;
        varying vec4 refVec;

        uniform ENVMAP m_EnvMap;
    #endif
#endif

void main(){
    vec2 newTexCoord;
    #if defined(NORMALMAP) || defined(PARALLAXMAP)
        vec3 norm = normalize(vNormal);
        vec3 tan = normalize(vTangent.xyz);
        mat3 tbnMat = mat3(tan, vTangent.w * cross( (norm), (tan)), norm);
    #endif
     
    #if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP))) && !defined(VERTEX_LIGHTING) 
        vec3 viewDir = normalize(g_CameraPosition - vPos);
        vec3 vViewDir =  viewDir * tbnMat;
       #ifdef STEEP_PARALLAX
           #ifdef NORMALMAP_PARALLAX
               //parallax map is stored in the alpha channel of the normal map         
               newTexCoord = steepParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);
           #else
               //parallax map is a texture
               newTexCoord = steepParallaxOffset(m_ParallaxMap, vViewDir, texCoord, m_ParallaxHeight);
           #endif
       #else
           #ifdef NORMALMAP_PARALLAX
               //parallax map is stored in the alpha channel of the normal map         
               newTexCoord = classicParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);
           #else
               //parallax map is a texture
               newTexCoord = classicParallaxOffset(m_ParallaxMap, vViewDir, texCoord, m_ParallaxHeight);
           #endif
       #endif
    #else
       newTexCoord = texCoord;    
    #endif
    
   #ifdef DIFFUSEMAP
      vec4 diffuseColor = texture2D(m_DiffuseMap, newTexCoord);
    #else
      vec4 diffuseColor = vec4(1.0);
    #endif

    float alpha = DiffuseSum.a * diffuseColor.a;

    #ifdef ALPHAMAP
       alpha = alpha * texture2D(m_AlphaMap, newTexCoord).r;
    #endif

    #ifdef DISCARD_ALPHA
        if(alpha < m_AlphaDiscardThreshold){
            discard;
        }
    #endif
 
    // ***********************
    // Read from textures
    // ***********************
    #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
      vec4 normalHeight = texture2D(m_NormalMap, newTexCoord);
      //Note the -2.0 and -1.0. We invert the green channel of the normal map, 
      //as it's compliant with normal maps generated with blender.
      //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
      //for more explanation.
    //mat3 tbnMat = mat3(vTangent.xyz, vTangent.w * cross( (vNormal), (vTangent.xyz)), vNormal.xyz);

    if (!gl_FrontFacing)
    {
        tbnMat[2] = -tbnMat[2];
    }
      vec3 normal = tbnMat * normalize((normalHeight.xyz * vec3(2.0,-2.0,2.0) - vec3(1.0,-1.0,1.0)));
      normal = normalize(normal);
    #elif !defined(VERTEX_LIGHTING)
      vec3 normal = normalize(vNormal);

      if (!gl_FrontFacing)
      {
          normal = -normal;
      }
    #endif

    #ifdef SPECULARMAP
      vec4 specularColor = texture2D(m_SpecularMap, newTexCoord);
    #else
      vec4 specularColor = vec4(1.0);
    #endif

    #ifdef LIGHTMAP
       vec3 lightMapColor;
       #ifdef SEPARATE_TEXCOORD
          lightMapColor = texture2D(m_LightMap, texCoord2).rgb;
       #else
          lightMapColor = texture2D(m_LightMap, texCoord).rgb;
       #endif
       specularColor.rgb *= lightMapColor;
       diffuseColor.rgb  *= lightMapColor;
    #endif

    // pack
    outGBuffer3.xyz = normal;
    outGBuffer0.a = alpha;

//    #ifdef USE_REFLECTION
//        vec4 refColor = Optics_GetEnvColor(m_EnvMap, refVec.xyz);
//    #endif
//        // Workaround, since it is not possible to modify varying variables
//        vec4 SpecularSum2 = vec4(SpecularSum, 1.0);
//    #ifdef USE_REFLECTION
//       // Interpolate light specularity toward reflection color
//       // Multiply result by specular map
//       specularColor = mix(SpecularSum2 * light.y, refColor, refVec.w) * specularColor;
//
//       SpecularSum2 = vec4(1.0);
//    #endif
//
//    vec3 DiffuseSum2 = DiffuseSum.rgb;
//    #ifdef COLORRAMP
//       DiffuseSum2.rgb  *= texture2D(m_ColorRamp, vec2(light.x, 0.0)).rgb;
//       SpecularSum2.rgb *= texture2D(m_ColorRamp, vec2(light.y, 0.0)).rgb;
//       light.xy = vec2(1.0);
//    #endif
    outGBuffer0.rgb = diffuseColor.rgb * DiffuseSum.rgb;
    outGBuffer1.rgb = specularColor.rgb * SpecularSum.rgb * 100.0f + AmbientSum * 0.01f;
    outGBuffer1.a = m_Shininess;

    // shading model id
    outGBuffer2.a = PHONG_LIGHTING;
}
