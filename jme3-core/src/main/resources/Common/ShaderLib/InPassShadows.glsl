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
        uniform vec3 g_PssmSplits;

        int pssmSliceOffset;

        void Shadow_ProcessPssmSlice() {
            #if defined(NUM_PSSM_SPLITS) && NUM_PSSM_SPLITS > 1
                pssmSliceOffset = int(dot(step(g_PssmSplits.xyz, gl_FragCoord.zzz), vec3(1.0)));
            #else
                pssmSliceOffset = 0;
            #endif
        }

        /**
         * Returns a float from 0.0 - 5.0 containing the index
         * of the cubemap face to fetch for the given direction
         */
        float Shadow_GetCubeMapFace(in vec3 direction) {
            vec3 mag = abs(direction);

            // Compare each component against the other two
            // Largest component is set to 1.0, the rest are 0.0
            vec3 largestComp = step(mag.yzx, mag) * step(mag.zxy, mag);

            // Negative components are set to 1.0, the positive are 0.0
            vec3 negComp = step(direction, vec3(0.0));

            // Each component contains the face index to use
            vec3 faceIndices = vec3(0.0, 2.0, 4.0) + negComp;

            // Pick the face index with the largest component
            return dot(largestComp, faceIndices);
        }

        float Shadow_ProcessDirectional(in int lightType, in vec3 lightDir, in float startArrayLayer, inout int startProjIndex) {
            float arraySlice = startArrayLayer + float(pssmSliceOffset);
            vec3 projCoord = vProjCoord[startProjIndex + pssmSliceOffset].xyz;
            startProjIndex += NUM_PSSM_SPLITS;
            return texture(g_ShadowMapArray, vec4(projCoord.xy, arraySlice, projCoord.z));
        }

        float Shadow_ProcessSpot(in int lightType, in vec3 lightDir, in float startArrayLayer, inout int startProjIndex) {
            vec4 projCoord = vProjCoord[startProjIndex];
            projCoord.xyz /= projCoord.w;
            startProjIndex ++;
            return texture(g_ShadowMapArray, vec4(projCoord.xy, startArrayLayer, projCoord.z));
        }

        float Shadow_Process(in int lightType, in vec3 lightDir, in float startArrayLayer, inout int startProjIndex) {
            float arraySlice = startArrayLayer;
            vec4 projCoord;

            if (lightType == 0) {
                arraySlice += float(pssmSliceOffset);
                projCoord = vProjCoord[startProjIndex + pssmSliceOffset];
                startProjIndex += NUM_PSSM_SPLITS;
            } else if (lightType == 1) {
                float face = Shadow_GetCubeMapFace(lightDir);
                arraySlice += face;
                projCoord = vProjCoord[startProjIndex + int(face)];
                projCoord.xyz /= projCoord.w;
                startProjIndex += 6;
            } else {
                projCoord = vProjCoord[startProjIndex];
                projCoord.xyz /= projCoord.w;
                startProjIndex += 1;
            }

            return texture(g_ShadowMapArray, vec4(projCoord.xy, arraySlice, projCoord.z));
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

    float Shadow_Process(in int lightType, in vec3 lightDir, in float startArrayLayer, inout int startProjIndex) {
        return 1.0;
    }
#endif
