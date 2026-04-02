
# JMonkeyEngine 4.0-alpha

Welcome to the first taste of JMonkeyEngine 4.0! This document covers the major changes the engine has received that users are likely to encounter. Of course, there are many internal changes that are not specifically addressed here.

## Vulkan

Vulkan has been added as a backend along with OpenGL. An agnostic layer has been added to the engine that allows engine utilities and applications to work with either backend. Applications may also choose to focus on a specific backend and use special features from it that are not supported on the other backend.

For most users, this involves simply selecting an `Engine` implementation to use throughout the application before starting up the app.

```java
Engine opengl = new OpenGLEngine();
// or...
Engine vulkan = new VulkanEngine(3);
```

## Material

The addition of Vulkan has forced Material to change rather drastically. Vulkan only supports passing in material parameters through *uniform buffers* instead of individually like OpenGL normally does.

```java
Material myMat = engine.createMaterial("Common/MatDefs/Misc/Unshaded.j3md");
UnshadedParams params = myMat.get("Parameters");
params.color.set(ColorRGBA.Blue);
params.vertexColor.set(true);
```

In many ways this system is already easier to use due to the reduction in parameter names to remember.

## Mesh

==== work in progress ====

## Camera

Camera has been split up into a main interface with multiple concrete classes handling the different modes covered by the previous Camera class.

* PerspectiveCamera handles the "primary" view mode with field-of-view.
* ParallelCamera handles parallel projection.
* GuiCamera handles gui projection.
* ObliqueCamera handles near planes set at skewed angles (for portal or mirror rendering)

We still wanted cameras to easily change viewing modes, so most implementations are designed to receive properties from a "base camera", and decide which properties to tweak or override. So to change a perspective view to a parallel view:

```java
Camera perspective = new PerspectiveCamera();
Camera parallel = new ParallelCamera(perspective);
```

Change properties of `perspective` will also affect `parallel`, if ParallelCamera does not override those properties.

Another big change to Camera is that management of where the ViewPort is rendered to the screen is managed by the ViewPort.

## ViewPort

With the removal of the opaque RenderManager, ViewPorts have gotten a lot more flexible in how they are managed. ViewPorts may now be created and managed directly instead of going through RenderManager. All that needs to be done to render them is submit them to the engine at render time.

ViewPorts are responsible for managing their own render queues and the target framebuffer area.

## RenderQueue

RenderQueue has been deprecated in favor of GeometryQueues. Instead of relying on internal Geometry properties to properly sort and render geometries, GeometryQueues load incoming geometries into Elements which can then be evaluated with backend-specific implementations to be as efficient as possible.
