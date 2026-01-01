#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Shadows.glsllib"

//Stripped version of the usual shadow fragment shader for SDSM; it intentionally leaves out some features.
uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
uniform mat4 m_ViewProjectionMatrixInverse;
uniform vec4 m_ViewProjectionMatrixRow2;

varying vec2 texCoord;

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
0.0, 0.5, 0.0, 0.0,
0.0, 0.0, 0.5, 0.0,
0.5, 0.5, 0.5, 1.0);

uniform mat4 m_LightViewProjectionMatrix0;
uniform mat4 m_LightViewProjectionMatrix1;
uniform mat4 m_LightViewProjectionMatrix2;
uniform mat4 m_LightViewProjectionMatrix3;

uniform vec2 g_ResolutionInverse;

uniform vec3 m_LightDir;

uniform vec2[3] m_Splits;

vec3 getPosition(in float depth, in vec2 uv){
    vec4 pos = vec4(uv, depth, 1.0) * 2.0 - 1.0;
    pos = m_ViewProjectionMatrixInverse * pos;
    return pos.xyz / pos.w;
}


float determineShadow(int index, vec4 worldPos){
    vec4 projCoord;
    if(index == 0){
        projCoord = biasMat * m_LightViewProjectionMatrix0 * worldPos;
        return GETSHADOW(m_ShadowMap0, projCoord);
    } else if(index == 1){
        projCoord = biasMat * m_LightViewProjectionMatrix1 * worldPos;
        return GETSHADOW(m_ShadowMap1, projCoord);
    } else if(index == 2){
        projCoord = biasMat * m_LightViewProjectionMatrix2 * worldPos;
        return GETSHADOW(m_ShadowMap2, projCoord);
    } else if(index == 3){
        projCoord = biasMat * m_LightViewProjectionMatrix3 * worldPos;
        return GETSHADOW(m_ShadowMap3, projCoord);
    }
    return 1f;
}

void main() {
    float depth = texture2D(m_DepthTexture,texCoord).r;
    vec4 color = texture2D(m_Texture,texCoord);

    //Discard shadow computation on the sky
    if(depth == 1.0){
        gl_FragColor = color;
        return;
    }

    vec4 worldPos = vec4(getPosition(depth,texCoord),1.0);

    float shadow = 1.0;

    int primary = 0;
    int secondary = -1;
    float mixture = 0;
    while(primary < 3){
        vec2 split = m_Splits[primary];
        if(depth < split.y){
            if(depth >= split.x){
                secondary = primary + 1;
                mixture = (depth - split.x) / (split.y - split.x);
            }
            break;
        }
        primary += 1;
    }
    shadow = determineShadow(primary, worldPos);
    if(secondary >= 0){
        float secondaryShadow = determineShadow(secondary, worldPos);
        shadow = mix(shadow, secondaryShadow, mixture);
    }

    shadow = shadow * m_ShadowIntensity + (1.0 - m_ShadowIntensity);

    gl_FragColor = color * vec4(shadow, shadow, shadow, 1.0);
}



