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
#import "Common/ShaderLib/SkyLightReflectionProbe.glsllib"
#if defined(USE_LIGHTS_CULL_MODE)
    uniform vec2 g_ResolutionInverse;
#else
    varying vec2 texCoord;
#endif

varying mat4 viewProjectionMatrixInverse;
uniform mat4 g_ViewMatrix;
uniform vec3 g_CameraPosition;
uniform vec4 g_AmbientLightColor;

#if defined(USE_TEXTURE_PACK_MODE)
    uniform int g_LightCount;
    uniform sampler2D m_LightPackData1;
    uniform sampler2D m_LightPackData2;
    uniform sampler2D m_LightPackData3;
#else
    uniform vec4 g_LightData[NB_LIGHTS];
#endif


void main(){
    vec2 innerTexCoord;
#if defined(USE_LIGHTS_CULL_MODE)
    innerTexCoord = gl_FragCoord.xy * g_ResolutionInverse;
#else
    innerTexCoord = texCoord;
#endif
    // unpack GBuffer
    vec4 shadingInfo = texture2D(Context_InGBuff2, innerTexCoord);
    int shadingModelId = int(floor(shadingInfo.a));
    // Perform corresponding pixel shading based on the shading model
    if(IS_LIT(shadingModelId)){
        // lit shading model
        // todo:For now, use the big branch first, and extract the common parts later
        if(shadingModelId == LEGACY_LIGHTING){
            vec3 vPos = getPosition(innerTexCoord, viewProjectionMatrixInverse);
            vec4 buff1 = texture2D(Context_InGBuff1, innerTexCoord);
            vec4 diffuseColor = texture2D(Context_InGBuff0, innerTexCoord);
            vec3 specularColor = floor(buff1.rgb) * 0.01f;
            vec3 AmbientSum = min(fract(buff1.rgb) * 100.0f, vec3(1.0f)) * g_AmbientLightColor.rgb;
            float Shininess = buff1.a;
            float alpha = diffuseColor.a;
            vec3 normal = texture2D(Context_InGBuff3, innerTexCoord).xyz;
            vec3 viewDir  = normalize(g_CameraPosition - vPos);


            gl_FragColor.rgb = AmbientSum * diffuseColor.rgb;
            gl_FragColor.a = alpha;
            int lightNum = 0;
            #if defined(USE_TEXTURE_PACK_MODE)
            float lightTexSizeInv = 1.0f / (float(PACK_NB_LIGHTS) - 1.0f);
            lightNum = g_LightCount;
            #else
            lightNum = NB_LIGHTS;
            #endif
            for( int i = 0;i < lightNum; ){
            #if defined(USE_TEXTURE_PACK_MODE)
                vec4 lightColor = texture2D(m_LightPackData1, vec2(i * lightTexSizeInv, 0));
                vec4 lightData1 = texture2D(m_LightPackData2, vec2(i * lightTexSizeInv, 0));
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
                #if defined(USE_TEXTURE_PACK_MODE)
                    spotFallOff =  computeSpotFalloff(texture2D(m_LightPackData3, vec2(i * lightTexSizeInv, 0)), lightVec);
                #else
                    spotFallOff =  computeSpotFalloff(g_LightData[i+2], lightVec);
                #endif
                #if __VERSION__ >= 110
                }
                #endif

                #ifdef NORMALMAP
                    //Normal map -> lighting is computed in tangent space
                    lightDir.xyz = normalize(lightDir.xyz * tbnMat);
                #else
                    //no Normal map -> lighting is computed in view space
                    lightDir.xyz = normalize(lightDir.xyz);
                #endif

                vec2 light = computeLighting(normal, viewDir, lightDir.xyz, lightDir.w * spotFallOff , Shininess);

                // Workaround, since it is not possible to modify varying variables
                //        #ifdef USE_REFLECTION
                //             // Interpolate light specularity toward reflection color
                //             // Multiply result by specular map
                //             specularColor = mix(specularColor * light.y, refColor, refVec.w) * specularColor;
                //             light.y = 1.0;
                //        #endif
                //
                //        #ifdef COLORRAMP
                //           diffuseColor.rgb  *= texture2D(m_ColorRamp, vec2(light.x, 0.0)).rgb;
                //           specularColor.rgb *= texture2D(m_ColorRamp, vec2(light.y, 0.0)).rgb;
                //           light.xy = vec2(1.0);
                //        #endif

                gl_FragColor.rgb += lightColor.rgb * diffuseColor.rgb  * vec3(light.x) +
                lightColor.rgb * specularColor.rgb * vec3(light.y);
                #if defined(USE_TEXTURE_PACK_MODE)
                i++;
                #else
                i+=3;
                #endif
            }
        }
        else if(shadingModelId == STANDARD_LIGHTING){
            // todo:
            vec3 vPos = getPosition(innerTexCoord, viewProjectionMatrixInverse);
            vec4 buff0 = texture2D(Context_InGBuff0, innerTexCoord);
            vec4 buff1 = texture2D(Context_InGBuff1, innerTexCoord);
            vec3 emissive = shadingInfo.rgb;
            vec3 diffuseColor = floor(buff0.rgb) * 0.01f;
            vec3 specularColor = floor(buff1.rgb) * 0.01f;
            vec3 ao = min(fract(buff0.rgb) * 10.0f, vec3(1.0f));
            vec3 fZero = min(fract(buff1.rgb) * 10.0f, vec3(0.5f));
            float Roughness = buff1.a;
            float indoorSunLightExposure = fract(shadingInfo.a) * 100.0f;
            float alpha = buff0.a;
            vec4 n1n2 = texture2D(Context_InGBuff3, innerTexCoord);
            vec3 normal = octDecode(n1n2.xy);
            vec3 norm = octDecode(n1n2.zw);
            vec3 viewDir  = normalize(g_CameraPosition - vPos);

            float ndotv = max( dot( normal, viewDir ),0.0);
            int lightNum = 0;
            #if defined(USE_TEXTURE_PACK_MODE)
            float lightTexSizeInv = 1.0f / (float(PACK_NB_LIGHTS) - 1.0f);
            lightNum = g_LightCount;
            #else
            lightNum = NB_LIGHTS;
            #endif
            gl_FragColor.rgb = vec3(0.0);
            for( int i = 0;i < lightNum; ){
                #if defined(USE_TEXTURE_PACK_MODE)
                vec4 lightColor = texture2D(m_LightPackData1, vec2(i * lightTexSizeInv, 0));
                vec4 lightData1 = texture2D(m_LightPackData2, vec2(i * lightTexSizeInv, 0));
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
                    #if defined(USE_TEXTURE_PACK_MODE)
                    spotFallOff =  computeSpotFalloff(texture2D(m_LightPackData3, vec2(i * lightTexSizeInv, 0)), lightVec);
                    #else
                    spotFallOff =  computeSpotFalloff(g_LightData[i+2], lightVec);
                    #endif
                    #if __VERSION__ >= 110
                }
                #endif

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

                vec3 directLighting = diffuseColor.rgb *directDiffuse + directSpecular;

                gl_FragColor.rgb += directLighting * spotFallOff;
                #if defined(USE_TEXTURE_PACK_MODE)
                i++;
                #else
                i++;
                #endif
            }
            // skyLight and reflectionProbe
            vec3 skyLightAndReflection = renderSkyLightAndReflectionProbes(indoorSunLightExposure, viewDir, vPos, normal, norm, Roughness, diffuseColor, specularColor, ndotv, ao);
            gl_FragColor.rgb += skyLightAndReflection;
            gl_FragColor.rgb += emissive;
            gl_FragColor.a = alpha;
        }
        else if(shadingModelId == SUBSURFACE_SCATTERING){
            // todo:
        }
    }
    else if(shadingModelId == UNLIT){
        gl_FragColor.rgb = shadingInfo.rgb;
        gl_FragColor.a = min(fract(shadingInfo.a) * 10.0f, 0.0f);
    }
}
