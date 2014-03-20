uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;

varying vec3 normal;

void main(){
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition,1.0);
    normal = inNormal;
}
