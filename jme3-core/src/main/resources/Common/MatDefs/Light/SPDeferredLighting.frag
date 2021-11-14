#import "Common/ShaderLib/Deferred.glsllib"
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

varying mat4 viewProjectionMatrixInverse;
uniform mat4 g_ViewMatrix;
uniform vec4 g_LightData[NB_LIGHTS];

void main(){




    for( int i = 0;i < NB_LIGHTS; i+=3){
        vec4 lightColor = g_LightData[i];
        vec4 lightData1 = g_LightData[i+1];
        vec4 lightDir;
        vec3 lightVec;
        lightComputeDir(vPos, lightColor.w, lightData1, lightDir,lightVec);

        float spotFallOff = 1.0;
        #if __VERSION__ >= 110
            // allow use of control flow
        if(lightColor.w > 1.0){
        #endif
            spotFallOff =  computeSpotFalloff(g_LightData[i+2], lightVec);
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

        vec2 light = computeLighting(normal, viewDir, lightDir.xyz, lightDir.w * spotFallOff , m_Shininess);

        // Workaround, since it is not possible to modify varying variables
        vec4 SpecularSum2 = vec4(SpecularSum, 1.0);
        #ifdef USE_REFLECTION
             // Interpolate light specularity toward reflection color
             // Multiply result by specular map
             specularColor = mix(SpecularSum2 * light.y, refColor, refVec.w) * specularColor;

             SpecularSum2 = vec4(1.0);
             light.y = 1.0;
        #endif

        vec3 DiffuseSum2 = DiffuseSum.rgb;
        #ifdef COLORRAMP
           DiffuseSum2.rgb  *= texture2D(m_ColorRamp, vec2(light.x, 0.0)).rgb;
           SpecularSum2.rgb *= texture2D(m_ColorRamp, vec2(light.y, 0.0)).rgb;
           light.xy = vec2(1.0);
        #endif

        gl_FragColor.rgb += DiffuseSum2.rgb   * lightColor.rgb * diffuseColor.rgb  * vec3(light.x) +
                            SpecularSum2.rgb * lightColor.rgb * specularColor.rgb * vec3(light.y);
    }
}
