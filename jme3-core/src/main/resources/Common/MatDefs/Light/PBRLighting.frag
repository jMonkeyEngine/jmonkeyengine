#import "Common/ShaderLib/GLSLCompat.glsllib"

varying vec2 texCoord;
vec2 newTexCoord; 
#ifdef SEPARATE_TEXCOORD
    varying vec2 texCoord2;
#endif

#ifdef DISCARD_ALPHA
    uniform float m_AlphaDiscardThreshold;
#endif

varying vec3 wPosition;    

varying vec3 wNormal;
varying vec4 wTangent;

#import "MatDefs/ShaderLib/PBRLightingParamReads.glsllib"
#import "MatDefs/ShaderLib/PBRLighting.glsllib"
//Important that these 2 ^ glsllibs are referenced AFTER other variables are declared above. 
// any variables above are declared there (rather than in a glslib) to reduce rendundancy, because these vars likely to used by more than 1 glslib.
// Only lighting vars are declared in PBRLighting.glslib, and only basePBR matParams are declared in PBRLightingParamReads.glslib
//This allows jme devs to fork this shader and make their own changes before the base PBR param-reads or before the final lighting calculation.
//For example, you could move texCoords based on g_Time prior to texReads for a simple moving water/lava effect. or blend values like albedo/roughness after tex reads but before the final lighting calculations to do things like dynamic texture splatting

vec4 albedo = vec4(1.0);
float alpha = 1.0;

float emissiveIntensity = 0.0;
float emissivePower = 0.0;
vec4 emissive = vec4(0.0);

vec3 ao = vec3(1.0);
vec3 lightMapColor = vec3(0.0);

float indoorSunLightExposure = 1.0;

//metallic pipeline vars:
float Metallic = 0.0;
float Roughness = 0.0;

//spec gloss pipeline vars:
vec4 specularColor;
float glossiness;

void main(){
    
    vec3 norm = normalize(wNormal);
    vec3 normal = norm;
    vec3 viewDir = normalize(g_CameraPosition - wPosition);
    
    vec3 tan = normalize(wTangent.xyz);
    mat3 tbnMat = mat3(tan, wTangent.w * cross( (norm), (tan)), norm); //note: these are intentionaly not surroudned by ifDefs relating to normal and parallax maps being defined, because
    vec3 vViewDir =  viewDir * tbnMat;                                 //other .glslibs may require normal or parallax mapping even if the base model does not hvae those maps

    //base PBR params and tex reads:
    readMatParamsAndTextures(tbnMat, vViewDir, albedo, Metallic, Roughness, specularColor, glossiness, lightMapColor, ao, normal, emissive, alpha);
    
    // Lighting calculation:    
    vec3 finalLightingValue = calculatePBRLighting(albedo, Metallic, Roughness, specularColor, glossiness, lightMapColor, ao, indoorSunLightExposure, normal, norm, viewDir);
    gl_FragColor.rgb += finalLightingValue;

    //apply final emissive value after lighting
    gl_FragColor += emissive;  //no need for #ifdef check because emissive will be 0,0,0,0 if emissive vars werent defined.

    gl_FragColor.a = alpha;
   
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
}
