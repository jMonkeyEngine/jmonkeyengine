#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform mat4 g_WorldViewProjectionMatrix;
uniform vec2 g_ResolutionInverse;

uniform float m_SubPixelShift;

attribute vec4 inPosition;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec4 posPos;

void main() {
    vec2 pos = inPosition.xy * 2.0 - 1.0;
    gl_Position = vec4(pos, 0.0, 1.0);    
    texCoord = inTexCoord;    
    posPos.xy = inTexCoord.xy;
    posPos.zw = inTexCoord.xy - (g_ResolutionInverse * vec2(0.5 + m_SubPixelShift));
}