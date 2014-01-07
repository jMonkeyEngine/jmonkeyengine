uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

uniform vec4 m_Ambient;
uniform vec4 m_Diffuse;
uniform vec4 m_Specular;
uniform float m_Shininess;

varying vec2 texCoord;

varying vec4 AmbientSum;
varying vec4 DiffuseSum;
varying vec4 SpecularSum;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;

#ifdef NORMALMAP
attribute vec3 inTangent;
varying mat3 tbnMat;
#endif

#ifdef VERTEX_COLOR
  attribute vec4 inColor;
#endif

varying vec3 vNormal;
varying float vDepth;

void main(){
   vec4 pos = vec4(inPosition, 1.0);
   gl_Position = g_WorldViewProjectionMatrix * pos;
   texCoord = inTexCoord;

   #if defined(NORMALMAP)
     vec4 wvNormal, wvTangent, wvBinormal;

     wvNormal   = vec4(inNormal, 0.0);
     wvTangent  = vec4(inTangent, 0.0);

     wvNormal.xyz   = normalize( (g_WorldMatrix * wvNormal).xyz   );
     wvTangent.xyz  = normalize( (g_WorldMatrix * wvTangent).xyz  );
     wvBinormal.xyz = cross(wvNormal.xyz, wvTangent.xyz);
     tbnMat = mat3(wvTangent.xyz, wvBinormal.xyz, wvNormal.xyz);

     vNormal = wvNormal.xyz;
   #else
     vec4 wvNormal;
     #ifdef V_TANGENT
        wvNormal = vec4(inTangent, 0.0);
     #else
        wvNormal = vec4(inNormal, 0.0);
     #endif
     vNormal = normalize( (g_WorldMatrix * wvNormal).xyz );
   #endif

   #ifdef MATERIAL_COLORS
      AmbientSum  = m_Ambient;
      DiffuseSum  = m_Diffuse;
      SpecularSum = m_Specular;
    #else
      AmbientSum  = vec4(0.0);
      DiffuseSum  = vec4(1.0);
      SpecularSum = vec4(1.0);
    #endif

    #ifdef VERTEX_COLOR
      DiffuseSum *= inColor;
    #endif
}