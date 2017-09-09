#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/InPassShadows.glsl"

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;
attribute vec4 inColor;

varying vec3 vPos;
varying vec3 vNormal;
varying vec2 vTexCoord;

void main() {
    vTexCoord = inTexCoord;

    vec4 modelSpacePos = vec4(inPosition, 1.0);
    vec3 modelSpaceNorm = inNormal;

    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos, modelSpaceNorm);
    #endif

    vPos = TransformWorldView(modelSpacePos).xyz;
    vNormal = TransformNormal(modelSpaceNorm);

    vec3 shadowPos = TransformWorld(modelSpacePos).xyz;
    Shadow_ProcessProjCoord(shadowPos);

    gl_Position = TransformWorldViewProjection(modelSpacePos);
}