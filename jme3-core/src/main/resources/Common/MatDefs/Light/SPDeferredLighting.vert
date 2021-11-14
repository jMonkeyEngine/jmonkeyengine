#import "Common/ShaderLib/Instancing.glsllib"
varying vec2 texCoord;

attribute vec3 inPosition;
attribute vec2 inTexCoord;

varying mat4 viewProjectionMatrixInverse;

void main(){
   texCoord = inTexCoord;
   vec4 pos = vec4(inPosition, 1.0);
   viewProjectionMatrixInverse = GetViewProjectionMatrixInverse();
   gl_Position = vec4(sign(pos.xy-vec2(0.5)), 0.0, 1.0);
}