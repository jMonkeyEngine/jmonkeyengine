#extension GL_ARB_shader_texture_lod : enable
void main(){
        //@input vec3 refVec the reflection vector
    //@input samplerCube cubeMap the cube map
    //@output vec4 color the output color

    color = textureCubeLod(cubeMap, refVec, 0.0);
}