#import "Common/ShaderLib/GLSLCompat.glsllib"

#import "Common/ShaderLib/MultiSample.glsllib"
/**
#######################
# Simple SSR shader 
#######################

Copyright (c) 2019, Riccardo Balbo
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/// ########### SETTINGS (most of these are setted by the material) ###########
////
//// # GENERAL
//// Use dFdx and dFdy instead of multiple samples
#define FAST_APPROXIMATIONS 0
//// Read only XY from normal map and generate Z
// #define RG_NORMAL_MAP 1
//// Read glossiness from Z component of the normal map (works with RG_NORMAL_MAP)
// #define GLOSSINESS_PACKET_IN_NORMAL_B 1
//// Aproximate surface normals from the depth buffer
// #define USE_APPROXIMATED_NORMALS 1
//// Approximate glossiness from the normal map
// #define USE_APPROXIMATED_GLOSSINESS 1
////
//// # RAYMARCHING
//// How many samples along the ray
// #define RAY_SAMPLES 16
//// How many samples around the hit position
// #define NEARBY_SAMPLES 4 // 0 to 4 , 0 to disable
//// Length of first sample in world space
//// Size of a pixel used by NEARBY_SAMPLES
#define PIXEL_SIZE_MULT 1.
//// A depth difference equals or below this will be considered 0
#define DEPTH_TEST_BIAS 0.0001
////
//// # DEBUG
// #define _ENABLE_TESTS 1
// #define _TEST_CONVERSIONS 1
// #define _TEST_SHOW_WPOS 1
// #define _TEST_SHOW_SCREEN_Z 1
// #define _TEST_SHOW_LINEAR_Z 1
// #define _TEST_SHOW_APROXIMATED_GLOSS 1
// #define _TEST_SHOW_RAY_GLOSS
///// ########### ########### ########### 


#ifdef SCENE_NORMALS
    uniform sampler2D m_Normals;
#endif

#if defined(RG_NORMAL_MAP) && !defined(GLOSSINESS_PACKET_IN_NORMAL_B)
    // If glossiness is not provided, fallback to glossiness approximation
    #define USE_APPROXIMATED_GLOSSINESS 1
#endif

#if NEARBY_SAMPLES>0
    const vec2 _SAMPLES[4]=vec2[](
        vec2(1.0, 0.0), 
        vec2(-1.0, 0.0), 
        vec2(0.0, 1.0), 
        vec2(0.0, -1.0)
    );
#endif

noperspective in vec2 texCoord;

uniform vec2 g_ResolutionInverse;
uniform sampler2D m_DepthTexture;
uniform sampler2D m_Texture;
uniform vec3 g_CameraPosition;
uniform mat4 g_ViewProjectionMatrixInverse;
uniform mat4 g_ViewProjectionMatrix;
uniform mat4 g_ProjectionMatrix;
uniform vec2 g_FrustumNearFar;
uniform vec3 m_FrustumCorner;
uniform vec2 m_NearReflectionsFade;
uniform vec2 m_FarReflectionsFade;
uniform int m_RaySamples;
uniform float m_StepLength;
uniform float m_ReflectionFactor;
/**
* In this shader we use two types of coordinates
* World coordinates = coordinates of a point in the 3d world
* Screen coordinates = coordinate of a point projected to the screen 
*        x=(0,1) for left and right 
*        y=(0,1) for bottom and top
*        z=(0,1) for near and far
*/

/**
* Represent a ray
*/
struct Ray {
    // World position of the surface from where the ray is originated
    vec3 wFrom;
    // Same as before but in screenspace
    vec3 sFrom;
    // Glossiness of the surface from where the ray is originated
    float surfaceGlossiness;
    // Its direction
    vec3 wDir;
    
    vec3 sDir;
    
    vec3 normal;
};

/**
* Returned when the ray hit or miss the scene
*/
struct HitResult {
    // Last tested screen position (-1,-1 if missed)
    vec3 screenPos;
    // How strong the reflection is
    float reflStrength;
};

/**
* Get screen space coordinates
*        x=(0,1) for left and right 
*        y=(0,1) for bottom and top
*        z=(0,1) for near and far
*/
vec3 getScreenPos(in vec2 texCoord,in float depth){
    vec3 screenpos= vec3(texCoord,depth);
    return screenpos;
}

/**
* Exponential to linear depth
*/
float linearizeDepth(in float depth){
    return (2. * g_FrustumNearFar.x) / (g_FrustumNearFar.y + g_FrustumNearFar.x - depth * (g_FrustumNearFar.y - g_FrustumNearFar.x));
}

/**
* Convert world space to screenspace (UV,DEPTH)
*/
vec3 wposToScreenPos(in vec3 wPos){
    vec4 ww = g_ViewProjectionMatrix * vec4(wPos, 1.0);
    ww.xyz /= ww.w;
    ww.xyz = ww.xyz * 0.5 + 0.5;
    return ww.xyz;
}

/**
* Convert screen space (UV,DEPTH) to world space
*/
vec3 screenPosToWPos(in vec3 screenPos){
    vec4 pos=vec4(screenPos,1.0)*2.0-1.0;
    pos = g_ViewProjectionMatrixInverse * pos;
    return pos.xyz/pos.w;
}

vec3 getPosition(float depthv, in vec2 uv){
  //Reconstruction from depth
  float depth = depthv;//(2.0 * g_FrustumNearFar.x) / (g_FrustumNearFar.y + g_FrustumNearFar.x - depthv * (g_FrustumNearFar.y-g_FrustumNearFar.x));

  //one frustum corner methodPreNormalPass
  float x = mix(-m_FrustumCorner.x, m_FrustumCorner.x, uv.x);
  float y = mix(-m_FrustumCorner.y, m_FrustumCorner.y, uv.y);

  return depth * vec3(x, y, m_FrustumCorner.z);
}

#define fresnelExp 5.0

float fresnel(vec3 direction, vec3 normal) {
    vec3 halfDirection = normalize(normal + direction);
    
    float cosine = dot(halfDirection, direction);
    float product = max(cosine, 0.0);
    float factor = 1.0 - pow(product, fresnelExp);
    
    return factor;
}



#ifdef USE_APPROXIMATED_NORMALS
    vec3 approximateNormal2(in vec3 pos,in vec2 texCoord){
        float step = g_ResolutionInverse.x ;
    float stepy = g_ResolutionInverse.y ;
    float depth2 = getDepth(m_DepthTexture,texCoord + vec2(step,-stepy)).r;
    float depth3 = getDepth(m_DepthTexture,texCoord + vec2(-step,-stepy)).r;
    vec3 pos2 = vec3(getPosition(depth2,texCoord + vec2(step,-stepy)));
    vec3 pos3 = vec3(getPosition(depth3,texCoord + vec2(-step,-stepy)));

    vec3 v1 = (pos - pos2).xyz;
    vec3 v2 = (pos3 - pos2).xyz;
    vec4 normal = vec4(normalize(cross(-v1, v2)), 1.0) ;
    return normal.xyz / normal.w;
    }
    /**
    * Use nearby positions to aproximate normals
    */
    // Adapted from https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-effects/src/main/resources/Common/MatDefs/SSAO/ssao.frag#L33
    vec3 approximateNormal(in vec3 pos,in vec2 texCoord){
        #ifdef FAST_APPROXIMATIONS
            vec3 v1=dFdx(pos);
            vec3 v2=dFdy(pos);
        #else
            float step = g_ResolutionInverse.x ;
            float stepy = g_ResolutionInverse.y ;
            float depth2 = texture(m_DepthTexture,texCoord + vec2(step,-stepy)).r;
            float depth3 = texture(m_DepthTexture,texCoord + vec2(-step,-stepy)).r;
            vec3 pos2=screenPosToWPos( getScreenPos(texCoord + vec2(step,-stepy),depth2) );
            vec3 pos3=screenPosToWPos( getScreenPos(texCoord + vec2(-step,-stepy),depth3) );
            vec3 v1 = (pos - pos2).xyz;
            vec3 v2 = (pos3 - pos2).xyz;              
        #endif
        return normalize(cross(-v1, v2));
    }
#else
    vec3 getNormal(in vec2 texCoord){
        vec3 wNormal = texture(m_Normals, texCoord).xyz * 2.0 - 1.0;
        vec4 normal = vec4(wNormal , 1.0);// * g_ProjectionMatrix;
        wNormal = normal.xyz / normal.w;
        wNormal.z = (2.0 * g_FrustumNearFar.x) / (g_FrustumNearFar.y + g_FrustumNearFar.x - wNormal.z * (g_FrustumNearFar.y-g_FrustumNearFar.x));
        #ifdef RG_NORMAL_MAP
            wNormal.z = sqrt(1-clamp(dot(wNormal.xy, wNormal.xy),0.,1.)); // Reconstruct Z
        #endif
        return normalize(wNormal);
    }
#endif

#ifdef USE_APPROXIMATED_GLOSSINESS
    /**
    * Use nearby normals to aproximate glossiness
    */
    float approximateGlossiness(in vec3 normal,in vec2 texCoord){
        vec3 d1 = dFdx(normal);
        vec3 d2 = dFdy(normal);
        float maxd=max(dot(d1,d1),dot(d2,d2));
        maxd=smoothstep(0.,1.,maxd);
        maxd=pow(maxd,8)*1.;
        return 1.-clamp(maxd,0,1);
    }
#endif

// ##### DEBUG
#ifdef _ENABLE_TESTS
    void _testConversions(){
        float depth=texture(m_DepthTexture,texCoord).r;
        vec3 screenpos=getScreenPos(texCoord.xy,depth);            
        vec3 wpos=screenPosToWPos(screenpos);
        screenpos=wposToScreenPos(wpos);          
        vec3 nwpos=screenPosToWPos(screenpos);  
        if(distance(nwpos,wpos)>0.01)outFragColor=vec4(1,0,0,1);
    }
    void _testShowWPos(){
        float depth=texture(m_DepthTexture,texCoord).r;
        vec3 screenpos=getScreenPos(texCoord.xy,depth);
        vec3 wpos=screenPosToWPos(screenpos);
        outFragColor.rgb=wpos;
    }
    void _testScreenZ(){
        float depth=texture(m_DepthTexture,texCoord).r;
        vec3 screenpos=getScreenPos(texCoord.xy,depth);
        outFragColor.rgb=vec3(screenpos.z);
    }
    void _testLinearZ(){
        float depth=texture(m_DepthTexture,texCoord).r;
        vec3 screenpos=getScreenPos(texCoord.xy,depth);
        outFragColor.rgb=vec3(linearizeDepth(screenpos.z));
    }
    void _testShowApproximatedGloss(){
        vec3 wNormal=texture(m_Normals,texCoord).rgb;
        wNormal.xyz=wNormal.xyz*2.-1.;
        #ifdef RG_NORMAL_MAP
            wNormal.z = sqrt(1-clamp(dot(wNormal.xy, wNormal.xy),0.,1.)); // Reconstruct Z
        #endif
        outFragColor.rgb=vec3(approximateGlossiness(wNormal,texCoord));
    }
    void _testShowRayGloss(in Ray ray){g_View
        outFragColor.rgb=vec3(ray.surfaceGlossiness);
    }
#endif
// ####

/**
* Create a ray for ray marching
*/
Ray createRay(in vec2 texCoord,in float depth){
    Ray ray;
    ray.sFrom=getScreenPos(texCoord,depth);
    ray.wFrom = screenPosToWPos(ray.sFrom);
    ray.surfaceGlossiness=1.;

    #ifdef USE_APPROXIMATED_NORMALS
        vec3 wNormal=approximateNormal(ray.wFrom, texCoord);
    #else
        vec3 wNormal= getNormal(texCoord);
        #ifdef GLOSSINESS_PACKET_IN_NORMAL_B
            ray.surfaceGlossiness = wNormal.z * m_ReflectionFactor;
        #elif defined(USE_APPROXIMATED_GLOSSINESS) 
            ray.surfaceGlossiness = min(ray.surfaceGlossiness, approximateGlossiness(wNormal,texCoord)) * m_ReflectionFactor;
        #endif
    #endif
    
    ray.normal = wNormal;

    // direction from camera to fragment (in world space)
    vec3 wDir = normalize(ray.wFrom - g_CameraPosition);

    // reflection vector
    ray.wDir = normalize(reflect(wDir, wNormal));
    return ray;
}

/**
* Actual ray marching happens here
*/
HitResult performRayMarching(in Ray ray){

    HitResult result;
    result.screenPos=vec3(-1,-1,-1);

    // Current position of the sample along the ray
    vec3 sampleWPos;

    // Same of before, but in screen space
    vec3 sampleScreenPos;

    // Position of the nearest surface at the sample position (in screen space)
    vec3 hitSurfaceScreenPos;

    // Length of the next step
    float stepLength = m_StepLength;

    float linearSourceDepth=linearizeDepth(ray.sFrom.z);

    for(int i = 0; i < m_RaySamples; i++) {
        // if(hit)break;
        sampleWPos = ray.wFrom + ray.wDir * stepLength;
        sampleScreenPos = wposToScreenPos(sampleWPos); // ray.sFrom + ray.sDir * stepLength;
           
        hitSurfaceScreenPos = getScreenPos(sampleScreenPos.xy, getDepth(m_DepthTexture, sampleScreenPos.xy).r);
        vec3 hitSurfaceWPos = screenPosToWPos(hitSurfaceScreenPos);
     
        int j=0;
        #if NEARBY_SAMPLES>0
        do{
        #endif
            // We need to linearize the depth to have consistent tests for distant samples
            float linearHitSurfaceDepth=linearizeDepth(hitSurfaceScreenPos.z);
            float linearSampleDepth=linearizeDepth(sampleScreenPos.z);
            bool hit=
                linearHitSurfaceDepth>linearSourceDepth // check if the thing we want to reflect is behind the source of the ray
                && abs(linearSampleDepth - linearHitSurfaceDepth) < DEPTH_TEST_BIAS; // check if the ray is (~almost) hitting the surface          
            // if first hit (letting the cycle running helds to better performances than breaking it)
            if(hit && result.screenPos.x == -1){
                result.screenPos=sampleScreenPos;
                // Fade distant reflections
                result.reflStrength=distance(hitSurfaceWPos,ray.wFrom);      
                result.reflStrength=smoothstep(m_NearReflectionsFade.x,m_NearReflectionsFade.y, result.reflStrength)
                *(1.-smoothstep(m_FarReflectionsFade.x,m_FarReflectionsFade.y, result.reflStrength));
            }
        #if NEARBY_SAMPLES>0
            hitSurfaceScreenPos = getScreenPos(sampleScreenPos.xy,
                texture(m_DepthTexture, sampleScreenPos.xy + _SAMPLES[j].xy * g_ResolutionInverse).r
            );
            j++;
        }while(j<=NEARBY_SAMPLES);
        #endif
                     
        // Compute next step length
        stepLength = length(ray.wFrom - hitSurfaceWPos);
    }
    return result;    
}


void main(){
    outFragColor=vec4(0);    

    float depth=getDepth(m_DepthTexture,texCoord).r;

    if(depth!=1){ // ignore the sky    
        // Build the ray
            Ray ray=createRay(texCoord, depth);
        // Perform ray marching
        HitResult result=performRayMarching(ray);

        // Used to fade reflections near screen edges to remove artifacts
        float d=distance(result.screenPos.xy,vec2(0.5));
        d=pow(1.-clamp(d,0.,.5)*2.,2);
        
        // Render reflections
        if(result.screenPos.x!=-1){
            outFragColor.rgb = texture2D(m_Texture,result.screenPos.xy).rgb;
            outFragColor.a = d*ray.surfaceGlossiness*result.reflStrength;
            //float fresnel = fresnel(ray.wDir, ray.normal);
            //outFragColor.rgb *= fresnel;
        }  
        //outFragColor = texture2D(m_Texture, texCoord);
            
        //outFragColor.a = 1.0;
        // Tests
        #ifdef _ENABLE_TESTS
            outFragColor=vec4(0,0,0,1);
            #ifdef _TEST_SHOW_WPOS
                _testShowWPos();
            #endif
            #ifdef _TEST_CONVERSIONS
                _testConversions();
            #endif  
            #ifdef _TEST_SHOW_SCREEN_Z
                _testScreenZ();
            #endif
            #ifdef _TEST_SHOW_LINEAR_Z
                _testLinearZ();
            #endif
            #ifdef _TEST_SHOW_APROXIMATED_GLOSS
                _testShowApproximatedGloss();
            #endif
            #ifdef _TEST_SHOW_RAY_GLOSS
                _testShowRayGloss(ray);
            #endif
        #endif

    }
}
