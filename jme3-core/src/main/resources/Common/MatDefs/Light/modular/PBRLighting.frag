#import "Common/ShaderLib/GLSLCompat.glsllib"

// enable apis and import PBRLightingUtils
#define ENABLE_PBRLightingUtils_getWorldPosition 1
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

void main(){
    vec3 wpos = PBRLightingUtils_getWorldPosition();
    vec3 worldViewDir = normalize(g_CameraPosition - wpos);
    
    // Load surface data
    PBRSurface surface=PBRLightingUtils_readPBRSurface(worldViewDir);
    
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

  
    // outputs the final value of the selected layer as a color for debug purposes. 
    #ifdef DEBUG_VALUES_MODE
        if(m_DebugValuesMode == 0){
            gl_FragColor.rgb = vec3(surface.albedo);
        }
        else if(m_DebugValuesMode == 1){
            gl_FragColor.rgb = vec3(surface.normal);
        }
        else if(m_DebugValuesMode == 2){
            gl_FragColor.rgb = vec3(surface.roughness);
        }
        else if(m_DebugValuesMode == 3){
            gl_FragColor.rgb = vec3(surface.metallic);
        }
        else if(m_DebugValuesMode == 4){
            gl_FragColor.rgb = surface.ao.rgb;
        }
        else if(m_DebugValuesMode == 5){
            gl_FragColor.rgb = vec3(surface.emission.rgb);          
        }        
    #endif   
}
