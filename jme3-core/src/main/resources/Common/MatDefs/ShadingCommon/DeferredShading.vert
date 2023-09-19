#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
varying vec2 texCoord;

attribute vec3 inPosition;
#if !defined(USE_LIGHTS_CULL_MODE)
   attribute vec2 inTexCoord;
#endif

varying mat4 viewProjectionMatrixInverse;

void main(){
#if !defined(USE_LIGHTS_CULL_MODE)
   texCoord = inTexCoord;
   vec4 pos = vec4(inPosition, 1.0);
   gl_Position = vec4(sign(pos.xy-vec2(0.5)), 0.0, 1.0);
#else
   gl_Position = TransformWorldViewProjection(vec4(inPosition, 1.0));// g_WorldViewProjectionMatrix * modelSpacePos;
#endif

   viewProjectionMatrixInverse = GetViewProjectionMatrixInverse();

}