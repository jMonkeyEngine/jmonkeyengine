#extension GL_EXT_texture_array : enable
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "MatDefs/ShaderLib/AfflictionLib.glsllib"
#import "MatDefs/ShaderLib/OcclusionParallax.glsllib"


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

#ifdef LIGHTMAP
  uniform sampler2D m_LightMap;
#endif

varying vec3 vNormal;




varying vec2 texCoord;

vec2 newTexCoord;


uniform vec3 g_CameraPosition;



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
uniform float m_ExpSqFog;
#endif




varying vec3 vPosition;
varying vec3 vnPosition;
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec4 vnLightDir;
varying vec3 lightVec;
varying vec3 inNormal;



vec3 norm;




#ifdef AFFLICTIONTEXTURE
    uniform sampler2D m_AfflictionTexture;
#endif

//defined for sub terrains that arent equal to each map tile size
#ifdef TILELOCATION
    uniform float m_TileWidth;
    uniform vec3 m_TileLocation;
#endif

uniform int m_PlaguedMapScale;
#ifdef AFFLICTIONALBEDOMAP
    uniform sampler2D m_PlaguedAlbedoMap ;
#endif

#ifdef AFFLICTIONNORMALMAP
    uniform sampler2D m_PlaguedNormalMap ;
#endif

#ifdef AFFLICTIONROUGHNESSMETALLICMAP
    uniform sampler2D m_PlaguedRoughnessMetallicMap;
#endif

#ifdef AFFLICTIONEMISSIVEMAP
    uniform sampler2D m_PlaguedEmissiveMap;
#endif

uniform float m_AfflictionRoughnessValue;
uniform float m_AfflictionMetallicValue;
uniform float m_AfflictionEmissiveValue;
uniform vec4 m_AfflictionEmissiveColor;



uniform sampler2DArray m_AlbedoTextureArray;
uniform sampler2DArray m_NormalParallaxTextureArray;
uniform sampler2DArray m_MetallicRoughnessAoEiTextureArray;


// mat3 tbnMat;


#ifdef ALPHAMAP
  uniform sampler2D m_AlphaMap;
#endif
#ifdef ALPHAMAP_1
  uniform sampler2D m_AlphaMap_1;
#endif
#ifdef ALPHAMAP_2
  uniform sampler2D m_AlphaMap_2;
#endif




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

#ifdef PARALLAXHEIGHT_0
  uniform float m_ParallaxHeight_0;
#endif
#ifdef PARALLAXHEIGHT_1
  uniform float m_ParallaxHeight_1;
#endif
#ifdef PARALLAXHEIGHT_2
  uniform float m_ParallaxHeight_2;
#endif
#ifdef PARALLAXHEIGHT_3
  uniform float m_ParallaxHeight_3;
#endif
#ifdef PARALLAXHEIGHT_4
  uniform float m_ParallaxHeight_4;
#endif
#ifdef PARALLAXHEIGHT_5
  uniform float m_ParallaxHeight_5;
#endif
#ifdef PARALLAXHEIGHT_6
  uniform float m_ParallaxHeight_6;
#endif
#ifdef PARALLAXHEIGHT_7
  uniform float m_ParallaxHeight_7;
#endif
#ifdef PARALLAXHEIGHT_8
  uniform float m_ParallaxHeight_8;
#endif
#ifdef PARALLAXHEIGHT_9
  uniform float m_ParallaxHeight_9;
#endif
#ifdef PARALLAXHEIGHT_10
  uniform float m_ParallaxHeight_10;
#endif
#ifdef PARALLAXHEIGHT_11
  uniform float m_ParallaxHeight_11;
#endif

//#ifdef EMISSIVECOLOR_0
    uniform vec4 EmissiveColor_0;
//#endif
//#ifdef EMISSIVECOLOR_1
    uniform vec4 EmissiveColor_1;
//#endif
//#ifdef EMISSIVECOLOR_2
    uniform vec4 EmissiveColor_2;
//#endif
//#ifdef EMISSIVECOLOR_3
    uniform vec4 EmissiveColor_3;
//#endif
//#ifdef EMISSIVECOLOR_4
    uniform vec4 EmissiveColor_4;
//#endif
//#ifdef EMISSIVECOLOR_5
    uniform vec4 EmissiveColor_5;
//#endif
//#ifdef EMISSIVECOLOR_6
    uniform vec4 EmissiveColor_6;
//#endif
//#ifdef EMISSIVECOLOR_7
    uniform vec4 EmissiveColor_7;
//#endif
//#ifdef EMISSIVECOLOR_8
    uniform vec4 EmissiveColor_8;
//#endif
//#ifdef EMISSIVECOLOR_9
    uniform vec4 EmissiveColor_9;
//#endif
//#ifdef EMISSIVECOLOR_10
    uniform vec4 EmissiveColor_10;
//#endif
//#ifdef EMISSIVECOLOR_11
    uniform vec4 EmissiveColor_11;
//#endif



#ifdef ALBEDOMAP_0
  uniform int m_AlbedoMap_0;
#endif
#ifdef ALBEDOMAP_1
  uniform int m_AlbedoMap_1;
#endif
#ifdef ALBEDOMAP_2
  uniform int m_AlbedoMap_2;
#endif
#ifdef ALBEDOMAP_3
  uniform int m_AlbedoMap_3;
#endif
#ifdef ALBEDOMAP_4
  uniform int m_AlbedoMap_4;
#endif
#ifdef ALBEDOMAP_5
  uniform int m_AlbedoMap_5;
#endif
#ifdef ALBEDOMAP_6
  uniform int m_AlbedoMap_6;
#endif
#ifdef ALBEDOMAP_7
  uniform int m_AlbedoMap_7;
#endif
#ifdef ALBEDOMAP_8
  uniform int m_AlbedoMap_8;
#endif
#ifdef ALBEDOMAP_9
  uniform int m_AlbedoMap_9;
#endif
#ifdef ALBEDOMAP_10
  uniform int m_AlbedoMap_10;
#endif
#ifdef ALBEDOMAP_11
  uniform int m_AlbedoMap_11;
#endif


//#ifdef ALBEDOMAP_0_SCALE
  uniform float m_AlbedoMap_0_scale;
//#endif
//#ifdef ALBEDOMAP_1_SCALE
  uniform float m_AlbedoMap_1_scale;
//#endif
//#ifdef ALBEDOMAP_2_SCALE
  uniform float m_AlbedoMap_2_scale;
//#endif
//#ifdef ALBEDOMAP_3_SCALE
  uniform float m_AlbedoMap_3_scale;
//#endif
//#ifdef ALBEDOMAP_4_SCALE
  uniform float m_AlbedoMap_4_scale;
//#endif
//#ifdef ALBEDOMAP_5_SCALE
  uniform float m_AlbedoMap_5_scale;
//#endif
//#ifdef ALBEDOMAP_6_SCALE
  uniform float m_AlbedoMap_6_scale;
//#endif
//#ifdef ALBEDOMAP_7_SCALE
  uniform float m_AlbedoMap_7_scale;
//#endif
//#ifdef ALBEDOMAP_8_SCALE
  uniform float m_AlbedoMap_8_scale;
//#endif
//#ifdef ALBEDOMAP_9_SCALE
  uniform float m_AlbedoMap_9_scale;
//#endif
//#ifdef ALBEDOMAP_10_SCALE
  uniform float m_AlbedoMap_10_scale;
//#endif
//#ifdef ALBEDOMAP_11_SCALE
  uniform float m_AlbedoMap_11_scale;
//#endif


#ifdef NORMALMAP_0
  uniform int m_NormalMap_0;
#endif
#ifdef NORMALMAP_1
  uniform int m_NormalMap_1;
#endif
#ifdef NORMALMAP_2
  uniform int m_NormalMap_2;
#endif
#ifdef NORMALMAP_3
  uniform int m_NormalMap_3;
#endif
#ifdef NORMALMAP_4
  uniform int m_NormalMap_4;
#endif
#ifdef NORMALMAP_5
  uniform int m_NormalMap_5;
#endif
#ifdef NORMALMAP_6
  uniform int m_NormalMap_6;
#endif
#ifdef NORMALMAP_7
  uniform int m_NormalMap_7;
#endif
#ifdef NORMALMAP_8
  uniform int m_NormalMap_8;
#endif
#ifdef NORMALMAP_9
  uniform int m_NormalMap_9;
#endif
#ifdef NORMALMAP_10
  uniform int m_NormalMap_10;
#endif
#ifdef NORMALMAP_11
  uniform int m_NormalMap_11;
#endif


#ifdef METALLICROUGHNESSMAP_0
  uniform int m_MetallicRoughnessMap_0;
#endif
#ifdef METALLICROUGHNESSMAP_1
  uniform int m_MetallicRoughnessMap_1;
#endif
#ifdef METALLICROUGHNESSMAP_2
  uniform int m_MetallicRoughnessMap_2;
#endif
#ifdef METALLICROUGHNESSMAP_3
  uniform int m_MetallicRoughnessMap_3;
#endif
#ifdef METALLICROUGHNESSMAP_4
  uniform int m_MetallicRoughnessMap_4;
#endif
#ifdef METALLICROUGHNESSMAP_5
  uniform int m_MetallicRoughnessMap_5;
#endif
#ifdef METALLICROUGHNESSMAP_6
  uniform int m_MetallicRoughnessMap_6;
#endif
#ifdef METALLICROUGHNESSMAP_7
  uniform int m_MetallicRoughnessMap_7;
#endif
#ifdef METALLICROUGHNESSMAP_8
  uniform int m_MetallicRoughnessMap_8;
#endif
#ifdef METALLICROUGHNESSMAP_9
  uniform int m_MetallicRoughnessMap_9;
#endif
#ifdef METALLICROUGHNESSMAP_10
  uniform int m_MetallicRoughnessMap_10;
#endif
#ifdef METALLICROUGHNESSMAP_11
  uniform int m_MetallicRoughnessMap_11;
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

vec4 tempAlbedo, tempNormal;
float tempParallax, tempMetallic, tempRoughness, tempAo, tempEmissiveIntensity;

float noiseHash;
float livelinessValue;
float afflictionValue;
int afflictionMode = 1;





#define DEFINE_COORD(index) vec2 coord##index = texCoord * m_AlbedoMap##index##_scale;




#define BLEND_MR_VALUES(index, ab)\
    packedAoValue = mix(packedAoValue , 1, ab);\
    Metallic = mix(Metallic, m_Metallic##index, ab);\
    Roughness = mix(Roughness, m_Roughness##index, ab);
    
#define BLEND_MRAOEI_MAP(index, ab)\
    packedMetallicRoughnessAoEiVec.rgba = texture2DArray(m_MetallicRoughnessAoEiTextureArray, vec3(coord##index, m_MetallicRoughnessMap##index)).rgba;\
    tempRoughness = packedMetallicRoughnessAoEiVec.g* m_Roughness##index;\
    tempMetallic = m_Metallic##index;\
    tempMetallic = tempMetallic * packedMetallicRoughnessAoEiVec.b;\
    tempAo = packedMetallicRoughnessAoEiVec.r;\
    tempEmissiveIntensity = packedMetallicRoughnessAoEiVec.a;\
    packedAoValue = mix(packedAoValue, tempAo, ab);\
    Metallic = mix(Metallic, tempMetallic, ab);\
    Roughness = mix(Roughness, tempRoughness, ab);
    



#define BLEND(index, ab)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo.rgb =  texture2DArray(m_AlbedoTextureArray, vec3(coord##index, m_AlbedoMap##index)).rgb;\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo.rgb = mix( albedo.rgb, tempAlbedo.rgb ,ab );\
    normal.rgb = mix(normal.xyz, wNormal.rgb, ab);
 
   

#define BLEND_NORMAL(index, ab)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo.rgb =  texture2DArray(m_AlbedoTextureArray, vec3(coord##index, m_AlbedoMap##index)).rgb;\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo.rgb = mix( albedo.rgb, tempAlbedo.rgb ,ab );\
    packedNormalParallaxVec.rgba = texture2DArray(m_NormalParallaxTextureArray, vec3(coord##index,  m_NormalMap##index)).rgba;\
    tempNormal.xyz = mixNormals(packedNormalParallaxVec.xyz, wNormal);\
    normal.xyz = mix(normal.xyz, tempNormal.xyz, ab);
    
    
#define TRI_BLEND_MR_VALUES(index, ab)\
    packedAoValue = mix(packedAoValue , 1, ab);\
    Metallic = mix(Metallic, m_Metallic##index, ab);\
    Roughness = mix(Roughness, m_Roughness##index, ab);
    
#define TRI_BLEND_MRAOEI_MAP(worldCoords, index, ab, blending)\
    packedMetallicRoughnessAoEiVec.rgba = getTriPlanarBlendFromTexArray(worldCoords, blending, m_MetallicRoughnessMap##index, m_AlbedoMap##index##_scale, m_MetallicRoughnessAoEiTextureArray).rgba;\
    tempRoughness = packedMetallicRoughnessAoEiVec.g* m_Roughness##index;\
    tempMetallic = m_Metallic##index;\
    tempMetallic = tempMetallic * packedMetallicRoughnessAoEiVec.b;\
    tempAo = packedMetallicRoughnessAoEiVec.r;\
    tempEmissiveIntensity = packedMetallicRoughnessAoEiVec.a;\
    packedAoValue = mix(packedAoValue, tempAo, ab);\
    Metallic = mix(Metallic, tempMetallic, ab);\
    Roughness = mix(Roughness, tempRoughness, ab);

#define TRI_BLEND(index, ab, worldCoords, blending)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo = getTriPlanarBlendFromTexArray(worldCoords, blending, m_AlbedoMap##index, m_AlbedoMap##index##_scale, m_AlbedoTextureArray);\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo = mix( albedo, tempAlbedo ,ab );\
    normal.rgb = mix(normal.xyz, wNormal.rgb, ab);    

   

#define TRI_BLEND_NORMAL(index, ab, worldCoords, blending)\
    afflictionMode = m_AfflictionMode##index;\
    tempAlbedo.rgb = getTriPlanarBlendFromTexArray(worldCoords, blending, m_AlbedoMap##index, m_AlbedoMap##index##_scale, m_AlbedoTextureArray).rgb;\
    packedNormalParallaxVec.rgba = getTriPlanarBlendFromTexArray(worldCoords, blending, m_NormalMap##index, m_AlbedoMap##index##_scale, m_NormalParallaxTextureArray).rgba;\
    tempAlbedo.rgb = alterLiveliness(tempAlbedo.rgb, livelinessValue, afflictionMode);\
    albedo.rgb = mix( albedo.rgb, tempAlbedo.rgb ,ab );\
    tempNormal.xyz = packedNormalParallaxVec.xyz;\
    tempNormal.xyz = mixNormals(tempNormal.xyz, wNormal);\
    normal.xyz = mix(normal.xyz, tempNormal.xyz, ab);
    
    


    
    
 #define BLEND_PARALLAX(index, ab)\
    albedo.r = alvedo.r;
    


#ifdef ALPHAMAP

//parallax removed for now, would been in BLEND_PARALLAX above..but that would be an extra texture read if the normal map was already read elsewhere, so eventually
// nest a method in the BLEND_NORMAL method defs, and see if the regular method can take in the float for tha alpha channel where parallax is packed and check for the packed parallax boolean

// calculateParallax(coord##index, m_ParallaxHeight##index, ab, m_NormalMap##index);

void calculateParallax(inout vec2 parallaxTexCoord, in float parallaxHeight, in float intensity, in int texIndex) {
//    #ifdef PARALLAX_OCCLUSION
//        #ifdef PARALLAX_LOD_DISTANCE  
//            if(camDist < m_ParallaxLODDistance && intensity > 0.2){
//                vec3 vViewDir =  viewDir * tbnMat;
//                Parallax_initFor(vViewDir,parallaxHeight);
//                Parallax_TextureArray_displaceCoords(parallaxTexCoord, m_NormalParallaxTextureArray, texIndex);
//            }
//        #else
//            if(intensity > 0.2){
//                vec3 vViewDir =  viewDir * tbnMat;
//                Parallax_initFor(vViewDir,parallaxHeight);
//                Parallax_TextureArray_displaceCoords(parallaxTexCoord, m_NormalParallaxTextureArray, texIndex);
//            }
//        #endif
 //   #else
//      #ifdef STEEP_PARALLAX
           //parallax map is stored in the alpha channel of the normal map         
 //          newTexCoord = steepParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);    
 //       #else
                //parallax map is stored in the alpha channel of the normal map         
//            newTexCoord = classicParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);
//        #endif
    
 //   #endif
}

void calculateTriParallax(inout vec2 parallaxTexCoord, in float parallaxHeight, in float intensity, in int texIndex) {
//    #ifdef PARALLAX_OCCLUSION
//        #ifdef PARALLAX_LOD_DISTANCE  
//            if(camDist < m_ParallaxLODDistance && intensity > 0.2){
//                vec3 vViewDir =  viewDir * tbnMat;
//                Parallax_initFor(vViewDir,parallaxHeight);
//                Tri_Parallax_TextureArray_displaceCoords(parallaxTexCoord, m_NormalParallaxTextureArray, texIndex);
//            }
//        #else
//            if(intensity > 0.2){
//                vec3 vViewDir =  viewDir * tbnMat;
//                Parallax_initFor(vViewDir,parallaxHeight);
//                Tri_Parallax_TextureArray_displaceCoords(parallaxTexCoord, m_NormalParallaxTextureArray, texIndex);
//            }
//        #endif
 //   #else
//      #ifdef STEEP_PARALLAX
           //parallax map is stored in the alpha channel of the normal map         
 //          newTexCoord = steepParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);    
 //       #else
                //parallax map is stored in the alpha channel of the normal map         
//            newTexCoord = classicParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);
//        #endif
    
 //   #endif
}


vec4 getTriPlanarBlendFromTexArray(in vec4 coords, in vec3 blending, in int idInTexArray, in float scale, in sampler2DArray texArray) {
    

      vec4 col1 = texture2DArray( texArray, vec3((coords.yz * scale), idInTexArray ) );
      vec4 col2 = texture2DArray( texArray, vec3((coords.xz * scale), idInTexArray ) );
      vec4 col3 = texture2DArray( texArray, vec3((coords.xy * scale), idInTexArray ) );
      // blend the results of the 3 planar projections.
      vec4 tex = col1 * blending.x + col2 * blending.y + col3 * blending.z;
      
      return tex;
}

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
        DEFINE_COORD(_0)
        #ifdef PARALLAXHEIGHT_0
            BLEND_PARALLAX(_0, alphaBlend.r)
        #endif
        
        #ifdef NORMALMAP_0
            BLEND_NORMAL(_0,  alphaBlend.r)
        #else
            BLEND(_0,  alphaBlend.r)
        #endif
        #ifdef METALLICROUGHNESSMAP_0            
            BLEND_MRAOEI_MAP(_0,  alphaBlend.r)
        #else
            BLEND_MR_VALUES(_0,  alphaBlend.r)
        #endif
        
    #endif
    #ifdef ALBEDOMAP_1
        DEFINE_COORD(_1)
        #ifdef PARALLAXHEIGHT_1
            BLEND_PARALLAX(_1, alphaBlend.g)
        #endif
                
        #ifdef NORMALMAP_1
            BLEND_NORMAL(_1,  alphaBlend.g)
        #else
            BLEND(_1,  alphaBlend.g)
        #endif
        #ifdef METALLICROUGHNESSMAP_1
            BLEND_MRAOEI_MAP(_1,  alphaBlend.g)
        #else
            BLEND_MR_VALUES(_1,  alphaBlend.g)
        #endif
        
    #endif
    #ifdef ALBEDOMAP_2
        DEFINE_COORD(_2)
        
        #ifdef PARALLAXHEIGHT_2
       //     BLEND_PARALLAX(_2, alphaBlend.b)
        #endif
        
        #ifdef NORMALMAP_2
            BLEND_NORMAL(_2,  alphaBlend.b)
        #else
            BLEND(_2,  alphaBlend.b)
        #endif
        #ifdef METALLICROUGHNESSMAP_2
            BLEND_MRAOEI_MAP(_2,  alphaBlend.b)
        #else
            BLEND_MR_VALUES(_2,  alphaBlend.b)
        #endif
        
        tempParallax = packedNormalParallaxVec.a;
        
        
    #endif
    #ifdef ALBEDOMAP_3 
        DEFINE_COORD(_3)
         #ifdef PARALLAXHEIGHT_3
            BLEND_PARALLAX(_3, alphaBlend.a)
        #endif
        
        #ifdef NORMALMAP_3
            BLEND_NORMAL(_3,  alphaBlend.a)
        #else
            BLEND(_3,  alphaBlend.a)
        #endif
        #ifdef METALLICROUGHNESSMAP_3
            BLEND_MRAOEI_MAP(_3,  alphaBlend.a)
        #else
            BLEND_MR_VALUES(_3,  alphaBlend.a)
        #endif
    #endif

    #ifdef ALPHAMAP_1
        #ifdef ALBEDOMAP_4
            DEFINE_COORD(_4)
            #ifdef PARALLAXHEIGHT_4
                  BLEND_PARALLAX(_4, alphaBlend.r)
            #endif
        
            #ifdef NORMALMAP_4
                BLEND_NORMAL(_4,  alphaBlend1.r)
            #else
                BLEND(_4,  alphaBlend1.r)
            #endif
            #ifdef METALLICROUGHNESSMAP_4
                BLEND_MRAOEI_MAP(_4,  alphaBlend1.r)
            #else
                BLEND_MR_VALUES(_4,  alphaBlend1.r)
            #endif
           
        #endif
        #ifdef ALBEDOMAP_5
            DEFINE_COORD(_5)
            #ifdef PARALLAXHEIGHT_5
                BLEND_PARALLAX(_5, alphaBlend.g)
            #endif
            #ifdef NORMALMAP_5
                BLEND_NORMAL(_5,  alphaBlend1.g)
            #else
                BLEND(_5,  alphaBlend1.g)
            #endif
            #ifdef METALLICROUGHNESSMAP_5
                BLEND_MRAOEI_MAP(_5,  alphaBlend1.g)
            #else
                BLEND_MR_VALUES(_5,  alphaBlend1.g)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_6
            DEFINE_COORD(_6)
             #ifdef PARALLAXHEIGHT_6
                BLEND_PARALLAX(_6, alphaBlend.b)
            #endif
            #ifdef NORMALMAP_6
                BLEND_NORMAL(_6,  alphaBlend1.b)
            #else
                BLEND(_6,  alphaBlend1.b)
            #endif
            #ifdef METALLICROUGHNESSMAP_6
                BLEND_MRAOEI_MAP(_6,  alphaBlend1.b)
            #else
                BLEND_MR_VALUES(_6,  alphaBlend1.b)
            #endif
            
             
        #endif
        #ifdef ALBEDOMAP_7
            DEFINE_COORD(_7)
             #ifdef PARALLAXHEIGHT_7
                BLEND_PARALLAX(_7, alphaBlend.a)
            #endif
            #ifdef NORMALMAP_7
                BLEND_NORMAL(_7,  alphaBlend1.a)
            #else
                BLEND(_7,  alphaBlend1.a)
            #endif
            #ifdef METALLICROUGHNESSMAP_7
                BLEND_MRAOEI_MAP(_7,  alphaBlend1.a)
            #else
                BLEND_MR_VALUES(_7,  alphaBlend1.a)
            #endif
             
        #endif
    #endif

    #ifdef ALPHAMAP_2
        #ifdef ALBEDOMAP_8
            DEFINE_COORD(_8)
             #ifdef PARALLAXHEIGHT_8
                BLEND_PARALLAX(_8, alphaBlend.r)
            #endif
             #ifdef NORMALMAP_8
                BLEND_NORMAL(_8,  alphaBlend2.r)
            #else
                BLEND(_8,  alphaBlend2.r)
            #endif
            #ifdef METALLICROUGHNESSMAP_8
                BLEND_MRAOEI_MAP(_8,  alphaBlend2.r)
            #else
                BLEND_MR_VALUES(_8,  alphaBlend2.r)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_9
            DEFINE_COORD(_9)
             #ifdef PARALLAXHEIGHT_9
                BLEND_PARALLAX(_9, alphaBlend.g)
            #endif
             #ifdef NORMALMAP_9
                BLEND_NORMAL(_9,  alphaBlend2.g)
            #else
                BLEND(_9,  alphaBlend2.g)
            #endif
            #ifdef METALLICROUGHNESSMAP_8
                BLEND_MRAOEI_MAP(_9,  alphaBlend2.g)
            #else
                BLEND_MR_VALUES(_9,  ,alphaBlend2.g)
            #endif
             
        #endif
        #ifdef ALBEDOMAP_10
            DEFINE_COORD(_10)
             #ifdef PARALLAXHEIGHT_10
                BLEND_PARALLAX(_10, alphaBlend.b)
            #endif
            #ifdef NORMALMAP_10
                BLEND_NORMAL(_10,  alphaBlend2.b)
            #else
                BLEND(_10,  alphaBlend2.b)
            #endif
            #ifdef METALLICROUGHNESSMAP_8
                BLEND_MRAOEI_MAP(_10,  alphaBlend2.b)
            #else
                BLEND_MR_VALUES(_10,  alphaBlend2.b)
            #endif
            
        #endif
        #ifdef ALBEDOMAP_11
             DEFINE_COORD(_11)
              #ifdef PARALLAXHEIGHT_11
                BLEND_PARALLAX(_11, alphaBlend.a)
            #endif
             #ifdef NORMALMAP_11
                BLEND_NORMAL(_11,  alphaBlend2.a)
            #else
                BLEND(_11,  alphaBlend2.a)
            #endif
            #ifdef METALLICROUGHNESSMAP_8
                BLEND_MRAOEI_MAP(_11,  alphaBlend2.a)
            #else
                BLEND_MR_VALUES(_11,  alphaBlend2.a)
            #endif
             
        #endif                   
    #endif

    return albedo;
  }


// TRI PLANAR ALPHA MAP TEXTURES_ _ _ _    \/

  #ifdef TRI_PLANAR_MAPPING


    vec4 calculateTriPlanarAlbedoBlend(in vec3 wNorm, in vec4 wVert, in vec2 texCoord, vec3 blending) {
            // tri-planar texture bending factor for this fragment's normal
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
            #ifdef METALLICROUGHNESSMAP_0
                TRI_BLEND_MRAOEI_MAP(wVertex,_0,  alphaBlend.r, blending)
            #else
                TRI_BLEND_MR_VALUES(_0,  alphaBlend.r)
            #endif

        #endif
        #ifdef ALBEDOMAP_1
            #ifdef NORMALMAP_1
                TRI_BLEND_NORMAL(_1,  alphaBlend.g, wVertex, blending)
            #else
                TRI_BLEND(_1,  alphaBlend.g, wVertex, blending)
            #endif            
            #ifdef METALLICROUGHNESSMAP_1
                TRI_BLEND_MRAOEI_MAP(wVertex,_1,  alphaBlend.g, blending)
            #else
                TRI_BLEND_MR_VALUES(_1,  alphaBlend.g)
            #endif

        #endif
        #ifdef ALBEDOMAP_2
            #ifdef NORMALMAP_2
                TRI_BLEND_NORMAL(_2,  alphaBlend.b, wVertex, blending)
            #else
                TRI_BLEND(_2,  alphaBlend.b, wVertex, blending)
            #endif            
            #ifdef METALLICROUGHNESSMAP_2
                TRI_BLEND_MRAOEI_MAP(wVertex,_2,  alphaBlend.b, blending)
            #else
                TRI_BLEND_MR_VALUES(_2,  alphaBlend.b)
            #endif

        #endif
        #ifdef ALBEDOMAP_3 
            #ifdef NORMALMAP_3
                TRI_BLEND_NORMAL(_3,  alphaBlend.a, wVertex, blending)
            #else
                TRI_BLEND(_3,  alphaBlend.a, wVertex, blending)
            #endif            
            #ifdef METALLICROUGHNESSMAP_3
                TRI_BLEND_MRAOEI_MAP(wVertex,_3,  alphaBlend.a, blending)
            #else
                TRI_BLEND_MR_VALUES(_3,  alphaBlend.a)
            #endif

        #endif

        #ifdef ALPHAMAP_1
            #ifdef ALBEDOMAP_4
                #ifdef NORMALMAP_4
                    TRI_BLEND_NORMAL(_4,  alphaBlend1.r, wVertex, blending)
                #else
                    TRI_BLEND(_4,  alphaBlend1.r, wVertex, blending)
                #endif                
                #ifdef METALLICROUGHNESSMAP_4
                    TRI_BLEND_MRAOEI_MAP(wVertex,_4,  alphaBlend1.r, blending)
                #else
                    TRI_BLEND_MR_VALUES(_4,  alphaBlend1.r)
                #endif

            #endif
            #ifdef ALBEDOMAP_5
                #ifdef NORMALMAP_5
                    TRI_BLEND_NORMAL(_5,  alphaBlend1.g, wVertex, blending)
                #else
                    TRI_BLEND(_5,  alphaBlend1.g, wVertex, blending)
                #endif
                #ifdef METALLICROUGHNESSMAP_5
                    TRI_BLEND_MRAOEI_MAP(wVertex,_5,  alphaBlend1.g, blending)
                #else
                    TRI_BLEND_MR_VALUES(_5,  alphaBlend1.g)
                #endif

            #endif
            #ifdef ALBEDOMAP_6
                #ifdef NORMALMAP_6
                    TRI_BLEND_NORMAL(_6,  alphaBlend1.b, wVertex, blending)
                #else
                    TRI_BLEND(_6,  alphaBlend1.b, wVertex, blending)
                #endif
                #ifdef METALLICROUGHNESSMAP_6
                    TRI_BLEND_MRAOEI_MAP(wVertex,_6,  alphaBlend1.b, blending)
                #else
                    TRI_BLEND_MR_VALUES(_6,  alphaBlend1.b)
                #endif

            #endif
            #ifdef ALBEDOMAP_7
                #ifdef NORMALMAP_7
                    TRI_BLEND_NORMAL(_7,  alphaBlend1.a, wVertex, blending)
                #else
                    TRI_BLEND(_7,  alphaBlend1.a, wVertex, blending)
                #endif
                #ifdef METALLICROUGHNESSMAP_7
                    TRI_BLEND_MRAOEI_MAP(wVertex,_7,  alphaBlend1.a, blending)
                #else
                    TRI_BLEND_MR_VALUES(_7,  alphaBlend1.a)
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
                #ifdef METALLICROUGHNESSMAP_8
                    TRI_BLEND_MRAOEI_MAP(wVertex,_8,  alphaBlend2.r, blending)
                #else
                    TRI_BLEND_MR_VALUES(_8,  alphaBlend2.r)
                #endif

            #endif
            #ifdef ALBEDOMAP_9
                 #ifdef NORMALMAP_9
                    TRI_BLEND_NORMAL(_9,  alphaBlend2.g, wVertex, blending)
                #else
                    TRI_BLEND(_9,  alphaBlend2.g, wVertex, blending)
                #endif
                #ifdef METALLICROUGHNESSMAP_9
                    TRI_BLEND_MRAOEI_MAP(wVertex,_9,  alphaBlend2.g, blending)
                #else
                    TRI_BLEND_MR_VALUES(_9,  alphaBlend2.g)
                #endif

            #endif
            #ifdef ALBEDOMAP_10
                #ifdef NORMALMAP_10
                    TRI_BLEND_NORMAL(_10,  alphaBlend2.b, wVertex, blending)
                #else
                    TRI_BLEND(_10,  alphaBlend2.b, wVertex, blending)
                #endif
                #ifdef METALLICROUGHNESSMAP_10
                    TRI_BLEND_MRAOEI_MAP(wVertex,_10,  alphaBlend2.b, blending)
                #else
                    TRI_BLEND_MR_VALUES(_10,  alphaBlend2.b)
                #endif

            #endif
            #ifdef ALBEDOMAP_11
                 #ifdef NORMALMAP_11
                    TRI_BLEND_NORMAL(_11,  alphaBlend2.a, wVertex, blending)
                #else
                    TRI_BLEND(_11,  alphaBlend2.a, wVertex, blending)
                #endif
                #ifdef METALLICROUGHNESSMAP_11
                    TRI_BLEND_MRAOEI_MAP(wVertex,_11,  alphaBlend2.a, blending)
                #else
                    TRI_BLEND_MR_VALUES(_11,  alphaBlend2.a)
                #endif

            #endif                   
        #endif
        


        return albedo;
    }

    
  #endif

#endif




#if defined(USE_VERTEX_COLORS_AS_SUN_INTENSITY) 
    varying vec4 vertColors; //probably wont happen for rock tower, but leave code here so its consistent to afflictedPbr.frag and just in case you make a custom rock tower with vert colors ever
#endif

#ifdef STATIC_SUN_INTENSITY
    uniform float m_StaticSunIntensity;
#endif

float brightestPointLight = 0.0;


void main(){    
    
    float indoorSunLightExposure = 1.0;//scale this to match R channel of vertex colors

     
    
    
    viewDir = normalize(g_CameraPosition - wPosition);

    norm  = normalize(wNormal);
    normal = norm;

//    #endif

    afflictionVector = vec4(1.0, 0.0, 1.0, 0.0);
    #ifdef AFFLICTIONTEXTURE
    
        #ifdef TILELOCATION 
        //subterrains that are not centred in tile or equal to tile width in total size need to have m_TileWidth pre-set.
            vec2 tileCoords;
            float xPos, zPos;

            vec3 locInTile = (wPosition - m_TileLocation);

             locInTile += vec3(m_TileWidth/2, 0, m_TileWidth/2);

             xPos = (locInTile.x / m_TileWidth);
             zPos = 1 - (locInTile.z / m_TileWidth);

            tileCoords = vec2(xPos, zPos);

            afflictionVector = texture2D(m_AfflictionTexture, tileCoords).rgba;
        
        
        //othrewise, the terrain's texCoords can be used for easiest texel fetching
        #else
            afflictionVector = texture2D(m_AfflictionTexture, texCoord.xy).rgba;
        #endif
    #endif

    livelinessValue = afflictionVector.r;
    afflictionValue = afflictionVector.g;


//get the 0,0 pixel at first corner of texture, and use this as sunlight value

    //----------------------
    // albedo calculations
    //----------------------
    
//always calculated since the
    vec3 blending;
    #ifdef ALBEDOMAP_0
      #ifdef ALPHAMAP
        #ifdef TRI_PLANAR_MAPPING
             blending = abs( wNormal );
                blending = (blending -0.2) * 0.7;
                blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
                float b = (blending.x + blending.y + blending.z);
                blending /= vec3(b, b, b);


            albedo = calculateTriPlanarAlbedoBlend(wNormal, wVertex, texCoord, blending);
            
        #else
            albedo = calculateAlbedoBlend(texCoord);
        #endif
      #else
        albedo = texture2D(m_AlbedoMap_0, texCoord);
      #endif
    #endif

        if(albedo.a <= 0.1){
            albedo.r = 1.0;
            
            discard;
         }








    #ifdef ROUGHNESSMAP
        Roughness = texture2D(m_RoughnessMap, texCoord).r * Roughness;
    #endif
    Roughness = max(Roughness, 1e-4);
    #ifdef METALLICMAP   
        Metallic = texture2D(m_MetallicMap, texCoord).r;
    #endif

    #ifdef METALLICMAP
        Metallic =  max(Metallic, 0.0);
    //    Metallic = texture2D(m_MetallicMap, texCoord).r * max(Metallic, 0.0);
    #else
        Metallic =  max(Metallic, 0.0);
    #endif
    

       

    //---------------------
    // normal calculations
    //---------------------
    #if defined(NORMALMAP_0) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)


    

    #ifdef TRI_PLANAR_MAPPING
    //    normal = calculateNormalTriPlanar(wNormal, wVertex, texCoord);
      #else
    //    normal = calculateNormal(texCoord);
      #endif

 //     normal += norm * 0.9;

//    normal = normalize(normal * vec3(2.0) - vec3(1.0));

    #else

      
//       normal = normalize(norm * vec3(2.0) - vec3(1.0));

       normal = norm;
    #endif

 //   normal = normalize(normal * vec3(2.0) - vec3(1.0));

//APPLY AFFLICTIONNESS TO THE PIXEL

vec4 afflictionAlbedo;    


float newAfflictionScale = m_PlaguedMapScale; //manually assigned as of now, since running into bugs...
vec2 newScaledCoords;

#ifdef AFFLICTIONALBEDOMAP
    #ifdef TRI_PLANAR_MAPPING
        newAfflictionScale = newAfflictionScale / 256;
        afflictionAlbedo = getTriPlanarBlend(wVertex, blending, m_PlaguedAlbedoMap , newAfflictionScale);

    #else
        newScaledCoords = mod(wPosition.xz / m_PlaguedMapScale, 0.985);
        afflictionAlbedo = texture2D(m_PlaguedAlbedoMap , newScaledCoords);
    #endif
   
#else
    afflictionAlbedo = vec4(0.55, 0.8, 0.00, 1.0);
#endif

vec3 afflictionNormal;
#ifdef AFFLICTIONNORMALMAP
    #ifdef TRI_PLANAR_MAPPING

        afflictionNormal = getTriPlanarBlend(wVertex, blending, m_PlaguedNormalMap , newAfflictionScale).rgb;

    #else
        afflictionNormal = texture2D(m_PlaguedNormalMap , newScaledCoords).rgb;
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
    vec4 metallicRoughnessAoEiVec = texture2D(m_PlaguedRoughnessMetallicMap, newScaledCoords);
    afflictionRoughness *= metallicRoughnessAoEiVec.g;
    afflictionMetallic *= metallicRoughnessAoEiVec.b;
    afflictionAo = metallicRoughnessAoEiVec.r;
    afflictionEmissiveIntensity *= metallicRoughnessAoEiVec.a; //important not to leave this channel all black by accident in the mraoei map if using affliction emissiveness    
    
#endif

#ifdef AFFLICTIONEMISSIVEMAP
    vec4 emissiveMapColor = texture2D(m_PlaguedEmissiveMap, newScaledCoords);
    afflictionEmissive *= emissiveMapColor;
#endif



    noiseHash = getStaticNoiseVar0(wPosition, afflictionValue);
    Roughness = alterAfflictionRoughness(afflictionValue, Roughness, afflictionRoughness, noiseHash * afflictionAlbedo.a);
    Metallic = alterAfflictionMetallic(afflictionValue, Metallic,  afflictionMetallic, noiseHash * afflictionAlbedo.a);//use the alpha channel of albedo map to alter opcaity for the matching affliction normals, roughness, and metalicness at each pixel
    albedo = alterAfflictionColor(afflictionValue, albedo, afflictionAlbedo, noiseHash * afflictionAlbedo.a);
    normal = alterAfflictionNormalsForTerrain(afflictionValue, normal, afflictionNormal, noiseHash * afflictionAlbedo.a, wNormal);
    afflictionEmissive = alterAfflictionGlow(afflictionValue, afflictionEmissive, noiseHash);
    //affliction ao value blended below after specular calculation



// spec gloss pipeline most likely will not be supported for this terrain shader anytime soon..
 #ifdef SPECGLOSSPIPELINE

        #ifdef USE_PACKED_SG
            vec4 specularColor = texture2D(m_SpecularGlossinessMap, newTexCoord);
            float glossiness = specularColor.a * m_Glossiness;
            specularColor *= m_Specular;
        #else
            #ifdef SPECULARMAP
                vec4 specularColor = texture2D(m_SpecularMap, newTexCoord);
            #else
                vec4 specularColor = vec4(1.0);
            #endif
            #ifdef GLOSSINESSMAP
                float glossiness = texture2D(m_GlossinesMap, newTexCoord).r * m_Glossiness;
            #else
                float glossiness = m_Glossiness;
            #endif
            specularColor *= m_Specular;
        #endif
        vec4 diffuseColor = albedo;// * (1.0 - max(max(specularColor.r, specularColor.g), specularColor.b));
        Roughness = 1.0 - glossiness;
        vec3 fZero = specularColor.xyz;
    #else      
        float specular = 0.5;
        float nonMetalSpec = 0.08 * specular;
        vec4 specularColor = (nonMetalSpec - nonMetalSpec * Metallic) + albedo * Metallic;
        vec4 diffuseColor = albedo - albedo * Metallic;
        vec3 fZero = vec3(specular);
    #endif

    gl_FragColor.rgb = vec3(0.0);


 
 
//simple ao calculation (no support for lightmaps like stock pbr shader)
    vec3 ao = vec3(packedAoValue);
    
    ao = alterAfflictionAo(afflictionValue, ao, vec3(afflictionAo), noiseHash); // alter the AO map for affliction values
    
    ao.rgb = ao.rrr;
    specularColor.rgb *= ao;
 
 
  //finalLightingScale ACCOUNTS FOR SUN EXPOSURE FOR INDOOR AND SHADED AREAS OUT OF THE SUN'S FULL LIGHTING.
    float finalLightingScale = 1.0; 
    #ifdef STATIC_SUN_INTENSITY
        indoorSunLightExposure = m_StaticSunIntensity; //single float value to indicate percentage of
                           //sunlight hitting the model (only works for small models or models with 100% consistent sunlighting)
    #endif
    #ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
        indoorSunLightExposure = vertColors.r * indoorSunLightExposure;      //use R channel of vertexColors for..       
    #endif 
                                                               // similar purpose as above... *^.  
                                                             //but uses r channel vert colors like an AO map specifically
                                                                 //for sunlight (solution for scaling lighting for indoor
                                                                  // and shadey/dimly lit models, especially big ones)
    brightestPointLight = 0.0;
    
    
    finalLightingScale *= indoorSunLightExposure; 
     
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
            
     //   #if defined(USE_VERTEX_COLORS_AS_SUN_INTENSITY) || defined(STATIC_SUN_INTENSITY)
            
            if(fallOff == 1.0){
                directLighting.rgb *= indoorSunLightExposure;// ... *^. to scale down how intense just the sun is (ambient and direct light are 1.0 fallOff)
                
            }
            else{
                    brightestPointLight = max(fallOff, brightestPointLight);
          
           }
   //     #endif
        
        
        
        gl_FragColor.rgb += directLighting * fallOff;
        
     
    }
    
    
    float minVertLighting;
    #ifdef BRIGHTEN_INDOOR_SHADOWS
        minVertLighting = 0.0833; //brighten shadows so that caves which are naturally covered from the DL shadows are not way too dark compared to when shadows are off
    #else
        minVertLighting = 0.0533;
    
    #endif
    
    finalLightingScale = max(finalLightingScale, brightestPointLight);
    
    finalLightingScale = max(finalLightingScale, minVertLighting); //essentially just the vertColors.r (aka indoor liht exposure) multiplied by the time of day scale.
    
    //IMPORTANT NOTE: You used to multiply finalLightingScale by the indirectLighting value, and need to do that here still
    //no need for anymore time of day code (also remove probe color scale ) as thats in ambient light now.

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

// multiply probes by the finalLightingScale, as determined by pixel's 
// sunlightExposure and adjusted for nearby point/spot lights
        color1.rgb *= finalLightingScale;
        color2.rgb *= finalLightingScale;
        color3.rgb *= finalLightingScale;
        
        
        gl_FragColor.rgb += color1 * clamp(weight1,0.0,1.0) + color2 * clamp(weight2,0.0,1.0) + color3 * clamp(weight3,0.0,1.0);

    #endif
    
    #if defined(EMISSIVE) || defined (EMISSIVEMAP)
        #ifdef EMISSIVEMAP
            emissive = texture2D(m_EmissiveMap, texCoord);
        #else
            emissive = m_Emissive;
        #endif

        gl_FragColor += emissive * pow(emissive.a, m_EmissivePower) * m_EmissiveIntensity;

    #else
   //     gl_FragColor += emissive * pow(emissive.a,  2) * 1;
    
    #endif

    gl_FragColor += emissive * pow(emissiveIntensity * 1.3, emissive.a) * (emissiveIntensity *1.5);


   //  gl_FragColor.rgb = afflictionVector.rgb;
   
   
   
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
    
    gl_FragColor.a = 1.0;
    
}