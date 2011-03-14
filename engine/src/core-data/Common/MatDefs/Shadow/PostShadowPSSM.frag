#ifdef HARDWARE_SHADOWS
    #define SHADOWMAP sampler2DShadow
    #define SHADOWCOMPARE(tex,coord) shadow2DProj(tex, coord).r
#else
    #define SHADOWMAP sampler2D
    #define SHADOWCOMPARE(tex,coord) step(coord.z, texture2DProj(tex, coord).r)
#endif

#if FILTER_MODE == 0
    #define GETSHADOW Shadow_DoShadowCompare
    #define KERNEL 1.0
#elif FILTER_MODE == 1
    #ifdef HARDWARE_SHADOWS
        #define GETSHADOW Shadow_DoShadowCompare
    #else
        #define GETSHADOW Shadow_DoBilinear_2x2
    #endif
    #define KERNEL 1.0
#elif FILTER_MODE == 2
    #define GETSHADOW Shadow_DoDither_2x2
    #define KERNEL 1.0
#elif FILTER_MODE == 3
    #define GETSHADOW Shadow_DoPCF
    #define KERNEL 4.0
#elif FILTER_MODE == 4
    #define GETSHADOW Shadow_DoPCF
    #define KERNEL 8.0
#endif

uniform SHADOWMAP m_ShadowMap0;
uniform SHADOWMAP m_ShadowMap1;
uniform SHADOWMAP m_ShadowMap2;
uniform SHADOWMAP m_ShadowMap3;

uniform vec4 m_Splits;

uniform float m_ShadowIntensity;

varying vec4 projCoord0;
varying vec4 projCoord1;
varying vec4 projCoord2;
varying vec4 projCoord3;

varying float shadowPosition;

const float texSize = 1024.0;
const float pixSize = 1.0 / texSize;
const vec2 pixSize2 = vec2(pixSize);

float Shadow_DoShadowCompareOffset(in SHADOWMAP tex, in vec4 projCoord, in vec2 offset){
    vec4 coord = vec4(projCoord.xy + offset.xy * pixSize2, projCoord.zw);
    return SHADOWCOMPARE(tex, coord);
}

float Shadow_DoShadowCompare(in SHADOWMAP tex, vec4 projCoord){
    return SHADOWCOMPARE(tex, projCoord);
}

float Shadow_BorderCheck(in vec2 coord){
    // Fastest, "hack" method (uses 4-5 instructions)
    vec4 t = vec4(coord.xy, 0.0, 1.0);
    t = step(t.wwxy, t.xyzz);
    return dot(t,t);
}

float Shadow_DoDither_2x2(in SHADOWMAP tex, in vec4 projCoord){
    float shadow = 0.0;
    vec2 o = mod(floor(gl_FragCoord.xy), 2.0);
    shadow += Shadow_DoShadowCompareOffset(tex,projCoord,vec2(-1.5,  1.5) + o);
    shadow += Shadow_DoShadowCompareOffset(tex,projCoord,vec2( 0.5,  1.5) + o);
    shadow += Shadow_DoShadowCompareOffset(tex,projCoord,vec2(-1.5, -0.5) + o);
    shadow += Shadow_DoShadowCompareOffset(tex,projCoord,vec2( 0.5, -0.5) + o);
    shadow *= 0.25 ;
    return shadow;
}

float Shadow_DoBilinear_2x2(in SHADOWMAP tex, in vec4 projCoord){
    vec4 gather = vec4(0.0);
    gather.x = Shadow_DoShadowCompareOffset(tex, projCoord, vec2(0.0, 0.0));
    gather.y = Shadow_DoShadowCompareOffset(tex, projCoord, vec2(1.0, 0.0));
    gather.z = Shadow_DoShadowCompareOffset(tex, projCoord, vec2(0.0, 1.0));
    gather.w = Shadow_DoShadowCompareOffset(tex, projCoord, vec2(1.0, 1.0));

    vec2 f = fract( projCoord.xy * texSize );
    vec2 mx = mix( gather.xz, gather.yw, f.x );
    return mix( mx.x, mx.y, f.y );
}

float Shadow_DoPCF(in SHADOWMAP tex, in vec4 projCoord){
    float shadow = 0.0;
    float bound = KERNEL * 0.5 - 0.5;
    bound *= PCFEDGE;
    for (float y = -bound; y <= bound; y += PCFEDGE){
        for (float x = -bound; x <= bound; x += PCFEDGE){
            shadow += clamp(Shadow_DoShadowCompareOffset(tex,projCoord,vec2(x,y)) +
                            Shadow_BorderCheck(projCoord.xy),
                            0.0, 1.0);
        }
    }

    shadow = shadow / (KERNEL * KERNEL);
    return shadow;
}

void main(){
    vec4 shadowPerSplit = vec4(0.0);
    shadowPerSplit.x = GETSHADOW(m_ShadowMap0, projCoord0);
    shadowPerSplit.y = GETSHADOW(m_ShadowMap1, projCoord1);
    shadowPerSplit.z = GETSHADOW(m_ShadowMap2, projCoord2);
    shadowPerSplit.w = GETSHADOW(m_ShadowMap3, projCoord3);

    vec4 less = step( shadowPosition, m_Splits );
    vec4 more = vec4(1.0) - step( shadowPosition, vec4(0.0, m_Splits.xyz) );
    float shadow = dot(shadowPerSplit, less * more );
    
    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);
    gl_FragColor = vec4(shadow, shadow, shadow, 1.0);
}

