#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
attribute vec3 inPosition;
#ifdef DISCARD_ALPHA
attribute vec2 inTexCoord;
varying vec2 texCoord;
#endif

#ifdef POINT_LIGHT
uniform vec3 g_CameraPosition;
uniform vec2 g_FrustumNearFar;
#endif

void main() {
    vec4 modelSpacePos = vec4(inPosition, 1.0);
  
    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos);
    #endif

    #ifdef DISCARD_ALPHA
        texCoord = inTexCoord;
    #endif

    gl_Position = TransformWorldViewProjection(modelSpacePos);

    #ifdef POINT_LIGHT
        vec3 lightDir = g_CameraPosition - TransformWorld(modelSpacePos).xyz;

        // The Z value to write into the depth map, should be [0.0, 1.0]
        float z = sqrt(length(lightDir) / g_FrustumNearFar.y);

        // Remap [0.0, 1.0] into [-1.0, 1.0]
        gl_Position.z = (clamp(z, 0.0, 1.0) * 2.0 - 1.0) * gl_Position.w;
    #endif
}