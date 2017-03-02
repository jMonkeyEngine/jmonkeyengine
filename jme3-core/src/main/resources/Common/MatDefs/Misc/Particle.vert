#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec4 inColor;
attribute vec4 inTexCoord;

varying vec4 color;

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

    #ifdef USE_TEXTURE
        texCoord = inTexCoord;
    #endif

    #ifdef POINT_SPRITE
        vec4 worldPos = g_WorldMatrix * pos;
        float d = distance(g_CameraPosition.xyz, worldPos.xyz);
        float size = (inSize * SIZE_MULTIPLIER * m_Quadratic) / d;
        gl_PointSize = max(1.0, size);

        //vec4 worldViewPos = g_WorldViewMatrix * pos;
        //gl_PointSize = (inSize * SIZE_MULTIPLIER * m_Quadratic)*100.0 / worldViewPos.z;

        color.a *= min(size, 1.0);
    #endif
}
