#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;

uniform sampler2D m_NormalsTexture;
uniform vec2 g_Resolution;

uniform vec4 m_EdgeColor;

uniform float m_EdgeWidth;
uniform float m_EdgeIntensity;

uniform float m_NormalThreshold;
uniform float m_DepthThreshold;

uniform float m_NormalSensitivity;
uniform float m_DepthSensitivity;

in vec2 texCoord;
out vec4 outFragColor;

vec4 fetchNormalDepth(vec2 tc){
    vec4 nd;
    nd.xyz = texture2D(m_NormalsTexture, tc).rgb;
    nd.w   = fetchTextureSample(m_DepthTexture,   tc,0).r;
    return nd;
}

void main(){
    vec3 color = getColor(m_Texture, texCoord).rgb;

    vec2 edgeOffset = vec2(m_EdgeWidth) / textureSize(m_NormalsTexture, 0);
    vec4 n1 = fetchNormalDepth(texCoord + vec2(-1.0, -1.0) * edgeOffset);
    vec4 n2 = fetchNormalDepth(texCoord + vec2( 1.0,  1.0) * edgeOffset);
    vec4 n3 = fetchNormalDepth(texCoord + vec2(-1.0,  1.0) * edgeOffset);
    vec4 n4 = fetchNormalDepth(texCoord + vec2( 1.0, -1.0) * edgeOffset);

    // Work out how much the normal and depth values are changing.
    vec4 diagonalDelta = abs(n1 - n2) + abs(n3 - n4);

    float normalDelta = dot(diagonalDelta.xyz, vec3(1.0));
    float depthDelta = diagonalDelta.w;

    // Filter out very small changes, in order to produce nice clean results.
    normalDelta = clamp((normalDelta - m_NormalThreshold) * m_NormalSensitivity, 0.0, 1.0);
    depthDelta  = clamp((depthDelta - m_DepthThreshold) * m_DepthSensitivity,    0.0, 1.0);

    // Does this pixel lie on an edge?
    float edgeAmount = clamp(normalDelta + depthDelta, 0.0, 1.0) * m_EdgeIntensity;

    // Apply the edge detection result to the main scene color.
    //color *= (1.0 - edgeAmount);
    color = mix (color,m_EdgeColor.rgb,edgeAmount);

    outFragColor = vec4(color, 1.0);
}
