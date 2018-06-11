#import "Common/ShaderLib/Skinning.glsllib"

void main(){
    #ifdef NUM_BONES
        modModelPosition = modelPosition;
        modModelNormal = modelNormal;
        modModelTangents = modelTangents;
        Skinning_Compute(modModelPosition, modModelNormal, modModelTangents);
    #endif
}