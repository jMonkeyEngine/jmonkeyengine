package com.jme3.scene.plugins.blender.constraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.ArmatureHelper;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.util.TempVars;

/**
 * A node that represents either spatial or bone in constraint simulation. The
 * node is applied its translation, rotation and scale for each frame of its
 * animation. Then the constraints are applied that will eventually alter it.
 * After that the feature's transformation is stored in VirtualTrack which is
 * converted to new bone or spatial track at the very end.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class SimulationNode {
    private static final Logger       LOGGER       = Logger.getLogger(SimulationNode.class.getName());

    /** The name of the node (for debugging purposes). */
    private String                    name;
    /** A list of children for the node (either bones or child spatials). */
    private List<SimulationNode>      children     = new ArrayList<SimulationNode>();
    /** A list of constraints that the current node has. */
    private List<Constraint>          constraints;
    /** A list of node's animations. */
    private List<Animation>           animations;

    /** The nodes spatial (if null then the boneContext should be set). */
    private Spatial                   spatial;
    /** The skeleton of the bone (not null if the node simulated the bone). */
    private Skeleton                  skeleton;
    /** Animation controller for the node's feature. */
    private AnimControl               animControl;

    /**
     * The star transform of a spatial. Needed to properly reset the spatial to
     * its start position.
     */
    private Transform                 spatialStartTransform;
    /** Star transformations for bones. Needed to properly reset the bones. */
    private Map<Bone, Transform>      boneStartTransforms;

    /**
     * Builds the nodes tree for the given feature. The feature (bone or
     * spatial) is found by its OMA. The feature must be a root bone or a root
     * spatial.
     * 
     * @param featureOMA
     *            the OMA of either bone or spatial
     * @param blenderContext
     *            the blender context
     */
    public SimulationNode(Long featureOMA, BlenderContext blenderContext) {
        this(featureOMA, blenderContext, true);
    }

    /**
     * Creates the node for the feature.
     * 
     * @param featureOMA
     *            the OMA of either bone or spatial
     * @param blenderContext
     *            the blender context
     * @param rootNode
     *            indicates if the feature is a root bone or root spatial or not
     */
    private SimulationNode(Long featureOMA, BlenderContext blenderContext, boolean rootNode) {
        Node spatial = (Node) blenderContext.getLoadedFeature(featureOMA, LoadedFeatureDataType.LOADED_FEATURE);
        if (spatial.getUserData(ArmatureHelper.ARMATURE_NODE_MARKER) != null) {
            this.skeleton = blenderContext.getSkeleton(featureOMA);

            Node nodeWithAnimationControl = blenderContext.getControlledNode(skeleton);
            this.animControl = nodeWithAnimationControl.getControl(AnimControl.class);

            boneStartTransforms = new HashMap<Bone, Transform>();
            for (int i = 0; i < skeleton.getBoneCount(); ++i) {
                Bone bone = skeleton.getBone(i);
                boneStartTransforms.put(bone, new Transform(bone.getWorldBindPosition(), bone.getWorldBindRotation(), bone.getWorldBindScale()));
            }
        } else {
            if (rootNode && spatial.getParent() != null) {
                throw new IllegalStateException("Given spatial must be a root node!");
            }
            this.spatial = spatial;
            this.spatialStartTransform = spatial.getLocalTransform().clone();
        }

        this.name = '>' + spatial.getName() + '<';

        constraints = this.findConstraints(featureOMA, blenderContext);
        if (constraints == null) {
            constraints = new ArrayList<Constraint>();
        }

        // add children nodes
        if (skeleton != null) {
            // bone with index 0 is a root bone and should not be considered
            // here
            for (int i = 1; i < skeleton.getBoneCount(); ++i) {
                BoneContext boneContext = blenderContext.getBoneContext(skeleton.getBone(i));
                List<Constraint> boneConstraints = this.findConstraints(boneContext.getBoneOma(), blenderContext);
                if (boneConstraints != null) {
                    constraints.addAll(boneConstraints);
                }
            }

            // each bone of the skeleton has the same anim data applied
            BoneContext boneContext = blenderContext.getBoneContext(skeleton.getBone(1));
            Long boneOma = boneContext.getBoneOma();
            animations = blenderContext.getAnimData(boneOma) == null ? null : blenderContext.getAnimData(boneOma).anims;
        } else {
            animations = blenderContext.getAnimData(featureOMA) == null ? null : blenderContext.getAnimData(featureOMA).anims;
            for (Spatial child : spatial.getChildren()) {
                if (child instanceof Node) {
                    children.add(new SimulationNode((Long) child.getUserData("oma"), blenderContext, false));
                }
            }
        }

        LOGGER.info("Removing invalid constraints.");
        List<Constraint> validConstraints = new ArrayList<Constraint>(constraints.size());
        for (Constraint constraint : this.constraints) {
            if (constraint.validate()) {
                validConstraints.add(constraint);
            } else {
                LOGGER.log(Level.WARNING, "Constraint {0} is invalid and will not be applied.", constraint.name);
            }
        }
        this.constraints = validConstraints;
    }

    /**
     * Tells if the node already contains the given constraint (so that it is
     * not applied twice).
     * 
     * @param constraint
     *            the constraint to be checked
     * @return <b>true</b> if the constraint already is stored in the node and
     *         <b>false</b> otherwise
     */
    public boolean contains(Constraint constraint) {
        boolean result = false;
        if (constraints != null && constraints.size() > 0) {
            for (Constraint c : constraints) {
                if (c.equals(constraint)) {
                    return true;
                }
            }
        }
        return result;
    }

    /**
     * Resets the node's feature to its starting transformation.
     */
    private void reset() {
        if (spatial != null) {
            spatial.setLocalTransform(spatialStartTransform);
            for (SimulationNode child : children) {
                child.reset();
            }
        } else if (skeleton != null) {
            for (Entry<Bone, Transform> entry : boneStartTransforms.entrySet()) {
                Transform t = entry.getValue();
                entry.getKey().setBindTransforms(t.getTranslation(), t.getRotation(), t.getScale());
            }
            skeleton.reset();
        }
    }

    /**
     * Simulates the spatial node.
     */
    private void simulateSpatial() {
        if (constraints != null && constraints.size() > 0) {
            boolean applyStaticConstraints = true;
            if (animations != null) {
                for (Animation animation : animations) {
                    float[] animationTimeBoundaries = computeAnimationTimeBoundaries(animation);
                    int maxFrame = (int) animationTimeBoundaries[0];
                    float maxTime = animationTimeBoundaries[1];

                    VirtualTrack vTrack = new VirtualTrack(maxFrame, maxTime);
                    for (Track track : animation.getTracks()) {
                        for (int frame = 0; frame < maxFrame; ++frame) {
                            spatial.setLocalTranslation(((SpatialTrack) track).getTranslations()[frame]);
                            spatial.setLocalRotation(((SpatialTrack) track).getRotations()[frame]);
                            spatial.setLocalScale(((SpatialTrack) track).getScales()[frame]);

                            for (Constraint constraint : constraints) {
                                constraint.apply(frame);
                                vTrack.setTransform(frame, spatial.getLocalTransform());
                            }
                        }
                        Track newTrack = vTrack.getAsSpatialTrack();
                        if (newTrack != null) {
                            animation.removeTrack(track);
                            animation.addTrack(newTrack);
                        }
                        applyStaticConstraints = false;
                    }
                }
            }

            // if there are no animations then just constraint the static
            // object's transformation
            if (applyStaticConstraints) {
                for (Constraint constraint : constraints) {
                    constraint.apply(0);
                }
            }
        }

        for (SimulationNode child : children) {
            child.simulate();
        }
    }

    /**
     * Simulates the bone node.
     */
    private void simulateSkeleton() {
        if (constraints != null && constraints.size() > 0) {
            boolean applyStaticConstraints = true;

            if (animations != null) {
                TempVars vars = TempVars.get();
                AnimChannel animChannel = animControl.createChannel();
                for (Animation animation : animations) {
                    float[] animationTimeBoundaries = this.computeAnimationTimeBoundaries(animation);
                    int maxFrame = (int) animationTimeBoundaries[0];
                    float maxTime = animationTimeBoundaries[1];

                    Map<Integer, VirtualTrack> tracks = new HashMap<Integer, VirtualTrack>();
                    Map<Integer, Transform> previousTransforms = new HashMap<Integer, Transform>();
                    for (int frame = 0; frame < maxFrame; ++frame) {
                        // this MUST be done here, otherwise setting next frame of animation will
                        // lead to possible errors
                        this.reset();
                        
                        // first set proper time for all bones in all the tracks ...
                        for (Track track : animation.getTracks()) {
                            float time = ((BoneTrack) track).getTimes()[frame];
                            Integer boneIndex = ((BoneTrack) track).getTargetBoneIndex();

                            track.setTime(time, 1, animControl, animChannel, vars);
                            skeleton.updateWorldVectors();

                            Transform previousTransform = previousTransforms.get(boneIndex);
                            if (previousTransform == null) {
                                Bone bone = skeleton.getBone(boneIndex);
                                previousTransform = new Transform();
                                previousTransform.setTranslation(bone.getLocalPosition());
                                previousTransform.setRotation(bone.getLocalRotation());
                                previousTransform.setScale(bone.getLocalScale());
                                previousTransforms.put(boneIndex, previousTransform);
                            }
                        }

                        // ... and then apply constraints ...
                        for (Constraint constraint : constraints) {
                            constraint.apply(frame);
                        }

                        // ... and fill in another frame in the result track
                        for (Track track : animation.getTracks()) {
                            Integer boneIndex = ((BoneTrack) track).getTargetBoneIndex();
                            Bone bone = skeleton.getBone(boneIndex);

                            // take the initial transform of a bone
                            Transform previousTransform = previousTransforms.get(boneIndex);

                            VirtualTrack vTrack = tracks.get(boneIndex);
                            if (vTrack == null) {
                                vTrack = new VirtualTrack(maxFrame, maxTime);
                                tracks.put(boneIndex, vTrack);
                            }

                            Vector3f bonePositionDifference = bone.getLocalPosition().subtract(previousTransform.getTranslation());
                            Quaternion boneRotationDifference = bone.getLocalRotation().mult(previousTransform.getRotation().inverse()).normalizeLocal();
                            Vector3f boneScaleDifference = bone.getLocalScale().divide(previousTransform.getScale());
                            if (frame > 0) {
                                bonePositionDifference = vTrack.translations.get(frame - 1).add(bonePositionDifference);
                                boneRotationDifference = vTrack.rotations.get(frame - 1).mult(boneRotationDifference);
                                boneScaleDifference = vTrack.scales.get(frame - 1).mult(boneScaleDifference);
                            }
                            vTrack.setTransform(frame, new Transform(bonePositionDifference, boneRotationDifference, boneScaleDifference));

                            previousTransform.setTranslation(bone.getLocalPosition());
                            previousTransform.setRotation(bone.getLocalRotation());
                            previousTransform.setScale(bone.getLocalScale());
                        }
                    }

                    for (Entry<Integer, VirtualTrack> trackEntry : tracks.entrySet()) {
                        Track newTrack = trackEntry.getValue().getAsBoneTrack(trackEntry.getKey());
                        if (newTrack != null) {
                            for (Track track : animation.getTracks()) {
                                if (((BoneTrack) track).getTargetBoneIndex() == trackEntry.getKey().intValue()) {
                                    animation.removeTrack(track);
                                    animation.addTrack(newTrack);
                                    break;
                                }
                            }
                        }
                        applyStaticConstraints = false;
                    }
                }
                vars.release();
                animControl.clearChannels();
                this.reset();
            }

            // if there are no animations then just constraint the static
            // object's transformation
            if (applyStaticConstraints) {
                for (Constraint constraint : constraints) {
                    constraint.apply(0);
                }
            }
        }
    }

    /**
     * Simulates the node.
     */
    public void simulate() {
        this.reset();
        if (spatial != null) {
            this.simulateSpatial();
        } else {
            this.simulateSkeleton();
        }
        this.reset();
    }

    /**
     * Computes the maximum frame and time for the animation. Different tracks
     * can have different lengths so here the maximum one is being found.
     * 
     * @param animation
     *            the animation
     * @return maximum frame and time of the animation
     */
    private float[] computeAnimationTimeBoundaries(Animation animation) {
        int maxFrame = Integer.MIN_VALUE;
        float maxTime = Float.MIN_VALUE;
        for (Track track : animation.getTracks()) {
            if (track instanceof BoneTrack) {
                maxFrame = Math.max(maxFrame, ((BoneTrack) track).getTranslations().length);
                maxTime = Math.max(maxTime, ((BoneTrack) track).getTimes()[((BoneTrack) track).getTimes().length - 1]);
            } else if (track instanceof SpatialTrack) {
                maxFrame = Math.max(maxFrame, ((SpatialTrack) track).getTranslations().length);
                maxTime = Math.max(maxTime, ((SpatialTrack) track).getTimes()[((SpatialTrack) track).getTimes().length - 1]);
            } else {
                throw new IllegalStateException("Unsupported track type for simuation: " + track);
            }
        }
        return new float[] { maxFrame, maxTime };
    }

    /**
     * Finds constraints for the node's features.
     * 
     * @param ownerOMA
     *            the feature's OMA
     * @param blenderContext
     *            the blender context
     * @return a list of feature's constraints or empty list if none were found
     */
    private List<Constraint> findConstraints(Long ownerOMA, BlenderContext blenderContext) {
        List<Constraint> result = new ArrayList<Constraint>();
        for (Constraint constraint : blenderContext.getAllConstraints()) {
            if (constraint.ownerOMA.longValue() == ownerOMA.longValue()) {
                if (constraint.isImplemented()) {
                    result.add(constraint);
                } else {
                    LOGGER.log(Level.WARNING, "Constraint named: ''{0}'' of type ''{1}'' is not implemented and will NOT be applied!", new Object[] { constraint.name, constraint.getConstraintTypeName() });
                }
            }
        }
        return result.size() > 0 ? result : null;
    }

    @Override
    public String toString() {
        return name;
    }
}