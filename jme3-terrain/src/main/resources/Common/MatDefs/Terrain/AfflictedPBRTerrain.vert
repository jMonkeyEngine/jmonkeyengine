#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec3 wPosition;
varying vec3 wNormal;


 uniform vec4 g_AmbientLightColor;


#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
#endif


void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);

    gl_Position = TransformWorldViewProjection(modelSpacePos);

    texCoord = inTexCoord;

    wPosition = TransformWorld(modelSpacePos).xyz;
    
    
    wNormal  = normalize(TransformWorldNormal(inNormal));


    #ifdef TRI_PLANAR_MAPPING
       wVertex = vec4(inPosition,0.0);       
    #endif
    
    
  
    
}