#extension GL_EXT_texture_array : enable
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/MatDefs/Terrain/AfflictionLib.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"

// shading model
#import "Common/ShaderLib/ShadingModel.glsllib"
// octahedral
#import "Common/ShaderLib/Octahedral.glsllib"

varying vec3 wPosition;
varying vec3 vNormal;
varying vec2 texCoord;
uniform vec3 g_CameraPosition;
varying vec3 vPosition;
varying vec3 vnPosition;
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec4 vnLightDir;
varying vec3 lightVec;
varying vec3 inNormal;
varying vec3 wNormal;

#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex; 
#endif

//texture arrays:
uniform sampler2DArray m_AlbedoTextureArray;
uniform sampler2DArray m_NormalParallaxTextureArray;
uniform sampler2DArray m_MetallicRoughnessAoEiTextureArray;

//texture-slot params for 12 unique texture slots (0-11) where the integer value points to the desired texture's index in the corresponding texture array:
#for i=0..12 ( $0 )
    uniform int m_AfflictionMode_$i;
    uniform float m_Roughness_$i;
    uniform float m_Metallic_$i;
    uniform float m_AlbedoMap_$i_scale;
    uniform vec4 m_EmissiveColor_$i;
    
    #ifdef ALBEDOMAP_$i
        uniform int m_AlbedoMap_$i;
    #endif        
    #ifdef NORMALMAP_$i
        uniform int m_NormalMap_$i;
    #endif    
    #ifdef METALLICROUGHNESSMAP_$i
        uniform int m_MetallicRoughnessMap_$i;
    #endif
#endfor 

//3 alpha maps :
#ifdef ALPHAMAP
  uniform sampler2D m_AlphaMap;
#endif
#ifdef ALPHAMAP_1
  uniform sampler2D m_AlphaMap_1;
#endif
#ifdef ALPHAMAP_2
  uniform sampler2D m_AlphaMap_2;
#endif

#ifdef DISCARD_ALPHA
    uniform float m_AlphaDiscardThreshold;
#endif

//fog vars for basic fog :
#ifdef USE_FOG
#import "Common/ShaderLib/MaterialFog.glsllib"
    uniform vec4 m_FogColor;
    float fogDistance;

    uniform vec2 m_LinearFog;
#endif
#ifdef FOG_EXP
    uniform float m_ExpFog;
#endif
#ifdef FOG_EXPSQ
    uniform  float m_ExpSqFog;
#endif

//sun intensity is a secondary AO value that can be painted per-vertex in the red channel of the 
// vertex colors, or it can be set as a static value for an entire material with the StaticSunIntensity float param 
#if defined(USE_VERTEX_COLORS_AS_SUN_INTENSITY) 
    varying vec4 vertColors; 
#endif

#ifdef STATIC_SUN_INTENSITY
    uniform float m_StaticSunIntensity;
#endif
//sun intensity AO value is only applied to the directional light, not to point lights, so it is important to track if the 
//sun is more/less bright than the brightest point light for each fragment to determine how the light probe's ambient light should be scaled later on in light calculation code
float brightestPointLight = 0.0;

//optional affliction paramaters that use the AfflictionAlphaMap's green channel for splatting m_SplatAlbedoMap and the red channel for splatting desaturation  :
#ifdef AFFLICTIONTEXTURE 
    uniform sampler2D m_AfflictionAlphaMap;
#endif
#ifdef USE_SPLAT_NOISE
     uniform float m_SplatNoiseVar;
#endif
//only defined for non-terrain geoemtries and terrains that are not positioned nor sized in correlation to the 2d array of AfflictionAlphaMaps used for splatting accross large tile based scenes in a grid
#ifdef TILELOCATION
    uniform float m_TileWidth;
    uniform vec3 m_TileLocation;
#endif
#ifdef AFFLICTIONALBEDOMAP
    uniform sampler2D m_SplatAlbedoMap;
#endif
#ifdef AFFLICTIONNORMALMAP
    uniform sampler2D m_SplatNormalMap;
#endif
#ifdef AFFLICTIONROUGHNESSMETALLICMAP
    uniform sampler2D m_SplatRoughnessMetallicMap;
#endif
#ifdef AFFLICTIONEMISSIVEMAP
    uniform sampler2D m_SplatEmissiveMap;
#endif

uniform int m_AfflictionSplatScale;
uniform float m_AfflictionRoughnessValue;
uniform float m_AfflictionMetallicValue;
uniform float m_AfflictionEmissiveValue;
uniform vec4 m_AfflictionEmissiveColor;

vec4 afflictionVector;
float noiseHash;
float livelinessValue;
float afflictionValue;
int afflictionMode = 1;

//general temp vars :
vec4 tempAlbedo, tempNormal, tempEmissiveColor;
float tempParallax, tempMetallic, tempRoughness, tempAo, tempEmissiveIntensity;

vec3 viewDir;
vec2 coord;
vec4 albedo = vec4(1.0);
vec3 normal = vec3(0.5,0.5,1);
vec3 norm;
float Metallic;
float Roughness;
float packedAoValue = 1.0;
vec4 emissive;
float emissiveIntensity = 1.0;
float indoorSunLightExposure = 1.0;
 
vec4 packedMetallicRoughnessAoEiVec;
vec4 packedNormalParallaxVec;  


void main(){    
    
    #ifdef USE_FOG
        fogDistance = distance(g_CameraPosition, wPosition.xyz);
    #endif
    
    float indoorSunLightExposure = 1.0;
    
    viewDir = normalize(g_CameraPosition - wPosition);

    norm  = normalize(wNormal);
    normal = norm;


    afflictionVector = vec4(1.0, 0.0, 1.0, 0.0); //r channel is sturation, g channel is affliction splat texture intensity, b and a unused (might use b channel for wetness eventually)
    
    #ifdef AFFLICTIONTEXTURE
    
        #ifdef TILELOCATION 
        //subterrains that are not centred in tile or equal to tile width in total size need to have m_TileWidth pre-set. (tileWidth is the x,z dimesnions that the AfflictionAlphaMap represents)..
            vec2 tileCoords;
            float xPos, zPos;

            vec3 locInTile = (wPosition - m_TileLocation);

             locInTile += vec3(m_TileWidth/2, 0, m_TileWidth/2);

             xPos = (locInTile.x / m_TileWidth);
             zPos = 1 - (locInTile.z / m_TileWidth);

            tileCoords = vec2(xPos, zPos);

            afflictionVector = texture2D(m_AfflictionAlphaMap, tileCoords).rgba;
        
        
     
        #else
           // ..othrewise when terrain size matches tileWidth, the terrain's texCoords can be used for simple texel fetching of the AfflictionAlphaMap
            afflictionVector = texture2D(m_AfflictionAlphaMap, texCoord.xy).rgba;
        #endif
    #endif

    livelinessValue = afflictionVector.r;
    afflictionValue = afflictionVector.g;


    #ifdef ALBEDOMAP_0
        #ifdef ALPHAMAP           

            vec4 alphaBlend;
            vec4 alphaBlend_0, alphaBlend_1, alphaBlend_2;
            int texChannelForAlphaBlending;

            alphaBlend_0 = texture2D( m_AlphaMap, texCoord.xy );

            #ifdef ALPHAMAP_1
                alphaBlend_1 = texture2D( m_AlphaMap_1, texCoord.xy );
            #endif
            #ifdef ALPHAMAP_2
                alphaBlend_2 = texture2D( m_AlphaMap_2, texCoord.xy );
            #endif

            vec2 texSlotCoords;   

            float finalAlphaBlendForLayer = 1.0;

            vec3 blending = abs( norm );
            blending = (blending -0.2) * 0.7;
            blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
            float b = (blending.x + blending.y + blending.z);
            blending /= vec3(b, b, b);

            #for i=0..12 (#ifdef ALBEDOMAP_$i $0 #endif)

                //assign texture slot's blending from index's correct alpha map
                if($i <= 3){
                    alphaBlend = alphaBlend_0;       
                }else if($i <= 7){
                    alphaBlend = alphaBlend_1;
                }else if($i <= 11){
                    alphaBlend = alphaBlend_2;
                }

                texChannelForAlphaBlending = int(mod($i, 4.0)); //pick the correct channel (r g b or a) based on the layer's index
                switch(texChannelForAlphaBlending) {
                    case 0:
                        finalAlphaBlendForLayer = alphaBlend.r;
                        break;
                    case 1:
                        finalAlphaBlendForLayer = alphaBlend.g;
                        break;
                    case 2:
                        finalAlphaBlendForLayer = alphaBlend.b;
                        break;
                    case 3:
                        finalAlphaBlendForLayer = alphaBlend.a;
                        break;            
                }

                afflictionMode = m_AfflictionMode_$i;            
                tempEmissiveColor = m_EmissiveColor_$i;

                #ifdef TRI_PLANAR_MAPPING   
            //tri planar
                    tempAlbedo = getTriPlanarBlendFromTexArray(wVertex, blending, m_AlbedoMap_$i, m_AlbedoMap_$i_scale, m_AlbedoTextureArray);

                    #ifdef NORMALMAP_$i
                        packedNormalParallaxVec.rgba = getTriPlanarBlendFromTexArray(wVertex, blending, m_NormalMap_$i, m_AlbedoMap_$i_scale, m_NormalParallaxTextureArray).rgba;
                        tempNormal.xyz = calculateTangentsAndApplyToNormals(packedNormalParallaxVec.xyz, wNormal);// this gets rid of the need for pre-generating tangents for TerrainPatches, since doing so doesn't seem to work (tbnMat is always blank for terrains even with tangents pre-generated, not sure why...)
                        tempParallax = packedNormalParallaxVec.w;

                        #ifdef PARALLAXHEIGHT_0    
                            //wip
                        #endif
                    #else
                        tempNormal.rgb = wNormal.rgb;
                    #endif
                    #ifdef METALLICROUGHNESSMAP_$i
                        packedMetallicRoughnessAoEiVec = getTriPlanarBlendFromTexArray(wVertex, blending, m_MetallicRoughnessMap_$i, m_AlbedoMap_$i_scale, m_MetallicRoughnessAoEiTextureArray).rgba;
                        tempRoughness = packedMetallicRoughnessAoEiVec.g * m_Roughness_$i;
                        tempMetallic = packedMetallicRoughnessAoEiVec.b * m_Metallic_$i;
                        tempAo = packedMetallicRoughnessAoEiVec.r;
                        tempEmissiveIntensity = packedMetallicRoughnessAoEiVec.a;        
                    #endif
                #else    

             // non triplanar
                    texSlotCoords = texCoord * m_AlbedoMap_$i_scale;

                    tempAlbedo =  texture2DArray(m_AlbedoTextureArray, vec3(texSlotCoords, m_AlbedoMap_$i));

                    #ifdef NORMALMAP_$i
                        packedNormalParallaxVec = texture2DArray(m_NormalParallaxTextureArray, vec3(texSlotCoords,  m_NormalMap_$i));
                        tempNormal.xyz = calculateTangentsAndApplyToNormals(packedNormalParallaxVec.xyz, wNormal);
                        tempParallax = packedNormalParallaxVec.w;

                        #ifdef PARALLAXHEIGHT_0  
                            //eventually add parallax code here if a PARALLAXHEIGHT_$i float is defined. but this shader is at the define limit currently, 
                           // so to do that will require removing defines around scale to use that for enabling parallax  per layer instead, since there's no reason for define around basic float scale anyways
                        #endif
                    #else
                        tempNormal.rgb = wNormal.rgb;
                    #endif

                    #ifdef METALLICROUGHNESSMAP_$i
                        packedMetallicRoughnessAoEiVec = texture2DArray(m_MetallicRoughnessAoEiTextureArray, vec3(texSlotCoords, m_MetallicRoughnessMap_$i));
                        tempRoughness = packedMetallicRoughnessAoEiVec.g * m_Roughness_$i;
                        tempMetallic = packedMetallicRoughnessAoEiVec.b * m_Metallic_$i;
                        tempAo = packedMetallicRoughnessAoEiVec.r;
                        tempEmissiveIntensity = packedMetallicRoughnessAoEiVec.a;        
                    #endif
                #endif        


                //blend to float values if no texture value for mrao map exists
                #if !defined(METALLICROUGHNESSMAP_$i)
                    tempRoughness =  m_Roughness_$i;
                    tempMetallic =  m_Metallic_$i;
                    tempAo = 1.0;
                #endif

              //note: most of these functions can be found in AfflictionLib.glslib
                tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode); //changes saturation of albedo for this layer; does nothing if not using AfflictionAlphaMap for affliction splatting        

                tempEmissiveColor *= tempEmissiveIntensity;

             //mix values from this index layer to final output values based on finalAlphaBlendForLayer 
                albedo.rgb = mix(albedo.rgb, tempAlbedo.rgb , finalAlphaBlendForLayer);        
                normal.rgb = mix(normal.rgb, tempNormal.rgb, finalAlphaBlendForLayer);        
                Metallic = mix(Metallic, tempMetallic, finalAlphaBlendForLayer);
                Roughness = mix(Roughness, tempRoughness, finalAlphaBlendForLayer);
                packedAoValue = mix(packedAoValue, tempAo, finalAlphaBlendForLayer);
                emissiveIntensity = mix(emissiveIntensity, tempEmissiveIntensity, finalAlphaBlendForLayer);
                emissive = mix(emissive, tempEmissiveColor, finalAlphaBlendForLayer);

            #endfor         
        #else
            albedo = texture2D(m_AlbedoMap_0, texCoord);
        #endif
    #endif

    float alpha = albedo.a;
    #ifdef DISCARD_ALPHA
        if(alpha < m_AlphaDiscardThreshold){
            discard;
        }
    #endif       

    //APPLY AFFLICTIONN TO THE PIXEL
    #ifdef AFFLICTIONTEXTURE
        vec4 afflictionAlbedo;    


        float newAfflictionScale = m_AfflictionSplatScale; 
        vec2 newScaledCoords;


        #ifdef AFFLICTIONALBEDOMAP
            #ifdef TRI_PLANAR_MAPPING
                newAfflictionScale = newAfflictionScale / 256;
                afflictionAlbedo = getTriPlanarBlend(wVertex, blending, m_SplatAlbedoMap , newAfflictionScale);
            #else
                newScaledCoords = mod(wPosition.xz / m_AfflictionSplatScale, 0.985);
                afflictionAlbedo = texture2D(m_SplatAlbedoMap , newScaledCoords);
            #endif

        #else
            afflictionAlbedo = vec4(1.0, 1.0, 1.0, 1.0);
        #endif

        vec3 afflictionNormal;
        #ifdef AFFLICTIONNORMALMAP
            #ifdef TRI_PLANAR_MAPPING

                afflictionNormal = getTriPlanarBlend(wVertex, blending, m_SplatNormalMap , newAfflictionScale).rgb;

            #else
                afflictionNormal = texture2D(m_SplatNormalMap , newScaledCoords).rgb;
            #endif

        #else
            afflictionNormal = norm; 

        #endif
        float afflictionMetallic = m_AfflictionMetallicValue;
        float afflictionRoughness = m_AfflictionRoughnessValue;
        float afflictionAo = 1.0;


        vec4 afflictionEmissive = m_AfflictionEmissiveColor;
        float afflictionEmissiveIntensity = m_AfflictionEmissiveValue;

        #ifdef AFFLICTIONROUGHNESSMETALLICMAP    
            vec4 metallicRoughnessAoEiVec;
            #ifdef TRI_PLANAR_MAPPING
                metallicRoughnessAoEiVec = texture2D(m_SplatRoughnessMetallicMap, newScaledCoords);
            #else
                metallicRoughnessAoEiVec = getTriPlanarBlend(wVertex, blending, m_SplatRoughnessMetallicMap, newAfflictionScale);
            #endif

            afflictionRoughness *= metallicRoughnessAoEiVec.g;
            afflictionMetallic *= metallicRoughnessAoEiVec.b;
            afflictionAo = metallicRoughnessAoEiVec.r;
            afflictionEmissiveIntensity *= metallicRoughnessAoEiVec.a; //important not to leave this channel all black by accident when creating the mraoei map if using affliction emissiveness    

        #endif

        #ifdef AFFLICTIONEMISSIVEMAP
            vec4 emissiveMapColor;
            #ifdef TRI_PLANAR_MAPPING
                emissiveMapColor = texture2D(m_SplatEmissiveMap, newScaledCoords);
            #else
                emissiveMapColor = getTriPlanarBlend(wVertex, blending, m_SplatEmissiveMap, newAfflictionScale);
            #endif
            afflictionEmissive *= emissiveMapColor;
        #endif

        float adjustedAfflictionValue = afflictionValue;
        #ifdef USE_SPLAT_NOISE
            noiseHash = getStaticNoiseVar0(wPosition, afflictionValue * m_SplatNoiseVar); //VERY IMPORTANT to replace this with a noiseMap texture, as calculating noise per pixel in-shader like this does lower framerate a lot

            adjustedAfflictionValue = getAdjustedAfflictionVar(afflictionValue);
            if(afflictionValue >= 0.99){
                adjustedAfflictionValue = afflictionValue;
            }
        #else
            noiseHash = 1.0;
        #endif           

        Roughness = alterAfflictionRoughness(adjustedAfflictionValue, Roughness, afflictionRoughness, noiseHash);
        Metallic = alterAfflictionMetallic(adjustedAfflictionValue, Metallic,  afflictionMetallic, noiseHash);
        albedo = alterAfflictionColor(adjustedAfflictionValue, albedo, afflictionAlbedo, noiseHash );
        normal = alterAfflictionNormalsForTerrain(adjustedAfflictionValue, normal, afflictionNormal, noiseHash , wNormal);
        emissive = alterAfflictionGlow(adjustedAfflictionValue, emissive, afflictionEmissive, noiseHash);
        emissiveIntensity = alterAfflictionEmissiveIntensity(adjustedAfflictionValue, emissiveIntensity, afflictionEmissiveIntensity, noiseHash);
        emissiveIntensity *= afflictionEmissive.a;
        //affliction ao value blended below after specular calculation
        
    #endif

    // spec gloss pipeline code would go here if supported, but likely will not be for terrain shaders as defines are limited and heavily used

    float specular = 0.5;
    float nonMetalSpec = 0.08 * specular;
    vec4 specularColor = (nonMetalSpec - nonMetalSpec * Metallic) + albedo * Metallic;
    vec4 diffuseColor = albedo - albedo * Metallic;
    vec3 fZero = vec3(specular);

    //gl_FragColor.rgb = vec3(0.0);
 
//simple ao calculation, no support for lightmaps like stock pbr shader.. (probably could add lightmap support with another texture array, but
//                                                                         that would add another texture read per slot and require removing 12 other defines to make room...)
    vec3 ao = vec3(packedAoValue);
    
    #ifdef AFFLICTIONTEXTURE
        ao = alterAfflictionAo(afflictionValue, ao, vec3(afflictionAo), noiseHash); // alter the AO map for affliction values
    #endif
    ao.rgb = ao.rrr;
    specularColor.rgb *= ao;
 
 
  
    #ifdef STATIC_SUN_INTENSITY
        indoorSunLightExposure = m_StaticSunIntensity; //single float value to indicate percentage of
                           //sunlight hitting the model (only works for small models or models with 100% consistent sunlighting accross every pixel)
    #endif
    #ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
        indoorSunLightExposure = vertColors.r * indoorSunLightExposure;      //use R channel of vertexColors for..       
    #endif 
                                                               // similar purpose as above...
                                                             //but uses r channel vert colors like an AO map specifically
                                                                 //for sunlight (solution for scaling lighting for indoor
                                                                  // and shadey/dimly lit models, especially big ones with)
    brightestPointLight = 0.0;

    // pack
    vec2 n1 = octEncode(normal);
    vec2 n2 = octEncode(norm);
    Context_OutGBuff3.xy = n1;
    Context_OutGBuff3.zw = n2;
    Context_OutGBuff0.rgb = floor(diffuseColor.rgb * 100.0f) + ao * 0.1f;
    Context_OutGBuff1.rgb = floor(specularColor.rgb * 100.0f) + fZero * 0.1f;
    Context_OutGBuff1.a = Roughness;
    Context_OutGBuff0.a = alpha;




    float minVertLighting;
    #ifdef BRIGHTEN_INDOOR_SHADOWS
        minVertLighting = 0.0833; //brighten shadows so that caves which are naturally covered from the DL shadows are not way too dark compared to when shadows are off (mostly only necessary for naturally dark scenes, or dark areas when using the sun intensity code above)
    #else
        minVertLighting = 0.0533;
    
    #endif
    
    indoorSunLightExposure = max(indoorSunLightExposure, brightestPointLight);   
    indoorSunLightExposure = max(indoorSunLightExposure, minVertLighting);       //scale the indoorSunLightExposure back up to account for the brightest point light nearby before scaling light probes by this value below   

    // shading model id
    Context_OutGBuff2.a = STANDARD_LIGHTING + indoorSunLightExposure * 0.01f;

    if(emissive.a > 0){    
        emissive = emissive * pow(emissive.a * 5, emissiveIntensity) * emissiveIntensity * 20 * emissive.a;    
    }
  //  emissive = emissive * pow(emissiveIntensity * 2.3, emissive.a);

    Context_OutGBuff2.rgb = emissive.rgb;
   
     // add fog after the lighting because shadows will cause the fog to darken
    // which just results in the geometry looking like it's changed color
    //#ifdef USE_FOG
    //    #ifdef FOG_LINEAR
    //        gl_FragColor = getFogLinear(gl_FragColor, m_FogColor, m_LinearFog.x, m_LinearFog.y, fogDistance);
    //    #endif
    //    #ifdef FOG_EXP
    //        gl_FragColor = getFogExp(gl_FragColor, m_FogColor, m_ExpFog, fogDistance);
    //    #endif
    //    #ifdef FOG_EXPSQ
    //        gl_FragColor = getFogExpSquare(gl_FragColor, m_FogColor, m_ExpSqFog, fogDistance);
    //    #endif
    //#endif
}
