// import the following for VR instancing
#import "Common/ShaderLib/InstanceVR.glsllib"

attribute vec3 inPosition;
attribute vec2 inTexCoord;
varying vec2 texCoord1;

void main(){
    texCoord1 = inTexCoord;
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    gl_Position = TransformWorldViewProjectionVR(modelSpacePos);
}