
#import "Common/ShaderLib/GLSLCompat.glsllib"

attribute vec3 inPosition;
varying vec2 texCoord;

const vec2 sub = vec2(1.0);

void main() {
    texCoord = inPosition.xy;
    vec2 pos = inPosition.xy * 2.0 - sub;
    gl_Position = vec4(pos, 0.0, 1.0);
}

