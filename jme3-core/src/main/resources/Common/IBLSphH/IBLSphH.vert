/**
*- Riccardo Balbo
*/
in vec3 inPosition;
in vec2 inTexCoord;

out vec2 TexCoords;
out vec3 LocalPos;


void main() {
    LocalPos = inPosition.xyz;
    TexCoords = inTexCoord.xy;
    vec2 pos = inPosition.xy * 2.0 - 1.0;
    gl_Position = vec4(pos, 0.0, 1.0);  
}