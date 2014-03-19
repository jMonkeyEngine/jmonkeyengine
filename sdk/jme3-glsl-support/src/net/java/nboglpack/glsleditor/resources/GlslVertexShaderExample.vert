// ------ Vertex shader ------ //

#pragma debug(on)

uniform vec3 LightPosition;
const float SpecularContribution = 0.3;
varying float LightIntensity;

/*
* main function
*/
void main()  {

    vec3 ecPosition = vec3 (gl_ModelViewMatrix * gl_Vertex);
    vec3 tnorm      = normalize(gl_NormalMatrix * gl_Normal);
    vec3 lightVec   = normalize(LightPosition - ecPosition);
    vec3 reflectVec = reflect(-lightVec, tnorm);
    vec3 viewVec    = normalize(-ecPosition);
    float diffuse   = max(dot(lightVec, tnorm), 0.0);
    float spec      = 0.0;

    if (diffuse > 0.0)    {
        spec = max(dot(reflectVec, viewVec), 0.0);
        spec = pow(spec, 16.0);
    }

    LightIntensity  = 0.7 * diffuse + SpecularContribution * spec;
    gl_Position     = ftransform();
}
