#import "Common/ShaderLib/GLSLCompat.glsllib"

in vec4 inPosition;
in vec2 inTexCoord;

out vec2 texCoord;

void main() {
    vec2 pos = inPosition.xy * 2.0 - 1.0;
    gl_Position = vec4(pos, 0.0, 1.0);
    texCoord = inTexCoord;
}