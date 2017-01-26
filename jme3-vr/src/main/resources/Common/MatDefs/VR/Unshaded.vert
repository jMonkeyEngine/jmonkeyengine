//#define VERTEX_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"

// import the following for VR instancing
#import "Common/ShaderLib/InstanceVR.glsllib"

attribute vec3 inPosition;

#if defined(HAS_COLORMAP) || (defined(HAS_LIGHTMAP) && !defined(SEPARATE_TEXCOORD))
    #define NEED_TEXCOORD1
#endif

attribute vec2 inTexCoord;
attribute vec4 inColor;

varying vec2 texCoord1;

void main(){
    #ifdef NEED_TEXCOORD1
        texCoord1 = inTexCoord;
    #endif

    vec4 modelSpacePos = vec4(inPosition, 1.0);

    // use the following transform function for VR instancing
    gl_Position = TransformWorldViewProjectionVR(modelSpacePos);
}