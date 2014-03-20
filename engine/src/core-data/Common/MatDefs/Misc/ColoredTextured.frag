varying vec2 texCoord;

uniform sampler2D m_ColorMap;
uniform vec4 m_Color;

void main(){
    vec4 texColor = texture2D(m_ColorMap, texCoord);
    gl_FragColor = vec4(mix(m_Color.rgb, texColor.rgb, texColor.a), 1.0);
}