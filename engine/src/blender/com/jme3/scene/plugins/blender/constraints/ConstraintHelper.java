package com.jme3.scene.plugins.blender.constraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.animations.IpoHelper;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class should be used for constraint calculations.
 * @author Marcin Roguski (Kaelthas)
 */
public class ConstraintHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER = Logger.getLogger(ConstraintHelper.class.getName());

    /**
     * Helper constructor. It's main task is to generate the affection functions. These functions are common to all
     * ConstraintHelper instances. Unfortunately this constructor might grow large. If it becomes too large - I shall
     * consider refactoring. The constructor parses the given blender version and stores the result. Some
     * functionalities may differ in different blender versions.
     * @param blenderVersion
     *            the version read from the blend file
     * @param fixUpAxis
     *            a variable that indicates if the Y asxis is the UP axis or not
     */
    public ConstraintHelper(String blenderVersion, BlenderContext blenderContext, boolean fixUpAxis) {
        super(blenderVersion, fixUpAxis);
    }

    /**
     * This method reads constraints for for the given structure. The
     * constraints are loaded only once for object/bone.
     * 
     * @param objectStructure
     *            the structure we read constraint's for
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     */
    public void loadConstraints(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.fine("Loading constraints.");
        // reading influence ipos for the constraints
        IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
        Map<String, Map<String, Ipo>> constraintsIpos = new HashMap<String, Map<String, Ipo>>();
        Pointer pActions = (Pointer) objectStructure.getFieldValue("action");
        if (pActions.isNotNull()) {
            List<Structure> actions = pActions.fetchData(blenderContext.getInputStream());
            for (Structure action : actions) {
                Structure chanbase = (Structure) action.getFieldValue("chanbase");
                List<Structure> actionChannels = chanbase.evaluateListBase(blenderContext);
                for (Structure actionChannel : actionChannels) {
                    Map<String, Ipo> ipos = new HashMap<String, Ipo>();
                    Structure constChannels = (Structure) actionChannel.getFieldValue("constraintChannels");
                    List<Structure> constraintChannels = constChannels.evaluateListBase(blenderContext);
                    for (Structure constraintChannel : constraintChannels) {
                        Pointer pIpo = (Pointer) constraintChannel.getFieldValue("ipo");
                        if (pIpo.isNotNull()) {
                            String constraintName = constraintChannel.getFieldValue("name").toString();
                            Ipo ipo = ipoHelper.fromIpoStructure(pIpo.fetchData(blenderContext.getInputStream()).get(0), blenderContext);
                            ipos.put(constraintName, ipo);
                        }
                    }
                    String actionName = actionChannel.getFieldValue("name").toString();
                    constraintsIpos.put(actionName, ipos);
                }
            }
        }

        // loading constraints connected with the object's bones
        Pointer pPose = (Pointer) objectStructure.getFieldValue("pose");
        if (pPose.isNotNull()) {
            List<Structure> poseChannels = ((Structure) pPose.fetchData(blenderContext.getInputStream()).get(0).getFieldValue("chanbase")).evaluateListBase(blenderContext);
            for (Structure poseChannel : poseChannels) {
                List<Constraint> constraintsList = new ArrayList<Constraint>();
                Long boneOMA = Long.valueOf(((Pointer) poseChannel.getFieldValue("bone")).getOldMemoryAddress());

                // the name is read directly from structure because bone might not yet be loaded
                String name = blenderContext.getFileBlock(boneOMA).getStructure(blenderContext).getFieldValue("name").toString();
                List<Structure> constraints = ((Structure) poseChannel.getFieldValue("constraints")).evaluateListBase(blenderContext);
                for (Structure constraint : constraints) {
                    String constraintName = constraint.getFieldValue("name").toString();
                    Map<String, Ipo> ipoMap = constraintsIpos.get(name);
                    Ipo ipo = ipoMap == null ? null : ipoMap.get(constraintName);
                    if (ipo == null) {
                        float enforce = ((Number) constraint.getFieldValue("enforce")).floatValue();
                        ipo = ipoHelper.fromValue(enforce);
                    }
                    constraintsList.add(new BoneConstraint(constraint, boneOMA, ipo, blenderContext));
                }
                blenderContext.addConstraints(boneOMA, constraintsList);
            }
        }

        // loading constraints connected with the object itself
        List<Structure> constraints = ((Structure) objectStructure.getFieldValue("constraints")).evaluateListBase(blenderContext);
        if (constraints != null && constraints.size() > 0) {
            Pointer pData = (Pointer) objectStructure.getFieldValue("data");
            String dataType = pData.isNotNull() ? pData.fetchData(blenderContext.getInputStream()).get(0).getType() : null;
            List<Constraint> constraintsList = new ArrayList<Constraint>(constraints.size());

            for (Structure constraint : constraints) {
                String constraintName = constraint.getFieldValue("name").toString();
                String objectName = objectStructure.getName();

                Map<String, Ipo> objectConstraintsIpos = constraintsIpos.get(objectName);
                Ipo ipo = objectConstraintsIpos != null ? objectConstraintsIpos.get(constraintName) : null;
                if (ipo == null) {
                    float enforce = ((Number) constraint.getFieldValue("enforce")).floatValue();
                    ipo = ipoHelper.fromValue(enforce);
                }

                constraintsList.add(this.getConstraint(dataType, constraint, objectStructure.getOldMemoryAddress(), ipo, blenderContext));
            }
            blenderContext.addConstraints(objectStructure.getOldMemoryAddress(), constraintsList);
        }
    }

    /**
     * This method creates a proper constraint object depending on the object's
     * data type. Supported data types: <li>Mesh <li>Armature <li>Camera <li>
     * Lamp Bone constraints are created in a different place.
     * 
     * @param dataType
     *            the type of the object's data
     * @param constraintStructure
     *            the constraint structure
     * @param ownerOMA
     *            the owner OMA
     * @param influenceIpo
     *            the influence interpolation curve
     * @param blenderContext
     *            the blender context
     * @return constraint object for the required type
     * @throws BlenderFileException
     *             thrown when problems with blender file occured
     */
    private Constraint getConstraint(String dataType, Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
        if (dataType == null || "Mesh".equalsIgnoreCase(dataType) || "Camera".equalsIgnoreCase(dataType) || "Lamp".equalsIgnoreCase(dataType)) {
            return new SpatialConstraint(constraintStructure, ownerOMA, influenceIpo, blenderContext);
        } else if ("Armature".equalsIgnoreCase(dataType)) {
            return new SkeletonConstraint(constraintStructure, ownerOMA, influenceIpo, blenderContext);
        } else {
            throw new IllegalArgumentException("Unsupported data type for applying constraints: " + dataType);
        }
    }

    /**
     * The method bakes all available and valid constraints.
     * 
     * @param blenderContext
     *            the blender context
     */
    public void bakeConstraints(BlenderContext blenderContext) {
        for (Constraint constraint : blenderContext.getAllConstraints()) {
            constraint.bake();
        }
    }

    /**
     * The method returns track for bone.
     * 
     * @param bone
     *            the bone
     * @param skeleton
     *            the bone's skeleton
     * @param animation
     *            the bone's animation
     * @return track for the given bone that was found among the given
     *         animations or null if none is found
     */
    /* package */BoneTrack getTrack(Bone bone, Skeleton skeleton, Animation animation) {
        int boneIndex = skeleton.getBoneIndex(bone);
        for (Track track : animation.getTracks()) {
            if (((BoneTrack) track).getTargetBoneIndex() == boneIndex) {
                return (BoneTrack) track;
            }
        }
        return null;
    }

    /**
     * The method returns track for spatial.
     * 
     * @param bone
     *            the spatial
     * @param animation
     *            the spatial's animation
     * @return track for the given spatial that was found among the given
     *         animations or null if none is found
     */
    /* package */SpatialTrack getTrack(Spatial spatial, Animation animation) {
        Track[] tracks = animation.getTracks();
        if (tracks != null && tracks.length == 1) {
            return (SpatialTrack) tracks[0];
        }
        return null;
    }

    /**
     * This method returns the transform read directly from the blender
     * structure. This can be used to read transforms from one of the object
     * types: <li>Spatial <li>Camera <li>Light
     * 
     * @param space
     *            the space where transform is evaluated
     * @param spatialOMA
     *            the OMA of the object
     * @param blenderContext
     *            the blender context
     * @return the object's transform in a given space
     */
    @SuppressWarnings("unchecked")
    /* package */Transform getNodeObjectTransform(Space space, Long spatialOMA, BlenderContext blenderContext) {
        switch (space) {
            case CONSTRAINT_SPACE_LOCAL:
                Structure targetStructure = (Structure) blenderContext.getLoadedFeature(spatialOMA, LoadedFeatureDataType.LOADED_STRUCTURE);

                DynamicArray<Number> locArray = ((DynamicArray<Number>) targetStructure.getFieldValue("loc"));
                Vector3f loc = new Vector3f(locArray.get(0).floatValue(), locArray.get(1).floatValue(), locArray.get(2).floatValue());
                DynamicArray<Number> rotArray = ((DynamicArray<Number>) targetStructure.getFieldValue("rot"));
                Quaternion rot = new Quaternion(new float[] { rotArray.get(0).floatValue(), rotArray.get(1).floatValue(), rotArray.get(2).floatValue() });
                DynamicArray<Number> sizeArray = ((DynamicArray<Number>) targetStructure.getFieldValue("size"));
                Vector3f size = new Vector3f(sizeArray.get(0).floatValue(), sizeArray.get(1).floatValue(), sizeArray.get(2).floatValue());

                if (blenderContext.getBlenderKey().isFixUpAxis()) {
                    float y = loc.y;
                    loc.y = loc.z;
                    loc.z = -y;

                    y = rot.getY();
                    float z = rot.getZ();
                    rot.set(rot.getX(), z, -y, rot.getW());

                    y = size.y;
                    size.y = size.z;
                    size.z = y;
                }

                Transform result = new Transform(loc, rot);
                result.setScale(size);
                return result;
            case CONSTRAINT_SPACE_WORLD:// TODO: get it from the object structure ???
                Object feature = blenderContext.getLoadedFeature(spatialOMA, LoadedFeatureDataType.LOADED_FEATURE);
                if (feature instanceof Spatial) {
                    return ((Spatial) feature).getWorldTransform();
                } else if (feature instanceof Skeleton) {
                    LOGGER.warning("Trying to get transformation for skeleton. This is not supported. Returning null.");
                    return null;
                } else {
                    throw new IllegalArgumentException("Given old memory address does not point to a valid object type (spatial, camera or light).");
                }
            default:
                throw new IllegalStateException("Invalid space type for target object: " + space.toString());
        }
    }

    /**
     * The method returns the transform for the given bone computed in the given
     * space.
     * 
     * @param space
     *            the computation space
     * @param bone
     *            the bone we get the transform from
     * @return the transform of the given bone
     */
    /* package */Transform getBoneTransform(Space space, Bone bone) {
        switch (space) {
            case CONSTRAINT_SPACE_LOCAL:
                Transform localTransform = new Transform(bone.getLocalPosition(), bone.getLocalRotation());
                localTransform.setScale(bone.getLocalScale());
                return localTransform;
            case CONSTRAINT_SPACE_WORLD:
                Transform worldTransform = new Transform(bone.getWorldBindPosition(), bone.getWorldBindRotation());
                worldTransform.setScale(bone.getWorldBindScale());
                return worldTransform;
            case CONSTRAINT_SPACE_POSE:
                Transform poseTransform = new Transform(bone.getLocalPosition(), bone.getLocalRotation());
                poseTransform.setScale(bone.getLocalScale());
                return poseTransform;
            case CONSTRAINT_SPACE_PARLOCAL:
                Transform parentLocalTransform = new Transform(bone.getLocalPosition(), bone.getLocalRotation());
                parentLocalTransform.setScale(bone.getLocalScale());
                return parentLocalTransform;
            default:
                throw new IllegalStateException("Invalid space type for target object: " + space.toString());
        }
    }

    /**
     * The method applies the transform for the given spatial, computed in the
     * given space.
     * 
     * @param spatial
     *            the spatial we apply the transform for
     * @param space
     *            the computation space
     * @param transform
     *            the transform being applied
     */
    /* package */void applyTransform(Spatial spatial, Space space, Transform transform) {
        switch (space) {
            case CONSTRAINT_SPACE_LOCAL:
                Transform ownerLocalTransform = spatial.getLocalTransform();
                ownerLocalTransform.getTranslation().addLocal(transform.getTranslation());
                ownerLocalTransform.getRotation().multLocal(transform.getRotation());
                ownerLocalTransform.getScale().multLocal(transform.getScale());
                break;
            case CONSTRAINT_SPACE_WORLD:
                Matrix4f m = this.getParentWorldTransformMatrix(spatial);
                m.invertLocal();
                Matrix4f matrix = this.toMatrix(transform);
                m.multLocal(matrix);

                float scaleX = (float) Math.sqrt(m.m00 * m.m00 + m.m10 * m.m10 + m.m20 * m.m20);
                float scaleY = (float) Math.sqrt(m.m01 * m.m01 + m.m11 * m.m11 + m.m21 * m.m21);
                float scaleZ = (float) Math.sqrt(m.m02 * m.m02 + m.m12 * m.m12 + m.m22 * m.m22);

                transform.setTranslation(m.toTranslationVector());
                transform.setRotation(m.toRotationQuat());
                transform.setScale(scaleX, scaleY, scaleZ);
                spatial.setLocalTransform(transform);
                break;
            case CONSTRAINT_SPACE_PARLOCAL:
            case CONSTRAINT_SPACE_POSE:
                throw new IllegalStateException("Invalid space type (" + space.toString() + ") for owner object.");
            default:
                throw new IllegalStateException("Invalid space type for target object: " + space.toString());
        }
    }

    /**
     * The method applies the transform for the given bone, computed in the
     * given space.
     * 
     * @param bone
     *            the bone we apply the transform for
     * @param space
     *            the computation space
     * @param transform
     *            the transform being applied
     */
    /* package */void applyTransform(Bone bone, Space space, Transform transform) {
        switch (space) {
            case CONSTRAINT_SPACE_LOCAL:
                bone.setBindTransforms(transform.getTranslation(), transform.getRotation(), transform.getScale());
                break;
            case CONSTRAINT_SPACE_WORLD:
                Matrix4f m = this.getParentWorldTransformMatrix(bone);
                // m.invertLocal();
                transform.setTranslation(m.mult(transform.getTranslation()));
                transform.setRotation(m.mult(transform.getRotation(), null));
                transform.setScale(transform.getScale());
                bone.setBindTransforms(transform.getTranslation(), transform.getRotation(), transform.getScale());
                // float x = FastMath.HALF_PI/2;
                // float y = -FastMath.HALF_PI;
                // float z = -FastMath.HALF_PI/2;
                // bone.setBindTransforms(new Vector3f(0,0,0), new Quaternion().fromAngles(x, y, z), new Vector3f(1,1,1));
                break;
            case CONSTRAINT_SPACE_PARLOCAL:
                Vector3f parentLocalTranslation = bone.getLocalPosition().add(transform.getTranslation());
                Quaternion parentLocalRotation = bone.getLocalRotation().mult(transform.getRotation());
                bone.setBindTransforms(parentLocalTranslation, parentLocalRotation, transform.getScale());
                break;
            case CONSTRAINT_SPACE_POSE:
                bone.setBindTransforms(transform.getTranslation(), transform.getRotation(), transform.getScale());
                break;
            default:
                throw new IllegalStateException("Invalid space type for target object: " + space.toString());
        }
    }

    /**
     * @return world transform matrix of the feature's parent or identity matrix
     *         if the feature has no parent
     */
    private Matrix4f getParentWorldTransformMatrix(Spatial spatial) {
        Matrix4f result = new Matrix4f();
        if (spatial.getParent() != null) {
            Transform t = spatial.getParent().getWorldTransform();
            result.setTransform(t.getTranslation(), t.getScale(), t.getRotation().toRotationMatrix());
        }
        return result;
    }

    /**
     * @return world transform matrix of the feature's parent or identity matrix
     *         if the feature has no parent
     */
    private Matrix4f getParentWorldTransformMatrix(Bone bone) {
        Matrix4f result = new Matrix4f();
        Bone parent = bone.getParent();
        if (parent != null) {
            result.setTransform(parent.getWorldBindPosition(), parent.getWorldBindScale(), parent.getWorldBindRotation().toRotationMatrix());
        }
        return result;
    }

    /**
     * Converts given transform to the matrix.
     * 
     * @param transform
     *            the transform to be converted
     * @return 4x4 matri that represents the given transform
     */
    private Matrix4f toMatrix(Transform transform) {
        Matrix4f result = Matrix4f.IDENTITY;
        if (transform != null) {
            result = new Matrix4f();
            result.setTranslation(transform.getTranslation());
            result.setRotationQuaternion(transform.getRotation());
            result.setScale(transform.getScale());
        }
        return result;
    }

    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
        return true;
    }

    /**
     * The space of target or owner transformation.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    public static enum Space {

        CONSTRAINT_SPACE_WORLD, CONSTRAINT_SPACE_LOCAL, CONSTRAINT_SPACE_POSE, CONSTRAINT_SPACE_PARLOCAL, CONSTRAINT_SPACE_INVALID;

        /**
         * This method returns the enum instance when given the appropriate
         * value from the blend file.
         * 
         * @param c
         *            the blender's value of the space modifier
         * @return the scape enum instance
         */
        public static Space valueOf(byte c) {
            switch (c) {
                case 0:
                    return CONSTRAINT_SPACE_WORLD;
                case 1:
                    return CONSTRAINT_SPACE_LOCAL;
                case 2:
                    return CONSTRAINT_SPACE_POSE;
                case 3:
                    return CONSTRAINT_SPACE_PARLOCAL;
                default:
                    return CONSTRAINT_SPACE_INVALID;
            }
        }
    }
}
