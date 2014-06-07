package com.jme3.scene.plugins.blender.animations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.SpatialTrack;
import com.jme3.asset.BlenderKey.AnimationMatchMethod;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo.ConstIpo;
import com.jme3.scene.plugins.blender.curves.BezierCurve;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * The helper class that helps in animations loading.
 * @author Marcin Roguski (Kaelthas)
 */
public class AnimationHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER = Logger.getLogger(AnimationHelper.class.getName());

    public AnimationHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * Loads all animations that are stored in the blender file. The animations are not yet applied to the scene features.
     * This should be called before objects are loaded.
     * @throws BlenderFileException
     *             an exception is thrown when problems with blender file reading occur
     */
    public void loadAnimations() throws BlenderFileException {
        LOGGER.info("Loading animations that will be later applied to scene features.");
        List<FileBlockHeader> actionHeaders = blenderContext.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
        if (actionHeaders != null) {
            for (FileBlockHeader header : actionHeaders) {
                Structure actionStructure = header.getStructure(blenderContext);
                LOGGER.log(Level.INFO, "Found animation: {0}.", actionStructure.getName());
                blenderContext.addAction(this.getTracks(actionStructure, blenderContext));
            }
        }
    }

    /**
     * The method applies animations to the given node. The names of the animations should be the same as actions names in the blender file.
     * @param node
     *            the node to whom the animations will be applied
     * @param animationMatchMethod
     *            the way animation should be matched with node
     */
    public void applyAnimations(Node node, AnimationMatchMethod animationMatchMethod) {
        List<BlenderAction> actions = this.getActions(node, animationMatchMethod);
        if (actions.size() > 0) {
            List<Animation> animations = new ArrayList<Animation>();
            for (BlenderAction action : actions) {
                SpatialTrack[] tracks = action.toTracks(node);
                if (tracks != null && tracks.length > 0) {
                    Animation spatialAnimation = new Animation(action.getName(), action.getAnimationTime());
                    spatialAnimation.setTracks(tracks);
                    animations.add(spatialAnimation);
                    blenderContext.addAnimation((Long) node.getUserData(ObjectHelper.OMA_MARKER), spatialAnimation);
                }
            }

            if (animations.size() > 0) {
                AnimControl control = new AnimControl();
                HashMap<String, Animation> anims = new HashMap<String, Animation>(animations.size());
                for (int i = 0; i < animations.size(); ++i) {
                    Animation animation = animations.get(i);
                    anims.put(animation.getName(), animation);
                }
                control.setAnimations(anims);
                node.addControl(control);
            }
        }
    }

    /**
     * The method applies skeleton animations to the given node.
     * @param node
     *            the node where the animations will be applied
     * @param skeleton
     *            the skeleton of the node
     * @param animationMatchMethod
     *            the way animation should be matched with skeleton
     */
    public void applyAnimations(Node node, Skeleton skeleton, AnimationMatchMethod animationMatchMethod) {
        node.addControl(new SkeletonControl(skeleton));
        blenderContext.setNodeForSkeleton(skeleton, node);
        List<BlenderAction> actions = this.getActions(skeleton, animationMatchMethod);

        if (actions.size() > 0) {
            List<Animation> animations = new ArrayList<Animation>();
            for (BlenderAction action : actions) {
                BoneTrack[] tracks = action.toTracks(skeleton);
                if (tracks != null && tracks.length > 0) {
                    Animation boneAnimation = new Animation(action.getName(), action.getAnimationTime());
                    boneAnimation.setTracks(tracks);
                    animations.add(boneAnimation);
                    Long animatedNodeOMA = ((Number) blenderContext.getMarkerValue(ObjectHelper.OMA_MARKER, node)).longValue();
                    blenderContext.addAnimation(animatedNodeOMA, boneAnimation);
                }
            }
            if (animations.size() > 0) {
                AnimControl control = new AnimControl(skeleton);
                HashMap<String, Animation> anims = new HashMap<String, Animation>(animations.size());
                for (int i = 0; i < animations.size(); ++i) {
                    Animation animation = animations.get(i);
                    anims.put(animation.getName(), animation);
                }
                control.setAnimations(anims);
                node.addControl(control);

                // make sure that SkeletonControl is added AFTER the AnimControl
                SkeletonControl skeletonControl = node.getControl(SkeletonControl.class);
                if (skeletonControl != null) {
                    node.removeControl(SkeletonControl.class);
                    node.addControl(skeletonControl);
                }
            }
        }
    }

    /**
     * This method creates an ipo object used for interpolation calculations.
     * 
     * @param ipoStructure
     *            the structure with ipo definition
     * @param blenderContext
     *            the blender context
     * @return the ipo object
     * @throws BlenderFileException
     *             this exception is thrown when the blender file is somehow
     *             corrupted
     */
    public Ipo fromIpoStructure(Structure ipoStructure, BlenderContext blenderContext) throws BlenderFileException {
        Structure curvebase = (Structure) ipoStructure.getFieldValue("curve");

        // preparing bezier curves
        Ipo result = null;
        List<Structure> curves = curvebase.evaluateListBase();// IpoCurve
        if (curves.size() > 0) {
            BezierCurve[] bezierCurves = new BezierCurve[curves.size()];
            int frame = 0;
            for (Structure curve : curves) {
                Pointer pBezTriple = (Pointer) curve.getFieldValue("bezt");
                List<Structure> bezTriples = pBezTriple.fetchData();
                int type = ((Number) curve.getFieldValue("adrcode")).intValue();
                bezierCurves[frame++] = new BezierCurve(type, bezTriples, 2);
            }
            curves.clear();
            result = new Ipo(bezierCurves, fixUpAxis, blenderContext.getBlenderVersion());
            blenderContext.addLoadedFeatures(ipoStructure.getOldMemoryAddress(), ipoStructure.getName(), ipoStructure, result);
        }
        return result;
    }

    /**
     * This method creates an ipo with only a single value. No track type is
     * specified so do not use it for calculating tracks.
     * 
     * @param constValue
     *            the value of this ipo
     * @return constant ipo
     */
    public Ipo fromValue(float constValue) {
        return new ConstIpo(constValue);
    }

    /**
     * This method retuns the bone tracks for animation.
     * 
     * @param actionStructure
     *            the structure containing the tracks
     * @param blenderContext
     *            the blender context
     * @return a list of tracks for the specified animation
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the blend
     *             file
     */
    private BlenderAction getTracks(Structure actionStructure, BlenderContext blenderContext) throws BlenderFileException {
        if (blenderVersion < 250) {
            return this.getTracks249(actionStructure, blenderContext);
        } else {
            return this.getTracks250(actionStructure, blenderContext);
        }
    }

    /**
     * This method retuns the bone tracks for animation for blender version 2.50
     * and higher.
     * 
     * @param actionStructure
     *            the structure containing the tracks
     * @param blenderContext
     *            the blender context
     * @return a list of tracks for the specified animation
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the blend
     *             file
     */
    private BlenderAction getTracks250(Structure actionStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Getting tracks!");
        Structure groups = (Structure) actionStructure.getFieldValue("groups");
        List<Structure> actionGroups = groups.evaluateListBase();// bActionGroup
        BlenderAction blenderAction = new BlenderAction(actionStructure.getName(), blenderContext.getBlenderKey().getFps());
        int lastFrame = 1;
        for (Structure actionGroup : actionGroups) {
            String name = actionGroup.getFieldValue("name").toString();
            List<Structure> channels = ((Structure) actionGroup.getFieldValue("channels")).evaluateListBase();
            BezierCurve[] bezierCurves = new BezierCurve[channels.size()];
            int channelCounter = 0;
            for (Structure c : channels) {
                int type = this.getCurveType(c, blenderContext);
                Pointer pBezTriple = (Pointer) c.getFieldValue("bezt");
                List<Structure> bezTriples = pBezTriple.fetchData();
                bezierCurves[channelCounter++] = new BezierCurve(type, bezTriples, 2);
            }

            Ipo ipo = new Ipo(bezierCurves, fixUpAxis, blenderContext.getBlenderVersion());
            lastFrame = Math.max(lastFrame, ipo.getLastFrame());
            blenderAction.featuresTracks.put(name, ipo);
        }
        blenderAction.stopFrame = lastFrame;
        return blenderAction;
    }

    /**
     * This method retuns the bone tracks for animation for blender version 2.49
     * (and probably several lower versions too).
     * 
     * @param actionStructure
     *            the structure containing the tracks
     * @param blenderContext
     *            the blender context
     * @return a list of tracks for the specified animation
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the blend
     *             file
     */
    private BlenderAction getTracks249(Structure actionStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Getting tracks!");
        Structure chanbase = (Structure) actionStructure.getFieldValue("chanbase");
        List<Structure> actionChannels = chanbase.evaluateListBase();// bActionChannel
        BlenderAction blenderAction = new BlenderAction(actionStructure.getName(), blenderContext.getBlenderKey().getFps());
        int lastFrame = 1;
        for (Structure bActionChannel : actionChannels) {
            String animatedFeatureName = bActionChannel.getFieldValue("name").toString();
            Pointer p = (Pointer) bActionChannel.getFieldValue("ipo");
            if (!p.isNull()) {
                Structure ipoStructure = p.fetchData().get(0);
                Ipo ipo = this.fromIpoStructure(ipoStructure, blenderContext);
                if (ipo != null) {// this can happen when ipo with no curves appear in blender file
                    lastFrame = Math.max(lastFrame, ipo.getLastFrame());
                    blenderAction.featuresTracks.put(animatedFeatureName, ipo);
                }
            }
        }
        blenderAction.stopFrame = lastFrame;
        return blenderAction;
    }

    /**
     * This method returns the type of the ipo curve.
     * 
     * @param structure
     *            the structure must contain the 'rna_path' field and
     *            'array_index' field (the type is not important here)
     * @param blenderContext
     *            the blender context
     * @return the type of the curve
     */
    public int getCurveType(Structure structure, BlenderContext blenderContext) {
        // reading rna path first
        BlenderInputStream bis = blenderContext.getInputStream();
        int currentPosition = bis.getPosition();
        Pointer pRnaPath = (Pointer) structure.getFieldValue("rna_path");
        FileBlockHeader dataFileBlock = blenderContext.getFileBlock(pRnaPath.getOldMemoryAddress());
        bis.setPosition(dataFileBlock.getBlockPosition());
        String rnaPath = bis.readString();
        bis.setPosition(currentPosition);
        int arrayIndex = ((Number) structure.getFieldValue("array_index")).intValue();

        // determining the curve type
        if (rnaPath.endsWith("location")) {
            return Ipo.AC_LOC_X + arrayIndex;
        }
        if (rnaPath.endsWith("rotation_quaternion")) {
            return Ipo.AC_QUAT_W + arrayIndex;
        }
        if (rnaPath.endsWith("scale")) {
            return Ipo.AC_SIZE_X + arrayIndex;
        }
        if (rnaPath.endsWith("rotation") || rnaPath.endsWith("rotation_euler")) {
            return Ipo.OB_ROT_X + arrayIndex;
        }
        LOGGER.log(Level.WARNING, "Unknown curve rna path: {0}", rnaPath);
        return -1;
    }

    /**
     * The method returns the actions for the given skeleton. The actions represent armature animation in blender.
     * @param skeleton
     *            the skeleton we fetch the actions for
     * @param animationMatchMethod
     *            the method of animation matching
     * @return a list of animations for the specified skeleton
     */
    private List<BlenderAction> getActions(Skeleton skeleton, AnimationMatchMethod animationMatchMethod) {
        List<BlenderAction> result = new ArrayList<BlenderAction>();

        // first get a set of bone names
        Set<String> boneNames = new HashSet<String>();
        for (int i = 0; i < skeleton.getBoneCount(); ++i) {
            String boneName = skeleton.getBone(i).getName();
            if (boneName != null && boneName.length() > 0) {
                boneNames.add(skeleton.getBone(i).getName());
            }
        }

        // finding matches
        Set<String> matchingNames = new HashSet<String>();
        for (Entry<String, BlenderAction> actionEntry : blenderContext.getActions().entrySet()) {
            // compute how many action tracks match the skeleton bones' names
            for (String boneName : boneNames) {
                if (actionEntry.getValue().hasTrackName(boneName)) {
                    matchingNames.add(boneName);
                }
            }

            BlenderAction action = null;
            if (animationMatchMethod == AnimationMatchMethod.AT_LEAST_ONE_NAME_MATCH && matchingNames.size() > 0) {
                action = actionEntry.getValue();
            } else if (matchingNames.size() == actionEntry.getValue().getTracksCount()) {
                action = actionEntry.getValue();
            }

            if (action != null) {
                // remove the tracks that do not match the bone names if the matching method is different from ALL_NAMES_MATCH
                if (animationMatchMethod != AnimationMatchMethod.ALL_NAMES_MATCH) {
                    action = action.clone();
                    action.removeTracksThatAreNotInTheCollection(matchingNames);
                }
                result.add(action);
            }

            matchingNames.clear();
        }
        return result;
    }

    /**
     * The method returns the actions for the given node. The actions represent object animation in blender.
     * @param node
     *            the node we fetch the actions for
     * @param animationMatchMethod
     *            the method of animation matching
     * @return a list of animations for the specified node
     */
    private List<BlenderAction> getActions(Node node, AnimationMatchMethod animationMatchMethod) {
        List<BlenderAction> result = new ArrayList<BlenderAction>();

        for (Entry<String, BlenderAction> actionEntry : blenderContext.getActions().entrySet()) {
            if (actionEntry.getValue().hasTrackName(node.getName())) {
                result.add(actionEntry.getValue());
            }
        }
        return result;
    }
}
