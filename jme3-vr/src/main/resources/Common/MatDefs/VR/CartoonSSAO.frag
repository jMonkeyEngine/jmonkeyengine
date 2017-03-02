//#define FRAGMENT_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform vec2 g_ResolutionInverse;
uniform vec2 m_FrustumNearFar;
uniform sampler2D m_Texture;
uniform sampler2D m_Normals;
uniform sampler2D m_DepthTexture;
uniform vec3 m_FrustumCorner;
uniform float m_Distance;

varying vec2 texCoord;

#define m_Scale 3.15
#define m_Bias 0.025
#define m_SampleRadius 200.0

vec4 fetchNormalDepth(vec2 tc){
    vec4 nd;
    nd.xyz = texture2D(m_Normals, tc).rgb;
    nd.w   = 150.0 * texture2D(m_DepthTexture,   tc).r;
    return nd;
}

vec3 getPosition(in vec2 uv){
    float depth= (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - texture2D(m_DepthTexture,uv).r * (m_FrustumNearFar.y-m_FrustumNearFar.x));
#ifdef INSTANCING
    float x = mix(-m_FrustumCorner.x, m_FrustumCorner.x, (uv.x - (uv.x > 0.5 ? 0.5 : 0.0)) * 2.0);
#else
    float x = mix(-m_FrustumCorner.x, m_FrustumCorner.x, uv.x);
#endif
    float y = mix(-m_FrustumCorner.y, m_FrustumCorner.y, uv.y);
    return depth* vec3(x, y, m_FrustumCorner.z);
}

vec3 getPosition(in vec2 uv, in float indepth){
    float depth= (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - indepth * (m_FrustumNearFar.y-m_FrustumNearFar.x));
#ifdef INSTANCING
    float x = mix(-m_FrustumCorner.x, m_FrustumCorner.x, (uv.x - (uv.x > 0.5 ? 0.5 : 0.0)) * 2.0);
#else
    float x = mix(-m_FrustumCorner.x, m_FrustumCorner.x, uv.x);
#endif
    float y = mix(-m_FrustumCorner.y, m_FrustumCorner.y, uv.y);
    return depth* vec3(x, y, m_FrustumCorner.z);
}

float doAmbientOcclusion(in vec2 tc, in vec3 pos, in vec3 norm){
    vec3 diff = getPosition(tc)- pos;
    float d = length(diff) * m_Scale;
    vec3 v = normalize(diff);
    return step(0.00002,d)*max(0.0, dot(norm, v) - m_Bias) * ( 1.0/(1.0 + d) ) * smoothstep(0.00002,0.0027,d);
}

void main(){
    float result;

    float firstdepth = texture2D(m_DepthTexture,texCoord).r;
    vec4 color = texture2D(m_Texture, texCoord);

    if( firstdepth == 1.0 ) {
        gl_FragColor = color;
        return;
    }

    vec3 position = getPosition(texCoord, firstdepth);
    vec3 normal = texture2D(m_Normals, texCoord).xyz * 2.0 - 1.0;

    vec2 rad = m_SampleRadius * g_ResolutionInverse / max(16.0, position.z);

    float ao = doAmbientOcclusion(texCoord + vec2( rad.x,  rad.y), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2(-rad.x,  rad.y), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2( rad.x, -rad.y), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2(-rad.x, -rad.y), position, normal);

    ao += doAmbientOcclusion(texCoord + vec2(-rad.x, 0.0), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2( rad.x, 0.0), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2(0.0, -rad.y), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2(0.0,  rad.y), position, normal);

    rad *= 0.7;

    ao += doAmbientOcclusion(texCoord + vec2(-rad.x, -rad.y), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2( rad.x, -rad.y), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2(-rad.x,  rad.y), position, normal);
    ao += doAmbientOcclusion(texCoord + vec2( rad.x,  rad.y), position, normal);

    result = 1.0 - clamp(ao * 0.4 - position.z * m_Distance * 2.5, 0.0, 0.6);

#ifndef NO_OUTLINE
    // ok, done with ambient occlusion, do cartoon edge

    vec2 mv = 0.5 * g_ResolutionInverse;

    vec4 n1 = fetchNormalDepth(texCoord + vec2(-mv.x, -mv.y));
    vec4 n2 = fetchNormalDepth(texCoord + vec2( mv.x,  mv.y));
    vec4 n3 = fetchNormalDepth(texCoord + vec2(-mv.x,  mv.y));
    vec4 n4 = fetchNormalDepth(texCoord + vec2( mv.x, -mv.y));

    // Work out how much the normal and depth values are changing.
    vec4 diagonalDelta = abs(n1 - n2) + abs(n3 - n4);

    float normalDelta = dot(diagonalDelta.xyz, vec3(1.0));
    float totalDelta = (diagonalDelta.w + normalDelta * 0.4) - position.z * m_Distance;

    gl_FragColor = color * vec4(result, result, result, 1.0) * (1.0 - clamp(totalDelta, 0.0, 1.0));
#else
    gl_FragColor = color * vec4(result, result, result, 1.0);    
#endif
}
