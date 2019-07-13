#define ALPHA_CUT_OFF 0.01

uniform sampler2D m_Texture;

uniform sampler2D m_SSR;

uniform vec2 g_Resolution;
uniform vec2 g_ResolutionInverse;
uniform float m_BlurScale;
uniform float m_Sigma;

noperspective in vec2 texCoord;
out vec4 outFragColor;

#ifdef USE_FAST_BLUR
// https://github.com/Jam3/glsl-fast-gaussian-blur
vec4 fastBlur(sampler2D image, vec2 direction) {
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.3846153846) * direction;
    vec2 off2 = vec2(3.2307692308) * direction;
    vec4 pixel = texture(image, texCoord);
    if(pixel.a > ALPHA_CUT_OFF){
      color += pixel * 0.2270270270;
    }
    pixel = texture(image, texCoord + off1);
    if(pixel.a > ALPHA_CUT_OFF){
        color += pixel * 0.3162162162;
    }
    pixel = texture(image, texCoord - off1);
    if(pixel.a > ALPHA_CUT_OFF){
        color += pixel * 0.3162162162;
    }
      
    pixel = texture(image, texCoord + off2);
    if(pixel.a > ALPHA_CUT_OFF){
        color += pixel * 0.0702702703;

      }
  
    pixel = texture(image, texCoord - off2);
    if(pixel.a > ALPHA_CUT_OFF){
        color += pixel * 0.0702702703;
    }
  
  return color;
}

#else 

float normpdf(in float x){
	return 0.39894*exp(-0.5*x*x/(m_Sigma*m_Sigma))/m_Sigma;
}
// based on: https://www.shadertoy.com/view/XdfGDH
vec4 blur(sampler2D image){
    const int mSize = 5;
    const int kSize = (mSize-1)/2;
    float kernel[mSize];
    vec4 final_colour = vec4(0.0);

    //create the 1-D kernel
    float Z = 0.0;
    for (int j = 0; j <= kSize; ++j){
            kernel[kSize+j] = kernel[kSize-j] = normpdf(float(j));
    }

    //get the normalization factor (as the gaussian has been clamped)
    for (int j = 0; j < mSize; ++j){
            Z += kernel[j];
    }

    //read out the texels
    for (int i=-kSize; i <= kSize; ++i){
            for (int j=-kSize; j <= kSize; ++j){
                vec4 color = texture(image, texCoord + vec2(i,j) * g_ResolutionInverse);
                if(color.a > ALPHA_CUT_OFF){
                    final_colour += kernel[kSize+j]*kernel[kSize+i]*color;
                }
            }
    }

    final_colour /= Z * Z / 1.1;
    return final_colour;
}

#endif

void main(){
    vec2 texCoord=texCoord;
    outFragColor=texture2D(m_Texture,texCoord);
    
    if(texture(m_SSR, texCoord).a > ALPHA_CUT_OFF){
        #ifdef USE_FAST_BLUR
            #ifdef HORIZONTAL
                vec4 sum = fastBlur(m_SSR, vec2(1 * m_BlurScale, 0) * g_ResolutionInverse);
            #else
                vec4 sum = fastBlur(m_SSR, vec2(0, 1 * m_BlurScale) * g_ResolutionInverse);
            #endif
        #else
            vec4 sum = blur(m_SSR);
        #endif
        outFragColor.rgb=mix(outFragColor.rgb, sum.rgb, sum.a);
    } else {
        //outFragColor = vec4(0.0);
        }
}
