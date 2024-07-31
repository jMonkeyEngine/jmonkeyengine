#ifndef __LIGHT_MODULE__
#define __LIGHT_MODULE__

/**
* Defines a light
*/


#ifndef Light
    #struct StdLight 
        vec4 color;
        vec3 position;
        float type;

        float invRadius;
        float spotAngleCos;
        vec3 spotDirection;

        bool ready;
        
        float NdotL;                  // cos angle between normal and light direction
        float NdotH;                  // cos angle between normal and half vector
        float LdotH;                  // cos angle between light direction and half vector
        float HdotV;                  // cos angle between view direction and half vector
        vec3 vector;  
        vec3 dir;
        float fallOff;
    #endstruct
    #define Light StdLight
#endif



#endif