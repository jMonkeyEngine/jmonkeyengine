/*
GLSL conversion of Michael Horsch water demo
http://www.bonzaisoftware.com/wfs.html
Converted by Mars_999
8/20/2005
*/

uniform sampler2D m_water_normalmap;
uniform sampler2D m_water_reflection;
uniform sampler2D m_water_refraction;
uniform sampler2D m_water_dudvmap;
uniform sampler2D m_water_depthmap;
uniform vec4 m_waterColor;
uniform float m_waterDepth;
uniform vec4 m_distortionScale;
uniform vec4 m_distortionMix;
uniform vec4 m_texScale;
uniform vec2 m_FrustumNearFar;
uniform float m_waterTransparency;



varying vec4 lightDir; //lightpos
varying vec4 waterTex1; //moving texcoords
varying vec4 waterTex2; //moving texcoords
varying vec4 position; //for projection
varying vec4 viewDir; //viewts
varying vec4 viewLightDir;
varying vec4 viewCamDir;

//unit 0 = m_water_reflection
//unit 1 = m_water_refraction
//unit 2 = m_water_normalmap
//unit 3 = m_water_dudvmap
//unit 4 = m_water_depthmap

 const vec4 two = vec4(2.0, 2.0, 2.0, 1.0);
 const vec4 mone = vec4(-1.0, -1.0, -1.0, 1.0);

 const vec4 ofive = vec4(0.5,0.5,0.5,1.0);

 const float exponent = 64.0;

float tangDot(in vec3 v1, in vec3 v2){
    float d = dot(v1,v2);
    #ifdef V_TANGENT
        d = 1.0 - d*d;
        return step(0.0, d) * sqrt(d);
    #else
        return d;
    #endif
}

vec4 readDepth(vec2 uv){
    float depth= (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - texture2D(m_water_depthmap, uv).r* (m_FrustumNearFar.y-m_FrustumNearFar.x));
    return vec4( depth);
}

void main(void)
{
 

     vec4 lightTS = normalize(lightDir);
     vec4 viewt = normalize(viewDir);
     vec4 disdis = texture2D(m_water_dudvmap, vec2(waterTex2 * m_texScale));
     vec4 fdist = texture2D(m_water_dudvmap, vec2(waterTex1 + disdis*m_distortionMix));
     fdist =normalize( fdist * 2.0 - 1.0)* m_distortionScale;
  

     //load normalmap
     vec4 nmap = texture2D(m_water_normalmap, vec2(waterTex1 + disdis*m_distortionMix));
     nmap = (nmap-ofive) * two;
    // nmap = nmap*2.0-1.0;
     vec4 vNorm = normalize(nmap);

     
     vec4 projCoord = position / position.w;
     projCoord =(projCoord+1.0)*0.5 + fdist;
     projCoord = clamp(projCoord, 0.001, 0.999);

     //load reflection,refraction and depth texture
     vec4 refl = texture2D(m_water_reflection, vec2(projCoord.x,1.0-projCoord.y));
     vec4 refr = texture2D(m_water_refraction, vec2(projCoord));
     vec4 wdepth =readDepth(vec2(projCoord));
  
     wdepth = vec4(pow(wdepth.x, m_waterDepth));
     vec4 invdepth = 1.0 - wdepth;


 // Blinn - Phong
  //    vec4 H = (viewt - lightTS);
  //   vec4 specular =vec4(pow(max(dot(H, vNorm), 0.0), exponent));

// Standard Phong

  //   vec4 R =reflect(-L, vNorm);
 //    vec4 specular =vec4( pow(max(dot(R, E), 0.0),exponent));

 
     //calculate specular highlight
     vec4 L=normalize(viewLightDir);  
    vec4 E=normalize(viewCamDir);
     vec4 vRef = normalize(reflect(-L,vNorm));
     float stemp =max(0.0, dot( vRef,E) );
     //initializing to 0 to avoid artifacts on old intel cards
     vec4 specular = vec4(0.0,0.0,0.0,0.0);
    if(stemp>0.0){
         stemp = pow(stemp, exponent);
         specular = vec4(stemp);
    }



    vec4 fresnelTerm = vec4(0.02+0.97*pow((1.0-dot(normalize(viewt), vNorm)),5.0));



    fresnelTerm=fresnelTerm*invdepth*m_waterTransparency;
    fresnelTerm=clamp(fresnelTerm,0.0,1.0);

    refr*=(fresnelTerm);
    refr *= invdepth;
    refr= refr+ m_waterColor*wdepth*fresnelTerm;

    gl_FragColor =(refr+ refl*(1.0-fresnelTerm))+specular;
}
