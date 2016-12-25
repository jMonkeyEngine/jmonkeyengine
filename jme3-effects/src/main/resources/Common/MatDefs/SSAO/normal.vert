#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/GLSLCompat.glsllib"
// These are included in the above now
//uniform mat4 g_WorldViewProjectionMatrix;
//uniform mat3 g_NormalMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec4 inTexCoord;

varying vec3 normal;
varying vec2 texCoord;

void main(void) {
   texCoord=inTexCoord.xy;
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   vec3 modelSpaceNormals = inNormal;
   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos,modelSpaceNormals);
   #endif
   normal = normalize(TransformNormal(modelSpaceNormals));
   gl_Position = TransformWorldViewProjection(modelSpacePos);
}