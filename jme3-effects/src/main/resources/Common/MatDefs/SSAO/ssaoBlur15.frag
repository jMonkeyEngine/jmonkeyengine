#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;
uniform sampler2D m_SSAOMap;
uniform bool m_UseOnlyAo;
uniform bool m_UseAo;
uniform float m_XScale;
uniform float m_YScale;
uniform vec2 m_FrustumNearFar;

in vec2 texCoord;
out vec4 fragColor;

vec4 getResult(vec4 color){
 
    #ifdef USE_ONLY_AO
        return color;
    #endif
    #ifdef USE_AO
        return getColor(m_Texture,texCoord)* color;
    #endif
    
    return getColor(m_Texture,texCoord);

}

float readDepth(in vec2 uv){
    float depthv =fetchTextureSample(m_DepthTexture,uv,0).r;
    return (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - depthv* (m_FrustumNearFar.y-m_FrustumNearFar.x));
}

 const float epsilon = 0.005;


    vec4 convolutionFilter(){
           vec4 sum = vec4(0.0);

            float x = texCoord.x;
            float y = texCoord.y;

            float xScale = m_XScale;
            float yScale = m_YScale;
       
            float zsum = 1.0;
        float Zp =readDepth(texCoord);


        vec2 sample = vec2(x - 2.0 * xScale, y - 2.0 * yScale);           
        float zTmp =readDepth(sample);
        float coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x - 0.0 * xScale, y - 2.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x + 2.0 * xScale, y - 2.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x - 1.0 * xScale, y - 1.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x + 1.0 * xScale, y - 1.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);
  
        sample = vec2(x - 2.0 * xScale, y - 0.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x + 2.0 * xScale, y - 0.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x - 1.0 * xScale, y + 1.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);
   
        sample = vec2(x + 1.0 * xScale, y + 1.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x - 2.0 * xScale, y + 2.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);
  
        sample = vec2(x - 0.0 * xScale, y + 2.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);

        sample = vec2(x + 2.0 * xScale, y + 2.0 * yScale);           
        zTmp =readDepth(sample);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
        zsum += coefZ;
        sum += coefZ* texture2D( m_SSAOMap, sample);


        return  sum / zsum;
    }


    void main(){
        //  float depth =texture2D(m_DepthTexture,uv).r;

       fragColor=getResult(convolutionFilter());
      // gl_FragColor=getResult(bilateralFilter());
      //  gl_FragColor=getColor(m_SSAOMap,texCoord);

    }