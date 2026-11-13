#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef POINT_SPRITE
#  if !defined(GL_ES) && __VERSION__ < 120
#    error Point sprite is not supported by the video hardware!
#  endif
#endif

varying vec4 color;

#ifdef HAS_GLOWMAP
uniform sampler2D m_GlowMap;
varying vec4 texCoord;
#endif

#ifdef HAS_GLOWCOLOR
uniform vec4 m_GlowColor;
#endif

void main(){
    if (color.a <= 0.01) {
        discard;
    }

    #ifdef HAS_GLOWMAP
        #ifdef POINT_SPRITE
            vec2 uv = mix(texCoord.xy, texCoord.zw, gl_PointCoord.xy);
        #else
            vec2 uv = texCoord.xy;
        #endif
        vec4 glow = texture2D(m_GlowMap, uv);
        #ifdef HAS_GLOWCOLOR
            glow *= m_GlowColor;
        #endif
        gl_FragColor = glow * color;
    #else
        #ifdef HAS_GLOWCOLOR
            gl_FragColor = m_GlowColor * color;
        #else
            gl_FragColor = vec4(0.0);
        #endif
    #endif
}
