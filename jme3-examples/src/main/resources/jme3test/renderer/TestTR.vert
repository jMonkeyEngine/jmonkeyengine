//uniform mat4 g_WorldViewProjectionMatrix;
//out vec4 TransformFeedback;

uniform vec3 m_CamPos;
uniform vec3 m_CamDir;

attribute mat4 inPosition; 

flat out mat4 instanceData;
flat out float visible;

//cone pos, cone dir norm, rad at dist 1
float coneInPoint(vec3 cP, vec3 dN, float rad, vec3 pos) {
	float d = dot(pos-cP,dN);
	float cRad = d * rad;
	vec3 a2 = pos-(d*dN+cP);
	float rSq = dot(a2,a2);
	//0.0 <= d
	//rSq <= cRad*cRad;
	return step(0.0,d) * step(rSq, cRad*cRad);
}

void main() {
	//simple cone, point frustrum checking
	//not finished/polished, just for test case
	visible = coneInPoint(m_CamPos, m_CamDir, 1.0, inPosition[3].xyz);
	
	instanceData = inPosition;
	
}