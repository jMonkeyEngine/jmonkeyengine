uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
uniform int m_NbSamples;
uniform float m_BlurStart;
uniform float m_BlurWidth;
uniform float m_LightDensity;
uniform bool m_Display;
uniform vec3 m_LightPosition;

varying vec2 texCoord;

void main(void)
{
   if(m_Display){

       vec4 colorRes= texture2D(m_Texture,texCoord);
       float factor=(m_BlurWidth/(float(m_NbSamples)-1.0));
       float scale;
       vec2 texCoo=texCoord - m_LightPosition.xy;
       vec2 scaledCoord;
       vec4 res = vec4(0.0);
       for(int i=0; i<m_NbSamples; i++) {
            scale = float(i) * factor + m_BlurStart ;
            scaledCoord=texCoo*scale + m_LightPosition.xy;
            if(texture2D(m_DepthTexture,scaledCoord).r==1.0){
                res += texture2D(m_Texture,scaledCoord);
            }
        }
        res /= float(m_NbSamples);

        //Blend the original color with the averaged pixels
        float mean = (res.r + res.g + res.b)/3.0;
        gl_FragColor =mix(colorRes ,mix( colorRes, res, m_LightDensity),mean);   
    }else{
        gl_FragColor= texture2D(m_Texture,texCoord);
    }
}
