// import the following for VR instancing
//#define VERTEX_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/InstanceVR.glsllib"

in vec3 inPosition;
in vec2 inTexCoord;
out vec2 texCoord1;

void main(){
    texCoord1 = inTexCoord;
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    gl_Position = TransformWorldViewProjectionVR(modelSpacePos);
}