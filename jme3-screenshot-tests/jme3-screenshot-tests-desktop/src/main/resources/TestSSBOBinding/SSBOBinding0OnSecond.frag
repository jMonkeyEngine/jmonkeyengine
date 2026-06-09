// Test: second block has explicit layout(binding=0).
// This exposes the ambiguity: query returns 0 for both the first block
// (no binding, default 0) and the second block (explicit binding=0).
// The fix reassigns binding=0 to blockIndex when blockIndex != 0,
// which incorrectly overrides the explicit binding=0 on GreenBlock.

layout(std430) buffer m_RedBlock {
    vec4 redColor;
};

layout(std430, binding=0) buffer m_GreenBlock {
    vec4 greenColor;
};

layout(std430) buffer m_BlueBlock {
    vec4 blueColor;
};

out vec4 fragColor;

void main(){
    fragColor = vec4(redColor.r, greenColor.g, blueColor.b, 1.0);
}
