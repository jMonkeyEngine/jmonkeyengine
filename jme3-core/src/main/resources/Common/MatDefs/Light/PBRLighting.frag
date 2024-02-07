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

#import "Common/ShaderLib/PBRLightingParamsReader.glsllib"
#import "Common/ShaderLib/PBRLighting.glsllib"
// It is important that these 2 glsllibs are referenced AFTER the other variables above have been declared. 
// The above variables are declared here (rather than in a glsllib) to reduce redundancy, since these variables are likely to be used by more than one glsllib.
// Only lighting variables are declared in PBRLighting.glsllib, and only basic PBR material params are declared in PBRLightingParamsReader.glsllib.
// This allows jme developers to create a fork of this shader and make their own changes before reading the base PBR parameters or before the final lighting calculation.
// For example, you can move texCoords based on g_Time before texReads for a simple moving water/lava effect, or blend values like albedo/roughness after the param reads
// but before final lighting calculations to do things like dynamic texture splatting.

vec4 albedo = vec4(1.0);
float alpha = 1.0;

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

    // Note: These are intentionally not surrounded by ifDefs relating to normal and parallax maps being defined, because
    // other .glsllibs may require normal or parallax mapping even if the base model does not have those maps
    vec3 tan = normalize(wTangent.xyz);
    mat3 tbnMat = mat3(tan, wTangent.w * cross( (norm), (tan)), norm); 
    vec3 vViewDir =  viewDir * tbnMat;                                 

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
