
void main(){
        //@input vec3 position position in model space
    //@input vec3 normal the normal of the vertex
    //@input vec3 camPosition camera position in world space
    //@input mat4 worldMatrix the world view matrix
    //@output vec3 refVec the reflection vector

    vec3 worldPos = (worldMatrix * vec4(position, 1.0)).xyz;
    vec3 N = normalize((worldMatrix * vec4(normal, 0.0)).xyz);
    vec3 I = normalize( camPosition - worldPos  ).xyz;
    refVec.xyz = reflect(-I, N);

}