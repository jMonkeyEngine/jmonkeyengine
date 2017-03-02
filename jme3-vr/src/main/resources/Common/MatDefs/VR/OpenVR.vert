attribute vec4 inPosition;

attribute vec2 inTexCoord;   // m_inUVred
attribute vec2 inTexCoord2;  // m_inUVgreen
attribute vec2 inTexCoord3;  // m_inUVblue

varying vec2 UVred;
varying vec2 UVgreen;
varying vec2 UVblue;

void main() {     
    gl_Position = inPosition;
    UVred = inTexCoord;
    UVgreen = inTexCoord2;
    UVblue = inTexCoord3;
}