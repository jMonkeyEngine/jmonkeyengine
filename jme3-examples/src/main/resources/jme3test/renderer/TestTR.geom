layout(points) in;
layout(points, max_vertices = 1) out;


flat in mat4 instanceData[1];
flat in float visible[1];

//layout(xfb_offset = 0,xfb_buffer = 0)
out mat4 TransformFeedback;

void main() {
   if(visible[0] == 1.0) {
      TransformFeedback = instanceData[0];
	 // gl_Position = gl_in[0].gl_Position; 
      EmitVertex();
      EndPrimitive();
   }
}