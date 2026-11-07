#import "Common/ShaderLib/GLSLCompat.glsllib"
#define ENABLE_PBRLightingUtils_getWorldPosition 1
#define ENABLE_PBRLightingUtils_getWorldNormal 1
#define ENABLE_PBRLightingUtils_computeDirectLight 1
#import "Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib"

void main() {
    PBRSurface surface;
    PBRLightingUtils_readSunLightExposureParams(surface);
    gl_FragColor = vec4(vec3(surface.exposure), 1.0);
}
