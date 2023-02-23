//#define VERTEX_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"

// import the following for VR instancing
#import "Common/ShaderLib/InstanceVR.glsllib"

attribute vec3 inPosition;
attribute vec3 inNormal;

varying vec3 normal;

void main(void)
{
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    normal = normalize(TransformNormal(inNormal));

    // use the following transform function for VR instancing
    gl_Position = TransformWorldViewProjectionVR(modelSpacePos);
}