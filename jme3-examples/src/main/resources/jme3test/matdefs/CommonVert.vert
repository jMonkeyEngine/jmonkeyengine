#pragma ShaderNode
vec4 CommonVert(const in mat4 worldViewProjectionMatrix, const in vec3 modelPosition){
    return worldViewProjectionMatrix * vec4(modelPosition, 1.0);
}

#pragma ShaderNode
vec4 DoThing( in vec4 color ){
    return color * 0.1;
}