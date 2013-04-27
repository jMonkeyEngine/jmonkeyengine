#import "Common/ShaderLib/Skinning.glsllib"
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat3 g_NormalMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec4 inTexCoord;

varying vec3 normal;
varying vec2 texCoord;

void main(void)
{
   texCoord=inTexCoord.xy;
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   vec3 modelSpaceNormals = inNormal;
   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos,modelSpaceNormals);
   #endif
   normal = normalize(g_NormalMatrix * modelSpaceNormals);
   gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;
}