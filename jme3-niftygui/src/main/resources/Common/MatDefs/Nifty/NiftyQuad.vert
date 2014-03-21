uniform mat4 g_WorldViewProjectionMatrix;

attribute vec4 inPosition;


void main() {
    vec2 pos = (g_WorldViewProjectionMatrix * inPosition).xy;
    gl_Position = vec4(pos, 0.0, 1.0);
}