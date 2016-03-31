#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
attribute vec3 inNormal;

varying vec3 normal;

void main(){
    gl_Position = TransformWorldViewProjection(vec4(inPosition,1.0));
    normal = inNormal;
}
