#import "Common/ShaderLib/GLSLCompat.glsllib"
#ifdef POINT_SPRITE
#  if !defined(GL_ES) && __VERSION__ < 120
#    error Point sprite is not supported by the video hardware!
#  endif
#endif

#ifdef USE_TEXTURE
uniform sampler2D m_Texture;
varying vec4 texCoord;
#endif

varying vec4 color;

void main(){
    if (color.a <= 0.01)
        discard;

    #ifdef USE_TEXTURE
        #ifdef POINT_SPRITE
            vec2 uv = mix(texCoord.xy, texCoord.zw, gl_PointCoord.xy);
        #else
            vec2 uv = texCoord.xy;
        #endif
        gl_FragColor = texture2D(m_Texture, uv) * color;
    #else
        gl_FragColor = color;
    #endif

    #ifdef PRE_SHADOW
        if (gl_FragColor.r <= 0.1 && 
            gl_FragColor.g <= 0.1 &&
            gl_FragColor.b <= 0.1) {
            discard;
        }
    #endif
}