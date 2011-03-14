attribute vec4 inPosition;
attribute vec2 inTexCoord;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;

varying vec2 texCoord;

void main(){
    gl_Position = g_WorldViewProjectionMatrix * inPosition;
    texCoord = inTexCoord;
}