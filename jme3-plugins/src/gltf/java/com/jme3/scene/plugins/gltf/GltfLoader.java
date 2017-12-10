package com.jme3.scene.plugins.gltf;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.jme3.animation.*;
import com.jme3.asset.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.CameraControl;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.IntMap;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

import javax.xml.bind.DatatypeConverter;
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
    private JsonArray cameras;

    private Material defaultMat;
    private AssetInfo info;
    private JsonObject docRoot;
    private Node rootNode;

    private FloatArrayPopulator floatArrayPopulator = new FloatArrayPopulator();
    private Vector3fArrayPopulator vector3fArrayPopulator = new Vector3fArrayPopulator();
    private QuaternionArrayPopulator quaternionArrayPopulator = new QuaternionArrayPopulator();
    private Matrix4fArrayPopulator matrix4fArrayPopulator = new Matrix4fArrayPopulator();
    private static Map<String, MaterialAdapter> defaultMaterialAdapters = new HashMap<>();
    private CustomContentManager customContentManager = new CustomContentManager();
    private boolean useNormalsFlag = false;
    private Quaternion tmpQuat = new Quaternion();
    private Transform tmpTransforms = new Transform();
    private Transform tmpTransforms2 = new Transform();
    private Matrix4f tmpMat = new Matrix4f();

    Map<SkinData, List<Spatial>> skinnedSpatials = new HashMap<>();
    IntMap<SkinBuffers> skinBuffers = new IntMap<>();

    static {
        defaultMaterialAdapters.put("pbrMetallicRoughness", new PBRMetalRoughMaterialAdapter());
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        return loadFromStream(assetInfo, assetInfo.openStream());
    }


    protected Object loadFromStream(AssetInfo assetInfo, InputStream stream) throws IOException {
        try {
            dataCache.clear();
            info = assetInfo;
            skinnedSpatials.clear();
            rootNode = new Node();

            if (defaultMat == null) {
                defaultMat = new Material(assetInfo.getManager(), "Common/MatDefs/Light/PBRLighting.j3md");
                defaultMat.setColor("BaseColor", ColorRGBA.White);
                defaultMat.setFloat("Metallic", 0f);
                defaultMat.setFloat("Roughness", 1f);
            }

            docRoot = new JsonParser().parse(new JsonReader(new InputStreamReader(stream))).getAsJsonObject();

            JsonObject asset = docRoot.getAsJsonObject().get("asset").getAsJsonObject();
            String generator = getAsString(asset, "generator");
            String version = getAsString(asset, "version");
            String minVersion = getAsString(asset, "minVersion");
            if (!isSupported(version, minVersion)) {
                logger.log(Level.SEVERE, "Gltf Loader doesn't support this gltf version: " + version + (minVersion != null ? ("/" + minVersion) : ""));
            }

            scenes = docRoot.getAsJsonArray("scenes");
            nodes = docRoot.getAsJsonArray("nodes");
            meshes = docRoot.getAsJsonArray("meshes");
            accessors = docRoot.getAsJsonArray("accessors");
            bufferViews = docRoot.getAsJsonArray("bufferViews");
            buffers = docRoot.getAsJsonArray("buffers");
            materials = docRoot.getAsJsonArray("materials");
            textures = docRoot.getAsJsonArray("textures");
            images = docRoot.getAsJsonArray("images");
            samplers = docRoot.getAsJsonArray("samplers");
            animations = docRoot.getAsJsonArray("animations");
            skins = docRoot.getAsJsonArray("skins");
            cameras = docRoot.getAsJsonArray("cameras");

            customContentManager.init(this);

            readSkins();
            readCameras();

            JsonPrimitive defaultScene = docRoot.getAsJsonPrimitive("scene");

            readScenes(defaultScene, rootNode);

            rootNode = customContentManager.readExtensionAndExtras("root", docRoot, rootNode);

            setupControls();

            //Loading animations
            if (animations != null) {
                for (int i = 0; i < animations.size(); i++) {
                    readAnimation(i);
                }
            }

            //only one scene let's not return the root.
            if (rootNode.getChildren().size() == 1) {
                rootNode = (Node) rootNode.getChild(0);
            }
            //no name for the scene... let's set the file name.
            if (rootNode.getName() == null) {
                rootNode.setName(assetInfo.getKey().getName());
            }
            return rootNode;
        } catch (Exception e) {
            throw new AssetLoadException("An error occurred loading " + assetInfo.getKey().getName(), e);
        } finally {
            stream.close();
        }
    }

    private void setDefaultParams(Material mat) {
        mat.setColor("BaseColor", ColorRGBA.White);
        mat.setFloat("Metallic", 0f);
        mat.setFloat("Roughness", 1f);
    }

    private boolean isSupported(String version, String minVersion) {
        return "2.0".equals(version);
    }

    public void readScenes(JsonPrimitive defaultScene, Node rootNode) throws IOException {
        if (scenes == null) {
            //no scene... lets handle this later...
            throw new AssetLoadException("Gltf files with no scene is not yet supported");
        }

        for (JsonElement scene : scenes) {
            Node sceneNode = new Node();
            //specs says that only the default scene should be rendered,
            // if there are several scenes, they are attached to the rootScene, but they are culled
            sceneNode.setCullHint(Spatial.CullHint.Always);

            sceneNode.setName(getAsString(scene.getAsJsonObject(), "name"));
            JsonArray sceneNodes = scene.getAsJsonObject().getAsJsonArray("nodes");
            sceneNode = customContentManager.readExtensionAndExtras("scene", scene, sceneNode);
            rootNode.attachChild(sceneNode);
            for (JsonElement node : sceneNodes) {
                readChild(sceneNode, node);
            }

        }

        //Setting the default scene cul hint to inherit.
        int activeChild = 0;
        if (defaultScene != null) {
            activeChild = defaultScene.getAsInt();
        }
        rootNode.getChild(activeChild).setCullHint(Spatial.CullHint.Inherit);
    }

    public Object readNode(int nodeIndex) throws IOException {
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
            Integer camIndex = getAsInteger(nodeData, "camera");
            if (camIndex != null) {
                Camera cam = fetchFromCache("cameras", camIndex, Camera.class);
                CameraNode node = new CameraNode(null, cam);
                node.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
                spatial = node;
            } else {
                Node node = new Node();
                spatial = node;
            }
        }

        Integer skinIndex = getAsInteger(nodeData, "skin");
        if (skinIndex != null) {
            SkinData skinData = fetchFromCache("skins", skinIndex, SkinData.class);
            List<Spatial> spatials = skinnedSpatials.get(skinData);
            spatials.add(spatial);
            skinData.used = true;
        }

        spatial.setLocalTransform(readTransforms(nodeData));

        if (spatial.getName() == null) {
            spatial.setName(getAsString(nodeData.getAsJsonObject(), "name"));
        }

        spatial = customContentManager.readExtensionAndExtras("node", nodeData, spatial);

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
            //parent is the Armature Node, we have to apply its transforms to the root bone's animation data
            BoneWrapper bw = (BoneWrapper) loaded;
            bw.isRoot = true;
            SkinData skinData = fetchFromCache("skins", bw.skinIndex, SkinData.class);
            if (skinData == null) {
                return;
            }
            skinData.parent = parent;
        }

    }

    public Transform readTransforms(JsonObject nodeData) {
        Transform transform = new Transform();
        JsonArray matrix = nodeData.getAsJsonArray("matrix");
        if (matrix != null) {
            //transforms are given as a mat4
            float[] tmpArray = new float[16];
            for (int i = 0; i < tmpArray.length; i++) {
                tmpArray[i] = matrix.get(i).getAsFloat();
            }
            //creates a row major matrix from color major data
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

    public Geometry[] readMeshPrimitives(int meshIndex) throws IOException {
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

            mesh = customContentManager.readExtensionAndExtras("primitive", meshObject, mesh);

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

        geomArray = customContentManager.readExtensionAndExtras("mesh", meshData, geomArray);

        addToCache("meshes", meshIndex, geomArray, meshes.size());
        return geomArray;
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

    public <R> R readAccessorData(int accessorIndex, Populator<R> populator) throws IOException {

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

        //TODO min / max...don't know what to do about them.
        //TODO sparse

        R data = populator.populate(bufferViewIndex, componentType, type, count, byteOffset, normalized);
        data = customContentManager.readExtensionAndExtras("accessor", accessor, data);
        return data;
    }

    public Object readBuffer(Integer bufferViewIndex, int byteOffset, int count, Object store, int numComponents, VertexBuffer.Format format) throws IOException {

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

        data = customContentManager.readExtensionAndExtras("bufferView", bufferView, data);

        if (store == null) {
            store = new byte[byteLength];
        }

        if (count == -1) {
            count = byteLength;
        }

        populateBuffer(store, data, count, byteOffset + bvByteOffset, byteStride, numComponents, format);

        return store;
    }

    public byte[] readData(int bufferIndex) throws IOException {

        assertNotNull(buffers, "No buffer defined");

        JsonObject buffer = buffers.get(bufferIndex).getAsJsonObject();
        String uri = getAsString(buffer, "uri");
        Integer bufferLength = getAsInteger(buffer, "byteLength");
        assertNotNull(bufferLength, "No byteLength defined for buffer " + bufferIndex);
        byte[] data = (byte[]) fetchFromCache("buffers", bufferIndex, Object.class);
        if (data != null) {
            return data;
        }
        data = getBytes(bufferIndex, uri, bufferLength);

        data = customContentManager.readExtensionAndExtras("buffer", buffer, data);

        addToCache("buffers", bufferIndex, data, buffers.size());
        return data;

    }

    protected byte[] getBytes(int bufferIndex, String uri, Integer bufferLength) throws IOException {
        byte[] data;
        if (uri != null) {
            if (uri.startsWith("data:")) {
                //base 64 embed data
                data = DatatypeConverter.parseBase64Binary(uri.substring(uri.indexOf(",") + 1));
            } else {
                //external file let's load it
                if (!uri.endsWith(".bin")) {
                    throw new AssetLoadException("Cannot load " + uri + ", a .bin extension is required.");
                }

                BinDataKey key = new BinDataKey(info.getKey().getFolder() + uri);
                InputStream input = (InputStream) info.getManager().loadAsset(key);
                data = new byte[bufferLength];
                DataInputStream dataStream = new DataInputStream(input);
                dataStream.readFully(data);
                dataStream.close();
            }
        } else {
            //no URI this should not happen in a gltf file, only in glb files.
            throw new AssetLoadException("Buffer " + bufferIndex + " has no uri");
        }
        return data;
    }

    public Material readMaterial(int materialIndex) throws IOException {
        assertNotNull(materials, "There is no material defined yet a mesh references one");

        JsonObject matData = materials.get(materialIndex).getAsJsonObject();
        JsonObject pbrMat = matData.getAsJsonObject("pbrMetallicRoughness");


        MaterialAdapter adapter = null;

        if (pbrMat != null) {
            adapter = getAdapterForMaterial(info, "pbrMetallicRoughness");
            if (adapter == null) {
                adapter = defaultMaterialAdapters.get("pbrMetallicRoughness");
            }
            adapter.init(info.getManager());
        }

        adapter = customContentManager.readExtensionAndExtras("material", matData, adapter);

        if (adapter == null) {
            logger.log(Level.WARNING, "Couldn't find any matching material definition for material " + materialIndex);
            adapter = defaultMaterialAdapters.get("pbrMetallicRoughness");
            adapter.init(info.getManager());
            setDefaultParams(adapter.getMaterial());
        }

        if (pbrMat != null) {
            adapter.setParam("baseColorFactor", getAsColor(pbrMat, "baseColorFactor", ColorRGBA.White));
            adapter.setParam("metallicFactor", getAsFloat(pbrMat, "metallicFactor", 1f));
            adapter.setParam("roughnessFactor", getAsFloat(pbrMat, "roughnessFactor", 1f));
            adapter.setParam("baseColorTexture", readTexture(pbrMat.getAsJsonObject("baseColorTexture")));
            adapter.setParam("metallicRoughnessTexture", readTexture(pbrMat.getAsJsonObject("metallicRoughnessTexture")));
        }

        adapter.getMaterial().setName(getAsString(matData, "name"));
        adapter.setParam("emissiveFactor", getAsColor(matData, "emissiveFactor", ColorRGBA.Black));
        String alphaMode = getAsString(matData, "alphaMode");
        adapter.setParam("alphaMode", alphaMode);
        if (alphaMode != null && alphaMode.equals("MASK")) {
            adapter.setParam("alphaCutoff", getAsFloat(matData, "alphaCutoff"));
        }
        adapter.setParam("doubleSided", getAsBoolean(matData, "doubleSided"));
        Texture2D normal = readTexture(matData.getAsJsonObject("normalTexture"));
        adapter.setParam("normalTexture", normal);
        if (normal != null) {
            useNormalsFlag = true;
        }
        adapter.setParam("occlusionTexture", readTexture(matData.getAsJsonObject("occlusionTexture")));
        adapter.setParam("emissiveTexture", readTexture(matData.getAsJsonObject("emissiveTexture")));


        return adapter.getMaterial();
    }

    public void readCameras() throws IOException {
        if (cameras == null) {
            return;
        }
        for (int i = 0; i < cameras.size(); i++) {

            //Can't access resolution here... actually it's a shame we can't access settings from anywhere.
            //users will have to call resize ont he camera.
            Camera cam = new Camera(1, 1);

            JsonObject camObj = cameras.get(i).getAsJsonObject();
            String type = getAsString(camObj, "type");
            assertNotNull(type, "No type defined ofr camera");
            JsonObject camData = camObj.getAsJsonObject(type);
            if (type.equals("perspective")) {
                float aspectRatio = getAsFloat(camData, "aspectRation", 1f);
                Float yfov = getAsFloat(camData, "yfov");
                assertNotNull(yfov, "No yfov for perspective camera");
                Float znear = getAsFloat(camData, "znear");
                assertNotNull(znear, "No znear for perspective camere");
                Float zfar = getAsFloat(camData, "zfar", znear * 1000f);

                cam.setFrustumPerspective(yfov * FastMath.RAD_TO_DEG, aspectRatio, znear, zfar);
                cam = customContentManager.readExtensionAndExtras("camera.perspective", camData, cam);

            } else {
                Float xmag = getAsFloat(camData, "xmag");
                assertNotNull(xmag, "No xmag for orthographic camera");
                Float ymag = getAsFloat(camData, "ymag");
                assertNotNull(ymag, "No ymag for orthographic camera");
                Float znear = getAsFloat(camData, "znear");
                assertNotNull(znear, "No znear for orthographic camere");
                Float zfar = getAsFloat(camData, "zfar", znear * 1000f);
                assertNotNull(zfar, "No zfar for orthographic camera");

                cam.setParallelProjection(true);
                cam.setFrustum(znear, zfar, -xmag, xmag, ymag, -ymag);

                cam = customContentManager.readExtensionAndExtras("camera.orthographic", camData, cam);
            }
            cam = customContentManager.readExtensionAndExtras("camera", camObj, cam);
            addToCache("cameras", i, cam, cameras.size());
        }
    }

    public Texture2D readTexture(JsonObject texture) throws IOException {
        return readTexture(texture, false);

    }

    public Texture2D readTexture(JsonObject texture, boolean flip) throws IOException {
        if (texture == null) {
            return null;
        }
        Integer textureIndex = getAsInteger(texture, "index");
        assertNotNull(textureIndex, "Texture as no index");
        assertNotNull(textures, "There are no textures, yet one is referenced by a material");

        JsonObject textureData = textures.get(textureIndex).getAsJsonObject();
        Integer sourceIndex = getAsInteger(textureData, "source");
        Integer samplerIndex = getAsInteger(textureData, "sampler");

        Texture2D texture2d = readImage(sourceIndex, flip);

        if (samplerIndex != null) {
            texture2d = readSampler(samplerIndex, texture2d);
        } else {
            texture2d.setWrap(Texture.WrapMode.Repeat);
        }

        texture2d = customContentManager.readExtensionAndExtras("texture", texture, texture2d);

        return texture2d;
    }

    public Texture2D readImage(int sourceIndex, boolean flip) throws IOException {
        if (images == null) {
            throw new AssetLoadException("No image defined");
        }

        JsonObject image = images.get(sourceIndex).getAsJsonObject();
        String uri = getAsString(image, "uri");
        Integer bufferView = getAsInteger(image, "bufferView");
        String mimeType = getAsString(image, "mimeType");
        Texture2D result;
        if (uri == null) {
            assertNotNull(bufferView, "Image " + sourceIndex + " should either have an uri or a bufferView");
            assertNotNull(mimeType, "Image " + sourceIndex + " should have a mimeType");
            byte[] data = (byte[]) readBuffer(bufferView, 0, -1, null, 1, VertexBuffer.Format.Byte);
            String extension = mimeType.split("/")[1];
            TextureKey key = new TextureKey("image" + sourceIndex + "." + extension, flip);
            result = (Texture2D) info.getManager().loadAssetFromStream(key, new ByteArrayInputStream(data));

        } else if (uri.startsWith("data:")) {
            //base64 encoded image
            String[] uriInfo = uri.split(",");
            byte[] data = DatatypeConverter.parseBase64Binary(uriInfo[1]);
            String headerInfo = uriInfo[0].split(";")[0];
            String extension = headerInfo.split("/")[1];
            TextureKey key = new TextureKey("image" + sourceIndex + "." + extension, flip);
            result = (Texture2D) info.getManager().loadAssetFromStream(key, new ByteArrayInputStream(data));
        } else {
            //external file image
            TextureKey key = new TextureKey(info.getKey().getFolder() + uri, flip);
            Texture tex = info.getManager().loadTexture(key);
            result = (Texture2D) tex;
        }
        return result;
    }

    public void readAnimation(int animationIndex) throws IOException {
        JsonObject animation = animations.get(animationIndex).getAsJsonObject();
        JsonArray channels = animation.getAsJsonArray("channels");
        JsonArray samplers = animation.getAsJsonArray("samplers");
        String name = getAsString(animation, "name");
        assertNotNull(channels, "No channels for animation " + name);
        assertNotNull(samplers, "No samplers for animation " + name);

        //temp data storage of track data
        TrackData[] tracks = new TrackData[nodes.size()];

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

            TrackData trackData = tracks[targetNode];
            if (trackData == null) {
                trackData = new TrackData();
                tracks[targetNode] = trackData;
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

            trackData = customContentManager.readExtensionAndExtras("animation.sampler", sampler, trackData);

            float[] times = fetchFromCache("accessors", timeIndex, float[].class);
            if (times == null) {
                times = readAccessorData(timeIndex, floatArrayPopulator);
                addToCache("accessors", timeIndex, times, accessors.size());
            }

            if (targetPath.equals("translation")) {
                trackData.timeArrays.add(new TrackData.TimeData(times, TrackData.Type.Translation));
                Vector3f[] translations = readAccessorData(dataIndex, vector3fArrayPopulator);
                trackData.translations = translations;
            } else if (targetPath.equals("scale")) {
                trackData.timeArrays.add(new TrackData.TimeData(times, TrackData.Type.Scale));
                Vector3f[] scales = readAccessorData(dataIndex, vector3fArrayPopulator);
                trackData.scales = scales;
            } else if (targetPath.equals("rotation")) {
                trackData.timeArrays.add(new TrackData.TimeData(times, TrackData.Type.Rotation));
                Quaternion[] rotations = readAccessorData(dataIndex, quaternionArrayPopulator);
                trackData.rotations = rotations;
            } else {
                //TODO support weights
                logger.log(Level.WARNING, "Morph animation is not supported");
                continue;
            }
            tracks[targetNode] = customContentManager.readExtensionAndExtras("channel", channel, trackData);
        }

        if (name == null) {
            name = "anim_" + animationIndex;
        }

        List<Spatial> spatials = new ArrayList<>();
        Animation anim = new Animation();
        anim.setName(name);
        int skinIndex = -1;

        List<Bone> usedBones = new ArrayList<>();
        for (int i = 0; i < tracks.length; i++) {
            TrackData trackData = tracks[i];
            if (trackData == null || trackData.timeArrays.isEmpty()) {
                continue;
            }
            trackData.update();
            if (trackData.length > anim.getLength()) {
                anim.setLength(trackData.length);
            }
            Object node = fetchFromCache("nodes", i, Object.class);
            if (node instanceof Spatial) {
                Spatial s = (Spatial) node;
                spatials.add(s);
                SpatialTrack track = new SpatialTrack(trackData.times, trackData.translations, trackData.rotations, trackData.scales);
                track.setTrackSpatial(s);
                anim.addTrack(track);
            } else if (node instanceof BoneWrapper) {
                BoneWrapper b = (BoneWrapper) node;
                //apply the inverseBindMatrix to animation data.
                b.update(trackData);
                usedBones.add(b.bone);

                if (skinIndex == -1) {
                    skinIndex = b.skinIndex;
                } else {
                    //Check if all bones affected by this animation are from the same skin, the track will be skipped.
                    if (skinIndex != b.skinIndex) {
                        logger.log(Level.WARNING, "Animation " + animationIndex + " (" + name + ") applies to bones that are not from the same skin: skin " + skinIndex + ", bone " + b.bone.getName() + " from skin " + b.skinIndex);
                        continue;
                    }
                }

                BoneTrack track = new BoneTrack(b.boneIndex, trackData.times, trackData.translations, trackData.rotations, trackData.scales);
                anim.addTrack(track);

            }
        }

        // Check each bone to see if their local pose is different from their bind pose.
        // If it is, we ensure that the bone has an animation track, else JME way of applying anim transforms will apply the bind pose to those bones,
        // instead of the local pose that is supposed to be the default
        if (skinIndex != -1) {
            SkinData skin = fetchFromCache("skins", skinIndex, SkinData.class);
            Skeleton skeleton = skin.skeletonControl.getSkeleton();
            for (Bone bone : skin.bones) {
                if (!usedBones.contains(bone) && !equalBindAndLocalTransforms(bone)) {
                    //create a track
                    float[] times = new float[]{0, anim.getLength()};

                    Vector3f t = bone.getLocalPosition().subtract(bone.getBindPosition());
                    Quaternion r = tmpQuat.set(bone.getBindRotation()).inverse().multLocal(bone.getLocalRotation());
                    Vector3f s = bone.getLocalScale().divide(bone.getBindScale());

                    Vector3f[] translations = new Vector3f[]{t, t};
                    Quaternion[] rotations = new Quaternion[]{r, r};
                    Vector3f[] scales = new Vector3f[]{s, s};

                    int boneIndex = skeleton.getBoneIndex(bone);
                    BoneTrack track = new BoneTrack(boneIndex, times, translations, rotations, scales);
                    anim.addTrack(track);
                }
            }
        }

        anim = customContentManager.readExtensionAndExtras("animations", animation, anim);

        if (skinIndex != -1) {
            //we have a bone animation.
            SkinData skin = fetchFromCache("skins", skinIndex, SkinData.class);
            skin.animControl.addAnim(anim);
        }

        if (!spatials.isEmpty()) {
            if (skinIndex != -1) {
                //there are some spatial tracks in this bone animation... or the other way around. Let's add the spatials in the skinnedSpatials.
                SkinData skin = fetchFromCache("skins", skinIndex, SkinData.class);
                List<Spatial> spat = skinnedSpatials.get(skin);
                spat.addAll(spatials);
                //the animControl will be added in the setupControls();
            } else {
                //Spatial animation
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

    public Texture2D readSampler(int samplerIndex, Texture2D texture) throws IOException {
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

        texture = customContentManager.readExtensionAndExtras("texture.sampler", sampler, texture);

        return texture;
    }

    public void readSkins() throws IOException {
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

            //These inverse bind matrices, once inverted again, will give us the real bind pose of the bones (in model space),
            //since the skeleton in not guaranteed to be exported in bind pose.
            Integer matricesIndex = getAsInteger(skin, "inverseBindMatrices");
            Matrix4f[] inverseBindMatrices = null;
            if (matricesIndex != null) {
                inverseBindMatrices = readAccessorData(matricesIndex, matrix4fArrayPopulator);
            } else {
                inverseBindMatrices = new Matrix4f[joints.size()];
                for (int i = 0; i < inverseBindMatrices.length; i++) {
                    inverseBindMatrices[i] = new Matrix4f();
                }
            }

            Bone[] bones = new Bone[joints.size()];
            for (int i = 0; i < joints.size(); i++) {
                int boneIndex = joints.get(i).getAsInt();
                //we don't need the inverse bind matrix, we need the bind matrix so let's invert it.
                Matrix4f modelBindMatrix = inverseBindMatrices[i].invertLocal();
                bones[i] = readNodeAsBone(boneIndex, i, index, modelBindMatrix);
            }

            for (int i = 0; i < joints.size(); i++) {
                findChildren(joints.get(i).getAsInt());
            }

            Skeleton skeleton = new Skeleton(bones);

            //Compute bind transforms. We need to do it from root bone to leaves bone.
            for (Bone bone : skeleton.getRoots()) {
                BoneWrapper bw = findBoneWrapper(bone);
                computeBindTransforms(bw, skeleton);
            }

            skeleton = customContentManager.readExtensionAndExtras("skin", skin, skeleton);
            SkinData skinData = new SkinData();
            skinData.bones = bones;
            skinData.skeletonControl = new SkeletonControl(skeleton);
            skinData.animControl = new AnimControl(skinData.skeletonControl.getSkeleton());
            addToCache("skins", index, skinData, nodes.size());
            skinnedSpatials.put(skinData, new ArrayList<Spatial>());

            // Set local transforms.
            // The skeleton may come in a given pose, that is not the rest pose, so let 's apply it.
            // We will need it later for animation
            for (int i = 0; i < joints.size(); i++) {
                applyPose(joints.get(i).getAsInt());
            }
            skeleton.updateWorldVectors();

            //If the user didn't ask to keep the pose we reset the skeleton user control
            if (!isKeepSkeletonPose(info)) {
                for (Bone bone : bones) {
                    bone.setUserControl(false);
                }
            }
        }
    }

    private void applyPose(int index) {
        BoneWrapper bw = fetchFromCache("nodes", index, BoneWrapper.class);
        bw.bone.setUserControl(true);
        bw.bone.setLocalTranslation(bw.localTransform.getTranslation());
        bw.bone.setLocalRotation(bw.localTransform.getRotation());
        bw.bone.setLocalScale(bw.localTransform.getScale());
    }

    private void computeBindTransforms(BoneWrapper boneWrapper, Skeleton skeleton) {
        Bone bone = boneWrapper.bone;
        tmpTransforms.fromTransformMatrix(boneWrapper.modelBindMatrix);
        if (bone.getParent() != null) {
            //root bone, model transforms are the same as the local transforms
            //but for child bones we need to combine it with the parents inverse model transforms.
            tmpMat.setTranslation(bone.getParent().getModelSpacePosition());
            tmpMat.setRotationQuaternion(bone.getParent().getModelSpaceRotation());
            tmpMat.setScale(bone.getParent().getModelSpaceScale());
            tmpMat.invertLocal();
            tmpTransforms2.fromTransformMatrix(tmpMat);
            tmpTransforms.combineWithParent(tmpTransforms2);
        }
        bone.setBindTransforms(tmpTransforms.getTranslation(), tmpTransforms.getRotation(), tmpTransforms.getScale());

        //resets the local transforms to bind transforms for all bones.
        //then computes the model transforms from local transforms for each bone.
        skeleton.resetAndUpdate();
        skeleton.setBindingPose();
        for (Integer childIndex : boneWrapper.children) {
            BoneWrapper child = fetchFromCache("nodes", childIndex, BoneWrapper.class);
            computeBindTransforms(child, skeleton);
        }
    }

    private BoneWrapper findBoneWrapper(Bone bone) {
        for (int i = 0; i < nodes.size(); i++) {
            BoneWrapper bw = fetchFromCache("nodes", i, BoneWrapper.class);
            if (bw != null && bw.bone == bone) {
                return bw;
            }
        }
        return null;
    }

    public Bone readNodeAsBone(int nodeIndex, int boneIndex, int skinIndex, Matrix4f modelBindMatrix) throws IOException {

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
        Transform boneTransforms = null;
        boneTransforms = readTransforms(nodeData);

        addToCache("nodes", nodeIndex, new BoneWrapper(bone, boneIndex, skinIndex, modelBindMatrix, boneTransforms), nodes.size());

        return bone;
    }

    private void findChildren(int nodeIndex) throws IOException {
        BoneWrapper bw = fetchFromCache("nodes", nodeIndex, BoneWrapper.class);
        JsonObject nodeData = nodes.get(nodeIndex).getAsJsonObject();
        JsonArray children = nodeData.getAsJsonArray("children");

        if (children != null) {
            for (JsonElement child : children) {
                int childIndex = child.getAsInt();
                BoneWrapper cbw = fetchFromCache("nodes", childIndex, BoneWrapper.class);
                if (cbw != null) {
                    bw.bone.addChild(cbw.bone);
                    bw.children.add(childIndex);
                } else {
                    //The child might be a Node
                    //Creating a dummy node to reed the subgraph
                    Node n = new Node();
                    readChild(n, child);
                    Spatial s = n.getChild(0);
                    //removing the spatial from the dummy node, it will be attached to the attachment node of the bone
                    s.removeFromParent();
                    bw.attachedSpatial = s;
                }
            }

        }
    }

    private void setupControls() {
        for (SkinData skinData : skinnedSpatials.keySet()) {
            List<Spatial> spatials = skinnedSpatials.get(skinData);
            Spatial spatial = skinData.parent;

            if (spatials.size() >= 1) {
                spatial = findCommonAncestor(spatials);
            }

            if (spatial != skinData.parent) {
                skinData.rootBoneTransformOffset = spatial.getWorldTransform().invert();
                skinData.rootBoneTransformOffset.combineWithParent(skinData.parent.getWorldTransform());
            }

            if (skinData.animControl != null && skinData.animControl.getSpatial() == null) {
                spatial.addControl(skinData.animControl);
            }
            spatial.addControl(skinData.skeletonControl);
        }

        for (int i = 0; i < nodes.size(); i++) {
            BoneWrapper bw = fetchFromCache("nodes", i, BoneWrapper.class);
            if (bw == null || bw.attachedSpatial == null) {
                continue;
            }
            SkinData skinData = fetchFromCache("skins", bw.skinIndex, SkinData.class);
            skinData.skeletonControl.getAttachmentsNode(bw.bone.getName()).attachChild(bw.attachedSpatial);
        }
    }

    private String readMeshName(int meshIndex) {
        JsonObject meshData = meshes.get(meshIndex).getAsJsonObject();
        return getAsString(meshData, "name");
    }

    public <T> T fetchFromCache(String name, int index, Class<T> type) {
        Object[] data = dataCache.get(name);
        if (data == null) {
            return null;
        }
        try {
            T ret = type.cast(data[index]);
            return ret;
        } catch (ClassCastException e) {
            return null;
        }

    }

    public void addToCache(String name, int index, Object object, int maxLength) {
        Object[] data = dataCache.get(name);
        if (data == null) {
            data = new Object[maxLength];
            dataCache.put(name, data);
        }
        data[index] = object;
    }

    public AssetInfo getInfo() {
        return info;
    }

    public JsonObject getDocRoot() {
        return docRoot;
    }

    public Node getRootNode() {
        return rootNode;
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


    private class BoneWrapper {
        Bone bone;
        int boneIndex;
        int skinIndex;
        Transform localTransform;
        Transform localTransformOffset;
        Matrix4f modelBindMatrix;
        boolean isRoot = false;
        boolean localUpdated = false;
        Spatial attachedSpatial;
        List<Integer> children = new ArrayList<>();

        public BoneWrapper(Bone bone, int boneIndex, int skinIndex, Matrix4f modelBindMatrix, Transform localTransform) {
            this.bone = bone;
            this.boneIndex = boneIndex;
            this.skinIndex = skinIndex;
            this.modelBindMatrix = modelBindMatrix;
            this.localTransform = localTransform;
            this.localTransformOffset = localTransform.clone();
        }

        /**
         * Applies the inverse Bind transforms to anim data. and the armature transforms if relevant.
         */
        public void update(TrackData data) {
            Transform bindTransforms = new Transform(bone.getBindPosition(), bone.getBindRotation(), bone.getBindScale());
            SkinData skinData = fetchFromCache("skins", skinIndex, SkinData.class);

            if (!localUpdated) {
                //LocalTransform of the bone are default position to use for animations when there is no track.
                //We need to transform them so that JME can us them in blendAnimTransform.
                reverseBlendAnimTransforms(localTransformOffset, bindTransforms);
                localUpdated = true;
            }

            for (int i = 0; i < data.getNbKeyFrames(); i++) {

                Vector3f translation = getTranslation(data, i);
                Quaternion rotation = getRotation(data, i);
                Vector3f scale = getScale(data, i);

                Transform t = new Transform(translation, rotation, scale);
                if (isRoot && skinData.rootBoneTransformOffset != null) {
                    //Apply the armature transforms to the root bone anim track.
                    t.combineWithParent(skinData.rootBoneTransformOffset);
                }

                reverseBlendAnimTransforms(t, bindTransforms);

                if (data.translations != null) {
                    data.translations[i] = t.getTranslation();
                }
                if (data.rotations != null) {
                    data.rotations[i] = t.getRotation();
                }
                if (data.scales != null) {
                    data.scales[i] = t.getScale();
                }
            }

            data.ensureTranslationRotations(localTransformOffset);
        }

        private void reverseBlendAnimTransforms(Transform t, Transform bindTransforms) {
            //This is wrong
            //You'd normally combine those transforms with transform.combineWithParent()
            //Here we actually do in reverse what JME does to combine anim transforms with bind transfoms (add trans/mult rot/ mult scale)
            //The code to fix is in Bone.blendAnimTransforms
            //TODO fix blendAnimTransforms
            t.getTranslation().subtractLocal(bindTransforms.getTranslation());
            t.getScale().divideLocal(bindTransforms.getScale());
            tmpQuat.set(bindTransforms.getRotation()).inverseLocal().multLocal(t.getRotation());
            t.setRotation(tmpQuat);
        }

        private Vector3f getTranslation(TrackData data, int i) {
            Vector3f translation;
            if (data.translations == null) {
                translation = bone.getLocalPosition();
            } else {
                translation = data.translations[i];
            }
            return translation;
        }

        private Quaternion getRotation(TrackData data, int i) {
            Quaternion rotation;
            if (data.rotations == null) {
                rotation = bone.getLocalRotation();
            } else {
                rotation = data.rotations[i];
            }
            return rotation;
        }

        private Vector3f getScale(TrackData data, int i) {
            Vector3f scale;
            if (data.scales == null) {
                scale = bone.getLocalScale();
            } else {
                scale = data.scales[i];
            }
            return scale;
        }
    }

    private class SkinData {
        SkeletonControl skeletonControl;
        AnimControl animControl;
        Spatial parent;
        Transform rootBoneTransformOffset;
        Bone[] bones;
        boolean used = false;
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
        T populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset, boolean normalized) throws IOException;
    }

    private class VertexBufferPopulator implements Populator<VertexBuffer> {
        VertexBuffer.Type bufferType;

        public VertexBufferPopulator(VertexBuffer.Type bufferType) {
            this.bufferType = bufferType;
        }

        @Override
        public VertexBuffer populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset, boolean normalized) throws IOException {

            if (bufferType == null) {
                logger.log(Level.WARNING, "could not assign data to any VertexBuffer type for buffer view " + bufferViewIndex);
                return null;
            }


            VertexBuffer vb = new VertexBuffer(bufferType);
            VertexBuffer.Format format = getVertexBufferFormat(componentType);
            VertexBuffer.Format originalFormat = format;
            if (normalized) {
                //Some float data can be packed into short buffers, "normalized" means they have to be unpacked.
                //In that case the buffer is a FloatBuffer
                format = VertexBuffer.Format.Float;
            }
            int numComponents = getNumberOfComponents(type);

            Buffer buff = VertexBuffer.createBuffer(format, numComponents, count);
            int bufferSize = numComponents * count;
            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the buffer with zeros.
                padBuffer(buff, bufferSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, count, buff, numComponents, originalFormat);
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
        public float[] populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset, boolean normalized) throws IOException {

            int numComponents = getNumberOfComponents(type);
            int dataSize = numComponents * count;
            float[] data = new float[dataSize];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, count, data, numComponents, getVertexBufferFormat(componentType));
            }

            return data;
        }

    }

    private class Vector3fArrayPopulator implements Populator<Vector3f[]> {

        @Override
        public Vector3f[] populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset, boolean normalized) throws IOException {

            int numComponents = getNumberOfComponents(type);
            int dataSize = numComponents * count;
            Vector3f[] data = new Vector3f[count];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, count, data, numComponents, getVertexBufferFormat(componentType));
            }
            return data;
        }
    }

    private class QuaternionArrayPopulator implements Populator<Quaternion[]> {

        @Override
        public Quaternion[] populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset, boolean normalized) throws IOException {

            int numComponents = getNumberOfComponents(type);
            int dataSize = numComponents * count;
            Quaternion[] data = new Quaternion[count];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, count, data, numComponents, getVertexBufferFormat(componentType));
            }

            return data;
        }
    }

    private class Matrix4fArrayPopulator implements Populator<Matrix4f[]> {

        @Override
        public Matrix4f[] populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset, boolean normalized) throws IOException {

            int numComponents = getNumberOfComponents(type);
            int dataSize = numComponents * count;
            Matrix4f[] data = new Matrix4f[count];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, count, data, numComponents, getVertexBufferFormat(componentType));
            }

            return data;
        }
    }

    private class JointArrayPopulator implements Populator<SkinBuffers> {

        @Override
        public SkinBuffers populate(Integer bufferViewIndex, int componentType, String type, int count, int byteOffset, boolean normalized) throws IOException {

            int numComponents = getNumberOfComponents(type);

            //can be bytes or shorts.
            VertexBuffer.Format format = VertexBuffer.Format.Byte;
            if (componentType == 5123) {
                format = VertexBuffer.Format.Short;
            }

            int dataSize = numComponents * count;
            short[] data = new short[dataSize];

            if (bufferViewIndex == null) {
                //no referenced buffer, specs says to pad the data with zeros.
                padBuffer(data, dataSize);
            } else {
                readBuffer(bufferViewIndex, byteOffset, count, data, numComponents, format);
            }

            return new SkinBuffers(data, format.getComponentSize());
        }
    }

}

