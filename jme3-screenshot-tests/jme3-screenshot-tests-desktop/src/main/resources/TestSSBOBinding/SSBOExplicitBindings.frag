// Test: all blocks have explicit non-zero bindings.
// Query returns non-zero for all, so all bindings are respected as-is.

layout(std430, binding=1) buffer m_RedBlock {
    vec4 redColor;
};

layout(std430, binding=2) buffer m_GreenBlock {
    vec4 greenColor;
};

layout(std430, binding=3) buffer m_BlueBlock {
    vec4 blueColor;
};

out vec4 fragColor;

void main(){
    fragColor = vec4(redColor.r, greenColor.g, blueColor.b, 1.0);
}
