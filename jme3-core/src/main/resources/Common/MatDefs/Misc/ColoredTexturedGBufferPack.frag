#import "Common/ShaderLib/Deferred.glsllib"
// shading model
#import "Common/ShaderLib/ShadingModel.glsllib"

varying vec2 texCoord;

uniform sampler2D m_ColorMap;
uniform vec4 m_Color;

void main(){
    vec4 texColor = texture2D(m_ColorMap, texCoord);
    vec4 color = vec4(mix(m_Color.rgb, texColor.rgb, texColor.a), 1.0);


    Context_OutGBuff2.rgb = color.rgb;

    // shading model id
    Context_OutGBuff2.a = UNLIT + color.a * 0.1f;
}