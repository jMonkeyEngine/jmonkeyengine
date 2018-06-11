#import "Common/ShaderLib/Skinning.glsllib"

void main(){
    #ifdef NUM_BONES
        modModelPosition = modelPosition;
        Skinning_Compute(modModelPosition);
    #endif
}