uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inTexCoord;
attribute vec3 inPosition;

varying vec3 texCoord;

void main(){
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition,1.0);
    texCoord=inTexCoord;
}