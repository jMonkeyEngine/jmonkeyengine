#import "Common/ShaderLib/MultiSample.glsllib"
 
uniform COLORTEXTURE m_Texture;
in vec2 texCoord;
 
uniform vec4 m_LineColor;
uniform vec4 m_PaperColor;
uniform float m_ColorInfluenceLine;
uniform float m_ColorInfluencePaper;
 
uniform float m_FillValue;
uniform float m_Luminance1;
uniform float m_Luminance2;
uniform float m_Luminance3;
uniform float m_Luminance4;
uniform float m_Luminance5;
 
uniform float m_LineDistance;
uniform float m_LineThickness;
 
void main() {
    vec4 texVal = getColor(m_Texture, texCoord);
    float linePixel = 0;
 
    float lum = texVal.r*0.2126 + texVal.g*0.7152 + texVal.b*0.0722;
 
    if (lum < m_Luminance1){
        if (mod(gl_FragCoord.x + gl_FragCoord.y, m_LineDistance * 2.0) < m_LineThickness)
            linePixel = 1;
    }
    if (lum < m_Luminance2){
        if (mod(gl_FragCoord.x - gl_FragCoord.y, m_LineDistance * 2.0) < m_LineThickness)
            linePixel = 1;
    }
    if (lum < m_Luminance3){
        if (mod(gl_FragCoord.x + gl_FragCoord.y - m_LineDistance, m_LineDistance) < m_LineThickness)
            linePixel = 1;
    }
    if (lum < m_Luminance4){
        if (mod(gl_FragCoord.x - gl_FragCoord.y - m_LineDistance, m_LineDistance) < m_LineThickness)
            linePixel = 1;
    }
    if (lum < m_Luminance5){ // No line, make a blob instead
        linePixel = m_FillValue;
    }
 
    // Mix line color with existing color information
    vec4 lineColor = mix(m_LineColor, texVal, m_ColorInfluenceLine);
    // Mix paper color with existing color information
    vec4 paperColor = mix(m_PaperColor, texVal, m_ColorInfluencePaper);
 
    gl_FragColor = mix(paperColor, lineColor, linePixel);
}