uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
uniform sampler2D m_SSAOMap;
uniform vec2 g_Resolution;
uniform bool m_UseOnlyAo;
uniform bool m_UseAo;
uniform float m_XScale;
uniform float m_YScale;
uniform vec2 m_FrustumNearFar;

varying vec2 texCoord;

vec4 getColor(vec4 color){

    
    #ifdef USE_ONLY_AO
        return color;
    #endif
    #ifdef USE_AO
        return texture2D(m_Texture,texCoord)* color;
    #endif
    
    return texture2D(m_Texture,texCoord);

}

float readDepth(in vec2 uv){
    float depthv =texture2D(m_DepthTexture,uv).r;
    return (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - depthv* (m_FrustumNearFar.y-m_FrustumNearFar.x));
}

 const float epsilon = 0.005;


/*
    const int kernelSize=7;
   
    vec4 bilateralFilter() {
        vec4 color = vec4(0.0);

        vec2 sample;
        float sum = 0.0;
        float coefZ;
        float Zp = readDepth(texCoord);

        for(int i = -(kernelSize-1); i <= (kernelSize-1); i+=2) {
            for(int j = -(kernelSize-1); j <= (kernelSize-1); j+=2) {
                  sample = texCoord + vec2(i,j) / g_Resolution;           
                float zTmp =readDepth(sample);
                coefZ = 1.0 / (epsilon + abs(Zp - zTmp));               
                sum += coefZ;

                color += coefZ * texture2D(m_SSAOMap,sample);
             
            }
        }

        return color / sum;
    }
*/

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

       gl_FragColor=getColor(convolutionFilter());
      // gl_FragColor=getColor(bilateralFilter());
      //  gl_FragColor=texture2D(m_SSAOMap,texCoord);

    }