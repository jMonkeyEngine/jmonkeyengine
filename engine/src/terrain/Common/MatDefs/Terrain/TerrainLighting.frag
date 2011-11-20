
uniform float m_Shininess;
uniform vec4 g_LightDirection;

varying vec4 AmbientSum;
varying vec4 DiffuseSum;
varying vec4 SpecularSum;

varying vec3 vNormal;
varying vec2 texCoord;
varying vec3 vPosition;
varying vec3 vnPosition;
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec4 vnLightDir;
varying vec3 lightVec;


#ifdef DIFFUSEMAP
  uniform sampler2D m_DiffuseMap;
#endif
#ifdef DIFFUSEMAP_1
  uniform sampler2D m_DiffuseMap_1;
#endif
#ifdef DIFFUSEMAP_2
  uniform sampler2D m_DiffuseMap_2;
#endif
#ifdef DIFFUSEMAP_3
  uniform sampler2D m_DiffuseMap_3;
#endif
#ifdef DIFFUSEMAP_4
  uniform sampler2D m_DiffuseMap_4;
#endif
#ifdef DIFFUSEMAP_5
  uniform sampler2D m_DiffuseMap_5;
#endif
#ifdef DIFFUSEMAP_6
  uniform sampler2D m_DiffuseMap_6;
#endif
#ifdef DIFFUSEMAP_7
  uniform sampler2D m_DiffuseMap_7;
#endif
#ifdef DIFFUSEMAP_8
  uniform sampler2D m_DiffuseMap_8;
#endif
#ifdef DIFFUSEMAP_9
  uniform sampler2D m_DiffuseMap_9;
#endif
#ifdef DIFFUSEMAP_10
  uniform sampler2D m_DiffuseMap_10;
#endif
#ifdef DIFFUSEMAP_11
  uniform sampler2D m_DiffuseMap_11;
#endif


#ifdef DIFFUSEMAP_0_SCALE
  uniform float m_DiffuseMap_0_scale;
#endif
#ifdef DIFFUSEMAP_1_SCALE
  uniform float m_DiffuseMap_1_scale;
#endif
#ifdef DIFFUSEMAP_2_SCALE
  uniform float m_DiffuseMap_2_scale;
#endif
#ifdef DIFFUSEMAP_3_SCALE
  uniform float m_DiffuseMap_3_scale;
#endif
#ifdef DIFFUSEMAP_4_SCALE
  uniform float m_DiffuseMap_4_scale;
#endif
#ifdef DIFFUSEMAP_5_SCALE
  uniform float m_DiffuseMap_5_scale;
#endif
#ifdef DIFFUSEMAP_6_SCALE
  uniform float m_DiffuseMap_6_scale;
#endif
#ifdef DIFFUSEMAP_7_SCALE
  uniform float m_DiffuseMap_7_scale;
#endif
#ifdef DIFFUSEMAP_8_SCALE
  uniform float m_DiffuseMap_8_scale;
#endif
#ifdef DIFFUSEMAP_9_SCALE
  uniform float m_DiffuseMap_9_scale;
#endif
#ifdef DIFFUSEMAP_10_SCALE
  uniform float m_DiffuseMap_10_scale;
#endif
#ifdef DIFFUSEMAP_11_SCALE
  uniform float m_DiffuseMap_11_scale;
#endif


#ifdef ALPHAMAP
  uniform sampler2D m_AlphaMap;
#endif
#ifdef ALPHAMAP_1
  uniform sampler2D m_AlphaMap_1;
#endif
#ifdef ALPHAMAP_2
  uniform sampler2D m_AlphaMap_2;
#endif

#ifdef NORMALMAP
  uniform sampler2D m_NormalMap;
#endif
#ifdef NORMALMAP_1
  uniform sampler2D m_NormalMap_1;
#endif
#ifdef NORMALMAP_2
  uniform sampler2D m_NormalMap_2;
#endif
#ifdef NORMALMAP_3
  uniform sampler2D m_NormalMap_3;
#endif
#ifdef NORMALMAP_4
  uniform sampler2D m_NormalMap_4;
#endif
#ifdef NORMALMAP_5
  uniform sampler2D m_NormalMap_5;
#endif
#ifdef NORMALMAP_6
  uniform sampler2D m_NormalMap_6;
#endif
#ifdef NORMALMAP_7
  uniform sampler2D m_NormalMap_7;
#endif
#ifdef NORMALMAP_8
  uniform sampler2D m_NormalMap_8;
#endif
#ifdef NORMALMAP_9
  uniform sampler2D m_NormalMap_9;
#endif
#ifdef NORMALMAP_10
  uniform sampler2D m_NormalMap_10;
#endif
#ifdef NORMALMAP_11
  uniform sampler2D m_NormalMap_11;
#endif


#ifdef TRI_PLANAR_MAPPING
  varying vec4 wVertex;
  varying vec3 wNormal;
#endif



float tangDot(in vec3 v1, in vec3 v2){
    float d = dot(v1,v2);
    #ifdef V_TANGENT
        d = 1.0 - d*d;
        return step(0.0, d) * sqrt(d);
    #else
        return d;
    #endif
}


float lightComputeDiffuse(in vec3 norm, in vec3 lightdir, in vec3 viewdir){
    return max(0.0, dot(norm, lightdir));
}

float lightComputeSpecular(in vec3 norm, in vec3 viewdir, in vec3 lightdir, in float shiny){
    #ifdef WARDISO
        // Isotropic Ward
        vec3 halfVec = normalize(viewdir + lightdir);
        float NdotH  = max(0.001, tangDot(norm, halfVec));
        float NdotV  = max(0.001, tangDot(norm, viewdir));
        float NdotL  = max(0.001, tangDot(norm, lightdir));
        float a      = tan(acos(NdotH));
        float p      = max(shiny/128.0, 0.001);
        return NdotL * (1.0 / (4.0*3.14159265*p*p)) * (exp(-(a*a)/(p*p)) / (sqrt(NdotV * NdotL)));
    #else
       // Standard Phong
       vec3 R = reflect(-lightdir, norm);
       return pow(max(tangDot(R, viewdir), 0.0), shiny);
    #endif
}

vec2 computeLighting(in vec3 wvPos, in vec3 wvNorm, in vec3 wvViewDir, in vec3 wvLightDir){
   float diffuseFactor = lightComputeDiffuse(wvNorm, wvLightDir, wvViewDir);
   float specularFactor = lightComputeSpecular(wvNorm, wvViewDir, wvLightDir, m_Shininess);
   specularFactor *= step(1.0, m_Shininess);

   float att = vLightDir.w;

   return vec2(diffuseFactor, specularFactor) * vec2(att);
}


#ifdef ALPHAMAP

  vec4 calculateDiffuseBlend(in vec2 texCoord) {
    vec4 alphaBlend   = texture2D( m_AlphaMap, texCoord.xy );
    
    #ifdef ALPHAMAP_1
      vec4 alphaBlend1   = texture2D( m_AlphaMap_1, texCoord.xy );
    #endif
    #ifdef ALPHAMAP_2
      vec4 alphaBlend2   = texture2D( m_AlphaMap_2, texCoord.xy );
    #endif

    vec4 diffuseColor = texture2D(m_DiffuseMap, texCoord * m_DiffuseMap_0_scale);
    diffuseColor *= alphaBlend.r;
    #ifdef DIFFUSEMAP_1
      vec4 diffuseColor1 = texture2D(m_DiffuseMap_1, texCoord * m_DiffuseMap_1_scale);
      diffuseColor = mix( diffuseColor, diffuseColor1, alphaBlend.g );
      #ifdef DIFFUSEMAP_2
        vec4 diffuseColor2 = texture2D(m_DiffuseMap_2, texCoord * m_DiffuseMap_2_scale);
        diffuseColor = mix( diffuseColor, diffuseColor2, alphaBlend.b );
        #ifdef DIFFUSEMAP_3
          vec4 diffuseColor3 = texture2D(m_DiffuseMap_3, texCoord * m_DiffuseMap_3_scale);
          diffuseColor = mix( diffuseColor, diffuseColor3, alphaBlend.a );
          #ifdef ALPHAMAP_1
              #ifdef DIFFUSEMAP_4
                vec4 diffuseColor4 = texture2D(m_DiffuseMap_4, texCoord * m_DiffuseMap_4_scale);
                diffuseColor = mix( diffuseColor, diffuseColor4, alphaBlend1.r );
                #ifdef DIFFUSEMAP_5
                  vec4 diffuseColor5 = texture2D(m_DiffuseMap_5, texCoord * m_DiffuseMap_5_scale);
                  diffuseColor = mix( diffuseColor, diffuseColor5, alphaBlend1.g );
                  #ifdef DIFFUSEMAP_6
                    vec4 diffuseColor6 = texture2D(m_DiffuseMap_6, texCoord * m_DiffuseMap_6_scale);
                    diffuseColor = mix( diffuseColor, diffuseColor6, alphaBlend1.b );
                    #ifdef DIFFUSEMAP_7
                      vec4 diffuseColor7 = texture2D(m_DiffuseMap_7, texCoord * m_DiffuseMap_7_scale);
                      diffuseColor = mix( diffuseColor, diffuseColor7, alphaBlend1.a );
                      #ifdef ALPHAMAP_2
                          #ifdef DIFFUSEMAP_8
                            vec4 diffuseColor8 = texture2D(m_DiffuseMap_8, texCoord * m_DiffuseMap_8_scale);
                            diffuseColor = mix( diffuseColor, diffuseColor8, alphaBlend2.r );
                            #ifdef DIFFUSEMAP_9
                              vec4 diffuseColor9 = texture2D(m_DiffuseMap_9, texCoord * m_DiffuseMap_9_scale);
                              diffuseColor = mix( diffuseColor, diffuseColor9, alphaBlend2.g );
                              #ifdef DIFFUSEMAP_10
                                vec4 diffuseColor10 = texture2D(m_DiffuseMap_10, texCoord * m_DiffuseMap_10_scale);
                                diffuseColor = mix( diffuseColor, diffuseColor10, alphaBlend2.b );
                                #ifdef DIFFUSEMAP_11
                                  vec4 diffuseColor11 = texture2D(m_DiffuseMap_11, texCoord * m_DiffuseMap_11_scale);
                                  diffuseColor = mix( diffuseColor, diffuseColor11, alphaBlend2.a );
                                #endif
                              #endif
                            #endif
                          #endif
                      #endif
                    #endif
                  #endif
                #endif
              #endif
          #endif
        #endif
      #endif
    #endif
    return diffuseColor;
  }

  vec3 calculateNormal(in vec2 texCoord) {
    vec3 normal = vec3(0,0,1);
    vec3 n = vec3(0,0,0);

    vec4 alphaBlend = texture2D( m_AlphaMap, texCoord.xy );

    #ifdef ALPHAMAP_1
      vec4 alphaBlend1 = texture2D( m_AlphaMap_1, texCoord.xy );
    #endif
    #ifdef ALPHAMAP_2
      vec4 alphaBlend2 = texture2D( m_AlphaMap_2, texCoord.xy );
    #endif

    #ifdef NORMALMAP
      n = texture2D(m_NormalMap, texCoord * m_DiffuseMap_0_scale).xyz;
      normal += n * alphaBlend.r;
    #endif

    #ifdef NORMALMAP_1
      n = texture2D(m_NormalMap_1, texCoord * m_DiffuseMap_1_scale).xyz;
      normal += n * alphaBlend.g;
    #endif

    #ifdef NORMALMAP_2
      n = texture2D(m_NormalMap_2, texCoord * m_DiffuseMap_2_scale).xyz;
      normal += n * alphaBlend.b;
    #endif

    #ifdef NORMALMAP_3
      n = texture2D(m_NormalMap_3, texCoord * m_DiffuseMap_3_scale).xyz;
      normal += n * alphaBlend.a;
    #endif

    #ifdef ALPHAMAP_1
        #ifdef NORMALMAP_4
          n = texture2D(m_NormalMap_4, texCoord * m_DiffuseMap_4_scale).xyz;
          normal += n * alphaBlend1.r;
        #endif

        #ifdef NORMALMAP_5
          n = texture2D(m_NormalMap_5, texCoord * m_DiffuseMap_5_scale).xyz;
          normal += n * alphaBlend1.g;
        #endif

        #ifdef NORMALMAP_6
          n = texture2D(m_NormalMap_6, texCoord * m_DiffuseMap_6_scale).xyz;
          normal += n * alphaBlend1.b;
        #endif

        #ifdef NORMALMAP_7
          n = texture2D(m_NormalMap_7, texCoord * m_DiffuseMap_7_scale).xyz;
          normal += n * alphaBlend1.a;
        #endif
    #endif

    #ifdef ALPHAMAP_2
        #ifdef NORMALMAP_8
          n = texture2D(m_NormalMap_8, texCoord * m_DiffuseMap_8_scale).xyz;
          normal += n * alphaBlend2.r;
        #endif

        #ifdef NORMALMAP_9
          n = texture2D(m_NormalMap_9, texCoord * m_DiffuseMap_9_scale);
          normal += n * alphaBlend2.g;
        #endif

        #ifdef NORMALMAP_10
          n = texture2D(m_NormalMap_10, texCoord * m_DiffuseMap_10_scale);
          normal += n * alphaBlend2.b;
        #endif

        #ifdef NORMALMAP_11
          n = texture2D(m_NormalMap_11, texCoord * m_DiffuseMap_11_scale);
          normal += n * alphaBlend2.a;
        #endif
    #endif

    normal = (normal.xyz * vec3(2.0) - vec3(1.0));
    return normalize(normal);
  }

  #ifdef TRI_PLANAR_MAPPING

    vec4 getTriPlanarBlend(in vec4 coords, in vec3 blending, in sampler2D map, in float scale) {
      vec4 col1 = texture2D( map, coords.yz * scale);
      vec4 col2 = texture2D( map, coords.xz * scale);
      vec4 col3 = texture2D( map, coords.xy * scale);
      // blend the results of the 3 planar projections.
      vec4 tex = col1 * blending.x + col2 * blending.y + col3 * blending.z;
      return tex;
    }

    vec4 calculateTriPlanarDiffuseBlend(in vec3 wNorm, in vec4 wVert, in vec2 texCoord) {
        // tri-planar texture bending factor for this fragment's normal
        vec3 blending = abs( wNorm );
        blending = (blending -0.2) * 0.7;
        blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
        float b = (blending.x + blending.y + blending.z);
        blending /= vec3(b, b, b);

        // texture coords
        vec4 coords = wVert;

        // blend the results of the 3 planar projections.
        vec4 tex0 = getTriPlanarBlend(coords, blending, m_DiffuseMap, m_DiffuseMap_0_scale);

        #ifdef DIFFUSEMAP_1
          // blend the results of the 3 planar projections.
          vec4 tex1 = getTriPlanarBlend(coords, blending, m_DiffuseMap_1, m_DiffuseMap_1_scale);
        #endif
        #ifdef DIFFUSEMAP_2
          // blend the results of the 3 planar projections.
          vec4 tex2 = getTriPlanarBlend(coords, blending, m_DiffuseMap_2, m_DiffuseMap_2_scale);
        #endif
        #ifdef DIFFUSEMAP_3
          // blend the results of the 3 planar projections.
          vec4 tex3 = getTriPlanarBlend(coords, blending, m_DiffuseMap_3, m_DiffuseMap_3_scale);
        #endif
        #ifdef DIFFUSEMAP_4
          // blend the results of the 3 planar projections.
          vec4 tex4 = getTriPlanarBlend(coords, blending, m_DiffuseMap_4, m_DiffuseMap_4_scale);
        #endif
        #ifdef DIFFUSEMAP_5
          // blend the results of the 3 planar projections.
          vec4 tex5 = getTriPlanarBlend(coords, blending, m_DiffuseMap_5, m_DiffuseMap_5_scale);
        #endif
        #ifdef DIFFUSEMAP_6
          // blend the results of the 3 planar projections.
          vec4 tex6 = getTriPlanarBlend(coords, blending, m_DiffuseMap_6, m_DiffuseMap_6_scale);
        #endif
        #ifdef DIFFUSEMAP_7
          // blend the results of the 3 planar projections.
          vec4 tex7 = getTriPlanarBlend(coords, blending, m_DiffuseMap_7, m_DiffuseMap_7_scale);
        #endif
        #ifdef DIFFUSEMAP_8
          // blend the results of the 3 planar projections.
          vec4 tex8 = getTriPlanarBlend(coords, blending, m_DiffuseMap_8, m_DiffuseMap_8_scale);
        #endif
        #ifdef DIFFUSEMAP_9
          // blend the results of the 3 planar projections.
          vec4 tex9 = getTriPlanarBlend(coords, blending, m_DiffuseMap_9, m_DiffuseMap_9_scale);
        #endif
        #ifdef DIFFUSEMAP_10
          // blend the results of the 3 planar projections.
          vec4 tex10 = getTriPlanarBlend(coords, blending, m_DiffuseMap_10, m_DiffuseMap_10_scale);
        #endif
        #ifdef DIFFUSEMAP_11
          // blend the results of the 3 planar projections.
          vec4 tex11 = getTriPlanarBlend(coords, blending, m_DiffuseMap_11, m_DiffuseMap_11_scale);
        #endif

        vec4 alphaBlend   = texture2D( m_AlphaMap, texCoord.xy );

        #ifdef ALPHAMAP_1
          vec4 alphaBlend1   = texture2D( m_AlphaMap_1, texCoord.xy );
        #endif
        #ifdef ALPHAMAP_2
          vec4 alphaBlend2   = texture2D( m_AlphaMap_2, texCoord.xy );
        #endif

        vec4 diffuseColor = tex0 * alphaBlend.r;
        #ifdef DIFFUSEMAP_1
          diffuseColor = mix( diffuseColor, tex1, alphaBlend.g );
          #ifdef DIFFUSEMAP_2
            diffuseColor = mix( diffuseColor, tex2, alphaBlend.b );
            #ifdef DIFFUSEMAP_3
              diffuseColor = mix( diffuseColor, tex3, alphaBlend.a );
              #ifdef ALPHAMAP_1
                  #ifdef DIFFUSEMAP_4
                    diffuseColor = mix( diffuseColor, tex4, alphaBlend1.r );
                    #ifdef DIFFUSEMAP_5
                      diffuseColor = mix( diffuseColor, tex5, alphaBlend1.g );
                      #ifdef DIFFUSEMAP_6
                        diffuseColor = mix( diffuseColor, tex6, alphaBlend1.b );
                        #ifdef DIFFUSEMAP_7
                          diffuseColor = mix( diffuseColor, tex7, alphaBlend1.a );
                          #ifdef ALPHAMAP_2
                              #ifdef DIFFUSEMAP_8
                                diffuseColor = mix( diffuseColor, tex8, alphaBlend2.r );
                                #ifdef DIFFUSEMAP_9
                                  diffuseColor = mix( diffuseColor, tex9, alphaBlend2.g );
                                  #ifdef DIFFUSEMAP_10
                                    diffuseColor = mix( diffuseColor, tex10, alphaBlend2.b );
                                    #ifdef DIFFUSEMAP_11
                                      diffuseColor = mix( diffuseColor, tex11, alphaBlend2.a );
                                    #endif
                                  #endif
                                #endif
                              #endif
                          #endif
                        #endif
                      #endif
                    #endif
                  #endif
              #endif
            #endif
          #endif
        #endif

        return diffuseColor;
    }

    vec3 calculateNormalTriPlanar(in vec3 wNorm, in vec4 wVert,in vec2 texCoord) {
      // tri-planar texture bending factor for this fragment's world-space normal
      vec3 blending = abs( wNorm );
      blending = (blending -0.2) * 0.7;
      blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
      float b = (blending.x + blending.y + blending.z);
      blending /= vec3(b, b, b);

      // texture coords
      vec4 coords = wVert;
      vec4 alphaBlend = texture2D( m_AlphaMap, texCoord.xy );

    #ifdef ALPHAMAP_1
      vec4 alphaBlend1 = texture2D( m_AlphaMap_1, texCoord.xy );
    #endif
    #ifdef ALPHAMAP_2
      vec4 alphaBlend2 = texture2D( m_AlphaMap_2, texCoord.xy );
    #endif

      vec3 normal = vec3(0,0,1);
      vec3 n = vec3(0,0,0);

      #ifdef NORMALMAP
          n = getTriPlanarBlend(coords, blending, m_NormalMap, m_DiffuseMap_0_scale).xyz;
          normal += n * alphaBlend.r;
      #endif

      #ifdef NORMALMAP_1
          n = getTriPlanarBlend(coords, blending, m_NormalMap_1, m_DiffuseMap_1_scale).xyz;
          normal += n * alphaBlend.g;
      #endif

      #ifdef NORMALMAP_2
          n = getTriPlanarBlend(coords, blending, m_NormalMap_2, m_DiffuseMap_2_scale).xyz;
          normal += n * alphaBlend.b;
      #endif

      #ifdef NORMALMAP_3
          n = getTriPlanarBlend(coords, blending, m_NormalMap_3, m_DiffuseMap_3_scale).xyz;
          normal += n * alphaBlend.a;
      #endif

      #ifdef ALPHAMAP_1
          #ifdef NORMALMAP_4
              n = getTriPlanarBlend(coords, blending, m_NormalMap_4, m_DiffuseMap_4_scale).xyz;
              normal += n * alphaBlend1.r;
          #endif

          #ifdef NORMALMAP_5
              n = getTriPlanarBlend(coords, blending, m_NormalMap_5, m_DiffuseMap_5_scale).xyz;
              normal += n * alphaBlend1.g;
          #endif

          #ifdef NORMALMAP_6
              n = getTriPlanarBlend(coords, blending, m_NormalMap_6, m_DiffuseMap_6_scale).xyz;
              normal += n * alphaBlend1.b;
          #endif

          #ifdef NORMALMAP_7
              n = getTriPlanarBlend(coords, blending, m_NormalMap_7, m_DiffuseMap_7_scale).xyz;
              normal += n * alphaBlend1.a;
          #endif
      #endif

      #ifdef ALPHAMAP_2
          #ifdef NORMALMAP_8
              n = getTriPlanarBlend(coords, blending, m_NormalMap_8, m_DiffuseMap_8_scale).xyz;
              normal += n * alphaBlend2.r;
          #endif

          #ifdef NORMALMAP_9
              n = getTriPlanarBlend(coords, blending, m_NormalMap_9, m_DiffuseMap_9_scale).xyz;
              normal += n * alphaBlend2.g;
          #endif

          #ifdef NORMALMAP_10
              n = getTriPlanarBlend(coords, blending, m_NormalMap_10, m_DiffuseMap_10_scale).xyz;
              normal += n * alphaBlend2.b;
          #endif

          #ifdef NORMALMAP_11
              n = getTriPlanarBlend(coords, blending, m_NormalMap_11, m_DiffuseMap_11_scale).xyz;
              normal += n * alphaBlend2.a;
          #endif
      #endif

      normal = (normal.xyz * vec3(2.0) - vec3(1.0));
      return normalize(normal);
    }
  #endif

#endif



void main(){

    //----------------------
    // diffuse calculations
    //----------------------
    #ifdef DIFFUSEMAP
      #ifdef ALPHAMAP
        #ifdef TRI_PLANAR_MAPPING
            vec4 diffuseColor = calculateTriPlanarDiffuseBlend(wNormal, wVertex, texCoord);
        #else
            vec4 diffuseColor = calculateDiffuseBlend(texCoord);
        #endif
      #else
        vec4 diffuseColor = texture2D(m_DiffuseMap, texCoord);
      #endif
    #else
      vec4 diffuseColor = vec4(1.0);
    #endif

        float spotFallOff = 1.0;
        if(g_LightDirection.w!=0.0){
              vec3 L=normalize(lightVec.xyz);
              vec3 spotdir = normalize(g_LightDirection.xyz);
              float curAngleCos = dot(-L, spotdir);             
              float innerAngleCos = floor(g_LightDirection.w) * 0.001;
              float outerAngleCos = fract(g_LightDirection.w);
              float innerMinusOuter = innerAngleCos - outerAngleCos;

              spotFallOff = (curAngleCos - outerAngleCos) / innerMinusOuter;

              if(spotFallOff <= 0.0){
                  gl_FragColor = AmbientSum * diffuseColor;
                  return;
              }else{
                  spotFallOff = clamp(spotFallOff, 0.0, 1.0);
              }
        }
    
    //---------------------
    // normal calculations
    //---------------------
    #if defined(NORMALMAP) || defined(NORMALMAP_1) || defined(NORMALMAP_2) || defined(NORMALMAP_3) || defined(NORMALMAP_4) || defined(NORMALMAP_5) || defined(NORMALMAP_6) || defined(NORMALMAP_7) || defined(NORMALMAP_8) || defined(NORMALMAP_9) || defined(NORMALMAP_10) || defined(NORMALMAP_11)
      #ifdef TRI_PLANAR_MAPPING
        vec3 normal = calculateNormalTriPlanar(wNormal, wVertex, texCoord);
      #else
        vec3 normal = calculateNormal(texCoord);
      #endif
    #else
      vec3 normal = vNormal;
    #endif


    //-----------------------
    // lighting calculations
    //-----------------------
    vec4 lightDir = vLightDir;
    lightDir.xyz = normalize(lightDir.xyz);

    vec2 light = computeLighting(vPosition, normal, vViewDir.xyz, lightDir.xyz)*spotFallOff;

    vec4 specularColor = vec4(1.0);

    //--------------------------
    // final color calculations
    //--------------------------
    gl_FragColor =  AmbientSum * diffuseColor +
                    DiffuseSum * diffuseColor  * light.x +
                    SpecularSum * specularColor * light.y;

    //gl_FragColor.a = alpha;
}