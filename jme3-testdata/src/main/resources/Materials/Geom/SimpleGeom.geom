layout (points) in;
layout (line_strip) out;
layout (max_vertices = 11) out;

uniform mat4 g_WorldViewProjectionMatrix;
const float PI = 3.1415926;
void main(){
    for (int i = 0; i <= 10; i++) {

            float ang = PI * 2.0 / 10.0 * float(i);
            vec4 offset = vec4(cos(ang) * 5.0, -sin(ang) * 5.0, 0.0, 0.0);
            gl_Position = g_WorldViewProjectionMatrix*vec4(gl_in[0].gl_Position.xyz + offset.xyz,1.0);

            EmitVertex();
        }

    EndPrimitive();
}

