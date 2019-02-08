void main(){
	startPos.xy =  (startPos * 0.5 + 0.5).xy * resolution;
	float len = distance(gl_FragCoord.xy,startPos.xy);
	outColor = inColor;
	float factor = float(int(len * 0.25));
    if(mod(factor, 2.0) > 0.0){
        discard;
    }

}
