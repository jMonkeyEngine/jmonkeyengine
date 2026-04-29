/**
*   This code is based on the following articles:
*               https://learnopengl.com/PBR/IBL/Diffuse-irradiance
*               https://learnopengl.com/PBR/IBL/Specular-IBL
*   - Riccardo Balbo
*/
const float PI = 3.14159265359;

float RadicalInverse_VdC(uint bits) {
    bits = (bits << 16u) | (bits >> 16u);
    bits = ((bits & 0x55555555u) << 1u) | ((bits & 0xAAAAAAAAu) >> 1u);
    bits = ((bits & 0x33333333u) << 2u) | ((bits & 0xCCCCCCCCu) >> 2u);
    bits = ((bits & 0x0F0F0F0Fu) << 4u) | ((bits & 0xF0F0F0F0u) >> 4u);
    bits = ((bits & 0x00FF00FFu) << 8u) | ((bits & 0xFF00FF00u) >> 8u);
    return float(bits) * 2.3283064365386963e-10; // / 0x100000000
}

vec4 Hammersley(uint i, uint N){
    vec4 store=vec4(0);
    store.x = float(i) / float(N);
    store.y = RadicalInverse_VdC(i);
    
    float phi = 2.0 * PI *store.x;
    store.z = cos(phi);
    store.w = sin(phi);

    return store;
} 
 
// float VanDerCorput(uint n, uint base){
//     float invBase = 1.0 / float(base);
//     float denom   = 1.0;
//     float result  = 0.0;

//     for(uint i = 0u; i < 32u; ++i)
//     {
//         if(n > 0u)
//         {
//             denom   = mod(float(n), 2.0);
//             result += denom * invBase;
//             invBase = invBase / 2.0;
//             n       = uint(float(n) / 2.0);
//         }
//     }

//     return result;
// }

// vec2 Hammersley(uint i, uint N){
//     return vec2(float(i)/float(N), VanDerCorput(i, 2u));
// }


// Shared roughness convention for the IBL bake path:
// roughness = perceptual roughness in [0, 1]
// alpha = roughness * roughness
// alpha2 = alpha * alpha
//
// ImportanceSampleGGX() and GeometrySmith() both expect alpha.
const float MIN_GGX_ALPHA = 0.0064;

float SafeGGXAlpha(float alpha) {
    return max(alpha, MIN_GGX_ALPHA);
}

vec3 ImportanceSampleGGX(vec4 Xi, float alpha, vec3 N){
    alpha = SafeGGXAlpha(alpha);
    float alpha2 = alpha * alpha;
    float cosTheta = sqrt((1.0 - Xi.y) / (1.0 + (alpha2 - 1.0) * Xi.y));
    float sinTheta = sqrt(1.0 - cosTheta*cosTheta);

    // from spherical coordinates to cartesian coordinates
    vec3 H;
    H.x = Xi.z * sinTheta;
    H.y = Xi.w * sinTheta;
    H.z = cosTheta;

    // from tangent-space vector to world-space sample vector
    vec3 up        = abs(N.z) < 0.999 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent   = normalize(cross(up, N));
    vec3 bitangent = cross(N, tangent);

    vec3 sampleVec = tangent * H.x + bitangent * H.y + N * H.z;
    return normalize(sampleVec);
}

float DistributionGGX(float NdotH, float alpha) {
    alpha = SafeGGXAlpha(alpha);
    float alpha2 = alpha * alpha;
    float denom = (NdotH * NdotH) * (alpha2 - 1.0) + 1.0;
    return alpha2 / (PI * denom * denom);
}

float ImportanceSampleGGXPdf(float NdotH, float VdotH, float alpha) {
    float D = DistributionGGX(NdotH, alpha);
    return max((D * NdotH) / max(4.0 * VdotH, 1e-4), 0.0);
}

float GeometrySchlickGGX(float NdotV, float alpha){
    alpha = SafeGGXAlpha(alpha);
    float k = alpha / 2.0;

    float nom   = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return nom / denom;
}
// ----------------------------------------------------------------------------
float GeometrySmith(vec3 N, vec3 V, vec3 L, float alpha){
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, alpha);
    float ggx1 = GeometrySchlickGGX(NdotL, alpha);

    return ggx1 * ggx2;
}
    
