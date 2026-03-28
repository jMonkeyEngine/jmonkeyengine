// Test: mixed explicit and implicit bindings, all non-zero explicit.
// RedBlock has binding=1, GreenBlock has binding=2, BlueBlock has none.
// Non-zero queries are respected; BlueBlock gets assigned its blockIndex.

layout(std430, binding=1) buffer m_RedBlock {
    vec4 redColor;
};

layout(std430, binding=2) buffer m_GreenBlock {
    vec4 greenColor;
};

layout(std430) buffer m_BlueBlock {
    vec4 blueColor;
};

out vec4 fragColor;

void main(){
    fragColor = vec4(redColor.r, greenColor.g, blueColor.b, 1.0);
}
