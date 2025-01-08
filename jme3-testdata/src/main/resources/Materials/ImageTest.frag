
#import "Common/ShaderLib/GLSLCompat.glsllib"

layout(RGBA8) uniform image2D m_TargetImage;

void main() {
    
    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
    imageStore(m_TargetImage, ivec2(gl_FragCoord.xy), vec4(0.0, 1.0, 0.0, 1.0));
    
}
