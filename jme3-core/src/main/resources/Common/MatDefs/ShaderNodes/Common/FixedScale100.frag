void main(){
	vec4 worldPos = worldMatrix * vec4(0.0, 0.0, 0.0, 1.0);
	vec3 dir = worldPos.xyz - cameraPos;
	float distance = dot(cameraDir, dir);
	float m11 = projectionMatrix[1][1];
	float halfHeight = (viewport.w - viewport.y) * 0.5;	
	scale = ((distance/halfHeight) * spriteHeight)/m11;
}
