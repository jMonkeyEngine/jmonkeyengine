uniform float m_tilingFactor;
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform mat3 g_NormalMatrix;

uniform float m_terrainSize;

attribute vec4 inTexCoord;
attribute vec3 inNormal;
attribute vec3 inPosition;

varying vec3 normal;
varying vec4 position;

void main()
{
 	normal = normalize(inNormal);
 	position = g_WorldMatrix * vec4(inPosition, 0.0);
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1);
}


