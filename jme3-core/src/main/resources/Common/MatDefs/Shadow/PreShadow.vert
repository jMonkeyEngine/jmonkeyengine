#import "Common/ShaderLib/Skinning.glsllib"
attribute vec3 inPosition;
attribute vec2 inTexCoord;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;

varying vec2 texCoord;

void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);
  
   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos);
   #endif
    gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;
    texCoord = inTexCoord;
}