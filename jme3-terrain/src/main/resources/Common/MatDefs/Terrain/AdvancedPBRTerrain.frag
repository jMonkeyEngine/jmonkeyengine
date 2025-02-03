#extension GL_EXT_texture_array : enable
#import "Common/ShaderLib/GLSLCompat.glsllib"

#define ENABLE_PBRLightingUtils_getWorldPosition 1
#define ENABLE_PBRLightingUtils_getLocalPosition 1
#define ENABLE_PBRLightingUtils_getWorldNormal 1
#define ENABLE_PBRLightingUtils_getTexCoord 1
#define ENABLE_PBRLightingUtils_computeDirectLightContribution 1
#define ENABLE_PBRLightingUtils_computeProbesContribution 1

#define ENABLE_PBRTerrainUtils_readPBRTerrainLayers 1

#import "Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib"
#import "Common/MatDefs/Terrain/Modular/PBRTerrainUtils.glsllib"
#ifdef AFFLICTIONTEXTURE
    #import "Common/MatDefs/Terrain/Modular/AfflictionLib.glsllib"
#endif

//declare PBR Lighting vars
uniform vec4 g_LightData[NB_LIGHTS];
uniform vec3 g_CameraPosition;

#ifdef DEBUG_VALUES_MODE
    uniform int m_DebugValuesMode;
#endif

#ifdef USE_FOG
    #import "Common/ShaderLib/MaterialFog.glsllib"
#endif

void main(){       
    vec3 wpos = PBRLightingUtils_getWorldPosition();
    vec3 worldViewDir = normalize(g_CameraPosition - wpos);
    
    // Create a blank PBRSurface.
    PBRSurface surface = PBRLightingUtils_createPBRSurface(worldViewDir);
    
    //pre-calculate necessary values for tri-planar blending
    TriPlanarUtils_calculateBlending(surface.geometryNormal);

    //reads terrain alphaMaps
    PBRTerrainUtils_readAlphaMaps();   
   
    //CUSTOM LIB EXAMPLE:
    #ifdef AFFLICTIONTEXTURE
        AfflictionLib_readAfflictionVector();
    #endif

    // read and blend up to 12 texture layers
    #for i=0..12 (#ifdef ALBEDOMAP_$i $0 #endif)
    
        PBRTerrainTextureLayer terrainTextureLayer_$i = PBRTerrainUtils_createAdvancedPBRTerrainLayer($i);
        
        #ifdef USE_FIRST_LAYER_AS_TRANSPARENCY
            if($i == 0){
                if(terrainTextureLayer_$i.blendValue > 0.01f){ 
                    discard;
                }     
            }    
        #endif 
        
        terrainTextureLayer_$i.emission = m_EmissiveColor_$i;
        
        #if defined(TRI_PLANAR_MAPPING) || defined(TRI_PLANAR_MAPPING_$i)
          //triplanar:
            
            PBRTerrainUtils_readTriPlanarAlbedoTexArray(ALBEDOMAP_$i, m_AlbedoMap_$i_scale, m_AlbedoTextureArray, terrainTextureLayer_$i);        
            #ifdef NORMALMAP_$i
                PBRTerrainUtils_readTriPlanarNormalTexArray(NORMALMAP_$i, m_AlbedoMap_$i_scale, m_NormalParallaxTextureArray, terrainTextureLayer_$i);
            #endif
            #ifdef METALLICROUGHNESSMAP_$i
                PBRTerrainUtils_readTriPlanarMetallicRoughnessAoEiTexArray(METALLICROUGHNESSMAP_$i, m_AlbedoMap_$i_scale, m_MetallicRoughnessAoEiTextureArray, terrainTextureLayer_$i);
            #endif
        #else 
          //non tri-planar:
        
            PBRTerrainUtils_readAlbedoTexArray(ALBEDOMAP_$i, m_AlbedoMap_$i_scale, m_AlbedoTextureArray, terrainTextureLayer_$i);        
            #ifdef NORMALMAP_$i
                PBRTerrainUtils_readNormalTexArray(NORMALMAP_$i, m_AlbedoMap_$i_scale, m_NormalParallaxTextureArray, terrainTextureLayer_$i);
            #endif
            #ifdef METALLICROUGHNESSMAP_$i
                PBRTerrainUtils_readMetallicRoughnessAoEiTexArray(METALLICROUGHNESSMAP_$i, m_AlbedoMap_$i_scale, m_MetallicRoughnessAoEiTextureArray, terrainTextureLayer_$i);
            #endif       
        #endif    
        
        //CUSTOM LIB EXAMPLE: uses a custom alpha map to desaturate albedo color for a color-removal effect
        #ifdef AFFLICTIONTEXTURE
            afflictionMode = m_AfflictionMode_$i;     
            terrainTextureLayer_$i.albedo.rgb = alterLiveliness(terrainTextureLayer_$i.albedo.rgb, livelinessValue, afflictionMode); //changes saturation of albedo for this layer; does nothing if not using AfflictionAlphaMap for affliction splatting     
        #endif

        //blends this layer
        PBRTerrainUtils_blendPBRTerrainLayer(surface, terrainTextureLayer_$i);
    #endfor                     
    
    #ifdef DISCARD_ALPHA
        if(surface.alpha < m_AlphaDiscardThreshold){
            discard;
        }
    #endif   

    PBRLightingUtils_readSunLightExposureParams(surface);

    //CUSTOM LIB EXAMPLE: uses a custom alpha map and noise to blend an extra splat layer overtop of all other layers
    #ifdef AFFLICTIONTEXTURE
        AfflictionLib_blendSplatLayers(surface);
    #endif

   //Calculate necessary variables in pbr surface prior to applying lighting. Ensure all texture/param reading and blending occurrs prior to this being called!
    PBRLightingUtils_calculatePreLightingValues(surface);
    
    // Calculate direct lights
    for(int i = 0;i < NB_LIGHTS; i+=3){
        vec4 lightData0 = g_LightData[i];
        vec4 lightData1 = g_LightData[i+1];
        vec4 lightData2 = g_LightData[i+2];    
        PBRLightingUtils_computeDirectLightContribution(
          lightData0, lightData1, lightData2, 
          surface
        );
    }    

    // Calculate env probes
    PBRLightingUtils_computeProbesContribution(surface);

    // Put it all together
    gl_FragColor.rgb = vec3(0.0);
    gl_FragColor.rgb += surface.bakedLightContribution;
    gl_FragColor.rgb += surface.directLightContribution;
    gl_FragColor.rgb += surface.envLightContribution;
    gl_FragColor.rgb += surface.emission;
    gl_FragColor.a = surface.alpha;      
  
    #ifdef USE_FOG
        gl_FragColor = MaterialFog_calculateFogColor(vec4(gl_FragColor));
    #endif
    
   //outputs the final value of the selected layer as a color for debug purposes. 
    #ifdef DEBUG_VALUES_MODE
        gl_FragColor = PBRLightingUtils_getColorOutputForDebugMode(m_DebugValuesMode, vec4(gl_FragColor.rgba), surface);
    #endif
}
