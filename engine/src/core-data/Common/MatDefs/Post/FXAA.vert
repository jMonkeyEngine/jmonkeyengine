uniform mat4 g_WorldViewProjectionMatrix;
uniform vec2 g_Resolution;

uniform float m_SubPixelShift;

attribute vec4 inPosition;
attribute vec2 inTexCoord;

varying vec2 texCoord;
varying vec4 posPos;

void main() {
    gl_Position = inPosition * 2.0 - 1.0; //vec4(pos, 0.0, 1.0);
    texCoord = inTexCoord;
    vec2 rcpFrame = vec2(1.0) / g_Resolution;
    posPos.xy = inTexCoord.xy;
    posPos.zw = inTexCoord.xy - (rcpFrame * vec2(0.5 + m_SubPixelShift));
}