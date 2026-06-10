#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute float inHeat;

varying float heat;

void main() {
    heat = inHeat;
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
