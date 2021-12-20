package com.jme3.scene.plugins.fbx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.fbx.AnimationList.AnimInverval;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.file.FbxFile;
import com.jme3.scene.plugins.fbx.file.FbxReader;
import com.jme3.scene.plugins.fbx.objects.FbxAnimCurve;
import com.jme3.scene.plugins.fbx.objects.FbxAnimNode;
import com.jme3.scene.plugins.fbx.objects.FbxBindPose;
import com.jme3.scene.plugins.fbx.objects.FbxCluster;
import com.jme3.scene.plugins.fbx.objects.FbxImage;
import com.jme3.scene.plugins.fbx.objects.FbxMaterial;
import com.jme3.scene.plugins.fbx.objects.FbxMesh;
import com.jme3.scene.plugins.fbx.objects.FbxNode;
import com.jme3.scene.plugins.fbx.objects.FbxObject;
import com.jme3.scene.plugins.fbx.objects.FbxSkin;
import com.jme3.scene.plugins.fbx.objects.FbxTexture;

/**
 * FBX file format loader
 * <p> Loads scene meshes, materials, textures, skeleton and skeletal animation.
 * Multiple animations can be defined with {@link AnimationList} passing into {@link SceneKey}
 * or loaded from different animation layer.</p>
 */
public class SceneLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(SceneLoader.class.getName());
    private static final double secondsPerUnit = 1 / 46186158000d; // Animation speed factor
    public static final boolean WARN_IGNORED_ATTRIBUTES = false;

    // Scene loading data
    private List<String> warnings = new ArrayList<>();
    private AnimationList animList;
    private String sceneName;
    public String sceneFilename;
    public String sceneFolderName;
    public AssetManager assetManager;
    public AssetInfo currentAssetInfo;

    // Scene global settings
    private float animFrameRate;
    public float unitSize;
    public int xAxis = 1;
    public int yAxis = 1;
    public int zAxis = 1;

    // Scene objects
    private Map<Long, FbxObject> allObjects = new HashMap<>(); // All supported FBX objects
    private Map<Long, FbxSkin> skinMap = new HashMap<>(); // Skin for bone clusters
    private Map<Long, FbxObject> aLayerMap = new HashMap<>(); // Animation layers
    public Map<Long, FbxNode> modelMap = new HashMap<>(); // Nodes
    private Map<Long, FbxNode> limbMap = new HashMap<>(); // Nodes that are actually bones
    private Map<Long, FbxBindPose> bindMap = new HashMap<>(); // Node bind poses
    private Map<Long, FbxMesh> geomMap = new HashMap<>(); // Mesh geometries
    private Skeleton skeleton;
    private AnimControl animControl;
    public Node sceneNode;

    public void warning(String warning) {
        warnings.add(warning);
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        this.currentAssetInfo = assetInfo;
        this.assetManager = assetInfo.getManager();
        AssetKey<?> assetKey = assetInfo.getKey();
        if(assetKey instanceof SceneKey)
            animList = ((SceneKey) assetKey).getAnimations();
        InputStream stream = assetInfo.openStream();
        final Node sceneNode = this.sceneNode = new Node(sceneName + "-scene");
        try {
            sceneFilename = assetKey.getName();
            sceneFolderName = assetKey.getFolder();
            String ext = assetKey.getExtension();
            sceneName = sceneFilename.substring(0, sceneFilename.length() - ext.length() - 1);
            if(sceneFolderName != null && sceneFolderName.length() > 0)
                sceneName = sceneName.substring(sceneFolderName.length());
            loadScene(stream);
            linkScene();
            if(warnings.size() > 0)
                logger.log(Level.WARNING, "Model load finished with warnings:\n" + join(warnings, "\n"));
        } finally {
            releaseObjects();
            if(stream != null)
                stream.close();
        }
        return sceneNode;
    }

    private void loadScene(InputStream stream) throws IOException {
        logger.log(Level.FINE, "Loading scene {0}", sceneFilename);
        long startTime = System.currentTimeMillis();
        FbxFile scene = FbxReader.readFBX(stream);
        for(FbxElement e : scene.rootElements) {
            // Is it possible for elements to be in wrong order?
            switch(e.id) {
            case "GlobalSettings":
                loadGlobalSettings(e);
                break;
            case "Objects":
                loadObjects(e);
                break;
            case "Connections":
                loadConnections(e);
                break;
            }
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.log(Level.FINE, "Loading done in {0} ms", estimatedTime);
    }

    private void loadGlobalSettings(FbxElement element) {
        for(FbxElement e2 : element.getFbxProperties()) {
            String propName = (String) e2.properties.get(0);
            switch(propName) {
            case "UnitScaleFactor":
                this.unitSize = ((Double) e2.properties.get(4)).floatValue();
                break;
            case "CustomFrameRate":
                float framerate = ((Double) e2.properties.get(4)).floatValue();
                if(framerate != -1)
                    this.animFrameRate = framerate;
                break;
            case "UpAxisSign":
                this.yAxis = ((Integer) e2.properties.get(4)).intValue();
                break;
            case "FrontAxisSign":
                this.zAxis = ((Integer) e2.properties.get(4)).intValue();
                break;
            case "CoordAxisSign":
                this.xAxis = ((Integer) e2.properties.get(4)).intValue();
                break;
            }
        }
    }

    private void loadObjects(FbxElement element) throws IOException {
        FbxObject obj = null;
        for(FbxElement e : element.children) {
            switch(e.id) {
            case "Geometry":
                FbxMesh mesh = new FbxMesh(this, e);
                obj = mesh;
                if(mesh.geometries != null)
                    geomMap.put(mesh.id, mesh);
                break;
            case "Material":
                obj = new FbxMaterial(this, e);
                break;
            case "Model":
                FbxNode node = new FbxNode(this, e);
                obj = node;
                modelMap.put(node.id, node);
                if(node.isLimb())
                    limbMap.put(node.id, node);
                break;
            case "Pose":
                FbxBindPose pose = new FbxBindPose(this, e);
                obj = pose;
                bindMap.put(pose.id, pose);
                break;
            case "Texture":
                obj = new FbxTexture(this, e);
                break;
            case "Video":
                obj = new FbxImage(this, e);
                break;
            case "Deformer":
                obj = loadDeformer(e);
                break;
            case "AnimationLayer":
                FbxObject layer = new FbxObject(this, e);
                obj = layer;
                aLayerMap.put(layer.id, layer);
                break;
            case "AnimationCurve":
                obj = new FbxAnimCurve(this, e);
                break;
            case "AnimationCurveNode":
                obj = new FbxAnimNode(this, e);
                break;
            default:
                obj = null;
                //warnings.add("Object with id '" + e.id + "' was ignored");
            }
            if(obj != null)
                allObjects.put(obj.id, obj);
        }
    }

    private FbxObject loadDeformer(FbxElement element) {
        String type = (String) element.properties.get(2);
        switch(type) {
        case "Skin":
            FbxSkin skinData = new FbxSkin(this, element);
            skinMap.put(skinData.id, skinData);
            return skinData;
        case "Cluster":
            FbxCluster clusterData = new FbxCluster(this, element);
            return clusterData;
        }
        return null;
    }

    private void loadConnections(FbxElement element) {
        for(FbxElement e : element.children) {
            if(e.id.equals("C")) {
                String type = (String) e.properties.get(0);
                long objId, refId;
                FbxObject obj, ref;
                switch(type) {
                case "OO":
                    objId = (Long) e.properties.get(1);
                    refId = (Long) e.properties.get(2);
                    obj = allObjects.get(objId);
                    ref = allObjects.get(refId);
                    if(ref != null) {
                        ref.link(obj);
                    } else if(refId == 0) {
                        obj.linkToZero();
                    }
                    break;
                case "OP":
                    objId = (Long) e.properties.get(1);
                    refId = (Long) e.properties.get(2);
                    String propName = (String) e.properties.get(3);
                    obj = allObjects.get(objId);
                    ref = allObjects.get(refId);
                    if(ref != null)
                        ref.link(obj, propName);
                    break;
                }
            }
        }
    }

    private void linkScene() {
        logger.log(Level.FINE, "Linking scene objects");
        long startTime = System.currentTimeMillis();
        applySkinning();
        buildAnimations();
        for(FbxMesh mesh : geomMap.values())
            mesh.clearMaterials();
        // Remove bones from node structures : JME creates attach node by itself
        for(FbxNode limb : limbMap.values())
            limb.node.removeFromParent();
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.log(Level.FINE, "Linking done in {0} ms", estimatedTime);
    }

    private void applySkinning() {
        for(FbxBindPose pose : bindMap.values())
            pose.fillBindTransforms();
        if(limbMap == null)
            return;
        List<Bone> bones = new ArrayList<>();
        for(FbxNode limb : limbMap.values()) {
            if(limb.bone != null) {
                bones.add(limb.bone);
                limb.buildBindPoseBoneTransform();
            }
        }
        skeleton = new Skeleton(bones.toArray(new Bone[bones.size()]));
        skeleton.setBindingPose();
        for(FbxNode limb : limbMap.values())
            limb.setSkeleton(skeleton);
        for(FbxSkin skin : skinMap.values())
            skin.generateSkinning();
        // Attach controls
        animControl = new AnimControl(skeleton);
        sceneNode.addControl(animControl);
        SkeletonControl control = new SkeletonControl(skeleton);
        sceneNode.addControl(control);
    }

    private void buildAnimations() {
        if(skeleton == null)
            return;
        if(animList == null || animList.list.size() == 0) {
            animList = new AnimationList();
            for(long layerId : aLayerMap.keySet()) {
                FbxObject layer = aLayerMap.get(layerId);
                animList.add(layer.name, layer.name, 0, -1);
            }
        }
        // Extract animations
        HashMap<String, Animation> anims = new HashMap<>();
        for(AnimInverval animInfo : animList.list) {
            float realLength = 0;
            float length = (animInfo.lastFrame - animInfo.firstFrame) / this.animFrameRate;
            float animStart = animInfo.firstFrame / this.animFrameRate;
            float animStop = animInfo.lastFrame / this.animFrameRate;
            Animation anim = new Animation(animInfo.name, length);
            // Search source layer for animation nodes
            long sourceLayerId = 0L;
            for(long layerId : aLayerMap.keySet()) {
                FbxObject layer = aLayerMap.get(layerId);
                if(layer.name.equals(animInfo.layerName)) {
                    sourceLayerId = layerId;
                    break;
                }
            }
            // Build bone tracks
            for(FbxNode limb : limbMap.values()) {
                // Animation channels may have different keyframes (non-baked animation).
                //   So we have to restore intermediate values for all channels cause of JME requires
                //   a bone track as a single channel with collective transformation for each keyframe
                Set<Long> stamps = new TreeSet<>(); // Sorted unique timestamps
                FbxAnimNode animTranslation = limb.animTranslation(sourceLayerId);
                FbxAnimNode animRotation = limb.animRotation(sourceLayerId);
                FbxAnimNode animScale = limb.animScale(sourceLayerId);
                boolean haveTranslation = haveAnyChannel(animTranslation);
                boolean haveRotation = haveAnyChannel(animRotation);
                boolean haveScale = haveAnyChannel(animScale);
                // Collect keyframes stamps
                if(haveTranslation)
                    animTranslation.exportTimes(stamps);
                if(haveRotation)
                    animRotation.exportTimes(stamps);
                if(haveScale)
                    animScale.exportTimes(stamps);
                if(stamps.isEmpty())
                    continue;
                long[] keyTimes = new long[stamps.size()];
                int cnt = 0;
                for(long t : stamps)
                    keyTimes[cnt++] = t;
                // Calculate keys interval by animation time interval
                int firstKeyIndex = 0;
                int lastKeyIndex = keyTimes.length - 1;
                for(int i = 0; i < keyTimes.length; ++i) {
                    float time = (float) (keyTimes[i] * secondsPerUnit); // Translate into seconds
                    if(time <= animStart)
                        firstKeyIndex = i;
                    if(time >= animStop && animStop >= 0) {
                        lastKeyIndex = i;
                        break;
                    }
                }
                int keysCount = lastKeyIndex - firstKeyIndex + 1;
                if(keysCount <= 0)
                    continue;
                float[] times = new float[keysCount];
                Vector3f[] translations = new Vector3f[keysCount];
                Quaternion[] rotations = new Quaternion[keysCount];
                Vector3f[] scales = null;
                // Calculate keyframes times
                for(int i = 0; i < keysCount; ++i) {
                    int keyIndex = firstKeyIndex + i;
                    float time = (float) (keyTimes[keyIndex] * secondsPerUnit); // Translate into seconds
                    times[i] = time - animStart;
                    realLength = Math.max(realLength, times[i]);
                }
                // Load keyframes from animation curves
                if(haveTranslation) {
                    for(int i = 0; i < keysCount; ++i) {
                        int keyIndex = firstKeyIndex + i;
                        FbxAnimNode n = animTranslation;
                        Vector3f tvec = n.getValue(keyTimes[keyIndex], n.value).subtractLocal(n.value); // Why do it here but not in other places? FBX magic?
                        translations[i] = tvec.divideLocal(unitSize);
                    }
                } else {
                    for(int i = 0; i < keysCount; ++i)
                        translations[i] = Vector3f.ZERO;
                }
                RotationOrder ro = RotationOrder.EULER_XYZ;
                if(haveRotation) {
                    for(int i = 0; i < keysCount; ++i) {
                        int keyIndex = firstKeyIndex + i;
                        FbxAnimNode n = animRotation;
                        Vector3f tvec = n.getValue(keyTimes[keyIndex], n.value);
                        rotations[i] = ro.rotate(tvec);
                    }
                } else {
                    for(int i = 0; i < keysCount; ++i)
                        rotations[i] = Quaternion.IDENTITY;
                }
                if(haveScale) {
                    scales = new Vector3f[keysCount];
                    for(int i = 0; i < keysCount; ++i) {
                        int keyIndex = firstKeyIndex + i;
                        FbxAnimNode n = animScale;
                        Vector3f tvec = n.getValue(keyTimes[keyIndex], n.value);
                        scales[i] = tvec;
                    }
                }
                BoneTrack track = null;
                if(haveScale)
                    track = new BoneTrack(limb.boneIndex, times, translations, rotations, scales);
                else
                    track = new BoneTrack(limb.boneIndex, times, translations, rotations);
                anim.addTrack(track);
            }
            if(realLength != length && animInfo.lastFrame == -1) {
                Track[] tracks = anim.getTracks();
                if(tracks == null || tracks.length == 0)
                    continue;
                anim = new Animation(animInfo.name, realLength);
                for(Track track : tracks)
                    anim.addTrack(track);
            }
            anims.put(anim.getName(), anim);
        }
        animControl.setAnimations(anims);
    }

    private void releaseObjects() {
        // Reset settings
        unitSize = 1;
        animFrameRate = 30;
        xAxis = 1;
        yAxis = 1;
        zAxis = 1;
        // Clear cache
        warnings.clear();
        animList = null;
        sceneName = null;
        sceneFilename = null;
        sceneFolderName = null;
        assetManager = null;
        currentAssetInfo = null;
        // Clear objects
        allObjects.clear();
        skinMap.clear();
        aLayerMap.clear();
        modelMap.clear();
        limbMap.clear();
        bindMap.clear();
        geomMap.clear();
        skeleton = null;
        animControl = null;
        sceneNode = null;
    }


    private static boolean haveAnyChannel(FbxAnimNode anims) {
        return anims != null && anims.haveAnyChannel();
    }

    private static String join(List<String> list, String glue) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < list.size(); ++i) {
            if(sb.length() != 0)
                sb.append(glue);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
