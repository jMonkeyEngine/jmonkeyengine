
## Engine Changes

In an effort to make the engine more efficient and intuitive, a lot of things have been drastically changed. I'll go over each one and explain why those changes were made. We'd love to hear feedback!

### Application

Application and SimpleApplication have been rehauled, and LegacyApplication has been removed. For developers interested in using standard JME rendering functionality such as PBR, SimpleApplication will handle a lot of boilerplate in setting that up. For those interested in using custom rendering, Application has become much cleaner to implement directly as familiar globals such as `getViewPort()` and `getCamera()` are no longer part of the API.

### Renderer

Renderer has been completely replaced by the Engine and CommandBuffer interfaces in order to be more conformant to how the vulkan backend operates. Engine handles general backend operations (such as buffer and image allocation) and CommandBuffer handles commands submitted to the graphics device (i.e. draw/compute commands).

### RenderManager

RenderManager has been removed in favor of distributing rendering logic to individual components.

### ShadingTechnique

ShadingTechnique is meant to solve several limitations from the current material system:

* Material data and shading logic is tightly coupled, which makes it unnecessarily difficult to use custom PBR shaders.
* It's easy to forget the names of material parameters. I've been using jme3 for years and I always needed to have `PBRLighting.j3md` open when working with PBR.
* Shader globals are "black-boxed".
* One set of fixed implementations have to interface with all shaders, which limits how those shaders can be implemented. I'm talking especially about TechniqueDefLogic implementations here, which are infamously difficult to customize.

ShadingTechnique solves these problems using an ECS-like system. Components carrying data only are attached to geometries. At render time, each geometry is rendered by a submitted ShadingTechnique based on what types of components the geometry is carrying. For those of you who use with Zay-ES, this should be familiar. A couple key differences being that components are mutable and that components can be inherited between nodes.

### Lights

Lighting received massive optimization in 4.0. Before, due to uniform and shader storage buffers not having very good integration, light data was uploaded to the graphics device every single time a lit geometry is rendered. Now we upload only a bit-set per geometry, where each bit specifies whether the corresponding light influences that geometry or not. We only have to update this bit-set on the device whenever the light composition of a geometry changes.

The light data itself is kept in one large SSBO that only needs to be bound once per render frame (for vulkan) or once per shader (for opengl). This reduces the number of shader recompilations required, since the `NB_LIGHTS` macro is no longer required, and we won't have any wasted light iterations in the shader.

### Mesh

There are several important updates to Mesh:

* `VertexBuffer.Type` has been removed. Attributes are identified by a string and then mapped to the specific attribute location by the ShadingTechnique.
* You can specify any vertex buffers you wish as vertex or instance buffers, instead of being limited to `VertexBuffer.Type.InstanceData`.
* All attributes can be interleaved with one another in the same buffer, not just `VertexBuffer.Type.InterleavedData`.
* Vertex buffers are structured using [struct arrays](#memory-structure), which makes attributes very easy to handle, even ones that are interleaved into the same buffer.

Meshes are attached to geometries through a mesh component. So in essence meshes are material parameters.

### ViewPort and Camera

ViewPorts are no longer strictly managed by any central entity (like they are in jme3 with RenderManager). Control over the viewport area (the area on the framebuffer that is to be rendered to) has been transferred from Camera to ViewPort.

Render queues are no longer fixed and may be changed per-viewport. You can specify exactly which queues a viewport needs to have and can even create custom queues with custom rendering logic.

Camera has received a massive upgrade. Camera functionality has been split into several subclasses:

* BaseCamera
* PerspectiveCamera
* ParallelCamera
* GuiCamera
* ObliqueCamera

This removes the need to check for special edge-cases like the gui render queue having culling computed in a different way. Each camera implementation delegates to another camera for certain properties. So if you need to convert a PerspectiveCamera to a ParallelCamera, it's really simple:

```java
PerspectiveCamera perspective = ...
ParallelCamera ortho = new ParallelCamera(perspective);
```
Any changes to `perspective` are immediately visible to `ortho` and vise versa.

Note that these changes mean that you can no longer have normal scene elements and gui elements in the same viewport. It's bad practice in jme3 anyway so I don't expect this to be any problem.

### Scene

The Scene class completely replaces Node and Geometry. Internally, all nodes are stored in one large `int[]`, which greatly enhances scene traversal and subset efficiency. The current structure suffers a lot from random access.

```java
Scene scene = new Scene();
int rootNode = scene.createNode();
scene.makeRoot(rootNode);
int character = scene.createNode();
scene.attachChild(rootNode, character);
```

Nodes are only considered "visible" if either it or one of its ancestors is a scene root. "Geometries" are nodes that have mesh and material data. All nodes can be geometries and all nodes can have children (unlike Geometry which cannot).

Several methods are provided for traversal, the most useful ones being `depthFirst` and `hierarchyOrdered`:

```java
for (int node : scene.depthFirst(rootNode)) {
    // do something
}
for (int node : scene.hierarchyOrdered()) {
    // do something
}
```

Scene is specifically structured to make `hierarchyOrdered` (where a node is only visited once all its ancestors are visited) as fast as possible.

Subsets can be created from a Scene:

```java
Scene.Subset set = scene.createSubset(node -> someCondition(node));
Scene.Subset set2 = set.filter(node -> someCondition2(node));
```

`Subset` is just a wrapper over a BitSet.

One big limitation of Scene is that nodes live entirely within the Scene object they are created from. You cannot attach a node from one Scene to a node from another Scene.

### Memory Structure

A lot has been done to improve how the engine interfaces with native and device memory. Struct and StructArray classes provide the structure and EngineBuffer implementations provide the memory. Structs and StructArrays can be bound to EngineBuffers in order to easily interface with the buffer's memory. In jme3, you just have to "know" how a particular buffer is structured in order to interact with it.

Struct example:

```java
public class MyStruct extends Struct {
    public final Field<Vector4f> vector = new Field(new Vector4f());
    public final Field<Integer> index = new Field(0);
    public MyStruct() {
        addFields(vector, index);
        bind(StructLayout.std140);
    }
}
```

The StructLayout class provides rules for determining the offsets of fields and for serializing certain java objects into bytes. `StructLayout.std140` and `StructLayout.std430` are commonly used by shader uniform buffers. `StructLayout.optimal` packs fields tightly like C structs.

```java
MyStruct struct = new MyStruct();
EngineBuffer buffer = ...
struct.bind(buffer, 0);
// changing struct changes buffer
struct.index.set(1);
```

To describe an array of structs:

```java
StructArray<MyStruct> array = new StructArray(10, new MyStruct());
EngineBuffer buffer = allocator.createBuffer(BufferType.Dynamic, array.size(), BufferRole.Vertex);
array.bind(buffer, 0);
// changing array changes buffer
array.index(2).vector.set(Vector4f.ZERO);
```

Note that StructArray repositions the underlying struct on each `index` call, so the following code will not work as intended.

```java
MyStruct e = array.index(3);
array.index(4).vector.set(e.vector.get());
```

AutoBuffer is used when the Struct/StructArray can change size and make it necessary the underlying buffer to be resized.

```java
AutoBuffer<StructArray<MyStruct>> array = new AutoBuffer<>(
        allocator,
        new StructArray<>(10, new MyStruct()),
        BufferType.Dynamic,
        BufferRole.Vertex);
// resizing the StructArray...
array.getStructure().setLength(20);
array.update(commandBuffer, OpLocation.DontCare);
```

AutoBuffer will allocate a larger memory block if its StructArray is too large for its current block. OpLocation controls whether the necessary copy operation occurs on the device or host (`DontCare` allows the most efficient location to be picked).

## Experimental

### Auto Batching

Auto batching takes advantage of several tricks in order to batch as many material together as possible:

1. **Bindless material parameters:** all material parameters in a certain struct type are bundled into the same large SSBO bound at the start of the render frame. An index is provided to the shader to access the correct struct.
2. **Bindless textures:** same idea is bindless material parameters but with textures. For vulkan, all textures are bound at the start of the render frame as one large descriptor array. An index is to the shader to retrieve the correct texture. For opengl, the 64-bit address of the texture is provided to the shader.
3. **Vertex pulling:** Again, like bindless material parameters, except with vertex data. All vertex data of a certain struct type is contained in one large buffer. An index to the first vertex in that struct array is provided to the shader, which then uses the current vertex index to fetch the correct vertex data.

All these techniques require only a handful of integers or longs to be sent to the shader per geometry. By bundling these integers into an instance buffer, any group of geometries that use the same rendering pipeline can be rendered together in the same draw call while keeping their unique properties.

A limitation to this method is the triangle count. For instancing, each instance is assumed to use the same number of triangles. So if a complex mesh and a simple mesh were batched together, the sunoke mesh would have far too many triangles dispatched which wastes a lot of compute power. The best solution to this problem is to only batch geometries that contain a similar enough number of triangles so that the extra triangle dispatches aren't as wasteful as another draw call.

Another limitation is that not all target hardware supports this. Android and iOS in particular are light on support.
