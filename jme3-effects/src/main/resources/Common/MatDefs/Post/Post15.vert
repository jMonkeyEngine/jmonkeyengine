#import "Common/ShaderLib/GLSLCompat.glsllib"

in vec4 inPosition;
in vec2 inTexCoord;

out vec2 texCoord;

void main() {
    gl_Position = vec4(inPosition.xy, 0.0, 1.0);
    texCoord = inTexCoord;
}