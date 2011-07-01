uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

varying float shadowPosition;

attribute vec3 inPosition;

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);


void main(){
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);

    shadowPosition = gl_Position.z;
    // get the vertex in world space
    vec4 worldPos = g_WorldMatrix * vec4(inPosition, 1.0);


    // populate the light view matrices array and convert vertex to light viewProj space
    projCoord0 = biasMat * m_LightViewProjectionMatrix0 * worldPos;
    projCoord1 = biasMat * m_LightViewProjectionMatrix1 * worldPos;
    projCoord2 = biasMat * m_LightViewProjectionMatrix2 * worldPos;
    projCoord3 = biasMat * m_LightViewProjectionMatrix3 * worldPos;
}