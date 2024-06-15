#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/ShadingModel.glsllib"
// octahedral
#import "Common/ShaderLib/Octahedral.glsllib"
// skyLight and reflectionProbe
uniform vec4 g_AmbientLightColor;
#import "Common/ShaderLib/SkyLightReflectionProbe.glsllib"
#ifdef USE_LIGHT_TEXTURES
    uniform vec2 g_ResolutionInverse;
#else
    varying vec2 texCoord;
#endif

varying mat4 viewProjectionMatrixInverse;
uniform mat4 g_ViewMatrix;
uniform vec3 g_CameraPosition;
uniform int m_NBLights;

#ifdef USE_LIGHT_TEXTURES
    uniform sampler2D m_LightTex1;
    uniform sampler2D m_LightTex2;
    uniform sampler2D m_LightTex3;
    uniform float m_LightTexInv;
    #ifdef TILED_LIGHTS
        uniform sampler2D m_Tiles;
        uniform sampler2D m_LightIndex;
        uniform vec3 m_LightIndexSize; // x=width, yz=size inverse
        #define TILES true
    #endif
#else
    uniform vec4 g_LightData[NB_LIGHTS];
#endif


void main(){
    #ifdef USE_LIGHT_TEXTURES
        vec2 innerTexCoord = gl_FragCoord.xy * g_ResolutionInverse;
    #else
        vec2 innerTexCoord = texCoord;
    #endif
    // unpack m_GBuffer
    vec4 shadingInfo = texture2D(m_GBuffer2, innerTexCoord);
    int shadingModelId = int(floor(shadingInfo.a));
    float depth = texture2D(m_GBuffer4, innerTexCoord).r;
    gl_FragDepth = depth;
    // Due to GPU architecture, each shading mode is performed for each pixel, which is very inefficient.
    // TODO: Remove these if statements if possible.
    if (shadingModelId == PHONG_LIGHTING) {
        // phong shading
        vec3 vPos = getPosition(innerTexCoord, depth, viewProjectionMatrixInverse);
        vec4 buff1 = texture2D(m_GBuffer1, innerTexCoord);
        vec4 diffuseColor = texture2D(m_GBuffer0, innerTexCoord);
        vec3 specularColor = floor(buff1.rgb) * 0.01;
        vec3 AmbientSum = min(fract(buff1.rgb) * 100.0, vec3(1.0)) * g_AmbientLightColor.rgb;
        float shininess = buff1.a;
        float alpha = diffuseColor.a;
        vec3 normal = texture2D(m_GBuffer3, innerTexCoord).xyz;
        vec3 viewDir = normalize(g_CameraPosition - vPos);
        gl_FragColor.rgb = AmbientSum * diffuseColor.rgb;
        gl_FragColor.a = alpha;
        //int lightNum = 0;
        #ifdef TILES
        // fetch index info from tile this fragment is contained by
        vec4 tileInfo = texture2D(m_Tiles, innerTexCoord);
        int x = int(tileInfo.x);
        int y = int(tileInfo.y);
        int componentIndex = int(tileInfo.z);
        int lightCount = int(tileInfo.w);
        vec4 lightIndex = vec4(-1.0);
        for (int i = 0; i < lightCount;) {
        #else
        for (int i = 0; i < NB_LIGHTS;) {
        #endif
            #ifdef USE_LIGHT_TEXTURES
                #ifdef TILED_LIGHTS
                    if (componentIndex == 0 || lightIndex.x < 0) {
                        // get indices from next pixel
                        lightIndex = texture2D(m_LightIndex, vec2(x, y) * m_LightIndexSize.yz);
                        if (componentIndex == 0 && ++x >= m_LightIndexSize.x) {
                            x = 0;
                            y++;
                        }
                    }
                    // apply index from each component in order
                    vec2 pixel = vec2(m_LightTexInv, 0);
                    switch (componentIndex) {
                        case 0: pixel.x *= lightIndex.x; break;
                        case 1: pixel.x *= lightIndex.y; break;
                        case 2: pixel.x *= lightIndex.z; break;
                        case 3: pixel.x *= lightIndex.w; break;
                    }
                    if (++componentIndex > 3) {
                        componentIndex = 0;
                    }
                #else
                    vec2 pixel = vec2(m_LightTexInv * i, 0);
                #endif
                vec4 lightColor = texture2D(m_LightTex1, pixel);
                vec4 lightData1 = texture2D(m_LightTex2, pixel);
            #else
                vec4 lightColor = g_LightData[i];
                vec4 lightData1 = g_LightData[i+1];
            #endif
            vec4 lightDir;
            vec3 lightVec;
            lightComputeDir(vPos, lightColor.w, lightData1, lightDir, lightVec);

            float spotFallOff = 1.0;
            #if __VERSION__ >= 110
            // allow use of control flow
            if (lightColor.w > 1.0) {
            #endif
                #ifdef USE_LIGHT_TEXTURES
                    spotFallOff = computeSpotFalloff(texture2D(m_LightTex3, pixel), lightVec);
                #else
                    spotFallOff = computeSpotFalloff(g_LightData[i+2], lightVec);
                #endif
            #if __VERSION__ >= 110
            }
            #endif

            #ifdef NORMALMAP
                // Normal map -> lighting is computed in tangent space
                lightDir.xyz = normalize(lightDir.xyz * tbnMat);
            #else
                // no Normal map -> lighting is computed in view space
                lightDir.xyz = normalize(lightDir.xyz);
            #endif

            vec2 light = computeLighting(normal, viewDir, lightDir.xyz,
                                         lightDir.w * spotFallOff, shininess);
            
            gl_FragColor.rgb += lightColor.rgb * diffuseColor.rgb  * vec3(light.x) +
                                lightColor.rgb * specularColor.rgb * vec3(light.y);
            
            #ifdef USE_LIGHT_TEXTURES
                i++;
            #else
                i += 3;
            #endif
        }
        //if (NB_LIGHTS == 1) {
        //    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        //}
    } else if (shadingModelId == PBR_LIGHTING) {
        // PBR shading
        vec3 vPos = getPosition(innerTexCoord, depth, viewProjectionMatrixInverse);
        vec4 buff0 = texture2D(m_GBuffer0, innerTexCoord);
        vec4 buff1 = texture2D(m_GBuffer1, innerTexCoord);
        vec3 emissive = shadingInfo.rgb;
        vec3 diffuseColor = floor(buff0.rgb) * 0.01f;
        vec3 specularColor = floor(buff1.rgb) * 0.01f;
        vec3 ao = min(fract(buff0.rgb) * 10.0f, vec3(1.0f));
        vec3 fZero = min(fract(buff1.rgb) * 10.0f, vec3(0.5f));
        float Roughness = buff1.a;
        float indoorSunLightExposure = fract(shadingInfo.a) * 100.0f;
        float alpha = buff0.a;
        vec4 n1n2 = texture2D(m_GBuffer3, innerTexCoord);
        vec3 normal = octDecode(n1n2.xy);
        vec3 norm = octDecode(n1n2.zw);
        vec3 viewDir = normalize(g_CameraPosition - vPos);
        float ndotv = max( dot( normal, viewDir ),0.0);
        int lightNum = 0;
        gl_FragColor.rgb = vec3(0.0);
        for (int i = 0; i < NB_LIGHTS;) {
            #ifdef USE_LIGHT_TEXTURES
                vec2 pixel = vec2(m_LightTexInv * i, 0);
                vec4 lightColor = texture2D(m_LightTex1, pixel);
                vec4 lightData1 = texture2D(m_LightTex2, pixel);
            #else
                vec4 lightColor = g_LightData[i];
                vec4 lightData1 = g_LightData[i+1];
            #endif
            vec4 lightDir;
            vec3 lightVec;
            lightComputeDir(vPos, lightColor.w, lightData1, lightDir,lightVec);

            float spotFallOff = 1.0;
            #if __VERSION__ >= 110
                // allow use of control flow
                if(lightColor.w > 1.0){
            #endif
                    #ifdef USE_LIGHT_TEXTURES
                        spotFallOff = computeSpotFalloff(texture2D(m_LightTex3, pixel), lightVec);
                    #else
                        spotFallOff = computeSpotFalloff(g_LightData[i+2], lightVec);
                    #endif
            #if __VERSION__ >= 110
                }
            #endif
            spotFallOff *= lightDir.w;

            #ifdef NORMALMAP
                //Normal map -> lighting is computed in tangent space
                lightDir.xyz = normalize(lightDir.xyz * tbnMat);
            #else
                //no Normal map -> lighting is computed in view space
                lightDir.xyz = normalize(lightDir.xyz);
            #endif

            vec3 directDiffuse;
            vec3 directSpecular;

            float hdotv = PBR_ComputeDirectLight(normal, lightDir.xyz, viewDir,
                    lightColor.rgb, fZero, Roughness, ndotv,
                    directDiffuse,  directSpecular);

            vec3 directLighting = diffuseColor.rgb * directDiffuse + directSpecular;

            gl_FragColor.rgb += directLighting * spotFallOff;
            #ifdef USE_LIGHT_TEXTURES
                i++;
            #else
                i += 3;
            #endif
        }
        // skyLight and reflectionProbe
        vec3 skyLight = renderSkyLightAndReflectionProbes(
                indoorSunLightExposure, viewDir, vPos, normal, norm,
                Roughness, diffuseColor, specularColor, ndotv, ao);
        gl_FragColor.rgb += skyLight;
        gl_FragColor.rgb += emissive;
        gl_FragColor.a = alpha;
        gl_FragColor.rgb = vec3(1-lightNum);
        gl_FragColor.a = 1.0;
    } /*else if (shadingModelId == SUBSURFACE_SCATTERING) {
        // TODO: implement subsurface scattering
    }*/ else if (shadingModelId == UNLIT) {
        // unlit shading
        gl_FragColor.rgb = shadingInfo.rgb;
        gl_FragColor.a = min(fract(shadingInfo.a) * 10.0f, 0.0f);
    } else {
        discard;
    }
    //gl_FragColor.r = gl_FragColor.a;
    //gl_FragColor.a = 0.5;
    //gl_FragColor.a = 0.01;
}
