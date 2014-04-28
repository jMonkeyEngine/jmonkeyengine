
void main(){
        modModelPosition = (mat4(0.0) +
            boneMatrices[int(boneIndex.x)] * boneWeight.x +
            boneMatrices[int(boneIndex.y)] * boneWeight.y +
            boneMatrices[int(boneIndex.z)] * boneWeight.z +
            boneMatrices[int(boneIndex.w)] * boneWeight.w) * modelPosition;

        mat3 rotMat = mat3(mat[0].xyz, mat[1].xyz, mat[2].xyz);
        modModelTangent = rotMat * modelTangent;
        modModelNormal = rotMat * modelNormal;
}