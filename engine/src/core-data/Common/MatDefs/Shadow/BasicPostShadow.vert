uniform mat4 m_LightViewProjectionMatrix;
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

varying vec4 projCoord;

attribute vec3 inPosition;

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);

void main(){
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);

    // get the vertex in world space
    vec4 worldPos = g_WorldMatrix * vec4(inPosition, 1.0);

    // convert vertex to light viewProj space
    //projCoord = biasMat * (m_LightViewProjectionMatrix * worldPos);
    vec4 coord = m_LightViewProjectionMatrix * worldPos;
    projCoord = biasMat * coord;
    //projCoord.z /= gl_DepthRange.far;
    //projCoord = (m_LightViewProjectionMatrix * worldPos);
    //projCoord /= projCoord.w;
    //projCoord.xy = projCoord.xy * vec2(0.5, -0.5) + vec2(0.5);

    // bias from [-1, 1] to [0, 1] for sampling shadow map
    //projCoord = (projCoord.xyzw * vec4(0.5)) + vec4(0.5);
}