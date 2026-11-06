#import "Common/ShaderLib/Instancing.glsllib"

in vec3 inPosition;
in vec3 inNormal;

out vec3 worldPosition;
out vec3 worldNormal;

vec4 wp;

void main() {
    wp = vec4(inPosition, 1.0);
    gl_Position = TransformWorldViewProjection(wp);
    worldPosition = TransformWorld(wp).rgb;
    worldNormal = normalize(inNormal); // Is this normalize needed?
}
