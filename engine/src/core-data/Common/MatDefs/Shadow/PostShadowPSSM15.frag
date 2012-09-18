// Because gpu_shader5 is actually where those
// gather functions are declared to work on shadowmaps
#extension GL_ARB_gpu_shader5 : enable

#ifdef HARDWARE_SHADOWS
    #define SHADOWMAP sampler2DShadow
    #define SHADOWCOMPAREOFFSET(tex,coord,offset) textureProjOffset(tex, coord, offset)
    #define SHADOWCOMPARE(tex,coord) textureProj(tex, coord)
    #define SHADOWGATHER(tex,coord) textureGather(tex, coord.xy, coord.z)
#else
    #define SHADOWMAP sampler2D
    #define SHADOWCOMPAREOFFSET(tex,coord,offset) step(coord.z, textureProjOffset(tex, coord, offset).r)
    #define SHADOWCOMPARE(tex,coord) step(coord.z, textureProj(tex, coord).r)
    #define SHADOWGATHER(tex,coord) step(coord.z, textureGather(tex, coord.xy))
#endif


#if FILTER_MODE == 0
    #define GETSHADOW SHADOWCOMPARE
    #define KERNEL 1
#elif FILTER_MODE == 1
    #ifdef HARDWARE_SHADOWS
        #define GETSHADOW SHADOWCOMPARE
    #else
        #define GETSHADOW Shadow_DoBilinear_2x2
    #endif
    #define KERNEL 1
#elif FILTER_MODE == 2
    #define GETSHADOW Shadow_DoDither_2x2
    #define KERNEL 1
#elif FILTER_MODE == 3
    #define GETSHADOW Shadow_DoPCF
    #define KERNEL 4
#elif FILTER_MODE == 4
    #define GETSHADOW Shadow_DoPCFPoisson
    #define KERNEL 4
#elif FILTER_MODE == 5
    #define GETSHADOW Shadow_DoPCF
    #define KERNEL 8
#endif

out vec4 outFragColor;

uniform SHADOWMAP m_ShadowMap0;
uniform SHADOWMAP m_ShadowMap1;
uniform SHADOWMAP m_ShadowMap2;
uniform SHADOWMAP m_ShadowMap3;

uniform vec4 m_Splits;
uniform float m_ShadowIntensity;

in vec4 projCoord0;
in vec4 projCoord1;
in vec4 projCoord2;
in vec4 projCoord3;
in float shadowPosition;
const vec2 pixSize2 = vec2(1.0 / SHADOWMAP_SIZE);
float scale = 1.0;

float Shadow_BorderCheck(in vec2 coord){
    // Fastest, "hack" method (uses 4-5 instructions)
    vec4 t = vec4(coord.xy, 0.0, 1.0);
    t = step(t.wwxy, t.xyzz);
    return dot(t,t);
}

float Shadow_DoDither_2x2(in SHADOWMAP tex, in vec4 projCoord){
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;

    vec2 pixSize = pixSize2 * scale;
    
    float shadow = 0.0;
    ivec2 o = ivec2(mod(floor(gl_FragCoord.xy), 2.0));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2(-1.5, 1.5)+o), projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2( 0.5, 1.5)+o), projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2(-1.5, -0.5)+o), projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2( 0.5, -0.5)+o), projCoord.zw));
    shadow *= 0.25;
    return shadow;
}

float Shadow_DoBilinear_2x2(in SHADOWMAP tex, in vec4 projCoord){
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;
  
    #ifdef GL_ARB_gpu_shader5
        vec4 coord = vec4(projCoord.xyz / projCoord.www,0.0);
        vec4 gather = SHADOWGATHER(tex, coord);
    #else
        vec4 gather = vec4(0.0);
        gather.x = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(0, 0));
        gather.y = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(1, 0));
        gather.z = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(0, 1));
        gather.w = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(1, 1));
   #endif

   vec2 f = fract( projCoord.xy * SHADOWMAP_SIZE );
   vec2 mx = mix( gather.xz, gather.yw, f.x );
   return mix( mx.x, mx.y, f.y );
}

float Shadow_DoPCF(in SHADOWMAP tex, in vec4 projCoord){
    
    vec2 pixSize = pixSize2 * scale;   
    float shadow = 0.0;
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;

    float bound = KERNEL * 0.5 - 0.5;
    bound *= PCFEDGE;
    for (float y = -bound; y <= bound; y += PCFEDGE){
        for (float x = -bound; x <= bound; x += PCFEDGE){
            vec4 coord = vec4(projCoord.xy + vec2(x,y) * pixSize, projCoord.zw);
            shadow += SHADOWCOMPARE(tex, coord);
        }
    }

    shadow = shadow / (KERNEL * KERNEL);
    return shadow;
}


//12 tap poisson disk
const vec2 poissonDisk[12] =vec2[12]( vec2(-0.1711046, -0.425016),
 vec2(-0.7829809, 0.2162201),
 vec2(-0.2380269, -0.8835521),
 vec2(0.4198045, 0.1687819),
 vec2(-0.684418, -0.3186957),
 vec2(0.6026866, -0.2587841),
 vec2(-0.2412762, 0.3913516),
 vec2(0.4720655, -0.7664126),
 vec2(0.9571564, 0.2680693),
 vec2(-0.5238616, 0.802707),
 vec2(0.5653144, 0.60262),
 vec2(0.0123658, 0.8627419));


float Shadow_DoPCFPoisson(in SHADOWMAP tex, in vec4 projCoord){

    float shadow = 0.0;
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;

    //failed attempt to rotate the poisson disk to add jitter
    //vec2 jitterFactor = vec2(sin(projCoord.x),cos(projCoord.x));// * 2.0f - 1.0f;
     
    vec2 texelSize = pixSize2 * 4.0 * PCFEDGE * scale;        
    
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[0] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[1] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[2] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[3] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[4] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[5] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[6] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[7] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[8] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[9] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[10] * texelSize, projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk[11] * texelSize, projCoord.zw));

    shadow = shadow * 0.08333333333;//this is divided by 12
    return shadow;
}

#ifdef DISCARD_ALPHA
    #ifdef COLOR_MAP
        uniform sampler2D m_ColorMap;
    #else    
        uniform sampler2D m_DiffuseMap;
    #endif
    uniform float m_AlphaDiscardThreshold;
    varying vec2 texCoord;
#endif

void main(){
    float shadow = 0.0;

  
    #ifdef DISCARD_ALPHA
        #ifdef COLOR_MAP
             float alpha = texture2D(m_ColorMap,texCoord).a;
        #else    
             float alpha = texture2D(m_DiffuseMap,texCoord).a;
        #endif
      
        if(alpha < m_AlphaDiscardThreshold){
            discard;
        }
    #endif
    if(shadowPosition < m_Splits.x){
        shadow = GETSHADOW(m_ShadowMap0, projCoord0);
    }else if( shadowPosition <  m_Splits.y){
        scale = 0.5;
        shadow = GETSHADOW(m_ShadowMap1, projCoord1);
    }else if( shadowPosition <  m_Splits.z){
        scale = 0.25;
        shadow = GETSHADOW(m_ShadowMap2, projCoord2);
    }else if( shadowPosition <  m_Splits.w){
        scale = 0.125;
        shadow = GETSHADOW(m_ShadowMap3, projCoord3);
    }
    
    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
    outFragColor =  vec4(shadow, shadow, shadow, 1.0);

}

