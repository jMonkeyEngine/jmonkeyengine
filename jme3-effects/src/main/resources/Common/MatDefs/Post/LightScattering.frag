#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;
uniform int m_NbSamples;
uniform float m_BlurStart;
uniform float m_BlurWidth;
uniform float m_LightDensity;
uniform vec3 m_LightPosition;

varying vec2 texCoord;

void main(void) {
   #ifdef DISPLAY

       vec4 colorRes = getColor(m_Texture, texCoord);
       float factor = (m_BlurWidth/(float(m_NbSamples) - 1.0));
       float scale;
       vec2 texCoo = texCoord - m_LightPosition.xy;
       vec2 scaledCoord;
       vec4 res = vec4(0.0);
       for(int i=0; i<m_NbSamples; i++) {
            scale = float(i) * factor + m_BlurStart ;
            scaledCoord = texCoo*scale + m_LightPosition.xy;
            if(fetchTextureSample(m_DepthTexture, scaledCoord, 0).r == 1.0){
                res += fetchTextureSample(m_Texture, scaledCoord, 0);
            }
        }
        res /= float(m_NbSamples);

        //Blend the original color with the averaged pixels
        float mean = (res.r + res.g + res.b)/3.0;
        gl_FragColor = mix(colorRes, mix( colorRes, res, m_LightDensity), mean);
    #else
        gl_FragColor = getColor(m_Texture, texCoord);
    #endif
}
