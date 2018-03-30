package com.jme3.scene.plugins.blender.constraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
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
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
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
    private static final Logger LOGGER = Logger.getLogger(SimulationNode.class.getName());

    private Long                 featureOMA;
    /** The blender context. */
    private BlenderContext       blenderContext;
    /** The name of the node (for debugging purposes). */
    private String               name;
    /** A list of children for the node (either bones or child spatials). */
    private List<SimulationNode> children = new ArrayList<SimulationNode>();
    /** A list of node's animations. */
    private List<Animation>      animations;

    /** The nodes spatial (if null then the boneContext should be set). */
    private Spatial     spatial;
    /** The skeleton of the bone (not null if the node simulated the bone). */
    private Skeleton    skeleton;
    /** Animation controller for the node's feature. */
    private AnimControl animControl;

    /**
     * The star transform of a spatial. Needed to properly reset the spatial to
     * its start position.
     */
    private Transform            spatialStartTransform;
    /** Star transformations for bones. Needed to properly reset the bones. */
    private Map<Bone, Transform> boneStartTransforms;

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
        this.featureOMA = featureOMA;
        this.blenderContext = blenderContext;
        Node spatial = (Node) blenderContext.getLoadedFeature(featureOMA, LoadedDataType.FEATURE);
        if (blenderContext.getMarkerValue(ObjectHelper.ARMATURE_NODE_MARKER, spatial) != null) {
            skeleton = blenderContext.getSkeleton(featureOMA);

            Node nodeWithAnimationControl = blenderContext.getControlledNode(skeleton);
            animControl = nodeWithAnimationControl.getControl(AnimControl.class);

            boneStartTransforms = new HashMap<Bone, Transform>();
            for (int i = 0; i < skeleton.getBoneCount(); ++i) {
                Bone bone = skeleton.getBone(i);
                boneStartTransforms.put(bone, new Transform(bone.getBindPosition(), bone.getBindRotation(), bone.getBindScale()));
            }
        } else {
            if (rootNode && spatial.getParent() != null) {
                throw new IllegalStateException("Given spatial must be a root node!");
            }
            this.spatial = spatial;
            spatialStartTransform = spatial.getLocalTransform().clone();
        }

        name = '>' + spatial.getName() + '<';

        // add children nodes
        if (skeleton != null) {
            Node node = blenderContext.getControlledNode(skeleton);
            Long animatedNodeOMA = ((Number) blenderContext.getMarkerValue(ObjectHelper.OMA_MARKER, node)).longValue();
            animations = blenderContext.getAnimations(animatedNodeOMA);
        } else {
            animations = blenderContext.getAnimations(featureOMA);
            for (Spatial child : spatial.getChildren()) {
                if (child instanceof Node) {
                    children.add(new SimulationNode((Long) blenderContext.getMarkerValue(ObjectHelper.OMA_MARKER, child), blenderContext, false));
                }
            }
        }
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
                entry.getKey().updateModelTransforms();
            }
            skeleton.reset();
        }
    }

    /**
     * Simulates the spatial node.
     */
    private void simulateSpatial() {
        List<Constraint> constraints = blenderContext.getConstraints(featureOMA);
        if (constraints != null && constraints.size() > 0) {
            LOGGER.fine("Simulating spatial.");
            boolean applyStaticConstraints = true;
            if (animations != null) {
                for (Animation animation : animations) {
                    float[] animationTimeBoundaries = this.computeAnimationTimeBoundaries(animation);
                    int maxFrame = (int) animationTimeBoundaries[0];
                    float maxTime = animationTimeBoundaries[1];

                    VirtualTrack vTrack = new VirtualTrack(spatial.getName(), maxFrame, maxTime);
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
        LOGGER.fine("Simulating skeleton.");
        Set<Long> alteredOmas = new HashSet<Long>();

        if (animations != null) {
            TempVars vars = TempVars.get();
            AnimChannel animChannel = animControl.createChannel();

            for (Animation animation : animations) {
                float[] animationTimeBoundaries = this.computeAnimationTimeBoundaries(animation);
                int maxFrame = (int) animationTimeBoundaries[0];
                float maxTime = animationTimeBoundaries[1];

                Map<Integer, VirtualTrack> tracks = new HashMap<Integer, VirtualTrack>();
                for (int frame = 0; frame < maxFrame; ++frame) {
                    // this MUST be done here, otherwise setting next frame of animation will
                    // lead to possible errors
                    this.reset();

                    // first set proper time for all bones in all the tracks ...
                    for (Track track : animation.getTracks()) {
                        float time = ((BoneTrack) track).getTimes()[frame];
                        track.setTime(time, 1, animControl, animChannel, vars);
                        skeleton.updateWorldVectors();
                    }

                    // ... and then apply constraints from the root bone to the last child ...
                    Set<Long> applied = new HashSet<Long>();
                    for (Bone rootBone : skeleton.getRoots()) {
                        // ignore the 0-indexed bone
                        if (skeleton.getBoneIndex(rootBone) > 0) {
                            this.applyConstraints(rootBone, alteredOmas, applied, frame, new Stack<Bone>());
                        }
                    }

                    // ... add virtual tracks if necessary, for bones that were altered but had no tracks before ...
                    for (Long boneOMA : alteredOmas) {
                        BoneContext boneContext = blenderContext.getBoneContext(boneOMA);
                        int boneIndex = skeleton.getBoneIndex(boneContext.getBone());
                        if (!tracks.containsKey(boneIndex)) {
                            tracks.put(boneIndex, new VirtualTrack(boneContext.getBone().getName(), maxFrame, maxTime));
                        }
                    }
                    alteredOmas.clear();

                    // ... and fill in another frame in the result track
                    for (Entry<Integer, VirtualTrack> trackEntry : tracks.entrySet()) {
                        Bone bone = skeleton.getBone(trackEntry.getKey());
                        Transform startTransform = boneStartTransforms.get(bone);

                        // track contains differences between the frame position and bind positions of bones/spatials
                        Vector3f bonePositionDifference = bone.getLocalPosition().subtract(startTransform.getTranslation());
                        Quaternion boneRotationDifference = startTransform.getRotation().inverse().mult(bone.getLocalRotation()).normalizeLocal();
                        Vector3f boneScaleDifference = bone.getLocalScale().divide(startTransform.getScale());

                        trackEntry.getValue().setTransform(frame, new Transform(bonePositionDifference, boneRotationDifference, boneScaleDifference));
                    }
                }

                for (Entry<Integer, VirtualTrack> trackEntry : tracks.entrySet()) {
                    Track newTrack = trackEntry.getValue().getAsBoneTrack(trackEntry.getKey());
                    if (newTrack != null) {
                        boolean trackReplaced = false;
                        for (Track track : animation.getTracks()) {
                            if (((BoneTrack) track).getTargetBoneIndex() == trackEntry.getKey().intValue()) {
                                animation.removeTrack(track);
                                animation.addTrack(newTrack);
                                trackReplaced = true;
                                break;
                            }
                        }
                        if (!trackReplaced) {
                            animation.addTrack(newTrack);
                        }
                    }
                }
            }
            vars.release();
            animControl.clearChannels();
            this.reset();
        }
    }

    /**
     * Applies constraints to the given bone and its children.
     * The goal is to apply constraint from root bone to the last child.
     * @param bone
     *            the bone whose constraints will be applied
     * @param alteredOmas
     *            the set of OMAS of the altered bones (is populated if necessary)
     * @param frame
     *            the current frame of the animation
     * @param bonesStack
     *            the stack of bones used to avoid infinite loops while applying constraints
     */
    private void applyConstraints(Bone bone, Set<Long> alteredOmas, Set<Long> applied, int frame, Stack<Bone> bonesStack) {
        if (!bonesStack.contains(bone)) {
            bonesStack.push(bone);
            BoneContext boneContext = blenderContext.getBoneContext(bone);
            if (!applied.contains(boneContext.getBoneOma())) {
                List<Constraint> constraints = this.findConstraints(boneContext.getBoneOma(), blenderContext);
                if (constraints != null && constraints.size() > 0) {
                    for (Constraint constraint : constraints) {
                        if (constraint.getTargetOMA() != null && constraint.getTargetOMA() > 0L) {
                            // first apply constraints of the target bone
                            BoneContext targetBone = blenderContext.getBoneContext(constraint.getTargetOMA());
                            this.applyConstraints(targetBone.getBone(), alteredOmas, applied, frame, bonesStack);
                        }
                        constraint.apply(frame);
                        if (constraint.getAlteredOmas() != null) {
                            alteredOmas.addAll(constraint.getAlteredOmas());
                        }
                        alteredOmas.add(boneContext.getBoneOma());
                    }
                }
                applied.add(boneContext.getBoneOma());
            }

            List<Bone> children = bone.getChildren();
            if (children != null && children.size() > 0) {
                for (Bone child : bone.getChildren()) {
                    this.applyConstraints(child, alteredOmas, applied, frame, bonesStack);
                }
            }
            bonesStack.pop();
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
        float maxTime = -Float.MAX_VALUE;
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
        List<Constraint> constraints = blenderContext.getConstraints(ownerOMA);
        if (constraints != null) {
            for (Constraint constraint : constraints) {
                if (constraint.isImplemented() && constraint.validate() && constraint.isTrackToBeChanged()) {
                    result.add(constraint);
                }
                // TODO: add proper warnings to some map or set so that they are not logged on every frame
            }
        }
        return result.size() > 0 ? result : null;
    }

    @Override
    public String toString() {
        return name;
    }
}