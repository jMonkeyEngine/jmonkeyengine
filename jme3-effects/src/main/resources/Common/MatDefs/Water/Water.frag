#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"
#import "Common/ShaderLib/WaterUtil.glsllib"

// Water pixel shader
// Copyright (C) JMonkeyEngine 3.0
// by Remy Bouquet (nehon) for JMonkeyEngine 3.0
// original HLSL version by Wojciech Toman 2009

uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;


uniform sampler2D m_HeightMap;
uniform sampler2D m_NormalMap;
uniform sampler2D m_FoamMap;
uniform sampler2D m_CausticsMap;
uniform sampler2D m_ReflectionMap;

uniform mat4 g_ViewProjectionMatrixInverse;
uniform mat4 m_TextureProjMatrix;
uniform vec3 m_CameraPosition;

uniform float m_WaterHeight;
uniform float m_Time;
uniform float m_WaterTransparency;
uniform float m_NormalScale;
uniform float m_R0;
uniform float m_MaxAmplitude;
uniform vec3 m_LightDir;
uniform vec4 m_LightColor;
uniform float m_ShoreHardness;
uniform float m_FoamHardness;
uniform float m_RefractionStrength;
uniform vec3 m_FoamExistence;
uniform vec3 m_ColorExtinction;
uniform float m_Shininess;
uniform vec4 m_WaterColor;
uniform vec4 m_DeepWaterColor;
uniform vec2 m_WindDirection;
uniform float m_SunScale;
uniform float m_WaveScale;
uniform float m_UnderWaterFogDistance;
uniform float m_CausticsIntensity;

#ifdef ENABLE_AREA
uniform vec3 m_Center;
uniform float m_Radius;
#endif


vec2 scale = vec2(m_WaveScale, m_WaveScale);
float refractionScale = m_WaveScale;

// Modifies 4 sampled normals. Increase first values to have more
// smaller "waves" or last to have more bigger "waves"
const vec4 normalModifier = vec4(3.0, 2.0, 4.0, 10.0);
// Strength of displacement along normal.
uniform float m_ReflectionDisplace;
// Water transparency along eye vector.
const float visibility = 3.0;
// foam intensity
uniform float m_FoamIntensity ;

varying vec2 texCoord;

mat3 MatrixInverse(in mat3 inMatrix){
    float det = dot(cross(inMatrix[0], inMatrix[1]), inMatrix[2]);
    mat3 T = transpose(inMatrix);
    return mat3(cross(T[1], T[2]),
        cross(T[2], T[0]),
        cross(T[0], T[1])) / det;
}


mat3 computeTangentFrame(in vec3 N, in vec3 P, in vec2 UV) {
    vec3 dp1 = dFdx(P);
    vec3 dp2 = dFdy(P);
    vec2 duv1 = dFdx(UV);
    vec2 duv2 = dFdy(UV);

    // solve the linear system
    vec3 dp1xdp2 = cross(dp1, dp2);
    mat2x3 inverseM = mat2x3(cross(dp2, dp1xdp2), cross(dp1xdp2, dp1));

    vec3 T = inverseM * vec2(duv1.x, duv2.x);
    vec3 B = inverseM * vec2(duv1.y, duv2.y);

    // construct tangent frame
    float maxLength = max(length(T), length(B));
    T = T / maxLength;
    B = B / maxLength;

    return mat3(T, B, N);
}

float saturate(in float val){
    return clamp(val,0.0,1.0);
}

vec3 saturate(in vec3 val){
    return clamp(val,vec3(0.0),vec3(1.0));
}

vec3 getPosition(in float depth, in vec2 uv){
    vec4 pos = vec4(uv, depth, 1.0) * 2.0 - 1.0;
    pos = g_ViewProjectionMatrixInverse * pos;
    return pos.xyz / pos.w;
}

// Function calculating fresnel term.
// - normal - normalized normal vector
// - eyeVec - normalized eye vector
float fresnelTerm(in vec3 normal,in vec3 eyeVec){
    float angle = 1.0 - max(0.0, dot(normal, eyeVec));
    float fresnel = angle * angle;
    fresnel = fresnel * fresnel;
    fresnel = fresnel * angle;
    return saturate(fresnel * (1.0 - saturate(m_R0)) + m_R0 - m_RefractionStrength);
}

vec2 m_FrustumNearFar=vec2(1.0,m_UnderWaterFogDistance);
const float LOG2 = 1.442695;

vec4 underWater(int sampleNum){


    float sceneDepth = fetchTextureSample(m_DepthTexture, texCoord, sampleNum).r;
    vec3 color2 = fetchTextureSample(m_Texture, texCoord, sampleNum).rgb;
    
    vec3 position = getPosition(sceneDepth, texCoord);  
    float level = m_WaterHeight;

    vec3 eyeVec = position - m_CameraPosition;    
 
    // Find intersection with water surface
    vec3 eyeVecNorm = normalize(eyeVec);
    float t = (level - m_CameraPosition.y) / eyeVecNorm.y;
    vec3 surfacePoint = m_CameraPosition + eyeVecNorm * t;

    vec2 texC = vec2(0.0);

    float cameraDepth = length(m_CameraPosition - surfacePoint);  
    texC = (surfacePoint.xz + eyeVecNorm.xz) * scale + m_Time * 0.03 * m_WindDirection;
    float bias = texture2D(m_HeightMap, texC).r;
    level += bias * m_MaxAmplitude;
    t = (level - m_CameraPosition.y) / eyeVecNorm.y;
    surfacePoint = m_CameraPosition + eyeVecNorm * t; 
    eyeVecNorm = normalize(m_CameraPosition - surfacePoint);

    #if __VERSION__ >= 130
        // Find normal of water surface
        float normal1 = textureOffset(m_HeightMap, texC, ivec2(-1.0,  0.0)).r;
        float normal2 = textureOffset(m_HeightMap, texC, ivec2( 1.0,  0.0)).r;
        float normal3 = textureOffset(m_HeightMap, texC, ivec2( 0.0, -1.0)).r;
        float normal4 = textureOffset(m_HeightMap, texC, ivec2( 0.0,  1.0)).r;
    #else
        // Find normal of water surface
        float normal1 = texture2D(m_HeightMap, (texC + vec2(-1.0, 0.0) / 256.0)).r;
        float normal2 = texture2D(m_HeightMap, (texC + vec2(1.0, 0.0) / 256.0)).r;
        float normal3 = texture2D(m_HeightMap, (texC + vec2(0.0, -1.0) / 256.0)).r;
        float normal4 = texture2D(m_HeightMap, (texC + vec2(0.0, 1.0) / 256.0)).r;
    #endif

    vec3 myNormal = normalize(vec3((normal1 - normal2) * m_MaxAmplitude,m_NormalScale,(normal3 - normal4) * m_MaxAmplitude));
    vec3 normal = myNormal*-1.0;
    float fresnel = fresnelTerm(normal, eyeVecNorm); 

    vec3 refraction = color2;
    #ifdef ENABLE_REFRACTION
        texC = texCoord.xy *sin (fresnel+1.0);
        texC = clamp(texC,0.0,1.0);
        refraction = fetchTextureSample(m_Texture, texC, sampleNum).rgb;
    #endif

   float waterCol = saturate(length(m_LightColor.rgb) / m_SunScale);
   refraction = mix(mix(refraction, m_DeepWaterColor.rgb * waterCol, m_WaterTransparency),  m_WaterColor.rgb* waterCol,m_WaterTransparency);

    vec3 foam = vec3(0.0);
    #ifdef ENABLE_FOAM    
        texC = (surfacePoint.xz + eyeVecNorm.xz * 0.1) * 0.05 + m_Time * 0.05 * m_WindDirection + sin(m_Time * 0.001 + position.x) * 0.005;
        vec2 texCoord2 = (surfacePoint.xz + eyeVecNorm.xz * 0.1) * 0.05 + m_Time * 0.1 * m_WindDirection + sin(m_Time * 0.001 + position.z) * 0.005;

        if(m_MaxAmplitude - m_FoamExistence.z> 0.0001){
            foam += ((texture2D(m_FoamMap, texC) + texture2D(m_FoamMap, texCoord2)) * m_FoamIntensity  * m_FoamIntensity * 0.3 *
               saturate((level - (m_WaterHeight + m_FoamExistence.z)) / (m_MaxAmplitude - m_FoamExistence.z))).rgb;
        }
        foam *= m_LightColor.rgb;    
    #endif



    vec3 specular = vec3(0.0);   
    vec3 color ;
    float fogFactor;

    if(position.y>level){
        #ifdef ENABLE_SPECULAR
            if(step(0.9999,sceneDepth)==1.0){
                vec3 lightDir=normalize(m_LightDir);
                vec3 mirrorEye = (2.0 * dot(eyeVecNorm, normal) * normal - eyeVecNorm);
                float dotSpec = saturate(dot(mirrorEye.xyz, -lightDir) * 0.5 + 0.5);
                specular = vec3((1.0 - fresnel) * saturate(-lightDir.y) * ((pow(dotSpec, 512.0)) * (m_Shininess * 1.8 + 0.2)));
                specular += specular * 25.0 * saturate(m_Shininess - 0.05);
                specular=specular * m_LightColor.rgb * 100.0;
            }
        #endif
        float fogIntensity= 8.0 * m_WaterTransparency;
        fogFactor = exp2( -fogIntensity * fogIntensity * cameraDepth * 0.03 * LOG2 );
        fogFactor = clamp(fogFactor, 0.0, 1.0);        
        color =mix(m_DeepWaterColor.rgb,refraction,fogFactor);   
        specular=specular*fogFactor;    
        color = saturate(color + max(specular, foam ));
    }else{
        vec3 caustics = vec3(0.0);
        #ifdef ENABLE_CAUSTICS 
            vec2 windDirection=m_WindDirection;
            texC = (position.xz + eyeVecNorm.xz * 0.1) * 0.05 + m_Time * 0.05 * windDirection + sin(m_Time  + position.x) * 0.01;
            vec2 texCoord2 = (position.xz + eyeVecNorm.xz * 0.1) * 0.05 + m_Time * 0.05 * windDirection + sin(m_Time  + position.z) * 0.01;
            caustics += (texture2D(m_CausticsMap, texC)+ texture2D(m_CausticsMap, texCoord2)).rgb;                  
            caustics=saturate(mix(m_WaterColor.rgb,caustics,m_CausticsIntensity));            
            color=mix(color2,caustics,m_CausticsIntensity);
        #else
            color=color2;
        #endif
                
        float fogDepth= (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - sceneDepth* (m_FrustumNearFar.y-m_FrustumNearFar.x));
        float fogIntensity= 18 * m_WaterTransparency;
        fogFactor = exp2( -fogIntensity * fogIntensity * fogDepth *  fogDepth * LOG2 );
        fogFactor = clamp(fogFactor, 0.0, 1.0);
        color =mix(m_DeepWaterColor.rgb,color,fogFactor);
    }

    return vec4(color, 1.0);   
}


// NOTE: This will be called even for single-sampling
vec4 main_multiSample(int sampleNum){
    // If we are underwater let's call the underwater function
    if(m_WaterHeight >= m_CameraPosition.y){
        #ifdef ENABLE_AREA      
            if(isOverExtent(m_CameraPosition, m_Center, m_Radius)){            
                return fetchTextureSample(m_Texture, texCoord, sampleNum);
            }   
        #endif
        return underWater(sampleNum);
    }

    float sceneDepth = fetchTextureSample(m_DepthTexture, texCoord, sampleNum).r;
    vec3 color2 = fetchTextureSample(m_Texture, texCoord, sampleNum).rgb;

    vec3 color = color2;
    vec3 position = getPosition(sceneDepth, texCoord);

    #ifdef ENABLE_AREA               
        if(isOverExtent(position, m_Center, m_Radius)){         
            return vec4(color2, 1.0);
        }
    #endif

    float level = m_WaterHeight;
    
    float isAtFarPlane = step(0.99998, sceneDepth);
    //#ifndef ENABLE_RIPPLES
    // This optimization won't work on NVIDIA cards if ripples are enabled
    if(position.y > level + m_MaxAmplitude + isAtFarPlane * 100.0){

        return vec4(color2, 1.0);
    }
    //#endif

    vec3 eyeVec = position - m_CameraPosition;    
    float cameraDepth = m_CameraPosition.y - position.y;

    // Find intersection with water surface
    vec3 eyeVecNorm = normalize(eyeVec);
    float t = (level - m_CameraPosition.y) / eyeVecNorm.y;
    vec3 surfacePoint = m_CameraPosition + eyeVecNorm * t;

    vec2 texC = vec2(0.0);
    int samples = 1;
    #ifdef ENABLE_HQ_SHORELINE
        samples = 10;
    #endif

    float biasFactor = 1.0 / samples;
    for (int i = 0; i < samples; i++){
        texC = (surfacePoint.xz + eyeVecNorm.xz * biasFactor) * scale + m_Time * 0.03 * m_WindDirection;

        float bias = texture2D(m_HeightMap, texC).r;

        bias *= biasFactor;
        level += bias * m_MaxAmplitude;
        t = (level - m_CameraPosition.y) / eyeVecNorm.y;
        surfacePoint = m_CameraPosition + eyeVecNorm * t;
    }

    float depth = length(position - surfacePoint);
    float depth2 = surfacePoint.y - position.y;

    // XXX: HACK ALERT: Increase water depth to infinity if at far plane
    // Prevents "foam on horizon" issue
    // For best results, replace the "100.0" below with the
    // highest value in the m_ColorExtinction vec3
    depth  += isAtFarPlane * 100.0;
    depth2 += isAtFarPlane * 100.0;

    eyeVecNorm = normalize(m_CameraPosition - surfacePoint);

    #if __VERSION__ >= 130
        // Find normal of water surface
        float normal1 = textureOffset(m_HeightMap, texC, ivec2(-1.0,  0.0)).r;
        float normal2 = textureOffset(m_HeightMap, texC, ivec2( 1.0,  0.0)).r;
        float normal3 = textureOffset(m_HeightMap, texC, ivec2( 0.0, -1.0)).r;
        float normal4 = textureOffset(m_HeightMap, texC, ivec2( 0.0,  1.0)).r;
    #else
        // Find normal of water surface
        float normal1 = texture2D(m_HeightMap, (texC + vec2(-1.0, 0.0) / 256.0)).r;
        float normal2 = texture2D(m_HeightMap, (texC + vec2(1.0, 0.0) / 256.0)).r;
        float normal3 = texture2D(m_HeightMap, (texC + vec2(0.0, -1.0) / 256.0)).r;
        float normal4 = texture2D(m_HeightMap, (texC + vec2(0.0, 1.0) / 256.0)).r;
    #endif

    vec3 myNormal = normalize(vec3((normal1 - normal2) * m_MaxAmplitude,m_NormalScale,(normal3 - normal4) * m_MaxAmplitude));
    vec3 normal = vec3(0.0);

    #ifdef ENABLE_RIPPLES
        texC = surfacePoint.xz * 0.8 + m_WindDirection * m_Time* 1.6;
        mat3 tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texC);
        vec3 normal0a = normalize(tangentFrame*(2.0 * texture2D(m_NormalMap, texC).xyz - 1.0));

        texC = surfacePoint.xz * 0.4 + m_WindDirection * m_Time* 0.8;
        tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texC);
        vec3 normal1a = normalize(tangentFrame*(2.0 * texture2D(m_NormalMap, texC).xyz - 1.0));

        texC = surfacePoint.xz * 0.2 + m_WindDirection * m_Time * 0.4;
        tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texC);
        vec3 normal2a = normalize(tangentFrame*(2.0 * texture2D(m_NormalMap, texC).xyz - 1.0));

        texC = surfacePoint.xz * 0.1 + m_WindDirection * m_Time * 0.2;
        tangentFrame = computeTangentFrame(myNormal, eyeVecNorm, texC);
        vec3 normal3a = normalize(tangentFrame*(2.0 * texture2D(m_NormalMap, texC).xyz - 1.0));

        normal = normalize(normal0a * normalModifier.x + normal1a * normalModifier.y +normal2a * normalModifier.z + normal3a * normalModifier.w);

         #if __VERSION__ >= 130
            // XXX: Here's another way to fix the terrain edge issue,
            // But it requires GLSL 1.3 and still looks kinda incorrect
            // around edges
            normal = isnan(normal.x) ? myNormal : normal;
        #else
            // To make the shader 1.2 compatible we use a trick :
            // we clamp the x value of the normal and compare it to it's former value instead of using isnan.
            normal = clamp(normal.x,0.0,1.0)!=normal.x ? myNormal : normal;
        #endif

    #else
        normal = myNormal;
    #endif

    vec3 refraction = color2;
    #ifdef ENABLE_REFRACTION
       // texC = texCoord.xy+ m_ReflectionDisplace * normal.x;
        texC = texCoord.xy;
        texC += sin(m_Time*1.8 + 3.0 * abs(position.y))* (refractionScale * min(depth2, 1.0));
        texC = clamp(texC,vec2(0.0),vec2(0.999));
        refraction = fetchTextureSample(m_Texture, texC, sampleNum).rgb;
    #endif
    vec3 waterPosition = surfacePoint.xyz;
    waterPosition.y -= (level - m_WaterHeight);
    vec4 texCoordProj = m_TextureProjMatrix * vec4(waterPosition, 1.0);

    texCoordProj.x = texCoordProj.x + m_ReflectionDisplace * normal.x;
    texCoordProj.z = texCoordProj.z + m_ReflectionDisplace * normal.z;
    texCoordProj /= texCoordProj.w;
    texCoordProj.y = 1.0 - texCoordProj.y;

    vec3 reflection = texture2D(m_ReflectionMap, texCoordProj.xy).rgb;

    float fresnel = fresnelTerm(normal, eyeVecNorm);

    float depthN = depth * m_WaterTransparency;
    float waterCol = saturate(length(m_LightColor.rgb) / m_SunScale);
    refraction = mix(mix(refraction, m_WaterColor.rgb * waterCol, saturate(depthN / visibility)),
        m_DeepWaterColor.rgb * waterCol, saturate(depth2 / m_ColorExtinction));


    vec3 foam = vec3(0.0);
    #ifdef ENABLE_FOAM
        texC = (surfacePoint.xz + eyeVecNorm.xz * 0.1) * 0.05 + m_Time * 0.05 * m_WindDirection + sin(m_Time * 0.001 + position.x) * 0.005;
        vec2 texCoord2 = (surfacePoint.xz + eyeVecNorm.xz * 0.1) * 0.05 + m_Time * 0.1 * m_WindDirection + sin(m_Time * 0.001 + position.z) * 0.005;

        vec4 foam1 = texture2D(m_FoamMap, texC);
        vec4 foam2 = texture2D(m_FoamMap, texCoord2);

        if(depth2 < m_FoamExistence.x){
            foam = (foam1.r + foam2).rgb * vec3(m_FoamIntensity);
        }else if(depth2 < m_FoamExistence.y){
            foam = mix((foam1 + foam2) * m_FoamIntensity , vec4(0.0),
                (depth2 - m_FoamExistence.x) / (m_FoamExistence.y - m_FoamExistence.x)).rgb;
        }

        
        if(m_MaxAmplitude - m_FoamExistence.z> 0.0001){
            foam += ((foam1 + foam2) * m_FoamIntensity  * m_FoamIntensity * 0.3 *
               saturate((level - (m_WaterHeight + m_FoamExistence.z)) / (m_MaxAmplitude - m_FoamExistence.z))).rgb;
        }
        foam *= m_LightColor.rgb;
    #endif

    vec3 specular = vec3(0.0);
    #ifdef ENABLE_SPECULAR
        vec3 lightDir=normalize(m_LightDir);
        vec3 mirrorEye = (2.0 * dot(eyeVecNorm, normal) * normal - eyeVecNorm);
        float dotSpec = saturate(dot(mirrorEye.xyz, -lightDir) * 0.5 + 0.5);
        specular = vec3((1.0 - fresnel) * saturate(-lightDir.y) * ((pow(dotSpec, 512.0)) * (m_Shininess * 1.8 + 0.2)));
        specular += specular * 25.0 * saturate(m_Shininess - 0.05);
        //foam does not shine
        specular=specular * m_LightColor.rgb - (5.0 * foam);
    #endif

    color = mix(refraction, reflection, fresnel);
    color = mix(refraction, color, saturate(depth * m_ShoreHardness));
    color = saturate(color + max(specular, foam ));
    color = mix(refraction, color, saturate(depth* m_FoamHardness));


    // XXX: HACK ALERT:
    // We trick the GeForces to think they have
    // to calculate the derivatives for all these pixels by using step()!
    // That way we won't get pixels around the edges of the terrain,
    // Where the derivatives are undefined
    return vec4(mix(color, color2, step(level, position.y)), 1.0);
}

void main(){
    #ifdef RESOLVE_MS
        vec4 color = vec4(0.0);
        for (int i = 0; i < m_NumSamples; i++){
            color += main_multiSample(i);
        }
        gl_FragColor = color / m_NumSamples;
    #else
        gl_FragColor = main_multiSample(0);
    #endif
}