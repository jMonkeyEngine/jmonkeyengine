
void main(){
        //@input vec2 texCoord The texture coordinates
    //@input float size the size of the dashes
    //@output vec4 color the output color

    //insert glsl code here
    outColor = inColor;
    outColor.a = step(1.0 - size, texCoord.x);
    
}