
The issue is that while components split up data into small chunks, often those chunks need to be unified on upload. Unifying the chunks is memory intensive, so it should be done as little as possible, but each shader may have different ways it wants to unify. Each shader can't really know how any other shader wants to unify.

Say I have a struct of PBR parameters:

```c
struct PBR {
    vec4 color;
    float metallic;
    float roughness;
}
```

An PBR extension computes transmission, so it needs to add a parameter.

```c
struct PBR {
    vec4 color;
    float metallic;
    float roughness;
    vec4 transmission; // added by extension
}
```

The optimal way is for the base PBR to use the new struct to avoid having color, metallic, and roughness stored redundantly.
Another option is to split transmission into its own buffer.

```c
struct PBR {
    vec4 color;
    float metallic;
    float roughness;    
}
struct Transmission {
    vec4 transmission;    
}
```

For binding these resources, each technique will have its own descriptor pool keyed to emit only the descriptor sets it needs to bind its resources. PBR would need to emit a descriptor set to bind one uniform/shader-storage buffer. Transmission would need to emit a descriptor set to bind two uniform/shader-storage buffers.

Glow techniques work the same way: as an extension.

```c
struct PBR {
    vec4 color;
    float metallic;
    float roughness;    
}
struct Glow {
    vec4 glowColor;
    float power;
    float intensity;
}
```

The issue is how to properly generate descriptor sets for these. The first approach that comes to mind is having each technique implementation just create a descriptor pool with the correct settings, but that would make technique extensions create multiple descriptor pools, one of which is wasted.

Material buffer data is stored in a "MaterialData" object that is passed to all techniques. To access a buffer, pass in the struct type describing it.

But how does a technique know which array element to access for a geometry? A component could be attached to the geometry that specifies the index of the material data. This doesn't necessarily have to be a component either. A `Map<Class, int[]>` could be used, keyed by the struct type. Using a component would allow for certain helper methods to be provided more easily. It's a trade-off between a little extra usability and the chance of running a bit faster.

Textures still have to be stored directly in a component, since each technique has to provide their own descriptor sets for this. The alternative is bindless textures, which is not supported well on android and ios platforms; probably best to avoid that for now.

Techniques have to provide their own descriptor sets, and it is assumed that at least some techniques will want to allocate a descriptor set per geometry. This can either be stored in a component attached to the geometry or in a cache keyed by the resources written to the descriptor set.

I've changed my mind on what a technique "extension" is. Before I thought that `MyPBR extends PBR` would work, but it doesn't because unnecessary infrastructure carries over. An extension is where `MyPBR` *mimics* `PBR`.
