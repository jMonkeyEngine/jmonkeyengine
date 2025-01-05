#ifndef __SURFACE_MODULE__
#define __SURFACE_MODULE__

#ifndef PBRSurface
    #struct StdPBRSurface
        // from geometry
        vec3 position; // position in w space
        vec3 viewDir; // view dir in worldSpace
        vec3 geometryNormal; // normals w/o normalmap
        vec3 normal; // normals w/ normalmap
        bool frontFacing; //gl_FrontFacing
        float depth;

        // from texture
        vec3 albedo;
        float alpha;
        float metallic;              // metallic value at the surface
        float roughness;
        vec3 diffuseColor;
        vec3 specularColor;
        vec3 fZero;
        vec3 ao;
        float exposure;
        vec3 emission;


        // computed
        float NdotV;

        // from env
        vec3 bakedLightContribution; // light from light map or other baked sources
        vec3 directLightContribution; // light from direct light sources
        vec3 envLightContribution; // light from environment 

        float brightestLightStrength;
    #endstruct
    #define PBRSurface StdPBRSurface    
#endif
#endif