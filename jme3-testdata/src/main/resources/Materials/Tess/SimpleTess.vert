#import "Common/ShaderLib/GLSLCompat.glsllib"

attribute vec3 inPosition;

void main(){
 gl_Position=vec4(inPosition,1);
}
