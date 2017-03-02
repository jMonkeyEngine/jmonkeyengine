//#define VERTEX_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"

attribute vec4 inPosition;
attribute vec2 inTexCoord;
varying vec2 texCoord;

void main() {  
    gl_Position = vec4(inPosition.xy * 2.0 - 1.0, 0.0, 1.0);    
    texCoord = inTexCoord;
}