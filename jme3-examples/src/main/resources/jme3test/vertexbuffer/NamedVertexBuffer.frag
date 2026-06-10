#import "Common/ShaderLib/GLSLCompat.glsllib"

varying float heat;

void main() {
    vec3 cold = vec3(0.05, 0.25, 1.0);
    vec3 hot = vec3(1.0, 0.15, 0.02);
    gl_FragColor = vec4(mix(cold, hot, clamp(heat, 0.0, 1.0)), 1.0);
}
