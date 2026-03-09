# Models for the glTF loader unit tests

Used in `com.jme3.scene.plugins.gltf.GltfLoaderTest`

- `TriangleUnsupportedExtensionRequired.gltf` is the embedded representation of
  the `Triangle` sample asset, with additional declarations in `extensionsUsed`
  and `extensionsRequired`, to test the behavior of the loader when encountering
  unknown extensions
- `unitSquare11x11_unsignedShortTexCoords-draco.glb` is a simple unit square with
  11x11 vertices, and texture coordinates that are stored as (normalized) unsigned
  short values. The asset is draco-compressed, to check the behavior of the Draco
  extension handler.

