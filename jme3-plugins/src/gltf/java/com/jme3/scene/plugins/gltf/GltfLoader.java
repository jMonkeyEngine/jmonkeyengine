package com.jme3.scene.plugins.gltf;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.jme3.animation.*;
import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

import java.io.*;
import java.nio.Buffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jme3.scene.plugins.gltf.GltfUtils.*;

/**
 * GLTF 2.0 loader
 * Created by Nehon on 07/08/2017.
 */
public class GltfLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(GltfLoader.class.getName());

    //Data cache for already parsed JME objects
    private Map<String, Object[]> dataCache = new HashMap<>();
    private JsonArray scenes;
    private JsonArray nodes;
    private JsonArray meshes;
    private JsonArray accessors;
    private JsonArray bufferViews;
    private JsonArray buffers;
    private JsonArray materials;
    private JsonArray textures;
    private JsonArray images;
    private JsonArray samplers;
    private JsonArray animations;
    private JsonArray skins;

    private Material defaultMat;
    private AssetInfo info;

    private FloatArrayPopulator floatArrayPopulator = new FloatArrayPopulator();
    private Vector3fArrayPopulator vector3fArrayPopulator = new Vector3fArrayPopulator();
    private QuaternionArrayPopulator quaternionArrayPopulator = new QuaternionArrayPopulator();
    private static Map<String, MaterialAdapter> defaultMaterialAdapters = new HashMap<>();
    private boolean useNormalsFlag = false;
    private Quaternion tmpQuat = new Quaternion();

    Map<SkinData, List<Spatial>> skinnedSpatials = new HashMap<>();
    IntMap<SkinBuffers> skinBuffers = new IntMap<>();

    static {
        defaultMaterialAdapters.put("pbrMetallicRoughness", new PBRMaterialAdapter());
    }

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
            textures = root.getAsJsonArray("textures");
            images = root.getAsJsonArray("images");
            samplers = root.getAsJsonArray("samplers");
            animations = root.getAsJsonArray("animations");
            skins = root.getAsJsonArray("skins");

            readSkins();

            JsonPrimitive defaultScene = root.getAsJsonPrimitive("scene");

            Node n = readScenes(defaultScene);

            setupControls();

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

    private boolean isSupported(String version, String minVersion) {
        return "2.0".equals(version);
    }

    private Node readScenes(JsonPrimitive defaultScene) throws IOException {
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
            root.attachChild(sceneNode);
            for (JsonElement node : sceneNodes) {
                readChild(sceneNode, node);
            }
        }

        //Loading animations
        if (animations != null) {
            for (int i = 0; i < animations.size(); i++) {
                readAnimation(i);
            }
        }

        //update skeletons
        if (skins != null) {
            for (int i = 0; i < skins.size(); i++) {
                SkinData sd = fetchFromCache("skins", i, SkinData.class);
                //reset to bind pose and update model transforms of each bones.
                sd.skeletonControl.getSkeleton().resetAndUpdate();
                //Compute sthe inverse bind transforms needed for skinning.
                sd.skeletonControl.getSkeleton().setBindingPose();
            }
        }

        //Setting the default scene cul hint to inherit.
        int activeChild = 0;
        if (defaultScene != null) {
            activeChild = defaultScene.getAsInt();
        }
        root.getChild(activeChild).setCullHint(Spatial.CullHint.Inherit);
        return root;
    }

    private Object readNode(int nodeIndex) throws IOException {
        Object obj = fetchFromCache("nodes", nodeIndex, Object.class);
        if (obj != null) {
            if (obj instanceof BoneWrapper) {
                //the node can be a previously loaded bone let's return it
                return obj;
            } else {
                //If a spatial is referenced several times, it may be attached to different parents,
                // and it's not possible in JME, so we have to clone it.
                return ((Spatial) obj).clone();
            }
        }
        Spatial spatial;
        JsonObject nodeData = nodes.get(nodeIndex).getAsJsonObject();
        JsonArray children = nodeData.getAsJsonArray("children");
        Integer meshIndex = getAsInteger(nodeData, "mesh");
        if (meshIndex != null) {
            assertNotNull(meshes, "Can't find any mesh data, yet a node references a mesh");

            //there is a mesh in this node, however gltf can split meshes in primitives (some kind of sub meshes),
            //We don't have this in JME so we have to make one mesh and one Geometry for each primitive.
            Geometry[] primitives = readMeshPrimitives(meshIndex);
            if (primitives.length == 1 && children == null) {
                //only one geometry, let's not wrap it in another node unless the node has children.
                spatial = primitives[0];
            } else {
                //several geometries, let's make a parent Node and attach them to it
                Node node = new Node();
                for (Geometry primitive : primitives) {
                    node.attachChild(primitive);
                }
                spatial = node;
            }
            spatial.setName(readMeshName(meshIndex));

        } else {
            //no mesh, we have a node. Can be a camera node or a regular node.
            //TODO handle camera nodes?
            Node node = new Node();

            spatial = node;
        }

        Integer skinIndex = getAsInteger(nodeData, "skin");
        if (skinIndex != null) {
            SkinData skinData = fetchFromCache("skins", skinIndex, SkinData.class);
            List<Spatial> spatials = skinnedSpatials.get(skinData);
            spatials.add(spatial);
        }

        spatial.setLocalTransform(readTransforms(nodeData));

        if (spatial.getName() == null) {
            spatial.setName(getAsString(nodeData.getAsJsonObject(), "name"));
        }

        addToCache("nodes", nodeIndex, spatial, nodes.size());
        return spatial;
    }

    private void readChild(Spatial parent, JsonElement nodeIndex) throws IOException {
        Object loaded = readNode(nodeIndex.getAsInt());
        if (loaded instanceof Spatial) {
            Spatial spatial = ((Spatial) loaded);
            ((Node) parent).attachChild(spatial);
            JsonObject nodeElem = nodes.get(nodeIndex.getAsInt()).getAsJsonObject();
            JsonArray children = nodeElem.getAsJsonArray("children");
            if (children != null) {
                for (JsonElement child : children) {
                    readChild(spatial, child);
                }
            }
        } else if (loaded instanceof BoneWrapper) {
            //parent is the Armature Node, we have to apply its transforms to the root bone's bind pose and to its animation data
            BoneWrapper bw = (BoneWrapper) loaded;
            bw.isRoot = true;
            SkinData skinData = fetchFromCache("skins", bw.skinIndex, SkinData.class);
            skinData.armatureTransforms = parent.getLocalTransform();
        }

    }

    private Transform readTransforms(JsonObject nodeData) {
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

    private Geometry[] readMeshPrimitives(int meshIndex) throws IOException {
        Geometry[] geomArray = (Geometry[]) fetchFromCache("meshes", meshIndex, Object.class);
        if (geomArray != null) {
            //cloning the geoms.
            Geometry[] geoms = new Geometry[geomArray.length];
            for (int i = 0; i < geoms.length; i++) {
                geoms[i] = geomArray[i].clone(false);
            }
            return geoms;
        }
        JsonObject meshData = meshes.get(meshIndex).getAsJsonObject();
        JsonArray primitives = meshData.getAsJsonArray("primitives");
        assertNotNull(primitives, "Can't find any primitives in mesh " + meshIndex);

        String name = getAsString(meshData, "name");

        geomArray = new Geometry[primitives.size()];
        int index = 0;
        for (JsonElement primitive : primitives) {
            JsonObject meshObject = primitive.getAsJsonObject();
            Mesh mesh = new Mesh();
            Integer mode = getAsInteger(meshObject, "mode");
            mesh.setMode(getMeshMode(mode));
            Integer indices = getAsInteger(meshObject, "indices");
            if (indices != null) {
                mesh.setBuffer(readAccessorData(indices, new VertexBufferPopulator(VertexBuffer.Type.Index)));
            }
            JsonObject attributes = meshObject.getAsJsonObject("attributes");
            assertNotNull(attributes, "No attributes defined for mesh " + mesh);

            skinBuffers.clear();

            for (Map.Entry<String, JsonElement> entry : attributes.entrySet()) {
                //special case for joints and weights buffer. If there are more than 4 bones per vertex, there might be several of them
                //we need to read them all and to keep only the 4 that have the most weight on the vertex.
                String bufferType = entry.getKey();
                if (bufferType.startsWith("JOINTS")) {
                    SkinBuffers buffs = getSkinBuffers(bufferType);
                    SkinBuffers buffer = readAccessorData(entry.getValue().getAsInt(), new JointArrayPopulator());
                    buffs.joints = buffer.joints;
                    buffs.componentSize = buffer.componentSize;
                } else if (bufferType.startsWith("WEIGHTS")) {
                    SkinBuffers buffs = getSkinBuffers(bufferType);
                    buffs.weights = readAccessorData(entry.getValue().getAsInt(), new FloatArrayPopulator());
                } else {
                    VertexBuffer vb = readAccessorData(entry.getValue().getAsInt(), new VertexBufferPopulator(getVertexBufferType(bufferType)));
                    if (vb != null) {
                        mesh.setBuffer(vb);
                    }
                }
            }
            handleSkinningBuffers(mesh, skinBuffers);

            if (mesh.getBuffer(VertexBuffer.Type.BoneIndex) != null) {
                //the mesh has some skinning let's create needed buffers for HW skinning
                //creating empty buffers for HW skinning
                //the buffers will be setup if ever used.
                VertexBuffer weightsHW = new VertexBuffer(VertexBuffer.Type.HWBoneWeight);
                VertexBuffer indicesHW = new VertexBuffer(VertexBuffer.Type.HWBoneIndex);
                //setting usage to cpuOnly so that the buffer is not send empty to the GPU
                indicesHW.setUsage(VertexBuffer.Usage.CpuOnly);
                weightsHW.setUsage(VertexBuffer.Usage.CpuOnly);
                mesh.setBuffer(weightsHW);
                mesh.setBuffer(indicesHW);
                mesh.generateBindPose();
            }

            Geometry geom = new Geometry(null, mesh);

            Integer materialIndex = getAsInteger(meshObject, "material");
            if (materialIndex == null) {
                geom.setMaterial(defaultMat);
            } else {
                useNormalsFlag = false;
                geom.setMaterial(readMaterial(materialIndex));
                if (geom.getMaterial().getAdditionalRenderState().getBlendMode() == RenderState.BlendMode.Alpha) {
                    //Alpha blending is on on this material let's place the geom in the transparent bucket
                    geom.setQueueBucket(RenderQueue.Bucket.Transparent);
                }
                if (useNormalsFlag && mesh.getBuffer(VertexBuffer.Type.Tangent) == null) {
                    //No tangent buffer, but there is a normal map, we have to generate them using MiiktSpace
                    MikktspaceTangentGenerator.generate(geom);
                }
            }

            if (name != null) {
                geom.setName(name + (primitives.size() > 1 ? ("_" + index) : ""));
            }

            geom.updateModelBound();
            geomArray[index] = geom;
            index++;

            //TODO targets(morph anim...)
        }

        addToCache("meshes", meshIndex, geomArray, meshes.size());
        return geomArray;
    }

    public static class WeightData {
        float value;
        short index;
        int componentSize;

        public WeightData(float value, short index, int componentSize) {
            this.value = value;
            this.index = index;
            this.componentSize = componentSize;
        }
    }

    private SkinBuffers getSkinBuffers(String bufferType) {
        int bufIndex = getIndex(bufferType);
        SkinBuffers buffs = skinBuffers.get(bufIndex);
        if (buffs == null) {
            buffs = new SkinBuffers();
            skinBuffers.put(bufIndex, buffs);
        }
        return buffs;
    }

    private <R> R readAccessorData(int accessorIndex, Populator<R> populator) throws IOException {

        assertNotNull(accessors, "No accessor attribute in the gltf file");

        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        Integer bufferViewIndex = getAsInteger(accessor, "bufferView");
        int byteOffset = getAsInteger(accessor, "byteOffset", 0);
        Integer componentType = getAsInteger(accessor, "componentType");
        assertNotNull(componentType, "No component type defined for accessor " + accessorIndex);
        Integer count = getAsInteger(accessor, "count");
        assertNotNull(count, "No count attribute defined for accessor " + accessorIndex);
        String type = getAsString(accessor, "type");
        assertNotNull(type, "No type attribute defined for accessor " + accessorIndex);

        boolean normalized = getAsBoolean(accessor, "normalized", false);
        //Some float data can be packed into short buffers, "normalized" means they have to be unpacked.
        //TODO support packed data
        //TODO min / max
        //TODO sparse
        //TODO extensions?
        //TODO extras?

        return populator.populate(bufferViewIndex, componentType, type, count, byteOffset);
    }

    private void readBuffer(Integer bufferViewIndex, int byteOffset, int bufferSize, Object store, int numComponents, int componentSize) throws IOException {

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
        populateBuffer(store, data, bufferSize, byteOffset + bvByteOffset, byteStride, numComponents, componentSize);

        //TODO extensions?
        //TODO extras?

    }

    private byte[] readData(int bufferIndex) throws IOException {

        assertNotNull(buffers, "No buffer defined");

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

    private Material readMaterial(int materialIndex) {
        assertNotNull(materials, "There is no material defined yet a mesh references one");

        JsonObject matData = materials.get(materialIndex).getAsJsonObject();
        JsonObject pbrMat = matData.getAsJsonObject("pbrMetallicRoughness");

        if (pbrMat == null) {
            logger.log(Level.WARNING, "Unable to find any pbrMetallicRoughness material entry in material " + materialIndex + ". Only PBR material is supported for now");
            return defaultMat;
        }
        MaterialAdapter adapter = null;
        if (info.getKey() instanceof GltfModelKey) {
            adapter = ((GltfModelKey) info.getKey()).getAdapterForMaterial("pbrMetallicRoughness");
        }
        if (adapter == null) {
            adapter = defaultMaterialAdapters.get("pbrMetallicRoughness");
        }

        Material mat = adapter.getMaterial(info.getManager());
        mat.setName(getAsString(matData, "name"));

        adapter.setParam(mat, "baseColorFactor", getAsColor(pbrMat, "baseColorFactor", ColorRGBA.White));
        adapter.setParam(mat, "metallicFactor", getAsFloat(pbrMat, "metallicFactor", 1f));
        adapter.setParam(mat, "roughnessFactor", getAsFloat(pbrMat, "roughnessFactor", 1f));
        adapter.setParam(mat, "emissiveFactor", getAsColor(matData, "emissiveFactor", ColorRGBA.Black));
        String alphaMode = getAsString(matData, "alphaMode");
        adapter.setParam(mat, "alphaMode", alphaMode);
        if (alphaMode != null && alphaMode.equals("MASK")) {
            adapter.setParam(mat, "alphaCutoff", getAsFloat(matData, "alphaCutoff"));
        }
        adapter.setParam(mat, "doubleSided", getAsBoolean(matData, "doubleSided"));

        adapter.setParam(mat, "baseColorTexture", readTexture(pbrMat.getAsJsonObject("baseColorTexture")));
        adapter.setParam(mat, "metallicRoughnessTexture", readTexture(pbrMat.getAsJsonObject("metallicRoughnessTexture")));
        Texture2D normal = readTexture(matData.getAsJsonObject("normalTexture"));
        adapter.setParam(mat, "normalTexture", normal);
        if (normal != null) {
            useNormalsFlag = true;
        }
        adapter.setParam(mat, "occlusionTexture", readTexture(matData.getAsJsonObject("occlusionTexture")));
        adapter.setParam(mat, "emissiveTexture", readTexture(matData.getAsJsonObject("emissiveTexture")));

        return mat;
    }

    private Texture2D readTexture(JsonObject texture) {
        if (texture == null) {
            return null;
        }
        Integer textureIndex = getAsInteger(texture, "index");
        assertNotNull(textureIndex, "Texture as no index");
        assertNotNull(textures, "There are no textures, yet one is referenced by a material");

        JsonObject textureData = textures.get(textureIndex).getAsJsonObject();
        Integer sourceIndex = getAsInteger(textureData, "source");
        Integer samplerIndex = getAsInteger(textureData, "sampler");

        Texture2D texture2d = readImage(sourceIndex);
        readSampler(samplerIndex, texture2d);

        return texture2d;
    }

    private Texture2D readImage(int sourceIndex) {
        if (images == null) {
            throw new AssetLoadException("No image defined");
        }

        JsonObject image = images.get(sourceIndex).getAsJsonObject();
        String uri = getAsString(image, "uri");
        if (uri == null) {
            //Image is embed in a buffer not supported yet
            //TODO support images embed in a buffer
            throw new AssetLoadException("Images embed in a buffer are not supported yet");
        } else if (uri.startsWith("data:")) {
            //base64 encoded image, not supported yet
            //TODO support base64 encoded images
            throw new AssetLoadException("Base64 encoded embed images are not supported yet");
        } else {
            TextureKey key = new TextureKey(info.getKey().getFolder() + uri, false);
            Texture tex = info.getManager().loadTexture(key);
            return (Texture2D) tex;
        }

    }

    private void readAnimation(int animationIndex) throws IOException {
        JsonObject animation = animations.get(animationIndex).getAsJsonObject();
        JsonArray channels = animation.getAsJsonArray("channels");
        JsonArray samplers = animation.getAsJsonArray("samplers");
        String name = getAsString(animation, "name");
        assertNotNull(channels, "No channels for animation " + name);
        assertNotNull(samplers, "No samplers for animation " + name);

        //temp data storage of track data
        AnimData[] animatedNodes = new AnimData[nodes.size()];

        for (JsonElement channel : channels) {

            JsonObject target = channel.getAsJsonObject().getAsJsonObject("target");

            Integer targetNode = getAsInteger(target, "node");
            String targetPath = getAsString(target, "path");
            if (targetNode == null) {
                //no target node for the channel, specs say to ignore the channel.
                continue;
            }
            assertNotNull(targetPath, "No target path for channel");

            if (targetPath.equals("weight")) {
                //Morph animation, not implemented in JME, let's warn the user and skip the channel
                logger.log(Level.WARNING, "Morph animation is not supported by JME yet, skipping animation");
                continue;
            }
            AnimData animData = animatedNodes[targetNode];
            if (animData == null) {
                animData = new AnimData();
                animatedNodes[targetNode] = animData;
            }

            Integer samplerIndex = getAsInteger(channel.getAsJsonObject(), "sampler");
            assertNotNull(samplerIndex, "No animation sampler provided for channel");
            JsonObject sampler = samplers.get(samplerIndex).getAsJsonObject();
            Integer timeIndex = getAsInteger(sampler, "input");
            assertNotNull(timeIndex, "No input accessor Provided for animation sampler");
            Integer dataIndex = getAsInteger(sampler, "output");
            assertNotNull(dataIndex, "No output accessor Provided for animation sampler");

            String interpolation = getAsString(sampler, "interpolation");
            if (interpolation == null || !interpolation.equals("LINEAR")) {
                //JME anim system only supports Linear interpolation (will be possible with monkanim though)
                //TODO rework this once monkanim is core, or allow a hook for animation loading to fit custom animation systems
                logger.log(Level.WARNING, "JME only supports linear interpolation for animations");
            }

            float[] times = fetchFromCache("accessors", timeIndex, float[].class);
            if (times == null) {
                times = readAccessorData(timeIndex, floatArrayPopulator);
                addToCache("accessors", timeIndex, times, accessors.size());
            }
            if (animData.times == null) {
                animData.times = times;
            } else {
                //check if we are loading the same time array
                //TODO specs actually don't forbid this...maybe remove this check and handle it.
                if (animData.times != times) {
                    logger.log(Level.WARNING, "Channel has different input accessors for samplers");
//                    for (float time : animData.times) {
//                        System.err.print(time + ", ");
//                    }
//                    System.err.println("");
//                    for (float time : times) {
//                        System.err.print(time + ", ");
//                    }
//                    System.err.println("");
                }
            }
            if (animData.length == null) {
                //animation length is the last timestamp
                animData.length = times[times.length - 1];
            }
            if (targetPath.equals("translation")) {
                Vector3f[] translations = readAccessorData(dataIndex, vector3fArrayPopulator);
                animData.translations = translations;
            } else if (targetPath.equals("scale")) {
                Vector3f[] scales = readAccessorData(dataIndex, vector3fArrayPopulator);
                animData.scales = scales;
            } else if (targetPath.equals("rotation")) {
                Quaternion[] rotations = readAccessorData(dataIndex, quaternionArrayPopulator);
                animData.rotations = rotations;
            }
        }

        if (name == null) {
            name = "anim_" + animationIndex;
        }

        List<Spatial> spatials = new ArrayList<>();
        Animation anim = new Animation();
        anim.setName(name);
        int skinIndex = -1;

        for (int i = 0; i < animatedNodes.length; i++) {
            AnimData animData = animatedNodes[i];
            if (animData == null) {
                continue;
            }
            if (animData.length > anim.getLength()) {
                anim.setLength(animData.length);
            }
            Object node = fetchFromCache("nodes", i, Object.class);
            if (node instanceof Spatial) {
                Spatial s = (Spatial) node;
                spatials.add(s);
                SpatialTrack track = new SpatialTrack(animData.times, animData.translations, animData.rotations, animData.scales);
                track.setTrackSpatial(s);
                anim.addTrack(track);
            } else if (node instanceof BoneWrapper) {
                BoneWrapper b = (BoneWrapper) node;
                //apply the inverseBindMatrix to animation data.
                b.update(animData);
                BoneTrack track = new BoneTrack(b.boneIndex, animData.times, animData.translations, animData.rotations, animData.scales);
                anim.addTrack(track);
                if (skinIndex == -1) {
                    skinIndex = b.skinIndex;
                } else {
                    //Check if all bones affected by this animation are from the same skin, otherwise raise an error.
                    if (skinIndex != b.skinIndex) {
                        throw new AssetLoadException("Animation " + animationIndex + " (" + name + ") applies to bones that are not from the same skin: skin " + skinIndex + ", bone " + b.bone.getName() + " from skin " + b.skinIndex);
                    }
                    //else everything is fine.
                }
            }
        }

        if (skinIndex != -1) {
            //we have a bone animation.
            SkinData skin = fetchFromCache("skins", skinIndex, SkinData.class);
            if (skin.animControl == null) {
                skin.animControl = new AnimControl(skin.skeletonControl.getSkeleton());
            }
            skin.animControl.addAnim(anim);
            //the controls will be added to the right spatial in setupControls()
        }


        if (!spatials.isEmpty()) {
            //Note that it's pretty unlikely to have an animation that is both a spatial animation and a bone animation...But you never know. The specs doesn't forbids it
            if (skinIndex != -1) {
                //there are some spatial tracks in this bone animation... or the other way around. Let's add the spatials in the skinnedSpatials.
                SkinData skin = fetchFromCache("skins", skinIndex, SkinData.class);
                List<Spatial> spat = skinnedSpatials.get(skin);
                spat.addAll(spatials);
                //the animControl will be added in the setupControls();
            } else {
                Spatial spatial = null;
                if (spatials.size() == 1) {
                    spatial = spatials.get(0);
                } else {
                    spatial = findCommonAncestor(spatials);
                }

                AnimControl control = spatial.getControl(AnimControl.class);
                if (control == null) {
                    control = new AnimControl();
                    spatial.addControl(control);
                }
                control.addAnim(anim);
            }
        }
    }

    private void readSampler(int samplerIndex, Texture2D texture) {
        if (samplers == null) {
            throw new AssetLoadException("No samplers defined");
        }
        JsonObject sampler = samplers.get(samplerIndex).getAsJsonObject();
        Texture.MagFilter magFilter = getMagFilter(getAsInteger(sampler, "magFilter"));
        Texture.MinFilter minFilter = getMinFilter(getAsInteger(sampler, "minFilter"));
        Texture.WrapMode wrapS = getWrapMode(getAsInteger(sampler, "wrapS"));
        Texture.WrapMode wrapT = getWrapMode(getAsInteger(sampler, "wrapT"));

        if (magFilter != null) {
            texture.setMagFilter(magFilter);
        }
        if (minFilter != null) {
            texture.setMinFilter(minFilter);
        }
        texture.setWrap(Texture.WrapAxis.S, wrapS);
        texture.setWrap(Texture.WrapAxis.T, wrapT);
    }

    private void readSkins() throws IOException {
        if (skins == null) {
            //no skins, no bone animation.
            return;
        }
        for (int index = 0; index < skins.size(); index++) {
            JsonObject skin = skins.get(index).getAsJsonObject();

            //Note that the "skeleton" index is intentionally ignored.
            //It's not mandatory and exporters tends to mix up how it should be used because the specs are not clear.
            //Anyway we have other means to detect both armature structures and root bones.

            JsonArray joints = skin.getAsJsonArray("joints");
            assertNotNull(joints, "No joints defined for skin");

            //inverseBindMatrices are also intentionally ignored. JME computes them from the bind transforms when initializing the skeleton.
            //Integer matricesIndex = getAsInteger(skin, "inverseBindMatrices");

            Bone[] bones = new Bone[joints.size()];
            for (int i = 0; i < joints.size(); i++) {
                int boneIndex = joints.get(i).getAsInt();
                //TODO actually a regular node or a geometry can be attached to a bone, we have to handle this and attach it to the AttachementNode.
                bones[i] = readNodeAsBone(boneIndex, i, index);
            }

            for (int i = 0; i < joints.size(); i++) {
                findChildren(joints.get(i).getAsInt());
            }

            Skeleton skeleton = new Skeleton(bones);

            SkinData skinData = new SkinData();
            skinData.skeletonControl = new SkeletonControl(skeleton);
            addToCache("skins", index, skinData, nodes.size());
            skinnedSpatials.put(skinData, new ArrayList<Spatial>());

        }

    }

    private Bone readNodeAsBone(int nodeIndex, int boneIndex, int skinIndex) throws IOException {

        BoneWrapper boneWrapper = fetchFromCache("nodes", nodeIndex, BoneWrapper.class);
        if (boneWrapper != null) {
            return boneWrapper.bone;
        }
        JsonObject nodeData = nodes.get(nodeIndex).getAsJsonObject();
        String name = getAsString(nodeData, "name");
        if (name == null) {
            name = "Bone_" + nodeIndex;
        }
        Bone bone = new Bone(name);
        Transform boneTransforms = readTransforms(nodeData);
        bone.setBindTransforms(boneTransforms.getTranslation(), boneTransforms.getRotation(), boneTransforms.getScale());

        addToCache("nodes", nodeIndex, new BoneWrapper(bone, boneIndex, skinIndex), nodes.size());

        return bone;
    }

    private void findChildren(int nodeIndex) {
        BoneWrapper bw = fetchFromCache("nodes", nodeIndex, BoneWrapper.class);
        JsonObject nodeData = nodes.get(nodeIndex).getAsJsonObject();
        JsonArray children = nodeData.getAsJsonArray("children");
        if (children != null) {
            for (JsonElement child : children) {
                int childIndex = child.getAsInt();
                BoneWrapper cbw = fetchFromCache("nodes", childIndex, BoneWrapper.class);
                if (cbw != null) {
                    bw.bone.addChild(cbw.bone);
                }
            }
        }
    }

    private void setupControls() {
        for (SkinData skinData : skinnedSpatials.keySet()) {
            List<Spatial> spatials = skinnedSpatials.get(skinData);
            Spatial spatial;
            if (spatials.size() >= 1) {
                spatial = findCommonAncestor(spatials);
            } else {
                spatial = spatials.get(0);
            }

            AnimControl animControl = spatial.getControl(AnimControl.class);
            if (animControl != null) {
                //The spatial already has an anim control, we need to merge it with the one in skinData. Then remove it.
                for (String name : animControl.getAnimationNames()) {
                    Animation anim = animControl.getAnim(name);
                    skinData.animControl.addAnim(anim);
                }
                spatial.removeControl(animControl);
            }

            spatial.addControl(skinData.animControl);
            spatial.addControl(skinData.skeletonControl);
        }
    }

    private String readMeshName(int meshIndex) {
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

    private class AnimData {
        Float length;
        float[] times;
        Vector3f[] translations;
        Quaternion[] rotations;
        Vector3f[] scales;
        //not used for now
        float[] weights;
    }

    private class BoneWrapper {
        Bone bone;
        int boneIndex;
        int skinIndex;
        boolean isRoot = false;

        public BoneWrapper(Bone bone, int boneIndex, int skinIndex) {
            this.bone = bone;
            this.boneIndex = boneIndex;
            this.skinIndex = skinIndex;
        }

        /**
         * Applies the inverse Bind transforms to anim data. and the armature transforms if relevant.
         */
        public void update(AnimData data) {
            Transform bindTransforms = new Transform(bone.getBindPosition(), bone.getBindRotation(), bone.getBindScale());
            SkinData skinData = fetchFromCache("skins", skinIndex, SkinData.class);
            if (isRoot) {

                bindTransforms.combineWithParent(skinData.armatureTransforms);
                bone.setBindTransforms(bindTransforms.getTranslation(), bindTransforms.getRotation(), bindTransforms.getScale());
            }

            for (int i = 0; i < data.translations.length; i++) {
                Transform t = new Transform(data.translations[i], data.rotations[i], data.scales[i]);
                if (isRoot) {
                    //Apply the armature transforms to the root bone anim track.
                    t.combineWithParent(skinData.armatureTransforms);
                }

                //This is wrong
                //You'd normally combine those transforms with transform.combineWithParent()
                //Here we actually do in reverse what JME does to combine anim transforms with bind transfoms (add trans/mult rot/ mult scale)
                //The code to fix is in Bone.blendAnimTransforms
                //TODO fix blendAnimTransforms
                t.getTranslation().subtractLocal(bindTransforms.getTranslation());
                t.getScale().divideLocal(bindTransforms.getScale());
                tmpQuat.set(bindTransforms.getRotation()).inverseLocal().multLocal(t.getRotation());
                t.setRotation(tmpQuat);

                data.translations[i] = t.getTranslation();
                data.rotations[i] = t.getRotation();
                data.scales[i] = t.getScale();
            }
        }
    }

    private class SkinData {
        SkeletonControl skeletonControl;
        AnimControl animControl;
        Transform armatureTransforms;
    }

    public static class SkinBuffers {
        short[] joints;
        float[] weights;
        int componentSize;

        public SkinBuffers(short[] joints, int componentSize) {
            this.joints = joints;
            this.componentSize = componentSize;
        }

        public SkinBuffers() {
        }
    }

    private interface Populator<T> {
        T populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset) throws IOException;
    }

    private class VertexBufferPopulator implements Populator<VertexBuffer> {
        VertexBuffer.Type bufferType;

        public VertexBufferPopulator(VertexBuffer.Type bufferType) {
            this.bufferType = bufferType;
        }

        @Override
        public VertexBuffer populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset) throws IOException {

            if (bufferType == null) {
                logger.log(Level.WARNING, "could not assign data to any VertexBuffer type for buffer view " + bufferViewIndex);
                return null;
            }

            VertexBuffer vb = new VertexBuffer(bufferType);
            VertexBuffer.Format format = getVertexBufferFormat(componentType);
            int numComponents = getNumberOfComponents(type);

            Buffer buff = VertexBuffer.createBuffer(format, numComponents, count);
            int bufferSize = numComponents * count;
            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the buffer with zeros.
                padBuffer(buff, bufferSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, bufferSize, buff, numComponents, format.getComponentSize());
            }

            if (bufferType == VertexBuffer.Type.Index) {
                numComponents = 3;
            }
            vb.setupData(VertexBuffer.Usage.Dynamic, numComponents, format, buff);

            return vb;
        }

    }

    private class FloatArrayPopulator implements Populator<float[]> {

        @Override
        public float[] populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset) throws IOException {

            int numComponents = getNumberOfComponents(type);
            int dataSize = numComponents * count;
            float[] data = new float[dataSize];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, dataSize, data, numComponents, 4);
            }

            return data;
        }

    }

    private class Vector3fArrayPopulator implements Populator<Vector3f[]> {

        @Override
        public Vector3f[] populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset) throws IOException {

            int numComponents = getNumberOfComponents(type);
            int dataSize = numComponents * count;
            Vector3f[] data = new Vector3f[count];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, dataSize, data, numComponents, 4);
            }
            return data;
        }
    }

    private class QuaternionArrayPopulator implements Populator<Quaternion[]> {

        @Override
        public Quaternion[] populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset) throws IOException {

            int numComponents = getNumberOfComponents(type);
            int dataSize = numComponents * count;
            Quaternion[] data = new Quaternion[count];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, dataSize, data, numComponents, 4);
            }

            return data;
        }
    }

    private class JointData {
        short[] joints;
        int componentSize;

        public JointData(short[] joints, int componentSize) {
            this.joints = joints;
            this.componentSize = componentSize;
        }
    }

    private class JointArrayPopulator implements Populator<SkinBuffers> {

        @Override
        public SkinBuffers populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset) throws IOException {

            int numComponents = getNumberOfComponents(type);

            //can be bytes or shorts.
            int componentSize = 1;
            if (componentType == 5123) {
                componentSize = 2;
            }

            int dataSize = numComponents * count;
            short[] data = new short[dataSize];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, dataSize, data, numComponents, componentSize);
            }

            return new SkinBuffers(data, componentSize);
        }
    }
}

