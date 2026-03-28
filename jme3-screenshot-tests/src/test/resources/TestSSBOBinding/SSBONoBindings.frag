// Test: no explicit binding on any block.
// All blocks get unique blockIndex values, query returns 0 for all,
// and each is assigned its blockIndex as binding point. No collisions.

layout(std430) buffer m_RedBlock {
    vec4 redColor;
};

layout(std430) buffer m_GreenBlock {
    vec4 greenColor;
};

layout(std430) buffer m_BlueBlock {
    vec4 blueColor;
};

out vec4 fragColor;

void main(){
    fragColor = vec4(redColor.r, greenColor.g, blueColor.b, 1.0);
}
