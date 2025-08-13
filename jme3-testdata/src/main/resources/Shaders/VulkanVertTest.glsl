#version 450

// added by preprocessor
#define VULKAN 1
#ifdef OPENGL
    // GLSLCompat stuff
#endif

layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec3 inColor;
layout (location = 2) in vec2 inTexCoord;

layout (location = 0) out vec3 fragColor;
layout (location = 1) out vec2 texCoord;

#use com.jme3.vulkan.material.UniformBuffer as UniformBuffer

#param Camera UniformBuffer(set = 0, binding = 0)
layout (Camera) uniform CameraBuffer {
    mat4 viewProjectionMatrix;
} cam;

#param Material UniformBuffer(set = 1, binding = 0)
layout (Material) uniform MaterialBuffer {
    vec3 color;
    float metallic;
    float roughness;
} mat;
#param Geometry UniformBuffer(set = 2, binding = 0)
layout (Geometry) uniform GeometryBuffer {
    mat4 worldMatrix;
} geom;

void main() {
    gl_Position = cam.viewProjectionMatrix * geom.worldMatrix * vec4(inPosition, 1.0);
    fragColor = inColor;
    texCoord = inTexCoord;
}