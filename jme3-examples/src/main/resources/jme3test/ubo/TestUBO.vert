
in vec3 inPosition;
uniform mat4 g_ViewProjectionMatrix;
void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    gl_Position = g_ViewProjectionMatrix*modelSpacePos;
}