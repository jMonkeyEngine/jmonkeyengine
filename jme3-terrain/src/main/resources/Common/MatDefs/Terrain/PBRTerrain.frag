#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

#import "Common/MatDefs/Terrain/AfflictionLib.glsllib"

#ifdef DEBUG_VALUES_MODE
  uniform int m_DebugValuesMode;
#endif


uniform vec4 g_LightData[NB_LIGHTS];
uniform vec4 g_AmbientLightColor;

varying vec3 wPosition;

#if NB_PROBES >= 1
    uniform samplerCube g_PrefEnvMap;
    uniform vec3 g_ShCoeffs[9];
    uniform mat4 g_LightProbeData;
#endif
#if NB_PROBES >= 2
    uniform samplerCube g_PrefEnvMap2;
    uniform vec3 g_ShCoeffs2[9];
    uniform mat4 g_LightProbeData2;
#endif
#if NB_PROBES == 3
    uniform samplerCube g_PrefEnvMap3;
    uniform vec3 g_ShCoeffs3[9];
    uniform mat4 g_LightProbeData3;
#endif


varying vec3 vNormal;
varying vec2 texCoord;

uniform vec3 g_CameraPosition;


vec3 norm;



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


varying vec3 vPosition;
varying vec3 vnPosition;
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec4 vnLightDir;
varying vec3 lightVec;
varying vec3 inNormal;

uniform int m_AfflictionMode_0;
uniform int m_AfflictionMode_1;
uniform int m_AfflictionMode_2;
uniform int m_AfflictionMode_3;
uniform int m_AfflictionMode_4;
uniform int m_AfflictionMode_5;
uniform int m_AfflictionMode_6;
uniform int m_AfflictionMode_7;
uniform int m_AfflictionMode_8;
uniform int m_AfflictionMode_9;
uniform int m_AfflictionMode_10;
uniform int m_AfflictionMode_11;

uniform float m_Roughness_0;
uniform float m_Roughness_1;
uniform float m_Roughness_2;
uniform float m_Roughness_3;
uniform float m_Roughness_4;
uniform float m_Roughness_5;
uniform float m_Roughness_6;
uniform float m_Roughness_7;
uniform float m_Roughness_8;
uniform float m_Roughness_9;
uniform float m_Roughness_10;
uniform float m_Roughness_11;

uniform float m_Metallic_0;
uniform float m_Metallic_1;
uniform float m_Metallic_2;
uniform float m_Metallic_3;
uniform float m_Metallic_4;
uniform float m_Metallic_5;
uniform float m_Metallic_6;
uniform float m_Metallic_7;
uniform float m_Metallic_8;
uniform float m_Metallic_9;
uniform float m_Metallic_10;
uniform float m_Metallic_11;


#ifdef AFFLICTIONTEXTURE
    uniform sampler2D m_AfflictionAlphaMap;
#endif

#ifdef USE_SPLAT_NOISE
     uniform float m_SplatNoiseVar;
#endif

//defined for sub terrains that arent equal to each map tile size when AfflictionAlphaMap is defined 
#ifdef TILELOCATION
    uniform float m_TileWidth;
    uniform vec3 m_TileLocation;
#endif

uniform int m_AfflictionSplatScale;
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

uniform float m_AfflictionRoughnessValue;
uniform float m_AfflictionMetallicValue;
uniform float m_AfflictionEmissiveValue;
uniform vec4 m_AfflictionEmissiveColor;


#ifdef ALBEDOMAP_0
  uniform sampler2D m_AlbedoMap_0;
#endif
#ifdef ALBEDOMAP_1
  uniform sampler2D m_AlbedoMap_1;
#endif
#ifdef ALBEDOMAP_2
  uniform sampler2D m_AlbedoMap_2;
#endif
#ifdef ALBEDOMAP_3
  uniform sampler2D m_AlbedoMap_3;
#endif
#ifdef ALBEDOMAP_4
  uniform sampler2D m_AlbedoMap_4;
#endif
#ifdef ALBEDOMAP_5
  uniform sampler2D m_AlbedoMap_5;
#endif
#ifdef ALBEDOMAP_6
  uniform sampler2D m_AlbedoMap_6;
#endif
#ifdef ALBEDOMAP_7
  uniform sampler2D m_AlbedoMap_7;
#endif
#ifdef ALBEDOMAP_8
  uniform sampler2D m_AlbedoMap_8;
#endif
#ifdef ALBEDOMAP_9
  uniform sampler2D m_AlbedoMap_9;
#endif
#ifdef ALBEDOMAP_10
  uniform sampler2D m_AlbedoMap_10;
#endif
#ifdef ALBEDOMAP_11
  uniform sampler2D m_AlbedoMap_11;
#endif


#ifdef ALBEDOMAP_0_SCALE
  uniform float m_AlbedoMap_0_scale;
#endif
#ifdef ALBEDOMAP_1_SCALE
  uniform float m_AlbedoMap_1_scale;
#endif
#ifdef ALBEDOMAP_2_SCALE
  uniform float m_AlbedoMap_2_scale;
#endif
#ifdef ALBEDOMAP_3_SCALE
  uniform float m_AlbedoMap_3_scale;
#endif
#ifdef ALBEDOMAP_4_SCALE
  uniform float m_AlbedoMap_4_scale;
#endif
#ifdef ALBEDOMAP_5_SCALE
  uniform float m_AlbedoMap_5_scale;
#endif
#ifdef ALBEDOMAP_6_SCALE
  uniform float m_AlbedoMap_6_scale;
#endif
#ifdef ALBEDOMAP_7_SCALE
  uniform float m_AlbedoMap_7_scale;
#endif
#ifdef ALBEDOMAP_8_SCALE
  uniform float m_AlbedoMap_8_scale;
#endif
#ifdef ALBEDOMAP_9_SCALE
  uniform float m_AlbedoMap_9_scale;
#endif
#ifdef ALBEDOMAP_10_SCALE
  uniform float m_AlbedoMap_10_scale;
#endif
#ifdef ALBEDOMAP_11_SCALE
  uniform float m_AlbedoMap_11_scale;
#endif


#ifdef ALPHAMAP
  uniform sampler2D m_AlphaMap;
#endif
#ifdef ALPHAMAP_1
  uniform sampler2D m_AlphaMap_1;
#endif
#ifdef ALPHAMAP_2
  uniform sampler2D m_AlphaMap_2;
#endif

#ifdef NORMALMAP_0
  uniform sampler2D m_NormalMap_0;
#endif
#ifdef NORMALMAP_1
  uniform sampler2D m_NormalMap_1;
#endif
#ifdef NORMALMAP_2
  uniform sampler2D m_NormalMap_2;
#endif
#ifdef NORMALMAP_3
  uniform sampler2D m_NormalMap_3;
#endif
#ifdef NORMALMAP_4
  uniform sampler2D m_NormalMap_4;
#endif
#ifdef NORMALMAP_5
  uniform sampler2D m_NormalMap_5;
#endif
#ifdef NORMALMAP_6
  uniform sampler2D m_NormalMap_6;
#endif
#ifdef NORMALMAP_7
  uniform sampler2D m_NormalMap_7;
#endif
#ifdef NORMALMAP_8
  uniform sampler2D m_NormalMap_8;
#endif
#ifdef NORMALMAP_9
  uniform sampler2D m_NormalMap_9;
#endif
#ifdef NORMALMAP_10
  uniform sampler2D m_NormalMap_10;
#endif
#ifdef NORMALMAP_11
  uniform sampler2D m_NormalMap_11;
#endif

#ifdef DISCARD_ALPHA
    uniform float m_AlphaDiscardThreshold;
#endif




vec4 afflictionVector;

varying vec3 wNormal;

#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
 
#endif


vec3 viewDir;

vec2 coord;
vec4 albedo;
vec3 normal = vec3(0.5,0.5,1);
vec3 newNormal;
float Metallic;
float Roughness;
float packedAoValue = 1.0;
vec4 emissive;
float emissiveIntensity = 1.0;

vec4 packedMetallicRoughnessAoEiVec;
vec4 packedNormalParallaxVec;

vec4 tempAlbedo, tempNormal, tempEmissiveColor;
float tempParallax, tempMetallic, tempRoughness, tempAo, tempEmissiveIntensity;

float noiseHash;
float livelinessValue;
float afflictionValue;
int afflictionMode = 1;
   


#define DEFINE_COORD(index) vec2 coord##index = texCoord * m_AlbedoMap##index##_scale;

#define BLEND(index, ab)\
    DEFINE_COORD(index)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo.rgb = texture2D(m_AlbedoMap##index, coord##index).rgb;\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo.rgb = mix( albedo.rgb, tempAlbedo.rgb ,ab );\
    normal.rgb = mix(normal.xyz, wNormal.rgb, ab);\
    Metallic = mix(Metallic, m_Metallic##index, ab);\
    Roughness = mix(Roughness, m_Roughness##index, ab);

    
    

#define BLEND_NORMAL(index, ab)\
    DEFINE_COORD(index)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo.rgb = texture2D(m_AlbedoMap##index, coord##index).rgb;\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo.rgb = mix( albedo.rgb, tempAlbedo.rgb ,ab );\
    Metallic = mix(Metallic, m_Metallic##index, ab);\
    Roughness = mix(Roughness, m_Roughness##index, ab);\
    tempNormal.xyz = texture2D(m_NormalMap##index, coord##index).xyz;\
    tempNormal.rgb = calculateTangentsAndApplyToNormals(tempNormal.rgb, wNormal);\
    normal.rgb = mix(normal.xyz, tempNormal.xyz, ab);

//BLEND METHODS FOR TRIPLANAR...  
#define TRI_BLEND(index, ab, worldCoords, blending)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo = getTriPlanarBlend(worldCoords, blending, m_AlbedoMap##index, m_AlbedoMap##index##_scale);\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo = mix( albedo, tempAlbedo ,ab );\
    normal.rgb = mix(normal.rgb, wNormal.rgb, ab);\
    Metallic = mix(Metallic, m_Metallic##index, ab);\
    Roughness = mix(Roughness, m_Roughness##index, ab);    


#define TRI_BLEND_NORMAL(index, ab, worldCoords, blending)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo.rgb = getTriPlanarBlend(worldCoords, blending, m_AlbedoMap##index, m_AlbedoMap##index##_scale).rgb;\
    tempNormal.rgb = getTriPlanarBlend(worldCoords, blending, m_NormalMap##index, m_AlbedoMap##index##_scale).rgb;\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo.rgb = mix( albedo.rgb, tempAlbedo.rgb ,ab );\
    tempNormal.rgb = calculateTangentsAndApplyToNormals(tempNormal.rgb, wNormal);\
    normal.rgb = mix(normal.xyz, tempNormal.xyz, ab);\
    Metallic = mix(Metallic, m_Metallic##index, ab);\
    Roughness = mix(Roughness, m_Roughness##index, ab);

#ifdef ALPHAMAP

vec4 calculateAlbedoBlend(in vec2 texCoord) {
    vec4 alphaBlend = texture2D( m_AlphaMap, texCoord.xy );
    vec4 albedo = vec4(1.0);
    
    

    Roughness = m_Roughness_0;
    Metallic = m_Metallic_0 ;

 vec3 blending = abs( wNormal );
        blending = (blending -0.2) * 0.7;
        blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
        float b = (blending.x + blending.y + blending.z);
        blending /= vec3(b, b, b);


    #ifdef ALPHAMAP_1
      vec4 alphaBlend1   = texture2D( m_AlphaMap_1, texCoord.xy );
    #endif
    #ifdef ALPHAMAP_2
      vec4 alphaBlend2   = texture2D( m_AlphaMap_2, texCoord.xy );
    #endif
    #ifdef ALBEDOMAP_0   
                    //NOTE! the old (phong) terrain shaders do not have an "_0" for the first diffuse map, it is just "DiffuseMap"
        #ifdef NORMALMAP_0
            BLEND_NORMAL(_0,  alphaBlend.r)
        #else
            BLEND(_0,  alphaBlend.r)
        #endif
        
    #endif
    #ifdef ALBEDOMAP_1
        #ifdef NORMALMAP_1
            BLEND_NORMAL(_1,  alphaBlend.g)
        #else
            BLEND(_1,  alphaBlend.g)
        #endif
        
    #endif
    #ifdef ALBEDOMAP_2
        #ifdef NORMALMAP_2
            BLEND_NORMAL(_2,  alphaBlend.b)
        #else
            BLEND(_2,  alphaBlend.b)
        #endif
        
    #endif
    #ifdef ALBEDOMAP_3 
        #ifdef NORMALMAP_3
            BLEND_NORMAL(_3,  alphaBlend.a)
        #else
            BLEND(_3,  alphaBlend.a)
        #endif
       
    #endif

    #ifdef ALPHAMAP_1
        #ifdef ALBEDOMAP_4
            #ifdef NORMALMAP_4
                BLEND_NORMAL(_4,  alphaBlend1.r)
            #else
                BLEND(_4,  alphaBlend1.r)
            #endif
           
        #endif
        #ifdef ALBEDOMAP_5
            #ifdef NORMALMAP_5
                BLEND_NORMAL(_5,  alphaBlend1.g)
            #else
                BLEND(_5,  alphaBlend1.g)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_6
            #ifdef NORMALMAP_6
                BLEND_NORMAL(_6,  alphaBlend1.b)
            #else
                BLEND(_6,  alphaBlend1.b)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_7
            #ifdef NORMALMAP_7
                BLEND_NORMAL(_7,  alphaBlend1.a)
            #else
                BLEND(_7,  alphaBlend1.a)
            #endif
             
        #endif
    #endif

    #ifdef ALPHAMAP_2
        #ifdef ALBEDOMAP_8
             #ifdef NORMALMAP_8
                BLEND_NORMAL(_8,  alphaBlend2.r)
            #else
                BLEND(_8,  alphaBlend2.r)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_9
             #ifdef NORMALMAP_9
                BLEND_NORMAL(_9,  alphaBlend2.g)
            #else
                BLEND(_9,  alphaBlend2.g)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_10
            #ifdef NORMALMAP_10
                BLEND_NORMAL(_10,  alphaBlend2.b)
            #else
                BLEND(_10,  alphaBlend2.b)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_11
             #ifdef NORMALMAP_11
                BLEND_NORMAL(_11,  alphaBlend2.a)
            #else
                BLEND(_11,  alphaBlend2.a)
            #endif
             
        #endif                   
    #endif

    return albedo;
  }



// TRI PLANAR ALPHA MAP TEXTURES_ _ _ _    \/

  #ifdef TRI_PLANAR_MAPPING


    vec4 calculateTriPlanarAlbedoBlend(in vec3 wNorm, in vec4 wVert, in vec2 texCoord, vec3 blending) {
         vec4 alphaBlend = texture2D( m_AlphaMap, texCoord.xy );
        vec4 albedo = vec4(1.0);



        Roughness = m_Roughness_0;
        Metallic = m_Metallic_0 ;

    


        #ifdef ALPHAMAP_1
          vec4 alphaBlend1   = texture2D( m_AlphaMap_1, texCoord.xy );
        #endif
        #ifdef ALPHAMAP_2
          vec4 alphaBlend2   = texture2D( m_AlphaMap_2, texCoord.xy );
        #endif
        #ifdef ALBEDOMAP_0   
                        //NOTE! the old (phong) terrain shaders do not have an "_0" for the first diffuse map, it is just "DiffuseMap"
            #ifdef NORMALMAP_0
                TRI_BLEND_NORMAL(_0,  alphaBlend.r, wVertex, blending)
            #else
                TRI_BLEND(_0,  alphaBlend.r, wVertex, blending)
            #endif

        #endif
        #ifdef ALBEDOMAP_1
            #ifdef NORMALMAP_1
                TRI_BLEND_NORMAL(_1,  alphaBlend.g, wVertex, blending)
            #else
                TRI_BLEND(_1,  alphaBlend.g, wVertex, blending)
            #endif

        #endif
        #ifdef ALBEDOMAP_2
            #ifdef NORMALMAP_2
                TRI_BLEND_NORMAL(_2,  alphaBlend.b, wVertex, blending)
            #else
                TRI_BLEND(_2,  alphaBlend.b, wVertex, blending)
            #endif

        #endif
        #ifdef ALBEDOMAP_3 
            #ifdef NORMALMAP_3
                TRI_BLEND_NORMAL(_3,  alphaBlend.a, wVertex, blending)
            #else
                TRI_BLEND(_3,  alphaBlend.a, wVertex, blending)
            #endif

        #endif

        #ifdef ALPHAMAP_1
            #ifdef ALBEDOMAP_4
                #ifdef NORMALMAP_4
                    TRI_BLEND_NORMAL(_4,  alphaBlend1.r, wVertex, blending)
                #else
                    TRI_BLEND(_4,  alphaBlend1.r, wVertex, blending)
                #endif

            #endif
            #ifdef ALBEDOMAP_5
                #ifdef NORMALMAP_5
                    TRI_BLEND_NORMAL(_5,  alphaBlend1.g, wVertex, blending)
                #else
                    TRI_BLEND(_5,  alphaBlend1.g, wVertex, blending)
                #endif

            #endif
            #ifdef ALBEDOMAP_6
                #ifdef NORMALMAP_6
                    TRI_BLEND_NORMAL(_6,  alphaBlend1.b, wVertex, blending)
                #else
                    TRI_BLEND(_6,  alphaBlend1.b, wVertex, blending)
                #endif

            #endif
            #ifdef ALBEDOMAP_7
                #ifdef NORMALMAP_7
                    TRI_BLEND_NORMAL(_7,  alphaBlend1.a, wVertex, blending)
                #else
                    TRI_BLEND(_7,  alphaBlend1.a, wVertex, blending)
                #endif

            #endif
        #endif

        #ifdef ALPHAMAP_2
            #ifdef ALBEDOMAP_8
                 #ifdef NORMALMAP_8
                    TRI_BLEND_NORMAL(_8,  alphaBlend2.r, wVertex, blending)
                #else
                    TRI_BLEND(_8,  alphaBlend2.r, wVertex, blending)
                #endif

            #endif
            #ifdef ALBEDOMAP_9
                 #ifdef NORMALMAP_9
                    TRI_BLEND_NORMAL(_9,  alphaBlend2.g, wVertex, blending)
                #else
                    TRI_BLEND(_9,  alphaBlend2.g, wVertex, blending)
                #endif

            #endif
            #ifdef ALBEDOMAP_10
                #ifdef NORMALMAP_10
                    TRI_BLEND_NORMAL(_10,  alphaBlend2.b, wVertex, blending)
                #else
                    TRI_BLEND(_10,  alphaBlend2.b, wVertex, blending)
                #endif

            #endif
            #ifdef ALBEDOMAP_11
                 #ifdef NORMALMAP_11
                    TRI_BLEND_NORMAL(_11,  alphaBlend2.a, wVertex, blending)
                #else
                    TRI_BLEND(_11,  alphaBlend2.a, wVertex, blending)
                #endif

            #endif                   
        #endif
        


        return albedo;
    }

    
  #endif

#endif



//sun intensity is a secondary AO value that can be painted per-vertex in the red channel of the 
// vertex colors, or it can be set as a static value for an entire material with StaticSunIntensity float param 
#if defined(USE_VERTEX_COLORS_AS_SUN_INTENSITY) 
    varying vec4 vertColors; 
#endif

#ifdef STATIC_SUN_INTENSITY
    uniform float m_StaticSunIntensity;
#endif
//sun intensity AO value is only applied to the directional light, not to point lights, so it is important to track if the 
//sun is more/less bright than the brightest point light to determine how the light probe's ambient light should be scaled later on..
float brightestPointLight = 0.0;



void main(){
    
    
    #ifdef USE_FOG
        fogDistance = distance(g_CameraPosition, wPosition.xyz);
    #endif
    
    float indoorSunLightExposure = 1.0;
    
    viewDir = normalize(g_CameraPosition - wPosition);

    norm  = normalize(wNormal);
    normal = norm;


    afflictionVector = vec4(1.0, 0.0, 1.0, 0.0); //r channel is saturation, g channel is affliction splat texture intensity, b and a unused (might use b channel for wetness eventually)
    
    #ifdef AFFLICTIONTEXTURE
    
        #ifdef TILELOCATION 
        //subterrains that are not centred in tile or equal to tile width in total size need to have m_TileWidth pre-set. (tileWidth is the x,z dimensions that the AfflictionAlphaMap represents)..
            vec2 tileCoords;
            float xPos, zPos;

            vec3 locInTile = (wPosition - m_TileLocation);

             locInTile += vec3(m_TileWidth/2, 0, m_TileWidth/2);

             xPos = (locInTile.x / m_TileWidth);
             zPos = 1 - (locInTile.z / m_TileWidth);

            tileCoords = vec2(xPos, zPos);

            afflictionVector = texture2D(m_AfflictionAlphaMap, tileCoords).rgba;
        
        
     
        #else
           // .. otherwise when terrain size matches tileWidth, the terrain's texCoords can be used for simple texel fetching of the AfflictionAlphaMap
            afflictionVector = texture2D(m_AfflictionAlphaMap, texCoord.xy).rgba;
        #endif
    #endif

    livelinessValue = afflictionVector.r;
    afflictionValue = afflictionVector.g;




    vec3 blending;
    #ifdef ALBEDOMAP_0
      #ifdef ALPHAMAP
        #ifdef TRI_PLANAR_MAPPING
             blending = abs( norm );
                blending = (blending -0.2) * 0.7;
                blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
                float b = (blending.x + blending.y + blending.z);
                blending /= vec3(b, b, b);


            albedo = calculateTriPlanarAlbedoBlend(norm, wVertex, texCoord, blending);
            
        #else
            albedo = calculateAlbedoBlend(texCoord);
        #endif
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
            vec4 metallicRoughnessAoEiVec = texture2D(m_SplatRoughnessMetallicMap, newScaledCoords);
            afflictionRoughness *= metallicRoughnessAoEiVec.g;
            afflictionMetallic *= metallicRoughnessAoEiVec.b;
            afflictionAo = metallicRoughnessAoEiVec.r;
            afflictionEmissiveIntensity *= metallicRoughnessAoEiVec.a; //important not to leave this channel all black by accident when creating the mraoei map if using affliction emissiveness    

        #endif

        #ifdef AFFLICTIONEMISSIVEMAP
            vec4 emissiveMapColor = texture2D(m_SplatEmissiveMap, newScaledCoords);
            afflictionEmissive *= emissiveMapColor;
        #endif

        float adjustedAfflictionValue = afflictionValue;
            #ifdef USE_SPLAT_NOISE
                noiseHash = getStaticNoiseVar0(wPosition, afflictionValue * m_SplatNoiseVar);

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


gl_FragColor.rgb = vec3(0.0);


 
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
                           //sunlight hitting the model (only works for small models or models with 100% consistent sunlight across every pixel)
    #endif
    #ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
        indoorSunLightExposure = vertColors.r * indoorSunLightExposure;      //use R channel of vertexColors for..       
    #endif 
                                                               // similar purpose as above...
                                                             //but uses r channel vert colors like an AO map specifically
                                                                 //for sunlight (solution for scaling lighting for indoor
                                                                  // and shady/dimly lit models, especially big ones with)
    brightestPointLight = 0.0;
    
     
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
        
        float hdotv = PBR_ComputeDirectLight(normal, lightDir.xyz, viewDir,
                            lightColor.rgb, fZero, Roughness, ndotv,
                            directDiffuse,  directSpecular);

        vec3 directLighting = diffuseColor.rgb *directDiffuse + directSpecular;
            
        #if defined(USE_VERTEX_COLORS_AS_SUN_INTENSITY) || defined(STATIC_SUN_INTENSITY)
           if(fallOff == 1.0){
                directLighting.rgb *= indoorSunLightExposure;// ... *^. to scale down how intense just the sun is (ambient and direct light are 1.0 fallOff)
                
            }
            else{
                    brightestPointLight = max(fallOff, brightestPointLight);
          
           }
       #endif
        
        
        
        gl_FragColor.rgb += directLighting * fallOff;
        
     
    }
    
    
    float minVertLighting;
    #ifdef BRIGHTEN_INDOOR_SHADOWS
        minVertLighting = 0.0833; //brighten shadows so that caves which are naturally covered from the DL shadows are not way too dark compared to when shadows are off (mostly only necessary for naturally dark scenes, or dark areas when using the sun intensity code above)
    #else
        minVertLighting = 0.0533;
    
    #endif
    
    indoorSunLightExposure = max(indoorSunLightExposure, brightestPointLight);   
    indoorSunLightExposure = max(indoorSunLightExposure, minVertLighting);       //scale the indoorSunLightExposure back up to account for the brightest point light nearby before scaling light probes by this value below   
    


    #if NB_PROBES >= 1
        vec3 color1 = vec3(0.0);
        vec3 color2 = vec3(0.0);
        vec3 color3 = vec3(0.0);
        float weight1 = 1.0;
        float weight2 = 0.0;
        float weight3 = 0.0;

        float ndf = renderProbe(viewDir, wPosition, normal, norm, Roughness, diffuseColor, specularColor, ndotv, ao, g_LightProbeData, g_ShCoeffs, g_PrefEnvMap, color1);
        #if NB_PROBES >= 2
            float ndf2 = renderProbe(viewDir, wPosition, normal, norm, Roughness, diffuseColor, specularColor, ndotv, ao, g_LightProbeData2, g_ShCoeffs2, g_PrefEnvMap2, color2);
        #endif
        #if NB_PROBES == 3
            float ndf3 = renderProbe(viewDir, wPosition, normal, norm, Roughness, diffuseColor, specularColor, ndotv, ao, g_LightProbeData3, g_ShCoeffs3, g_PrefEnvMap3, color3);
        #endif

        #if NB_PROBES >= 2
            float invNdf =  max(1.0 - ndf,0.0);
            float invNdf2 =  max(1.0 - ndf2,0.0);
            float sumNdf = ndf + ndf2;
            float sumInvNdf = invNdf + invNdf2;
            #if NB_PROBES == 3
                float invNdf3 = max(1.0 - ndf3,0.0);
                sumNdf += ndf3;
                sumInvNdf += invNdf3;
                weight3 =  ((1.0 - (ndf3 / sumNdf)) / (NB_PROBES - 1)) *  (invNdf3 / sumInvNdf);
            #endif

            weight1 = ((1.0 - (ndf / sumNdf)) / (NB_PROBES - 1)) *  (invNdf / sumInvNdf);
            weight2 = ((1.0 - (ndf2 / sumNdf)) / (NB_PROBES - 1)) *  (invNdf2 / sumInvNdf);

            float weightSum = weight1 + weight2 + weight3;

            weight1 /= weightSum;
            weight2 /= weightSum;
            weight3 /= weightSum;
        #endif

        #ifdef USE_AMBIENT_LIGHT
            color1.rgb *= g_AmbientLightColor.rgb;
            color2.rgb *= g_AmbientLightColor.rgb;
            color3.rgb *= g_AmbientLightColor.rgb;
        #endif


// multiply probes by the indoorSunLightExposure, as determined by pixel's  sunlightExposure and adjusted for 
// nearby point/spot lights ( will be multiplied by 1.0 and left unchanged if you are not defining any of the sunlight exposure variables for dimming indoors areas)
        color1.rgb *= indoorSunLightExposure;
        color2.rgb *= indoorSunLightExposure;
        color3.rgb *= indoorSunLightExposure;
        
        
        gl_FragColor.rgb += color1 * clamp(weight1,0.0,1.0) + color2 * clamp(weight2,0.0,1.0) + color3 * clamp(weight3,0.0,1.0);

    #endif



    if(emissive.a > 0){
    
        emissive = emissive * pow(emissive.a * 5, emissiveIntensity) * emissiveIntensity * 20 * emissive.a;
    
    }
    
  //  emissive = emissive * pow(emissiveIntensity * 2.3, emissive.a);

    gl_FragColor += emissive;


   
     // add fog after the lighting because shadows will cause the fog to darken
    // which just results in the geometry looking like it's changed color
    #ifdef USE_FOG
        #ifdef FOG_LINEAR
            gl_FragColor = getFogLinear(gl_FragColor, m_FogColor, m_LinearFog.x, m_LinearFog.y, fogDistance);
        #endif
        #ifdef FOG_EXP
            gl_FragColor = getFogExp(gl_FragColor, m_FogColor, m_ExpFog, fogDistance);
        #endif
        #ifdef FOG_EXPSQ
            gl_FragColor = getFogExpSquare(gl_FragColor, m_FogColor, m_ExpSqFog, fogDistance);
        #endif
    #endif 
    
    //outputs the final value of the selected layer as a color for debug purposes. 
    #ifdef DEBUG_VALUES_MODE
        if(m_DebugValuesMode == 0){
                gl_FragColor.rgb = vec3(albedo);

        }
        else if(m_DebugValuesMode == 1){
                gl_FragColor.rgb = vec3(normal);

        }
        else if(m_DebugValuesMode == 2){
                gl_FragColor.rgb = vec3(Roughness);

        }
        else if(m_DebugValuesMode == 3){
                gl_FragColor.rgb = vec3(Metallic);

        }
        else if(m_DebugValuesMode == 4){
                gl_FragColor.rgb = ao.rgb;

        }
        else if(m_DebugValuesMode == 5){
                gl_FragColor.rgb = vec3(emissive.rgb);  
        }
        
    #endif
    
    gl_FragColor.a = albedo.a;

}
