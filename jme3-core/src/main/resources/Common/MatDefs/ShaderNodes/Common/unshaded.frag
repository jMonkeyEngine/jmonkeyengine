#if __VERSION__ >= 130
#define texture2D texture
#endif

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