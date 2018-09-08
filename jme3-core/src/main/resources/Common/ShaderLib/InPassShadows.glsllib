#import "Common/ShaderLib/GLSLCompat.glsllib"

#extension GL_EXT_texture_array : enable

#ifndef NUM_PSSM_SPLITS
#define NUM_PSSM_SPLITS 0
#endif

#ifdef IN_PASS_SHADOWS

    uniform mat4 g_ShadowMatrices[(NB_LIGHTS/3) + NUM_PSSM_SPLITS];

#if NUM_PSSM_SPLITS > 0
    varying vec3 dirProjCoord[NUM_PSSM_SPLITS];
#else
    varying vec3 dirProjCoord[1];
#endif

    #ifdef VERTEX_SHADER
        void Shadow_ProcessProjCoord(vec3 worldPos) {
#if NUM_PSSM_SPLITS > 0
            for (int i = 0; i < NUM_PSSM_SPLITS; i++) {
                #if __VERSION__ >= 150
                    dirProjCoord[i] = mat4x3(g_ShadowMatrices[i]) * vec4(worldPos, 1.0);
                #else
                    dirProjCoord[i] = (g_ShadowMatrices[i] * vec4(worldPos, 1.0)).xyz;
                #endif
            }
#endif
        }
    #else
        uniform sampler2DArrayShadow g_ShadowMapArray;
        uniform vec3 g_PssmSplits;

        float pssmSliceOffset;

        void Shadow_ProcessPssmSlice() {
            #if NUM_PSSM_SPLITS > 1
                pssmSliceOffset = dot(step(g_PssmSplits.xyz, gl_FragCoord.zzz), vec3(1.0));
            #else
                pssmSliceOffset = 0.0;
            #endif
        }

        vec3 Shadow_GetCubeMapTC(in vec3 direction) {
            vec3 axis = abs(direction);
            float largest = max(axis.x, max(axis.y, axis.z));
            vec3 tc;
            if (largest == axis.x) {
                if (direction.x > 0.0) {
                    tc = vec3( direction.z, -direction.y, 0.0);
                } else {
                    tc = vec3(-direction.z, -direction.y, 1.0);
                }
            } else if (largest == axis.y) {
                if (direction.y > 0.0) {
                    tc = vec3(-direction.x, direction.z, 2.0);
                } else {
                    tc = vec3(-direction.x, -direction.z, 3.0);
                }
            } else {
                if (direction.z > 0.0) {
                    tc = vec3(-direction.x, -direction.y, 4.0);
                } else {
                    tc = vec3(direction.x,  -direction.y, 5.0);
                }
            }
            largest = 1.0 / largest;
            tc.xy = 0.5 * (tc.xy * vec2(largest) + 1.0);
            return tc;
        }

        float Shadow_Process(int lightIndex, float lightType, float shadowMapIndex, 
                             vec3 lightVec, vec3 lightDir, 
                             vec3 worldPos, float invRadius) {
            vec4 tc;

            if (lightType <= 0.2) {
                vec3 projCoord = dirProjCoord[int(pssmSliceOffset)];
                tc = vec4(projCoord.xy, shadowMapIndex + pssmSliceOffset, projCoord.z);
            } else if (lightType <= 0.3) {
                vec3 projCoord = Shadow_GetCubeMapTC(lightVec.xyz);
                float dist = sqrt(length(lightVec) * invRadius);
                tc = vec4(projCoord.xy, shadowMapIndex + projCoord.z, dist);
            } else {
                tc = g_ShadowMatrices[NUM_PSSM_SPLITS + lightIndex] * vec4(worldPos, 1.0);
                tc.xyz /= tc.w;
                tc = vec4(tc.xy, shadowMapIndex, tc.z);
            }

            #if __VERSION__ >= 150
                return texture(g_ShadowMapArray, tc);
            #else
                return shadow2DArray(g_ShadowMapArray, tc).x;
            #endif
        }
    #endif

#elif NUM_PSSM_SPLITS > 0

    // A lightweight version of in-pass lighting that only handles directional lights
    // Control flow and loop iteration count are static

    varying vec4 vProjCoord[NUM_PSSM_SPLITS];

    #ifdef VERTEX_SHADER
        uniform mat4 g_DirectionalShadowMatrix[NUM_PSSM_SPLITS];
        void Shadow_ProcessProjCoord(vec3 worldPos) {
            for (int i = 0; i < NUM_PSSM_SPLITS; i++) {
                vProjCoord[i] = g_DirectionalShadowMatrix[i] * vec4(worldPos, 1.0);
            }
        }
    #else
        uniform sampler2DShadow g_DirectionalShadowMap[NUM_PSSM_SPLITS];
        uniform vec4 g_PssmSplits;

        const vec2 invTexSize = vec2(1.0 / 1024.0);

        float Shadow_SampleOffset(sampler2DShadow shadowMap, vec4 projCoord, vec2 offset) {
            return shadow2D(shadowMap, vec3(projCoord.xy + offset * invTexSize, projCoord.z)).r;
        }

        float Shadow_Sample(sampler2DShadow shadowMap, vec4 projCoord) {
            return shadow2D(shadowMap, projCoord.xyz).r;
        }

        #define GET_SHADOW(i) if (z < g_PssmSplits[i]) return Shadow_Sample(g_DirectionalShadowMap[i], vProjCoord[i]);

        void Shadow_ProcessPssmSlice() {
        }

        float Shadow_ProcessDirectional() {
            float z = gl_FragCoord.z;

            GET_SHADOW(0);
            #if NUM_PSSM_SPLITS > 1
                GET_SHADOW(1)
                #if NUM_PSSM_SPLITS > 2
                    GET_SHADOW(2)
                    #if NUM_PSSM_SPLITS > 3
                        GET_SHADOW(3)
                    #endif
                #endif
            #endif

            return 1.0;
        }
    #endif
#else
    #define NUM_PSSM_SPLITS 0
    
    const int pssmSliceOffset = 0;

    void Shadow_ProcessProjCoord(vec3 worldPos) {
    }

    void Shadow_ProcessPssmSlice() {
    }

    float Shadow_Process(int lightIndex, float lightType, float shadowMapIndex, 
                             vec3 lightVec, vec3 lightDir, 
                             vec3 worldPos, float invRadius) {
        return 1.0;
    }
#endif
