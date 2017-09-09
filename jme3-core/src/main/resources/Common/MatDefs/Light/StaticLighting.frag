#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/InPassShadows.glsl"

#ifndef NUM_DIR_LIGHTS
#define NUM_DIR_LIGHTS 0
#endif

#ifndef NUM_POINT_LIGHTS
#define NUM_POINT_LIGHTS 0
#endif

#ifndef NUM_SPOT_LIGHTS
#define NUM_SPOT_LIGHTS 0
#endif

#define DIR_SHADOW_LIGHT_START    (0)
#define DIR_SHADOW_LIGHT_END      (NUM_SHADOW_DIR_LIGHTS * 2)

#define DIR_LIGHT_START           (DIR_SHADOW_LIGHT_END)
#define DIR_LIGHT_END             (NUM_DIR_LIGHTS * 2)

#define POINT_LIGHT_START         (DIR_LIGHT_END)
#define POINT_LIGHT_END           (POINT_LIGHT_START + NUM_POINT_LIGHTS * 2)

#define SPOT_SHADOW_LIGHT_START   (POINT_LIGHT_END)
#define SPOT_SHADOW_LIGHT_END     (SPOT_SHADOW_LIGHT_START + NUM_SHADOW_SPOT_LIGHTS * 3)

#define SPOT_LIGHT_START          (SPOT_SHADOW_LIGHT_END)
#define SPOT_LIGHT_END            (SPOT_LIGHT_START + NUM_SPOT_LIGHTS * 3)

#define LIGHT_DATA_SIZE           (SPOT_LIGHT_END)

uniform sampler2D m_AmbientMap;
uniform float m_AlphaDiscardThreshold;
uniform float m_Shininess;
uniform vec4 g_AmbientLightColor;

#if LIGHT_DATA_SIZE > 0
uniform vec4 g_LightData[LIGHT_DATA_SIZE];
#else
const vec4 g_LightData[1] = vec4[]( vec4(1.0) );
#endif

varying vec3 vPos;
varying vec3 vNormal;
varying vec2 vTexCoord;

struct surface_t {
    vec3 position;
    vec3 normal;
    vec3 viewDir;
    vec3 ambient;
    vec4 diffuse;
    vec4 specular;
    float shininess;
};

vec2 Lighting_ProcessLighting(vec3 norm, vec3 viewDir, vec3 lightDir, float attenuation, float shininess) {
    float diffuseFactor = max(0.0, dot(norm, lightDir));
    vec3 H = normalize(viewDir + lightDir);
    float HdotN = max(0.0, dot(H, norm));
    float specularFactor = pow(HdotN, shininess);
    return vec2(diffuseFactor, diffuseFactor * specularFactor) * vec2(attenuation);
}

vec2 Lighting_ProcessDirectional(int lightIndex, surface_t surface) {
    vec3 lightDirection = g_LightData[lightIndex + 1].xyz;
    vec2 light = Lighting_ProcessLighting(surface.normal, surface.viewDir, -lightDirection, 1.0, surface.shininess);
    return light;
}

float Lighting_ProcessAttenuation(float invRadius, float dist) {
    #ifdef SRGB
        float atten = (1.0 - invRadius * dist) / (1.0 + invRadius * dist * dist);
        return clamp(atten, 0.0, 1.0);
    #else
        return max(0.0, 1.0 - invRadius * dist);
    #endif
}

vec2 Lighting_ProcessPoint(int lightIndex, surface_t surface) {
    vec4 lightPosition  = g_LightData[lightIndex + 1];
    vec3 lightDirection = lightPosition.xyz - surface.position;
    float dist = length(lightDirection);
    lightDirection /= vec3(dist);
    float atten = Lighting_ProcessAttenuation(lightPosition.w, dist);
    return Lighting_ProcessLighting(surface.normal, surface.viewDir, lightDirection, atten, surface.shininess);
}

vec2 Lighting_ProcessSpot(int lightIndex, surface_t surface) {
    vec4 lightPosition  = g_LightData[lightIndex + 1];
    vec4 lightDirection = g_LightData[lightIndex + 2];
    vec3 lightVector    = lightPosition.xyz - surface.position;
    float dist          = length(lightVector);
    lightVector        /= vec3(dist);
    float atten         = Lighting_ProcessAttenuation(lightPosition.w, dist);
    atten              *= computeSpotFalloff(lightDirection, lightVector);
    return Lighting_ProcessLighting(surface.normal, surface.viewDir, lightVector, atten, surface.shininess);
}

void Lighting_ProcessAll(surface_t surface, out vec3 ambient, out vec3 diffuse, out vec3 specular) {

    ambient  = g_AmbientLightColor.rgb;
    diffuse  = vec3(0.0);
    specular = vec3(0.0);

    Shadow_ProcessPssmSlice();

#if LIGHT_DATA_SIZE > 0
    int projIndex = 0;

    for (int i = DIR_SHADOW_LIGHT_START; i < DIR_SHADOW_LIGHT_END; i += 2) {
        vec4 lightColor = g_LightData[i];
        vec2 lightDiffSpec = Lighting_ProcessDirectional(i, surface);
        float shadow = Shadow_ProcessDirectional(projIndex, lightColor.w);
        lightDiffSpec *= vec2(shadow);
        diffuse  += lightColor.rgb * lightDiffSpec.x;
        specular += lightColor.rgb * lightDiffSpec.y;
        projIndex += NUM_PSSM_SPLITS;
    }

    for (int i = DIR_LIGHT_START; i < DIR_LIGHT_END; i += 2) {
        vec3 lightColor = g_LightData[i].rgb;
        vec2 lightDiffSpec = Lighting_ProcessDirectional(i, surface);
        diffuse  += lightColor.rgb * lightDiffSpec.x;
        specular += lightColor.rgb * lightDiffSpec.y;
    }

    for (int i = POINT_LIGHT_START; i < POINT_LIGHT_END; i += 2) {
        vec3 lightColor = g_LightData[i].rgb;
        vec2 lightDiffSpec = Lighting_ProcessPoint(i, surface);
        diffuse  += lightColor.rgb * lightDiffSpec.x;
        specular += lightColor.rgb * lightDiffSpec.y;
    }

    for (int i = SPOT_SHADOW_LIGHT_START; i < SPOT_SHADOW_LIGHT_END; i += 3) {
        vec4 lightColor = g_LightData[i];
        vec2 lightDiffSpec = Lighting_ProcessSpot(i, surface);
        float shadow = Shadow_ProcessSpot(projIndex, lightColor.w);
        lightDiffSpec *= vec2(shadow);
        diffuse  += lightColor.rgb * lightDiffSpec.x;
        specular += lightColor.rgb * lightDiffSpec.y;
        projIndex++;
    }

    for (int i = SPOT_LIGHT_START; i < SPOT_LIGHT_END; i += 3) {
        vec3 lightColor = g_LightData[i].rgb;
        vec2 lightDiffSpec = Lighting_ProcessSpot(i, surface);
        diffuse  += lightColor * lightDiffSpec.x;
        specular += lightColor * lightDiffSpec.y;
    }

#endif
}

surface_t getSurface() {
    surface_t s;
    s.position = vPos;
    s.normal = normalize(vNormal);
    if (!gl_FrontFacing) {
        s.normal = -s.normal;
    }
    s.viewDir = normalize(-vPos);
#ifdef AMBIENTMAP
    s.ambient = texture2D(m_AmbientMap, vTexCoord).rgb;
#else
    s.ambient = vec3(1.0);
#endif
    s.diffuse = vec4(1.0);
    s.specular = vec4(1.0);
    s.shininess = m_Shininess;
    return s;
}

void main() {
    vec3 ambient, diffuse, specular;

    surface_t surface = getSurface();
    Lighting_ProcessAll(surface, ambient, diffuse, specular);

    vec4 color = vec4(1.0);
    color.rgb = surface.ambient.rgb  * ambient +
                surface.diffuse.rgb  * diffuse +
                surface.specular.rgb * specular;

    #ifdef DISCARD_ALPHA
        if (color.a < m_AlphaDiscardThreshold) {
            discard;
        }
    #endif

    gl_FragColor = color;

/*
    vec4 projCoord = vProjCoord[0];
    projCoord.xyz /= projCoord.w;
    float shad = shadow2D(g_ShadowMapArray, vec4(projCoord.xy, 0.0, projCoord.z)).r;
    vec3 amb = texture2D(m_AmbientMap, vTexCoord).rgb;
    gl_FragColor = vec4(amb * vec3(shad), 1.0);
*/
}