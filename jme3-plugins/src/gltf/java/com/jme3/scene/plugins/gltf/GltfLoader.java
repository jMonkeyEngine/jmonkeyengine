package com.jme3.scene.plugins.gltf;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;

import java.io.*;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

import static com.jme3.scene.plugins.gltf.GltfUtils.*;

/**
 * GLTF 2.0 loader
 * Created by Nehon on 07/08/2017.
 */
public class GltfLoader implements AssetLoader {

    //Data cache for already parsed JME objects
    private Map<String, Object[]> dataCache = new HashMap<>();
    private JsonArray scenes;
    private JsonArray nodes;
    private JsonArray meshes;
    private JsonArray accessors;
    private JsonArray bufferViews;
    private JsonArray buffers;
    private JsonArray materials;
    private Material defaultMat;
    private byte[] tmpByteArray;
    private AssetInfo info;

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        try {
            dataCache.clear();
            info = assetInfo;

            if (defaultMat == null) {
                defaultMat = new Material(assetInfo.getManager(), "Common/MatDefs/Light/PBRLighting.j3md");
                defaultMat.setColor("BaseColor", ColorRGBA.White);
                defaultMat.setFloat("Metallic", 0f);
                defaultMat.setFloat("Roughness", 1f);
            }


            JsonObject root = new JsonParser().parse(new JsonReader(new InputStreamReader(assetInfo.openStream()))).getAsJsonObject();

            JsonObject asset = root.getAsJsonObject().get("asset").getAsJsonObject();
            String generator = getAsString(asset, "generator");
            String version = getAsString(asset, "version");
            String minVersion = getAsString(asset, "minVersion");
            if (!isSupported(version, minVersion)) {
                //TODO maybe just warn. gltf specs claims it will be backward compatible so at worst the user will miss some data.
                throw new AssetLoadException("Gltf Loader doesn't support this gltf version: " + version + (minVersion != null ? ("/" + minVersion) : ""));
            }

            scenes = root.getAsJsonArray("scenes");
            nodes = root.getAsJsonArray("nodes");
            meshes = root.getAsJsonArray("meshes");
            accessors = root.getAsJsonArray("accessors");
            bufferViews = root.getAsJsonArray("bufferViews");
            buffers = root.getAsJsonArray("buffers");
            materials = root.getAsJsonArray("materials");

            allocatedTmpByteArray();

            JsonPrimitive defaultScene = root.getAsJsonPrimitive("scene");

            Node n = loadScenes(defaultScene);
            //only one scene let's not return the root.
            if (n.getChildren().size() == 1) {
                n = (Node) n.getChild(0);
            }
            //no name for the scene... let's set the file name.
            if (n.getName() == null) {
                n.setName(assetInfo.getKey().getName());
            }
            return n;
        } catch (Exception e) {
            throw new AssetLoadException("An error occurred loading " + assetInfo.getKey().getName(), e);
        }
    }

    private void allocatedTmpByteArray() {
        //Allocate the tmpByteArray to the biggest bufferView
        if (bufferViews == null) {
            throw new AssetLoadException("No buffer view defined but one is referenced by an accessor");
        }
        int maxLength = 0;
        for (JsonElement bufferView : bufferViews) {
            Integer byteLength = getAsInteger(bufferView.getAsJsonObject(), "byteLength");
            if (byteLength != null && maxLength < byteLength) {
                maxLength = byteLength;
            }
        }
        tmpByteArray = new byte[maxLength];
    }

    private boolean isSupported(String version, String minVersion) {
        return "2.0".equals(version);
    }

    private Node loadScenes(JsonPrimitive defaultScene) throws IOException {
        if (scenes == null) {
            //no scene... lets handle this later...
            throw new AssetLoadException("Gltf files with no scene is not yet supported");
        }
        Node root = new Node();
        for (JsonElement scene : scenes) {
            Node sceneNode = new Node();
            //specs says that only the default scene should be rendered,
            // if there are several scenes, they are attached to the rootScene, but they are culled
            sceneNode.setCullHint(Spatial.CullHint.Always);

            sceneNode.setName(getAsString(scene.getAsJsonObject(), "name"));
            JsonArray sceneNodes = scene.getAsJsonObject().getAsJsonArray("nodes");
            for (JsonElement node : sceneNodes) {
                sceneNode.attachChild(loadNode(node.getAsInt()));
            }
            root.attachChild(sceneNode);
        }

        //Setting the default scene cul hint to inherit.
        int activeChild = 0;
        if (defaultScene != null) {
            activeChild = defaultScene.getAsInt();
        }
        root.getChild(activeChild).setCullHint(Spatial.CullHint.Inherit);
        return root;
    }

    private Spatial loadNode(int nodeIndex) throws IOException {
        Spatial spatial = fetchFromCache("nodes", nodeIndex, Spatial.class);
        if (spatial != null) {
            //If a spatial is referenced several times, it may be attached to different parents,
            // and it's not possible in JME, so we have to clone it.
            return spatial.clone();
        }
        JsonObject nodeData = nodes.get(nodeIndex).getAsJsonObject();
        Integer meshIndex = getAsInteger(nodeData, "mesh");
        if (meshIndex != null) {
            if (meshes == null) {
                throw new AssetLoadException("Can't find any mesh data, yet a node references a mesh");
            }

            //TODO material
            Material mat = defaultMat;

            //there is a mesh in this node, however gltf can split meshes in primitives (some kind of sub meshes),
            //We don't have this in JME so we have to make one mesh and one Geometry for each primitive.
            Mesh[] primitives = loadMeshPrimitives(meshIndex);
            if (primitives.length > 1) {
                //only one mesh, lets just make a geometry.
                Geometry geometry = new Geometry(null, primitives[0]);
                geometry.setMaterial(mat);
                geometry.updateModelBound();
                spatial = geometry;
            } else {
                //several meshes, let's make a parent Node and attach several geometries to it
                Node node = new Node();
                for (Mesh primitive : primitives) {
                    Geometry geom = new Geometry(null, primitive);
                    geom.setMaterial(mat);
                    geom.updateModelBound();
                    node.attachChild(geom);
                }
                spatial = node;
            }


            spatial.setName(loadMeshName(meshIndex));

        } else {
            //no mesh, we have a node. Can be a camera node or a regular node.
            //TODO handle camera nodes?
            Node node = new Node();
            JsonArray children = nodeData.getAsJsonArray("children");
            if (children != null) {
                for (JsonElement child : children) {
                    node.attachChild(loadNode(child.getAsInt()));
                }
            }
            spatial = node;
        }
        if (spatial.getName() == null) {
            spatial.setName(getAsString(nodeData.getAsJsonObject(), "name"));
        }
        spatial.setLocalTransform(loadTransforms(nodeData));

        addToCache("nodes", nodeIndex, spatial, nodes.size());
        return spatial;
    }

    private Transform loadTransforms(JsonObject nodeData) {
        Transform transform = new Transform();
        JsonArray matrix = nodeData.getAsJsonArray("matrix");
        if (matrix != null) {
            //transforms are given as a mat4
            float[] tmpArray = new float[16];
            for (int i = 0; i < tmpArray.length; i++) {
                tmpArray[i] = matrix.get(i).getAsFloat();
            }
            Matrix4f mat = new Matrix4f(tmpArray);
            transform.fromTransformMatrix(mat);
            return transform;
        }
        //no matrix transforms: no transforms or transforms givens as translation/rotation/scale
        JsonArray translation = nodeData.getAsJsonArray("translation");
        if (translation != null) {
            transform.setTranslation(
                    translation.get(0).getAsFloat(),
                    translation.get(1).getAsFloat(),
                    translation.get(2).getAsFloat());
        }
        JsonArray rotation = nodeData.getAsJsonArray("rotation");
        if (rotation != null) {
            transform.setRotation(new Quaternion(
                    rotation.get(0).getAsFloat(),
                    rotation.get(1).getAsFloat(),
                    rotation.get(2).getAsFloat(),
                    rotation.get(3).getAsFloat()));
        }
        JsonArray scale = nodeData.getAsJsonArray("scale");
        if (scale != null) {
            transform.setScale(
                    scale.get(0).getAsFloat(),
                    scale.get(1).getAsFloat(),
                    scale.get(2).getAsFloat());
        }

        return transform;
    }

    private Mesh[] loadMeshPrimitives(int meshIndex) throws IOException {
        Mesh[] meshArray = (Mesh[]) fetchFromCache("meshes", meshIndex, Object.class);
        if (meshArray != null) {
            return meshArray;
        }
        JsonObject meshData = meshes.get(meshIndex).getAsJsonObject();
        JsonArray primitives = meshData.getAsJsonArray("primitives");
        if (primitives == null) {
            throw new AssetLoadException("Can't find any primitives in mesh " + meshIndex);
        }

        meshArray = new Mesh[primitives.size()];
        int index = 0;
        for (JsonElement primitive : primitives) {
            JsonObject meshObject = primitive.getAsJsonObject();
            Mesh mesh = new Mesh();
            Integer mode = getAsInteger(meshObject, "mode");
            mesh.setMode(getMeshMode(mode));
            Integer indices = getAsInteger(meshObject, "indices");
            if (indices != null) {
                mesh.setBuffer(loadVertexBuffer(indices, VertexBuffer.Type.Index));

            }
            JsonObject attributes = meshObject.getAsJsonObject("attributes");
            assertNotNull(attributes, "No attributes defined for mesh " + mesh);
            for (Map.Entry<String, JsonElement> entry : attributes.entrySet()) {
                mesh.setBuffer(loadVertexBuffer(entry.getValue().getAsInt(), getVertexBufferType(entry.getKey())));
            }
            meshArray[index] = mesh;
            index++;

            //TODO material, targets(morph anim...)
        }

        addToCache("meshes", meshIndex, meshArray, meshes.size());
        return meshArray;
    }

    private VertexBuffer loadVertexBuffer(int accessorIndex, VertexBuffer.Type bufferType) throws IOException {

        if (accessors == null) {
            throw new AssetLoadException("No accessor attribute in the gltf file");
        }
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        Integer bufferViewIndex = getAsInteger(accessor, "bufferView");
        int byteOffset = getAsInteger(accessor, "byteOffset", 0);
        Integer componentType = getAsInteger(accessor, "componentType");
        assertNotNull(componentType, "No component type defined for accessor " + accessorIndex);
        boolean normalized = getAsBoolean(accessor, "normalized", false);
        Integer count = getAsInteger(accessor, "count");
        assertNotNull(count, "No count attribute defined for accessor " + accessorIndex);
        String type = getAsString(accessor, "type");
        assertNotNull(type, "No type attribute defined for accessor " + accessorIndex);

        VertexBuffer vb = new VertexBuffer(bufferType);
        VertexBuffer.Format format = getVertexBufferFormat(componentType);
        int numComponents = getNumberOfComponents(type);

        Buffer buff = VertexBuffer.createBuffer(format, numComponents, count);
        readBuffer(bufferViewIndex, byteOffset, numComponents * count, buff, numComponents);
        if (bufferType == VertexBuffer.Type.Index) {
            numComponents = 3;
        }
        vb.setupData(VertexBuffer.Usage.Dynamic, numComponents, format, buff);

        //TODO min / max
        //TODO sparse
        //TODO extensions?
        //TODO extras?
        return vb;
    }

    private void readBuffer(Integer bufferViewIndex, int byteOffset, int bufferSize, Buffer buff, int numComponents) throws IOException {
        if (bufferViewIndex == null) {
            //no referenced buffer, specs says to pad the buffer with zeros.
            padBuffer(buff, bufferSize);
            return;
        }

        JsonObject bufferView = bufferViews.get(bufferViewIndex).getAsJsonObject();
        Integer bufferIndex = getAsInteger(bufferView, "buffer");
        assertNotNull(bufferIndex, "No buffer defined for bufferView " + bufferViewIndex);
        int bvByteOffset = getAsInteger(bufferView, "byteOffset", 0);
        Integer byteLength = getAsInteger(bufferView, "byteLength");
        assertNotNull(byteLength, "No byte length defined for bufferView " + bufferViewIndex);
        int byteStride = getAsInteger(bufferView, "byteStride", 0);

        //target defines ELEMENT_ARRAY_BUFFER or ARRAY_BUFFER, but we already know that since we know we load the indexbuffer or any other...
        //not sure it's useful for us, but I guess it's useful when you map data directly to the GPU.
        //int target = getAsInteger(bufferView, "target", 0);

        byte[] data = readData(bufferIndex);
        populateBuffer(buff, data, bufferSize, byteOffset + bvByteOffset, byteStride, numComponents);

        //TODO extensions?
        //TODO extras?

    }

    private byte[] readData(int bufferIndex) throws IOException {

        if (buffers == null) {
            throw new AssetLoadException("No buffer defined");
        }
        JsonObject buffer = buffers.get(bufferIndex).getAsJsonObject();
        String uri = getAsString(buffer, "uri");
        Integer bufferLength = getAsInteger(buffer, "byteLength");
        assertNotNull(bufferLength, "No byteLength defined for buffer " + bufferIndex);
        if (uri != null) {
            if (uri.startsWith("data:")) {
                //inlined base64 data
                //data:<mimeType>;base64,<base64 data>
                //TODO handle inlined base64
                throw new AssetLoadException("Inlined base64 data is not supported yet");
            } else {
                //external file let's load it
                if (!uri.endsWith(".bin")) {
                    throw new AssetLoadException("Cannot load " + uri + ", a .bin extension is required.");
                }
                byte[] data = (byte[]) fetchFromCache("buffers", bufferIndex, Object.class);
                if (data != null) {
                    return data;
                }
                InputStream input = (InputStream) info.getManager().loadAsset(info.getKey().getFolder() + uri);
                data = new byte[bufferLength];
                input.read(data);
                addToCache("buffers", bufferIndex, data, buffers.size());

                return data;
            }
        } else {
            //no URI we are in a binary file so the data is in the 2nd chunk
            //TODO handle binary GLTF (GLB)
            throw new AssetLoadException("Binary gltf is not supported yet");
        }

    }

    private String loadMeshName(int meshIndex) {
        JsonObject meshData = meshes.get(meshIndex).getAsJsonObject();
        return getAsString(meshData, "name");
    }

    private <T> T fetchFromCache(String name, int index, Class<T> type) {
        Object[] data = dataCache.get(name);
        if (data == null) {
            return null;
        }
        return type.cast(data[index]);
    }

    private void addToCache(String name, int index, Object object, int maxLength) {
        Object[] data = dataCache.get(name);
        if (data == null) {
            data = new Object[maxLength];
            dataCache.put(name, data);
        }
        data[index] = object;
    }

}

