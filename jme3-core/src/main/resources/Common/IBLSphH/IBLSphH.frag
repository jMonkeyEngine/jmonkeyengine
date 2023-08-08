/**

*   - Riccardo Balbo
*/
#import "Common/IBL/Math.glsllib"

// #define NUM_SH_COEFFICIENT 9
#ifndef PI
    #define PI 3.1415926535897932384626433832795
#endif

out vec4 outFragColor;
in vec2 TexCoords;
in vec3 LocalPos;


uniform samplerCube m_Texture;
#ifdef SH_COEF
    uniform sampler2D m_ShCoef;
#endif
uniform vec2 m_Resolution;
uniform int m_FaceId;

const float sqrtPi = sqrt(PI);
const float sqrt3Pi = sqrt(3 / PI);
const float sqrt5Pi = sqrt(5 / PI);
const float sqrt15Pi = sqrt(15 / PI);

#ifdef REMAP_MAX_VALUE
    uniform float m_RemapMaxValue;
#endif


vec3 getVectorFromCubemapFaceTexCoord(float x, float y, float mapSize, int face) {
    float u;
    float v;

    /* transform from [0..res - 1] to [- (1 - 1 / res) .. (1 - 1 / res)]
        * (+ 0.5f is for texel center addressing) */
    u = (2.0 * (x + 0.5) / mapSize) - 1.0;
    v = (2.0 * (y + 0.5) / mapSize) - 1.0;
    

    // Warp texel centers in the proximity of the edges.
    float a = pow(mapSize, 2.0) / pow(mapSize - 1, 3.0);

    u = a * pow(u, 3) + u;
    v = a * pow(v, 3) + v;
    //compute vector depending on the face
    // Code from Nvtt : https://github.com/castano/nvidia-texture-tools/blob/master/src/nvtt/CubeSurface.cpp#L101
    vec3 o =vec3(0);
    switch(face) {
        case 0:
            o= normalize(vec3(1, -v, -u));
            break;
        case 1:
            o= normalize(vec3(-1, -v, u));
            break;
        case 2:
            o= normalize(vec3(u, 1, v));
            break;
        case 3:
            o= normalize(vec3(u, -1, -v));
            break;
        case 4:
            o= normalize(vec3(u, -v, 1));
            break;
        case 5:
            o= normalize(vec3(-u, -v, -1.0));
            break;
    }

    return o;
}

float atan2(in float y, in float x) {
    bool s = (abs(x) > abs(y));
    return mix(PI / 2.0 - atan(x, y), atan(y, x), s);
}

float areaElement(float x, float y) {
    return atan2(x * y, sqrt(x * x + y * y + 1.));
}

float getSolidAngleAndVector(float x, float y, float mapSize, int face, out vec3 store) {
    /* transform from [0..res - 1] to [- (1 - 1 / res) .. (1 - 1 / res)]
        (+ 0.5f is for texel center addressing) */
    float u = (2.0 * (x + 0.5) / mapSize) - 1.0;
    float v = (2.0 * (y + 0.5) / mapSize) - 1.0;

    store = getVectorFromCubemapFaceTexCoord(x, y, mapSize, face);

    /* Solid angle weight approximation :
        * U and V are the -1..1 texture coordinate on the current face.
        * Get projected area for this texel */
    float x0, y0, x1, y1;
    float invRes = 1.0 / mapSize;
    x0 = u - invRes;
    y0 = v - invRes;
    x1 = u + invRes;
    y1 = v + invRes;

    return areaElement(x0, y0) - areaElement(x0, y1) - areaElement(x1, y0) + areaElement(x1, y1);
}

void evalShBasis(vec3 texelVect, int i, out float shDir) {
    float xV = texelVect.x;
    float yV = texelVect.y;
    float zV = texelVect.z;

    float x2 = xV * xV;
    float y2 = yV * yV;
    float z2 = zV * zV;

    if(i==0) shDir = (1. / (2. * sqrtPi));
    else if(i==1) shDir = -(sqrt3Pi * yV) / 2.;
    else if(i == 2) shDir = (sqrt3Pi * zV) / 2.;
    else if(i == 3) shDir = -(sqrt3Pi * xV) / 2.;
    else if(i == 4) shDir = (sqrt15Pi * xV * yV) / 2.;
    else if(i == 5) shDir = -(sqrt15Pi * yV * zV) / 2.;
    else if(i == 6) shDir = (sqrt5Pi * (-1. + 3. * z2)) / 4.;
    else if(i == 7) shDir = -(sqrt15Pi * xV * zV) / 2.;
    else shDir = sqrt15Pi * (x2 - y2) / 4.;
}

vec3 pixelFaceToV(int faceId, float pixelX, float pixelY, float cubeMapSize) {
    vec2 normalizedCoords = vec2((2.0 * pixelX + 1.0) / cubeMapSize, (2.0 * pixelY + 1.0) / cubeMapSize);

    vec3 direction;
    if(faceId == 0) {
        direction = vec3(1.0, -normalizedCoords.y, -normalizedCoords.x);
    } else if(faceId == 1) {
        direction = vec3(-1.0, -normalizedCoords.y, normalizedCoords.x);
    } else if(faceId == 2) {
        direction = vec3(normalizedCoords.x, 1.0, normalizedCoords.y);
    } else if(faceId == 3) {
        direction = vec3(normalizedCoords.x, -1.0, -normalizedCoords.y);
    } else if(faceId == 4) {
        direction = vec3(normalizedCoords.x, -normalizedCoords.y, 1.0);
    } else if(faceId == 5) {
        direction = vec3(-normalizedCoords.x, -normalizedCoords.y, -1.0);
    }

    return normalize(direction);
}

void sphKernel() {
    int width = int(m_Resolution.x);
    int height = int(m_Resolution.y);
    vec3 texelVect=vec3(0);    
    float shDir=0;
    float weight=0;
    vec4 color=vec4(0);

    int i=int(gl_FragCoord.x);

    #ifdef SH_COEF
        vec4 r=texelFetch(m_ShCoef, ivec2(i, 0), 0);
        vec3 shCoef=r.rgb;
        float weightAccum = r.a;
    #else
        vec3 shCoef=vec3(0.0);
        float weightAccum = 0.0;
    #endif

    for(int y = 0; y < height; y++) {
        for(int x = 0; x < width; x++) {
            weight = getSolidAngleAndVector(float(x), float(y), float(width), m_FaceId, texelVect);
            evalShBasis(texelVect, i, shDir);
            color = texture(m_Texture, texelVect);
            shCoef.x = (shCoef.x + color.r * shDir * weight);
            shCoef.y = (shCoef.y + color.g * shDir * weight);
            shCoef.z = (shCoef.z + color.b * shDir * weight);
            weightAccum += weight;
        }
    }



    #ifdef REMAP_MAX_VALUE
        shCoef.xyz=shCoef.xyz*m_RemapMaxValue;
        weightAccum=weightAccum*m_RemapMaxValue;
    #endif

    outFragColor = vec4(shCoef.xyz,weightAccum);

}

void main() {
    sphKernel();
}