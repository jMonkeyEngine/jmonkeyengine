#import "Common/ShaderLib/GLSLCompat.glsllib"

// enable apis and import PBRLightingUtils
#define ENABLE_PBRLightingUtils_getWorldPosition 1
//#define ENABLE_PBRLightingUtils_getLocalPosition 1
#define ENABLE_PBRLightingUtils_getWorldNormal 1
#define ENABLE_PBRLightingUtils_getWorldTangent 1
#define ENABLE_PBRLightingUtils_getTexCoord 1
#define ENABLE_PBRLightingUtils_readPBRSurface 1
#define ENABLE_PBRLightingUtils_computeDirectLightContribution 1
#define ENABLE_PBRLightingUtils_computeProbesContribution 1

#import "Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib"

#ifdef DEBUG_VALUES_MODE
    uniform int m_DebugValuesMode;
#endif

uniform vec4 g_LightData[NB_LIGHTS];
uniform vec3 g_CameraPosition;

#ifdef USE_FOG
    #import "Common/ShaderLib/MaterialFog.glsllib"
#endif

void main(){
    vec3 wpos = PBRLightingUtils_getWorldPosition();
    vec3 worldViewDir = normalize(g_CameraPosition - wpos);
    
    // Create a blank PBRSurface.
    PBRSurface surface = PBRLightingUtils_createPBRSurface(worldViewDir);
    
    // Read surface data from standard PBR matParams. (note: matParams are declared in 'PBRLighting.j3md' and initialized as uniforms in 'PBRLightingUtils.glsllib')
    PBRLightingUtils_readPBRSurface(surface);          

    //Calculate necessary variables from pbr surface prior to applying lighting. Ensure all texture/param reading and blending occurrs prior to this being called!
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
