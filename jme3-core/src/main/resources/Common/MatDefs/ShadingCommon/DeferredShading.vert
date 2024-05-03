
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;

#ifdef USE_LIGHTS_CULL_MODE
    varying vec2 texCoord;
#endif
varying mat4 viewProjectionMatrixInverse;

void main(){
    
    #ifndef USE_LIGHTS_CULL_MODE
        texCoord = inPosition.xy;
        gl_Position = vec4(sign(inPosition.xy - vec2(0.5)), 0.0, 1.0);
    #else
        gl_Position = TransformWorldViewProjection(vec4(inPosition, 1.0));
    #endif

   viewProjectionMatrixInverse = GetViewProjectionMatrixInverse();

}

