uniform mat4 g_WorldViewProjectionMatrix;

attribute vec2 inTexCoord;
attribute vec3 inPosition;

varying vec2 texCoord;

void main(){
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition,1.0);
    texCoord=inTexCoord;
}