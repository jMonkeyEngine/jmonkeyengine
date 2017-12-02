#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec4 inColor;
attribute vec4 inTexCoord;

varying vec4 color;
// z and w values in projection space
varying vec2 projPos;
varying vec2 vPos; // Position of the pixel in clip space



#ifdef USE_TEXTURE
varying vec4 texCoord;
#endif

#ifdef POINT_SPRITE
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_WorldMatrix;
uniform vec3 g_CameraPosition;
uniform float m_Quadratic;
const float SIZE_MULTIPLIER = 4.0;
attribute float inSize;
#endif

void main(){
    vec4 pos = vec4(inPosition, 1.0);

    gl_Position = g_WorldViewProjectionMatrix * pos;
    color = inColor;

    projPos = gl_Position.zw;
   // projPos.x = 0.5 * (projPos.x) + 0.5;

    // Transforms the vPosition data to the range [0,1]
    vPos = (gl_Position.xy / gl_Position.w + 1.0) / 2.0;

    #ifdef USE_TEXTURE
        texCoord = inTexCoord;
    #endif

    #ifdef POINT_SPRITE
        vec4 worldPos = g_WorldMatrix * pos;
        float d = distance(g_CameraPosition.xyz, worldPos.xyz);
        gl_PointSize = max(1.0, (inSize * SIZE_MULTIPLIER * m_Quadratic) / d);

        //vec4 worldViewPos = g_WorldViewMatrix * pos;
        //gl_PointSize = (inSize * SIZE_MULTIPLIER * m_Quadratic)*100.0 / worldViewPos.z;

        color.a *= min(gl_PointSize, 1.0);
    #endif
}