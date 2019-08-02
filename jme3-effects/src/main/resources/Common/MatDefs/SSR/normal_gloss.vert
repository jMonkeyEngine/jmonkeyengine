#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec4 inTexCoord;

varying vec3 wNormal;
varying vec2 texCoord;

#if defined(NORMALMAP)
    attribute vec4 inTangent;
    varying vec4 wTangent;
#endif

void main(void)
{
   texCoord=inTexCoord.xy;
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   vec3 modelSpaceNormals = inNormal;
   
   #if defined(NORMALMAP)
      wTangent = vec4(TransformWorldNormal(inTangent.xyz),inTangent.w);
   #endif
   
   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos,modelSpaceNormals);
   #endif
   wNormal = normalize(TransformWorldNormal(modelSpaceNormals));
   gl_Position = TransformWorldViewProjection(modelSpacePos);
}