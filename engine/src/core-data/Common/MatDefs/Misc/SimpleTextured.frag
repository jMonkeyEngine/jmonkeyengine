#import "Common/ShaderLib/Texture.glsllib"

varying vec2 texCoord;

uniform sampler2D m_ColorMap;

void main(){
    //Texture_GetColor(m_ColorMap, texCoord)
    //vec4 color = texture2D(m_ColorMap, texCoord);
    //color.rgb *= color.a;
    //gl_FragColor = vec4(color.a);

    #ifdef NORMAL_LATC
        vec3 newNorm = vec3(texture2D(m_ColorMap, texCoord).ag, 0.0);
        newNorm = Common_UnpackNormal(newNorm);
        newNorm.b = sqrt(1.0 - (newNorm.x * newNorm.x) - (newNorm.y * newNorm.y));
        newNorm = Common_PackNormal(newNorm);
        gl_FragColor = vec4(newNorm, 1.0);
    #elif defined(SHOW_ALPHA)
        gl_FragColor = vec4(texture2D(m_ColorMap, texCoord).a);
    #else
        gl_FragColor = Texture_GetColor(m_ColorMap, texCoord);
    #endif
    #ifdef NORMALIZE
        gl_FragColor = vec4(normalize(gl_FragColor.xyz), gl_FragColor.a);
    #endif
}