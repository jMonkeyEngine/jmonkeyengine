#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec3 wPosition;
varying vec3 wNormal;
varying vec3 lPosition;

uniform vec4 g_AmbientLightColor;

#ifdef USE_FOG
    varying float fogDistance;
    uniform vec3 g_CameraPosition;
#endif

void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);

    gl_Position = TransformWorldViewProjection(modelSpacePos);

    texCoord = inTexCoord;

    wPosition = (g_WorldMatrix * vec4(inPosition, 1.0)).xyz;    
    
    wNormal  = normalize(TransformWorldNormal(inNormal));
    
    lPosition = modelSpacePos.xyz;     
    
    #ifdef USE_FOG
        fogDistance = distance(g_CameraPosition, (g_WorldMatrix * modelSpacePos).xyz);
    #endif
    
}
