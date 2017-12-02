in vec4 inPosition;

in vec2 inTexCoord;   // m_inUVred
in vec2 inTexCoord2;  // m_inUVgreen
in vec2 inTexCoord3;  // m_inUVblue

out vec2 UVred;
out vec2 UVgreen;
out vec2 UVblue;

void main() {     
    gl_Position = inPosition;
    UVred = inTexCoord;
    UVgreen = inTexCoord2;
    UVblue = inTexCoord3;
}