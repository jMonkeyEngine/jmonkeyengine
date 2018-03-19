#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/InPassShadows.glsl"
#import "Common/ShaderLib/PBR.glsllib"

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
#define DIR_LIGHT_END             (DIR_LIGHT_START + NUM_DIR_LIGHTS * 2)

#define POINT_SHADOW_LIGHT_START  (DIR_LIGHT_END)
#define POINT_SHADOW_LIGHT_END    (POINT_SHADOW_LIGHT_START + NUM_SHADOW_POINT_LIGHTS * 2)

#define POINT_LIGHT_START         (POINT_SHADOW_LIGHT_END)
#define POINT_LIGHT_END           (POINT_LIGHT_START + NUM_POINT_LIGHTS * 2)

#define SPOT_SHADOW_LIGHT_START   (POINT_LIGHT_END)
#define SPOT_SHADOW_LIGHT_END     (SPOT_SHADOW_LIGHT_START + NUM_SHADOW_SPOT_LIGHTS * 3)

#define SPOT_LIGHT_START          (SPOT_SHADOW_LIGHT_END)
#define SPOT_LIGHT_END            (SPOT_LIGHT_START + NUM_SPOT_LIGHTS * 3)

#define LIGHT_DATA_SIZE           (NUM_SHADOW_DIR_LIGHTS * 2 )

uniform vec3 g_CameraPosition;

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
    float roughness;
    float ndotv;
};

void Lighting_Process(in int lightIndex, in surface_t surface, out vec3 outDiffuse, out vec3 outSpecular, inout int startProjIndex) {
    vec4 lightColor = g_LightData[lightIndex];
    vec4 lightData1 = g_LightData[lightIndex + 1];
    float shadowMapIndex = -1.0;

    if (lightColor.w < 0.0) {
        lightColor.w = -lightColor.w;
        shadowMapIndex = floor(lightColor.w);
        lightColor.w = lightColor.w - shadowMapIndex;
    }

    vec4 lightDir;
    vec3 lightVec;
    lightComputeDir(surface.position, lightColor.w, lightData1, lightDir, lightVec);

    if (shadowMapIndex >= 0.0) {
        lightDir.w *= Shadow_Process(lightColor.w, lightDir.xyz, shadowMapIndex, startProjIndex);
    }

    if (lightColor.w >= 0.5) {
        lightDir.w *= computeSpotFalloff(g_LightData[lightIndex + 2], lightDir.xyz);
    }

    lightColor.rgb *= lightDir.w;
    
    PBR_ComputeDirectLightSpecWF(surface.normal, lightDir.xyz, surface.viewDir,
                                 lightColor.rgb, surface.specular.rgb, surface.roughness, surface.ndotv,
                                 outDiffuse, outSpecular);
}

void Lighting_ProcessAll(surface_t surface, out vec3 ambient, out vec3 diffuse, out vec3 specular) {

    ambient  = g_AmbientLightColor.rgb;
    diffuse  = vec3(0.0);
    specular = vec3(0.0);

    Shadow_ProcessPssmSlice();

#if LIGHT_DATA_SIZE > 0
    int projIndex = 0;

    for (int i = SPOT_SHADOW_LIGHT_START; i < SPOT_SHADOW_LIGHT_END; i += 3) {
        vec3 outDiffuse, outSpecular;
        Lighting_Process(i, surface, outDiffuse, outSpecular, projIndex);
        diffuse   += outDiffuse;
        specular  += outSpecular;
    }

    for (int i = SPOT_LIGHT_START; i < SPOT_LIGHT_END; i += 3) {
        vec3 outDiffuse, outSpecular;
        Lighting_Process(i, surface, outDiffuse, outSpecular, projIndex);
        diffuse   += outDiffuse;
        specular  += outSpecular;
    }

    for (int i = DIR_SHADOW_LIGHT_START; i < DIR_SHADOW_LIGHT_END; i += 2) {
        vec3 outDiffuse, outSpecular;
        Lighting_Process(i, surface, outDiffuse, outSpecular, projIndex);
        diffuse   += outDiffuse;
        specular  += outSpecular;
    }

    for (int i = DIR_LIGHT_START; i < DIR_LIGHT_END; i += 2) {
        vec3 outDiffuse, outSpecular;
        Lighting_Process(i, surface, outDiffuse, outSpecular, projIndex);
        diffuse   += outDiffuse;
        specular  += outSpecular;
    }

    for (int i = POINT_SHADOW_LIGHT_START; i < POINT_SHADOW_LIGHT_END; i += 2) {
        vec3 outDiffuse, outSpecular;
        Lighting_Process(i, surface, outDiffuse, outSpecular, projIndex);
        diffuse   += outDiffuse;
        specular  += outSpecular;
    }

    for (int i = POINT_LIGHT_START; i < POINT_LIGHT_END; i += 2) {
        vec3 outDiffuse, outSpecular;
        Lighting_Process(i, surface, outDiffuse, outSpecular, projIndex);
        diffuse   += outDiffuse;
        specular  += outSpecular;
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
    s.viewDir = normalize(g_CameraPosition - s.position);
#ifdef AMBIENTMAP
    s.ambient = texture2D(m_AmbientMap, vTexCoord).rgb;
#else
    s.ambient = vec3(1.0);
#endif
    s.diffuse = vec4(1.0);
    s.specular = vec4(0.04, 0.04, 0.04, 1.0);
    s.roughness = 0.1;
    s.ndotv = max(0.0, dot(s.viewDir, s.normal));
    return s;
}

void main() {
    vec3 ambient, diffuse, specular;

    surface_t surface = getSurface();
    Lighting_ProcessAll(surface, ambient, diffuse, specular);

    vec4 color = vec4(1.0);
    color.rgb = ambient * surface.ambient.rgb +
                diffuse * surface.diffuse.rgb +
                specular;

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