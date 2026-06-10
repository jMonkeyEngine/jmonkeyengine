#import "Common/ShaderLib/GLSLCompat.glsllib"

in vec3 inPosition;
in float inGradient;

uniform mat4 g_WorldViewProjectionMatrix;

out float gradient;

void main() {
    gradient = inGradient;
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
