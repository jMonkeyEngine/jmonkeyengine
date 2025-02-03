#ifndef __TERRAIN_LAYER_MODULE__
    #define __TERRAIN_LAYER_MODULE__

    #ifndef PBRTerrainTextureLayer
        #struct StdPBRTerrainTextureLayer

            float blendValue;

            vec4 albedo;
            float alpha;
            vec3 normal;
            float height;  //parallax unused currently
            float metallic;             
            float roughness;
            float ao;
            vec4 emission;

        #endstruct
        #define PBRTerrainTextureLayer StdPBRTerrainTextureLayer    
    #endif
#endif