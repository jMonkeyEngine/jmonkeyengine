#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
varying vec2 texCoord;
varying mat4 viewProjectionMatrixInverse;
uniform mat4 g_ViewMatrix;
uniform vec3 g_CameraPosition;
uniform vec4 g_LightData[NB_LIGHTS];

void main(){
    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);






//    vec3 vPos = getPosition(texCoord, viewProjectionMatrixInverse);
//    vec4 buff1 = texture2D(Context_InGBuff1, texCoord);
//    vec4 diffuseColor = texture2D(Context_InGBuff0, texCoord);
//    vec3 specularColor = floor(buff1.rgb) * 0.1f;
//    vec3 AmbientSum = min(fract(buff1.rgb) * 10.0f, vec3(1.0f));
//    float Shininess = floor(diffuseColor.w);
//    float alpha = min(fract(diffuseColor.w) * 10.0f, 1.0f);
//    vec3 normal = texture2D(Context_InGBuff1, texCoord).xyz;
//    vec3 viewDir  = normalize(g_CameraPosition - vPos);
//
//
//    gl_FragColor.rgb = AmbientSum * diffuseColor.rgb;
//    gl_FragColor.a = alpha;
//    for( int i = 0;i < NB_LIGHTS; i+=3){
//        vec4 lightColor = g_LightData[i];
//        vec4 lightData1 = g_LightData[i+1];
//        vec4 lightDir;
//        vec3 lightVec;
//        lightComputeDir(vPos, lightColor.w, lightData1, lightDir,lightVec);
//
//        float spotFallOff = 1.0;
//        #if __VERSION__ >= 110
//            // allow use of control flow
//        if(lightColor.w > 1.0){
//        #endif
//            spotFallOff =  computeSpotFalloff(g_LightData[i+2], lightVec);
//        #if __VERSION__ >= 110
//        }
//        #endif
//
//        #ifdef NORMALMAP
//            //Normal map -> lighting is computed in tangent space
//            lightDir.xyz = normalize(lightDir.xyz * tbnMat);
//        #else
//            //no Normal map -> lighting is computed in view space
//            lightDir.xyz = normalize(lightDir.xyz);
//        #endif
//
//        vec2 light = computeLighting(normal, viewDir, lightDir.xyz, lightDir.w * spotFallOff , Shininess);
//
//        // Workaround, since it is not possible to modify varying variables
////        #ifdef USE_REFLECTION
////             // Interpolate light specularity toward reflection color
////             // Multiply result by specular map
////             specularColor = mix(specularColor * light.y, refColor, refVec.w) * specularColor;
////             light.y = 1.0;
////        #endif
////
////        #ifdef COLORRAMP
////           diffuseColor.rgb  *= texture2D(m_ColorRamp, vec2(light.x, 0.0)).rgb;
////           specularColor.rgb *= texture2D(m_ColorRamp, vec2(light.y, 0.0)).rgb;
////           light.xy = vec2(1.0);
////        #endif
//
//        gl_FragColor.rgb += lightColor.rgb * diffuseColor.rgb  * vec3(light.x) +
//                            lightColor.rgb * specularColor.rgb * vec3(light.y);
//    }
}
