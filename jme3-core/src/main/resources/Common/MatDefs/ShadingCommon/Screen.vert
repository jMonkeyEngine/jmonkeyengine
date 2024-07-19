
#import "Common/ShaderLib/GLSLCompat.glsllib"

attribute vec3 inPosition;
varying vec2 texCoord;

void main() {
    texCoord = inPosition.xy;
    vec2 pos = inPosition.xy * vec2(2.0) - vec2(1.0);
    gl_Position = vec4(pos, 0.0, 1.0);
}

