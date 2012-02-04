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

    ivec2 texSize = textureSize(tex, 0);
    vec2 pixSize = 1.0 / vec2(texSize);

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

    ivec2 texSize = textureSize(tex, 0);
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

   vec2 f = fract( projCoord.xy * texSize );
   vec2 mx = mix( gather.xz, gather.yw, f.x );
   return mix( mx.x, mx.y, f.y );
}

float Shadow_DoPCF(in SHADOWMAP tex, in vec4 projCoord){
    float pixSize = 1.0 / textureSize(tex,0).x;

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

void main(){
    float shadow = 0.0;

    if(shadowPosition < m_Splits.x){
        shadow = GETSHADOW(m_ShadowMap0, projCoord0);
    }else if( shadowPosition <  m_Splits.y){
        shadow = GETSHADOW(m_ShadowMap1, projCoord1);
    }else if( shadowPosition <  m_Splits.z){
        shadow = GETSHADOW(m_ShadowMap2, projCoord2);
    }else if( shadowPosition <  m_Splits.w){
        shadow = GETSHADOW(m_ShadowMap3, projCoord3);
    }
    
    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
    outFragColor = vec4(shadow, shadow, shadow, 1.0);
}

