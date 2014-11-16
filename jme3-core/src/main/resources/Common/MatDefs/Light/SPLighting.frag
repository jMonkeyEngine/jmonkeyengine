#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
#ifndef VERTEX_LIGHTING
    #import "Common/ShaderLib/PhongLighting.glsllib"
    #import "Common/ShaderLib/Lighting.glsllib"
#endif

varying vec2 texCoord;
#ifdef SEPARATE_TEXCOORD
  varying vec2 texCoord2;
#endif

varying vec3 AmbientSum;
varying vec4 DiffuseSum;
varying vec3 SpecularSum;

#ifndef VERTEX_LIGHTING
    uniform mat4 g_ViewMatrix;
    uniform vec4 g_LightData[NB_LIGHTS];
    varying vec3 vPos;    
#else
    varying vec3 specularAccum;
    varying vec4 diffuseAccum;
#endif

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
#endif

#ifdef LIGHTMAP
  uniform sampler2D m_LightMap;
#endif
  
#ifdef NORMALMAP
  uniform sampler2D m_NormalMap;   
  varying vec3 vTangent;
  varying vec3 vBinormal;
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
     
    #if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP))) && !defined(VERTEX_LIGHTING) 
     
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
      //as it's complient with normal maps generated with blender.
      //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
      //for more explanation.
      vec3 normal = normalize((normalHeight.xyz * vec3(2.0,-2.0,2.0) - vec3(1.0,-1.0,1.0)));
    #elif !defined(VERTEX_LIGHTING)
      vec3 normal = normalize(vNormal);            
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

    #ifdef VERTEX_LIGHTING
        gl_FragColor.rgb = AmbientSum  * diffuseColor.rgb 
                            +diffuseAccum.rgb *diffuseColor.rgb
                            +specularAccum.rgb * specularColor.rgb;
        gl_FragColor.a=1.0;                           
    #else       
        
        int i = 0;
        gl_FragColor.rgb = AmbientSum * diffuseColor.rgb;

        #ifdef USE_REFLECTION
             vec4 refColor = Optics_GetEnvColor(m_EnvMap, refVec.xyz);
        #endif

        #ifdef NORMALMAP   
            mat3 tbnMat = mat3(normalize(vTangent.xyz) , normalize(vBinormal.xyz) , normalize(vNormal.xyz));
            vec3 viewDir = normalize(-vPos.xyz * tbnMat);
        #else
            vec3 viewDir = normalize(-vPos.xyz);
        #endif

        for( int i = 0;i < NB_LIGHTS; i+=3){
            vec4 lightColor = g_LightData[i];
            vec4 lightData1 = g_LightData[i+1];                
            vec4 lightDir;
            vec3 lightVec;            
            lightComputeDir(vPos, lightColor.w, lightData1, lightDir,lightVec);

            float spotFallOff = 1.0;
            #if __VERSION__ >= 110
                // allow use of control flow
            if(lightColor.w > 1.0){
            #endif
                spotFallOff =  computeSpotFalloff(g_LightData[i+2], lightVec);
            #if __VERSION__ >= 110
            }
            #endif
         
            #ifdef NORMALMAP         
                //Normal map -> lighting is computed in tangent space
                lightDir.xyz = normalize(lightDir.xyz * tbnMat);                
            #else
                //no Normal map -> lighting is computed in view space
                lightDir.xyz = normalize(lightDir.xyz);                
            #endif

            vec2 light = computeLighting(normal, viewDir, lightDir.xyz, lightDir.w * spotFallOff , m_Shininess);

            #ifdef COLORRAMP
                diffuseColor.rgb  *= texture2D(m_ColorRamp, vec2(light.x, 0.0)).rgb;
                specularColor.rgb *= texture2D(m_ColorRamp, vec2(light.y, 0.0)).rgb;
            #endif

            // Workaround, since it is not possible to modify varying variables
            vec4 SpecularSum2 = vec4(SpecularSum, 1.0);
            #ifdef USE_REFLECTION                    
                 // Interpolate light specularity toward reflection color
                 // Multiply result by specular map
                 specularColor = mix(SpecularSum2 * light.y, refColor, refVec.w) * specularColor;

                 SpecularSum2 = vec4(1.0);
                 light.y = 1.0;
            #endif

            gl_FragColor.rgb += DiffuseSum.rgb * lightColor.rgb * diffuseColor.rgb  * vec3(light.x) +
                                SpecularSum2.rgb * specularColor.rgb * vec3(light.y);
        }
           
     #endif
    gl_FragColor.a = alpha;
}
