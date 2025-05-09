/**
*   This code is based on the following articles:
*               https://learnopengl.com/PBR/IBL/Diffuse-irradiance
*               https://learnopengl.com/PBR/IBL/Specular-IBL
*   - Riccardo Balbo
*/
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/IBL/Math.glsl"

in vec2 TexCoords;
in vec3 LocalPos;

uniform samplerCube m_EnvMap;
uniform float m_Roughness;
uniform int m_FaceId;

void brdfKernel(){
    float NdotV=TexCoords.x;
    float m_Roughness=TexCoords.y;

    vec3 V;
    V.x = sqrt(1.0 - NdotV*NdotV);
    V.y = 0.0;
    V.z = NdotV;
    float A = 0.0;
    float B = 0.0;
    vec3 N = vec3(0.0, 0.0, 1.0);
    const uint SAMPLE_COUNT = 1024u;
    for(uint i = 0u; i < SAMPLE_COUNT; i++){
        vec4 Xi = Hammersley(i, SAMPLE_COUNT);
        vec3 H  = ImportanceSampleGGX(Xi, m_Roughness, N);
        vec3 L  = normalize(2.0 * dot(V, H) * H - V);
        float NdotL = max(L.z, 0.0);
        float NdotH = max(H.z, 0.0);
        float VdotH = max(dot(V, H), 0.0);
        if(NdotL > 0.0){
            float G = GeometrySmith(N, V, L, m_Roughness*m_Roughness);
            float G_Vis = (G * VdotH) / (NdotH * NdotV);
            float Fc = pow(1.0 - VdotH, 5.0);
            A += (1.0 - Fc) * G_Vis;
            B += Fc * G_Vis;
        }
    }
    A /= float(SAMPLE_COUNT);
    B /= float(SAMPLE_COUNT);
    outFragColor.rg=vec2(A, B);
    outFragColor.ba=vec2(0);
}

void irradianceKernel(){		
    // the sample direction equals the hemisphere's orientation 
    vec3 N = normalize(LocalPos);
    vec3 irradiance = vec3(0.0); 
    vec3 up = vec3(0.0, 1.0, 0.0);
    vec3 right = cross(up, N);
    up = cross(N, right);
    float sampleDelta = 0.025;
    float nrSamples = 0.0; 
    for(float phi = 0.0; phi < 2.0 * PI; phi += sampleDelta){
        for(float theta = 0.0; theta < 0.5 * PI; theta += sampleDelta){
            // spherical to cartesian (in tangent space)
            vec3 tangentSample = vec3(sin(theta) * cos(phi),  sin(theta) * sin(phi), cos(theta));
            // tangent space to world
            vec3 sampleVec = tangentSample.x * right + tangentSample.y * up + tangentSample.z * N;
            irradiance += texture(m_EnvMap, sampleVec).rgb * cos(theta) * sin(theta);
            nrSamples++;
        }
    }
    irradiance = PI * irradiance * (1.0 / float(nrSamples));  
    outFragColor = vec4(irradiance, 1.0);
}

void prefilteredEnvKernel(){		
    vec3 N = normalize(LocalPos);    
    vec3 R = N;
    vec3 V = R;

    float a2 = m_Roughness * m_Roughness; 

    const uint SAMPLE_COUNT = 1024u;
    float totalWeight = 0.0;   
    vec3 prefilteredColor = vec3(0.0);     
    for(uint i = 0u; i < SAMPLE_COUNT; ++i) {
        vec4 Xi = Hammersley(i, SAMPLE_COUNT);
        vec3 H  = ImportanceSampleGGX(Xi, a2, N);
        float VoH = max(dot(V, H), 0.0);
        vec3 L  = normalize(2.0 * VoH * H - V);
        float NdotL = max(dot(N, L), 0.0);
        if(NdotL > 0.0) {
            vec3 sampleColor = texture(m_EnvMap, L).rgb;
            
            float luminance = dot(sampleColor, vec3(0.2126, 0.7152, 0.0722));
            if (luminance > 64.0) { // TODO use average?
                sampleColor *= 64.0/luminance;
            }
            
            // TODO: use mipmap
            prefilteredColor += sampleColor * NdotL;
            totalWeight      += NdotL;
        }

    }

    if(totalWeight > 0.001){
         prefilteredColor /= totalWeight;    
    }

    outFragColor = vec4(prefilteredColor, 1.0);
}  

void main(){ 
    #if defined(SIBL)
        prefilteredEnvKernel();
    #elif defined(IRRADIANCE)
        irradianceKernel();
    #else
        brdfKernel();
    #endif
}