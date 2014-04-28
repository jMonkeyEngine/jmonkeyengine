void main(){
    #ifdef colorMap
        color *= texture2D(colorMap, texCoord);
    #endif

    #ifdef vertColor
        color *= vertColor;
    #endif

    #ifdef matColor
        color *= matColor;
    #endif

}