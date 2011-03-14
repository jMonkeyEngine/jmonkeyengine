uniform mat4 g_WorldViewProjectionMatrix;
uniform mat3 g_NormalMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec4 inTexCoord;

varying vec3 normal;
varying vec2 texCoord;

void main(void)
{
   texCoord=inTexCoord.xy;
   normal = normalize(g_NormalMatrix * inNormal);
   gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition,1.0);
}