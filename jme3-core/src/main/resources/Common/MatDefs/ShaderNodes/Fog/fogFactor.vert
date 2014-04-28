const float LOG2 = 1.442695;
void main(){ 
        vec4 viewSpacePos = modelViewMatrix * modelPosition;
        fogFactor = exp2(-fogDensity * fogDensity * viewSpacePos.z *  viewSpacePos.z * LOG2 );
        fogFactor = clamp(fogFactor, 0.0, 1.0);
}