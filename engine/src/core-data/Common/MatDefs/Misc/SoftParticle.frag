uniform sampler2D m_DepthTexture;
uniform float m_Softness; // Power used in the contrast function
varying vec2 vPos; // Position of the pixel
varying vec2 projPos;// z and w valus in projection space

#ifdef USE_TEXTURE
uniform sampler2D m_Texture;
varying vec4 texCoord;
#endif

varying vec4 color;

float Contrast(float d){
    float val = clamp( 2.0*( (d > 0.5) ? 1.0-d : d ), 0.0, 1.0);
    float a = 0.5 * pow(val, m_Softness);
    return (d > 0.5) ? 1.0 - a : a;
}

float stdDiff(float d){   
    return clamp((d)*m_Softness,0.0,1.0);
}


void main(){
    if (color.a <= 0.01)
        discard;

    vec4 c = vec4(1.0,1.0,1.0,1.0);//color;
    #ifdef USE_TEXTURE
        #ifdef POINT_SPRITE
            vec2 uv = mix(texCoord.xy, texCoord.zw, gl_PointCoord.xy);
        #else
            vec2 uv = texCoord.xy;
        #endif
        c = texture2D(m_Texture, uv) * color;
    #endif


    float depthv = texture2D(m_DepthTexture, vPos).x*2.0-1.0; // Scene depth
    depthv*=projPos.y;   
    float particleDepth = projPos.x;
	
    float zdiff =depthv-particleDepth;
    if(zdiff<=0.0){
        discard;
    }
    // Computes alpha based on the particles distance to the rest of the scene
    c.a = c.a * stdDiff(zdiff);// Contrast(zdiff);
    gl_FragColor =c;
}