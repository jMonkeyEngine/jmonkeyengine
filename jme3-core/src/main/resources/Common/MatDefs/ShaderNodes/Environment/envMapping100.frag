#extension GL_ARB_shader_texture_lod : enable
#extension GL_EXT_shader_texture_lod : enable
void main(){
        //@input vec3 refVec the reflection vector
    //@input samplerCube cubeMap the cube map
    //@output vec4 color the output color

    #ifdef GL_ES
        #ifdef GL_EXT_shader_texture_lod
            color = textureCubeLodEXT(cubeMap, refVec, 0.0);
        #else
            color = textureCube(cubeMap, refVec);
        #endif
    #else
        color = textureCubeLod(cubeMap, refVec, 0.0);
    #endif
}
