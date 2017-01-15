#import "Common/ShaderLib/GLSLCompat.glsllib"
varying vec3 normal;

void main(){
   gl_FragColor = vec4((normal * vec3(0.5)) + vec3(0.5), 1.0);
}