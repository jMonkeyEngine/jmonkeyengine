#pragma ShaderNode defaults(vec4(1.0), ,vec4(1.0))
vec4 ColorMult(const in vec4 color1, const in vec4 color2, const in vec4 color3){
    return color1 * color2 * color3;
}

