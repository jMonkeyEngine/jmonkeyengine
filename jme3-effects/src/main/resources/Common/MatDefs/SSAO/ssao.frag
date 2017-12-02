#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform vec2 g_Resolution;
uniform vec2 g_ResolutionInverse;
uniform vec2 m_FrustumNearFar;
uniform COLORTEXTURE m_Texture;
uniform sampler2D m_Normals;
uniform sampler2D m_RandomMap;
uniform DEPTHTEXTURE m_DepthTexture;
uniform vec3 m_FrustumCorner;
uniform float m_SampleRadius;
uniform float m_Intensity;
uniform float m_Scale;
uniform float m_Bias;
uniform bool m_UseOnlyAo;
uniform bool m_UseAo;
uniform vec2[4] m_Samples;

varying vec2 texCoord;

vec3 getPosition(float depthv, in vec2 uv){
  //Reconstruction from depth
  float depth = (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - depthv * (m_FrustumNearFar.y-m_FrustumNearFar.x));

  //one frustum corner method
  float x = mix(-m_FrustumCorner.x, m_FrustumCorner.x, uv.x);
  float y = mix(-m_FrustumCorner.y, m_FrustumCorner.y, uv.y);

  return depth * vec3(x, y, m_FrustumCorner.z);
}

vec3 approximateNormal(in vec3 pos,in vec2 texCoord){
    float step = g_ResolutionInverse.x ;
    float stepy = g_ResolutionInverse.y ;
    float depth2 = getDepth(m_DepthTexture,texCoord + vec2(step,-stepy)).r;
    float depth3 = getDepth(m_DepthTexture,texCoord + vec2(-step,-stepy)).r;
    vec3 pos2 = vec3(getPosition(depth2,texCoord + vec2(step,-stepy)));
    vec3 pos3 = vec3(getPosition(depth3,texCoord + vec2(-step,-stepy)));

    vec3 v1 = (pos - pos2).xyz;
    vec3 v2 = (pos3 - pos2).xyz;
    return normalize(cross(-v1, v2));
}

vec3 getNormal(in vec2 uv){
  return normalize(texture2D(m_Normals, uv).xyz * 2.0 - 1.0);
}

vec2 getRandom(in vec2 uv){  
   vec4 rand = texture2D(m_RandomMap,g_Resolution * uv / 128.0 * 3.0)*2.0 -1.0;
   return normalize(rand.xy);
}

float doAmbientOcclusion(in vec2 tc, in vec3 pos, in vec3 norm){
   float depthv = getDepth(m_DepthTexture, tc).r;
   vec3 diff = getPosition(depthv, tc)- pos;
   vec3 v = normalize(diff);
   float d = length(diff) * m_Scale;

   return max(0.0, dot(norm, v) - m_Bias) * ( 1.0/(1.0 + d) ) * m_Intensity;
}

vec2 reflection(in vec2 v1,in vec2 v2){
    vec2 result= 2.0 * dot(v2, v1) * v2;
    result = v1-result;
    return result;
}

void main(){

   float result;

   float depthv = getDepth(m_DepthTexture, texCoord).r;
   //optimization, do not calculate AO if depth is 1
   if(depthv == 1.0){
           gl_FragColor = vec4(1.0);
           return;
   }
   vec3 position = getPosition(depthv, texCoord);

   #ifdef APPROXIMATE_NORMALS
        vec3 normal = approximateNormal(position, texCoord);
   #else
        vec3 normal = getNormal(texCoord);
   #endif

   vec2 rand = getRandom(texCoord);

   float ao = 0.0;
   float rad =m_SampleRadius / position.z;


   int iterations = 4;
   for (int j = 0; j < iterations; ++j){
      vec2 coord1 = reflection(vec2(m_Samples[j]), vec2(rand)) * vec2(rad,rad);
      vec2 coord2 = vec2(coord1.x* 0.707 - coord1.y* 0.707, coord1.x* 0.707 + coord1.y* 0.707) ;

      ao += doAmbientOcclusion(texCoord + coord1.xy * 0.25, position, normal);
      ao += doAmbientOcclusion(texCoord + coord2 * 0.50, position, normal);
      ao += doAmbientOcclusion(texCoord + coord1.xy * 0.75, position, normal);
      ao += doAmbientOcclusion(texCoord + coord2 * 1.00, position, normal);
   }
   ao /= float(iterations) * 4.0;
   result = 1.0 - ao;

   gl_FragColor = vec4(result);
}