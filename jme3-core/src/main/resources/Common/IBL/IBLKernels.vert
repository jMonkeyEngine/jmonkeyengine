#import "Common/ShaderLib/GLSLCompat.glsllib"

/**
*   This code is based on the following articles:
*               https://learnopengl.com/PBR/IBL/Diffuse-irradiance
*               https://learnopengl.com/PBR/IBL/Specular-IBL
*   - Riccardo Balbo
*/
in vec3 inPosition;
in vec2 inTexCoord;
in vec3 inNormal;

out vec2 TexCoords;
out vec3 LocalPos;

uniform mat4 g_ViewMatrix;
uniform mat4 g_WorldMatrix;
uniform mat4 g_ProjectionMatrix;

void main() {
    LocalPos = inPosition.xyz;  
    TexCoords = inTexCoord.xy;
    #ifdef BRDF
        vec2 pos = inPosition.xy * 2.0 - 1.0;      
        gl_Position = vec4(pos, 0.0, 1.0);
    #else       
        mat4 rotView = mat4(mat3(g_ViewMatrix)); // remove translation from the view matrix
        vec4 clipPos = g_ProjectionMatrix * rotView * vec4(LocalPos, 1.0);
        gl_Position = clipPos.xyww;
    #endif
}