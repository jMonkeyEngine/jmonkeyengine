#import "Common/ShaderLib/GLSLCompat.glsllib"
attribute vec4 inPosition;
attribute vec2 inTexCoord;

varying vec2 texCoord;

void main() {     
    vec2 pos = inPosition.xy * 2.0 - 1.0;
    gl_Position = vec4(pos, 0.0, 1.0);    
    texCoord = inTexCoord;
}