#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

varying vec2 texCoord;
#ifdef SEPARATE_TEXCOORD
  varying vec2 texCoord2;
#endif

varying vec4 Color;

uniform vec4 g_LightData[NB_LIGHTS];

uniform vec3 g_CameraPosition;

uniform float m_Roughness;
uniform float m_Metallic;

varying vec3 wPosition;    


//#ifdef INDIRECT_LIGHTING
//  uniform sampler2D m_IntegrateBRDF;
  uniform samplerCube g_PrefEnvMap;
  uniform samplerCube g_IrradianceMap;
  uniform vec4 g_ProbeData;
//#endif

#ifdef BASECOLORMAP
  uniform sampler2D m_BaseColorMap;
#endif
#ifdef METALLICMAP
  uniform sampler2D m_MetallicMap;
#endif
#ifdef ROUGHNESSMAP
  uniform sampler2D m_RoughnessMap;
#endif

#ifdef EMISSIVE
    uniform vec4 m_Emissive;
#endif
#ifdef EMISSIVEMAP
    uniform sampler2D m_EmissiveMap;
#endif
#if defined(EMISSIVE) || defined(EMISSIVEMAP)
    uniform float m_EmissivePower;
    uniform float m_EmissiveIntensity;
#endif 

#ifdef SPECGLOSSPIPELINE
  uniform sampler2D m_SpecularMap;
  uniform sampler2D m_GlossMap;
#endif

#ifdef PARALLAXMAP
  uniform sampler2D m_ParallaxMap;  
#endif
#if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP)))
    uniform float m_ParallaxHeight;
#endif

#ifdef LIGHTMAP
  uniform sampler2D m_LightMap;
#endif
  
#if defined(NORMALMAP) || defined(PARALLAXMAP)
  uniform sampler2D m_NormalMap;   
  varying vec3 wTangent;
  varying vec3 wBinormal;
#endif
varying vec3 wNormal;

#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif

void main(){
    vec2 newTexCoord;
    vec3 viewDir = normalize(g_CameraPosition - wPosition);

    #if defined(NORMALMAP) || defined(PARALLAXMAP)
        mat3 tbnMat = mat3(normalize(wTangent.xyz) , normalize(wBinormal.xyz) , normalize(wNormal.xyz));       
    #endif

    #if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP)))
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
    
    #ifdef BASECOLORMAP
        vec4 albedo = texture2D(m_BaseColorMap, newTexCoord);
    #else
        vec4 albedo = Color;
    #endif
    #ifdef ROUGHNESSMAP
        float Roughness = texture2D(m_RoughnessMap, newTexCoord).r * max(m_Roughness,1e-8);
    #else
        float Roughness =  max(m_Roughness,1e-8);
    #endif
    #ifdef METALLICMAP   
        float Metallic = texture2D(m_MetallicMap, newTexCoord).r;
    #else
        float Metallic =  max(m_Metallic,0.00);
    #endif
 
    float alpha = Color.a * albedo.a;

    #ifdef DISCARD_ALPHA
        if(alpha < m_AlphaDiscardThreshold){
            discard;
        }
    #endif
 
    // ***********************
    // Read from textures
    // ***********************
    #if defined(NORMALMAP)
      vec4 normalHeight = texture2D(m_NormalMap, newTexCoord);
      //Note the -2.0 and -1.0. We invert the green channel of the normal map, 
      //as it's complient with normal maps generated with blender.
      //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
      //for more explanation.
      vec3 normal = normalize((normalHeight.xyz * vec3(2.0,-2.0,2.0) - vec3(1.0,-1.0,1.0)));
      normal = tbnMat * normal;
      //normal = normalize(normal * inverse(tbnMat));
    #else
      vec3 normal = normalize(wNormal);            
    #endif

   
    #ifdef LIGHTMAP
       vec3 lightMapColor;
       #ifdef SEPARATE_TEXCOORD
          lightMapColor = texture2D(m_LightMap, texCoord2).rgb;
       #else
          lightMapColor = texture2D(m_LightMap, texCoord).rgb;
       #endif
       specularColor.rgb *= lightMapColor;
       albedo.rgb  *= lightMapColor;
    #endif

    float specular = 0.5;
    #ifdef SPECGLOSSPIPELINE
          vec4 specularColor = texture2D(m_SpecularMap, newTexCoord);
          vec4 diffuseColor = albedo;
          Roughness = 1.0 - texture2D(m_GlossMap, newTexCoord).r;          
    #else      
        float nonMetalSpec = 0.08 * specular;
        vec4 specularColor = (nonMetalSpec - nonMetalSpec * Metallic) + albedo * Metallic;
        vec4 diffuseColor = albedo - albedo * Metallic;
    #endif

    gl_FragColor.rgb = vec3(0.0);
    float ndotv = max( dot( normal, viewDir ),0.0);
    for( int i = 0;i < NB_LIGHTS; i+=3){
        vec4 lightColor = g_LightData[i];
        vec4 lightData1 = g_LightData[i+1];                
        vec4 lightDir;
        vec3 lightVec;            
        lightComputeDir(wPosition, lightColor.w, lightData1, lightDir, lightVec);

        float fallOff = 1.0;
        #if __VERSION__ >= 110
            // allow use of control flow
        if(lightColor.w > 1.0){
        #endif
            fallOff =  computeSpotFalloff(g_LightData[i+2], lightVec);
        #if __VERSION__ >= 110
        }
        #endif
        //point light attenuation
        fallOff *= lightDir.w;

        lightDir.xyz = normalize(lightDir.xyz);            
        vec3 directDiffuse;
        vec3 directSpecular;
        
        PBR_ComputeDirectLight(normal, lightDir.xyz, viewDir,
                            lightColor.rgb,specular, Roughness, ndotv,
                            directDiffuse,  directSpecular);

        vec3 directLighting = diffuseColor.rgb *directDiffuse + directSpecular * specularColor.rgb;
        
        gl_FragColor.rgb += directLighting * fallOff;
    }

 //   #ifdef INDIRECT_LIGHTING
        vec3 rv = reflect(-viewDir.xyz, normal.xyz);
        //prallax fix for spherical bounds.
        rv = g_ProbeData.w * (wPosition - g_ProbeData.xyz) +rv;
       
         //horizon fade from http://marmosetco.tumblr.com/post/81245981087
        float horiz = dot(rv, wNormal.xyz);
        float horizFadePower= 1.0 - Roughness;
        horiz = clamp( 1.0 + horizFadePower * horiz, 0.0, 1.0 );
        horiz *= horiz;
        
        vec3 indirectDiffuse = vec3(0.0);
        vec3 indirectSpecular = vec3(0.0);    
        indirectDiffuse = textureCube(g_IrradianceMap, rv.xyz).rgb * albedo.rgb;
        
        indirectSpecular = ApproximateSpecularIBLPolynomial(g_PrefEnvMap, specularColor.rgb, Roughness, ndotv, rv.xyz);
        indirectSpecular *= vec3(horiz);

        vec3 indirectLighting =  indirectDiffuse + indirectSpecular;
        
        gl_FragColor.rgb = gl_FragColor.rgb + indirectLighting ;        
  //  #endif
 
    #if defined(EMISSIVE) || defined (EMISSIVEMAP)
        #ifdef EMISSIVEMAP
            vec4 emissive = texture2D(m_EmissiveMap, newTexCoord);
        #else
            vec4 emissive = m_Emissive;
        #endif
        gl_FragColor += emissive * pow(emissive.a, m_EmissivePower) * m_EmissiveIntensity;
    #endif
           
    gl_FragColor.a = alpha;
   
}
