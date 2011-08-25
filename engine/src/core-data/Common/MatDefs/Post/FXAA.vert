uniform mat4 g_WorldViewProjectionMatrix;
uniform vec2 g_Resolution;
attribute vec4 inPosition;
attribute vec2 inTexCoord;
varying vec2 texCoord;
uniform float m_SubPixelShift;
varying vec4 posPos;
void main() {
    gl_Position = inPosition * 2.0 - 1.0; //vec4(pos, 0.0, 1.0);
    texCoord = inTexCoord;
    vec2 rcpFrame = vec2(1.0/g_Resolution.x, 1.0/g_Resolution.y);
    posPos.xy = inTexCoord.xy;
    posPos.zw = inTexCoord.xy -
                  (rcpFrame * (0.5 + m_SubPixelShift));
}