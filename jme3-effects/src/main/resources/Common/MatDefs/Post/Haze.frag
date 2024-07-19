#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_ColorMap;
uniform sampler2D m_DepthMap;
uniform vec4 m_HazeColor;
uniform vec2 m_Range;

varying vec2 texCoord;

float mapRange(in float value, in vec2 range) {
    if (value > range.x && value < range.y) {
        return (range.y - value) / (range.y - range.x);
    } else {
        return 0.0;
    }
}

void main() {
    
    vec4 color = texture2D(m_ColorMap, texCoord);
    float depth = texture2D(m_DepthMap, texCoord).r;
    gl_FragColor.rgb = mix(color.rgb, m_HazeColor.rgb, mapRange(depth, m_Range) * m_HazeColor.a);
    //gl_FragColor = mix(color, m_HazeColor, depth);
    //gl_FragColor = color;
    
}
