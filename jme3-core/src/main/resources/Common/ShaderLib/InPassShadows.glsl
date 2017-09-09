#ifndef NUM_SHADOW_DIR_LIGHTS
#define NUM_SHADOW_DIR_LIGHTS 0
#endif
#ifndef NUM_SHADOW_POINT_LIGHTS
#define NUM_SHADOW_POINT_LIGHTS 0
#endif
#ifndef NUM_SHADOW_SPOT_LIGHTS
#define NUM_SHADOW_SPOT_LIGHTS 0
#endif
#ifndef NUM_PSSM_SPLITS
#define NUM_PSSM_SPLITS 0
#endif

#define SHADOW_DATA_SIZE (NUM_SHADOW_DIR_LIGHTS * NUM_PSSM_SPLITS + NUM_SHADOW_POINT_LIGHTS * 6 + NUM_SHADOW_SPOT_LIGHTS)

#if SHADOW_DATA_SIZE > 0

    varying vec4 vProjCoord[SHADOW_DATA_SIZE];

    #ifdef VERTEX_SHADER
        uniform mat4 g_ShadowMatrices[SHADOW_DATA_SIZE];

        void Shadow_ProcessProjCoord(vec3 worldPos) {
            for (int i = 0; i < SHADOW_DATA_SIZE; i++) {
                vProjCoord[i] = g_ShadowMatrices[i] * vec4(worldPos, 1.0);
            }
        }
    #else
        uniform sampler2DArrayShadow g_ShadowMapArray;
        uniform vec4 g_PssmSplits;

        int pssmSliceOffset;

        void Shadow_ProcessPssmSlice() {
            #ifdef NUM_PSSM_SPLITS
                float z = gl_FragCoord.z;
                if (z < g_PssmSplits[0]) {
                    pssmSliceOffset = 0;
                } else if (z < g_PssmSplits[1]) {
                    pssmSliceOffset = 1;
                } else if (z < g_PssmSplits[2]) {
                    pssmSliceOffset = 2;
                } else {
                    pssmSliceOffset = 3;
                }
            #else
                pssmSliceOffset = 0;
            #endif
        }

        float Shadow_ProcessDirectional(int startProjIndex, float startArrayLayer) {
            float arraySlice = startArrayLayer + float(pssmSliceOffset);
            vec3 projCoord = vProjCoord[startProjIndex + pssmSliceOffset].xyz;
            return texture(g_ShadowMapArray, vec4(projCoord.xy, arraySlice, projCoord.z));
        }

        float Shadow_ProcessSpot(int startProjIndex, float startArrayLayer) {
            vec4 projCoord = vProjCoord[startProjIndex];
            projCoord.xyz /= projCoord.w;
            return texture(g_ShadowMapArray, vec4(projCoord.xy, startArrayLayer, projCoord.z));
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
    #define NUM_SHADOW_DIR_LIGHTS 0
    #define NUM_SHADOW_POINT_LIGHTS 0
    #define NUM_SHADOW_SPOT_LIGHTS 0
    #define NUM_PSSM_SPLITS 0

    void Shadow_ProcessProjCoord(vec3 worldPos) {
    }

    void Shadow_ProcessPssmSlice() {
    }

    float Shadow_ProcessDirectional(int startLightIndex, float startArrayLayer) {
        return 1.0;
    }

    float Shadow_ProcessSpot(int startLightIndex, float startArrayLayer) {
        return 1.0;
    }
#endif
