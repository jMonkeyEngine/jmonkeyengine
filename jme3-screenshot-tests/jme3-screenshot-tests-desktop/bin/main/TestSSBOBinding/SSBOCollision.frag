// Test: collision scenario — demonstrates the binding bug.
// GreenBlock has no binding (blockIndex=1, query=0, reassigned to 1).
// BlueBlock has explicit binding=1 (query=1, kept at 1).
// Both end up at binding point 1: the last buffer bound wins,
// so GreenBlock reads BlueBlock's data and green is lost.

layout(std430) buffer m_RedBlock {
    vec4 redColor;
};

layout(std430) buffer m_GreenBlock {
    vec4 greenColor;
};

layout(std430, binding=1) buffer m_BlueBlock {
    vec4 blueColor;
};

out vec4 fragColor;

void main(){
    fragColor = vec4(redColor.r, greenColor.g, blueColor.b, 1.0);
}
