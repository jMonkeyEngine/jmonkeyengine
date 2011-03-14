uniform mat4 g_ViewMatrix;
uniform mat4 g_ProjectionMatrix;
uniform mat3 g_NormalMatrix;

uniform vec3 m_NormalScale;

attribute vec3 inPosition;
attribute vec3 inNormal;

varying vec3 direction;

void main(){
    // set w coordinate to 0
    vec4 pos = vec4(inPosition, 0.0);

    // compute rotation only for view matrix
    pos = g_ViewMatrix * pos;

    // now find projection
    pos.w = 1.0;
    gl_Position = g_ProjectionMatrix * pos;

    direction = normalize(inNormal * m_NormalScale);
}
