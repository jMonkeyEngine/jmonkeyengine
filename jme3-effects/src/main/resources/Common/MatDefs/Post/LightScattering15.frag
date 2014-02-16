#import "Common/ShaderLib/MultiSample.glsllib"

uniform COLORTEXTURE m_Texture;
uniform DEPTHTEXTURE m_DepthTexture;

uniform int m_NbSamples;
uniform float m_BlurStart;
uniform float m_BlurWidth;
uniform float m_LightDensity;
uniform bool m_Display;
uniform vec3 m_LightPosition;

in vec2 texCoord;
out vec4 fragColor;

void main(void)
{
   if(m_Display){

       vec4 colorRes= getColor(m_Texture,texCoord);
       float factor=(m_BlurWidth/float(m_NbSamples-1.0));
       float scale;
       vec2 texCoo=texCoord - m_LightPosition.xy;
       vec2 scaledCoord;
       vec4 res = vec4(0.0);
       for(int i=0; i<m_NbSamples; i++) {
            scale = i * factor + m_BlurStart ;
            scaledCoord=texCoo*scale + m_LightPosition.xy;            
            if(fetchTextureSample(m_DepthTexture, scaledCoord,0).r==1.0){
                res += fetchTextureSample(m_Texture,scaledCoord,0);
            }
        }
        res /= m_NbSamples;

        //Blend the original color with the averaged pixels
        float mean = (res.r + res.g + res.b)/3;
        fragColor =mix(colorRes ,mix( colorRes, res, m_LightDensity),mean);  
    }else{
        fragColor = getColor(m_Texture,texCoord);
    }
}
