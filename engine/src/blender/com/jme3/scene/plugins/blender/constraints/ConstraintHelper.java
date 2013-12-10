package com.jme3.scene.plugins.blender.constraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.ArmatureHelper;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.animations.IpoHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.util.TempVars;

/**
 * This class should be used for constraint calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ConstraintHelper extends AbstractBlenderHelper {
    private static final Logger     LOGGER                      = Logger.getLogger(ConstraintHelper.class.getName());

    private static final Quaternion POS_PARLOC_SPACE_QUATERNION = new Quaternion(new float[] { FastMath.HALF_PI, 0, 0 });
    private static final Quaternion NEG_PARLOC_SPACE_QUATERNION = new Quaternion(new float[] { -FastMath.HALF_PI, 0, 0 });

    /**
     * Helper constructor.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public ConstraintHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
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

                // the name is read directly from structure because bone might
                // not yet be loaded
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

                constraintsList.add(this.createConstraint(dataType, constraint, objectStructure.getOldMemoryAddress(), ipo, blenderContext));
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
    private Constraint createConstraint(String dataType, Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
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
        List<SimulationNode> simulationRootNodes = new ArrayList<SimulationNode>();
        for (Constraint constraint : blenderContext.getAllConstraints()) {
            boolean constraintUsed = false;
            for (SimulationNode node : simulationRootNodes) {
                if (node.contains(constraint)) {
                    constraintUsed = true;
                    break;
                }
            }

            if (!constraintUsed) {
                if (constraint instanceof BoneConstraint) {
                    BoneContext boneContext = blenderContext.getBoneContext(constraint.ownerOMA);
                    simulationRootNodes.add(new SimulationNode(boneContext.getArmatureObjectOMA(), blenderContext));
                } else if (constraint instanceof SpatialConstraint) {
                    Spatial spatial = (Spatial) blenderContext.getLoadedFeature(constraint.ownerOMA, LoadedFeatureDataType.LOADED_FEATURE);
                    while (spatial.getParent() != null) {
                        spatial = spatial.getParent();
                    }
                    simulationRootNodes.add(new SimulationNode((Long) blenderContext.getMarkerValue(ObjectHelper.OMA_MARKER, spatial), blenderContext));
                } else {
                    throw new IllegalStateException("Unsupported constraint type: " + constraint);
                }
            }
        }

        for (SimulationNode node : simulationRootNodes) {
            node.simulate();
        }
    }

    /**
     * The method retreives the transform from a feature in a given space.
     * 
     * @param oma
     *            the OMA of the feature (spatial or armature node)
     * @param subtargetName
     *            the feature's subtarget (bone in a case of armature's node)
     * @param space
     *            the space the transform is evaluated to
     * @return thensform of a feature in a given space
     */
    public Transform getTransform(Long oma, String subtargetName, Space space) {
        Spatial feature = (Spatial) blenderContext.getLoadedFeature(oma, LoadedFeatureDataType.LOADED_FEATURE);
        boolean isArmature = blenderContext.getMarkerValue(ArmatureHelper.ARMATURE_NODE_MARKER, feature) != null;
        if (isArmature) {
            blenderContext.getSkeleton(oma).updateWorldVectors();
            BoneContext targetBoneContext = blenderContext.getBoneByName(oma, subtargetName);
            Bone bone = targetBoneContext.getBone();

            if (bone.getParent() == null && (space == Space.CONSTRAINT_SPACE_LOCAL || space == Space.CONSTRAINT_SPACE_PARLOCAL)) {
                space = Space.CONSTRAINT_SPACE_POSE;
            }

            TempVars tempVars = TempVars.get();// use readable names of the matrices so that the code is more clear
            Transform result;
            switch (space) {
                case CONSTRAINT_SPACE_WORLD:
                    Spatial model = (Spatial) blenderContext.getLoadedFeature(targetBoneContext.getSkeletonOwnerOma(), LoadedFeatureDataType.LOADED_FEATURE);
                    Matrix4f boneModelMatrix = this.toMatrix(bone.getModelSpacePosition(), bone.getModelSpaceRotation(), bone.getModelSpaceScale(), tempVars.tempMat4);
                    Matrix4f modelWorldMatrix = this.toMatrix(model.getWorldTransform(), tempVars.tempMat42);
                    Matrix4f boneMatrixInWorldSpace = modelWorldMatrix.multLocal(boneModelMatrix);
                    result = new Transform(boneMatrixInWorldSpace.toTranslationVector(), boneMatrixInWorldSpace.toRotationQuat(), boneMatrixInWorldSpace.toScaleVector());
                    break;
                case CONSTRAINT_SPACE_LOCAL:
                    assert bone.getParent() != null : "CONSTRAINT_SPACE_LOCAL should be evaluated as CONSTRAINT_SPACE_POSE if the bone has no parent!";
                    result = new Transform(bone.getLocalPosition(), bone.getLocalRotation(), bone.getLocalScale());
                    break;
                case CONSTRAINT_SPACE_POSE:
                    Matrix4f boneWorldMatrix = this.toMatrix(this.getTransform(oma, subtargetName, Space.CONSTRAINT_SPACE_WORLD), tempVars.tempMat4);
                    Matrix4f armatureInvertedWorldMatrix = this.toMatrix(feature.getWorldTransform(), tempVars.tempMat42).invertLocal();
                    Matrix4f bonePoseMatrix = armatureInvertedWorldMatrix.multLocal(boneWorldMatrix);
                    result = new Transform(bonePoseMatrix.toTranslationVector(), bonePoseMatrix.toRotationQuat(), bonePoseMatrix.toScaleVector());
                    break;
                case CONSTRAINT_SPACE_PARLOCAL:
                    Matrix4f parentLocalMatrix = tempVars.tempMat4;
                    if (bone.getParent() != null) {
                        Bone parent = bone.getParent();
                        this.toMatrix(parent.getLocalPosition(), parent.getLocalRotation(), parent.getLocalScale(), parentLocalMatrix);
                    } else {
                        parentLocalMatrix.loadIdentity();
                    }
                    Matrix4f boneLocalMatrix = this.toMatrix(bone.getLocalPosition(), bone.getLocalRotation(), bone.getLocalScale(), tempVars.tempMat42);
                    Matrix4f resultMatrix = parentLocalMatrix.multLocal(boneLocalMatrix);

                    Vector3f loc = resultMatrix.toTranslationVector();
                    Quaternion rot = resultMatrix.toRotationQuat().normalizeLocal().multLocal(NEG_PARLOC_SPACE_QUATERNION);
                    Vector3f scl = resultMatrix.toScaleVector();
                    result = new Transform(loc, rot, scl);
                    break;
                default:
                    throw new IllegalStateException("Unknown space type: " + space);
            }
            tempVars.release();
            return result;
        } else {
            switch (space) {
                case CONSTRAINT_SPACE_LOCAL:
                    return feature.getLocalTransform();
                case CONSTRAINT_SPACE_WORLD:
                    return feature.getWorldTransform();
                case CONSTRAINT_SPACE_PARLOCAL:
                case CONSTRAINT_SPACE_POSE:
                    throw new IllegalStateException("Nodes can have only Local and World spaces applied!");
                default:
                    throw new IllegalStateException("Unknown space type: " + space);
            }
        }
    }

    /**
     * Applies transform to a feature (bone or spatial). Computations transform
     * the given transformation from the given space to the feature's local
     * space.
     * 
     * @param oma
     *            the OMA of the feature we apply transformation to
     * @param subtargetName
     *            the name of the feature's subtarget (bone in case of armature)
     * @param space
     *            the space in which the given transform is to be applied
     * @param transform
     *            the transform we apply
     */
    public void applyTransform(Long oma, String subtargetName, Space space, Transform transform) {
        Spatial feature = (Spatial) blenderContext.getLoadedFeature(oma, LoadedFeatureDataType.LOADED_FEATURE);
        boolean isArmature = blenderContext.getMarkerValue(ArmatureHelper.ARMATURE_NODE_MARKER, feature) != null;
        if (isArmature) {
            Skeleton skeleton = blenderContext.getSkeleton(oma);
            BoneContext targetBoneContext = blenderContext.getBoneByName(oma, subtargetName);
            Bone bone = targetBoneContext.getBone();

            if (bone.getParent() == null && (space == Space.CONSTRAINT_SPACE_LOCAL || space == Space.CONSTRAINT_SPACE_PARLOCAL)) {
                space = Space.CONSTRAINT_SPACE_POSE;
            }

            TempVars tempVars = TempVars.get();
            switch (space) {
                case CONSTRAINT_SPACE_LOCAL:
                    assert bone.getParent() != null : "CONSTRAINT_SPACE_LOCAL should be evaluated as CONSTRAINT_SPACE_POSE if the bone has no parent!";
                    bone.setBindTransforms(transform.getTranslation(), transform.getRotation(), transform.getScale());
                    break;
                case CONSTRAINT_SPACE_WORLD: {
                    Matrix4f boneMatrixInWorldSpace = this.toMatrix(transform, tempVars.tempMat4);
                    Matrix4f modelWorldMatrix = this.toMatrix(this.getTransform(targetBoneContext.getSkeletonOwnerOma(), null, Space.CONSTRAINT_SPACE_WORLD), tempVars.tempMat42);
                    Matrix4f boneMatrixInModelSpace = modelWorldMatrix.invertLocal().multLocal(boneMatrixInWorldSpace);
                    Bone parent = bone.getParent();
                    if (parent != null) {
                        Matrix4f parentMatrixInModelSpace = this.toMatrix(parent.getModelSpacePosition(), parent.getModelSpaceRotation(), parent.getModelSpaceScale(), tempVars.tempMat4);
                        boneMatrixInModelSpace = parentMatrixInModelSpace.invertLocal().multLocal(boneMatrixInModelSpace);
                    }
                    bone.setBindTransforms(boneMatrixInModelSpace.toTranslationVector(), boneMatrixInModelSpace.toRotationQuat(), boneMatrixInModelSpace.toScaleVector());
                    break;
                }
                case CONSTRAINT_SPACE_POSE: {
                    Matrix4f armatureWorldMatrix = this.toMatrix(feature.getWorldTransform(), tempVars.tempMat4);
                    Matrix4f boneMatrixInWorldSpace = armatureWorldMatrix.multLocal(this.toMatrix(transform, tempVars.tempMat42));
                    Matrix4f invertedModelMatrix = this.toMatrix(this.getTransform(targetBoneContext.getSkeletonOwnerOma(), null, Space.CONSTRAINT_SPACE_WORLD), tempVars.tempMat42).invertLocal();
                    Matrix4f boneMatrixInModelSpace = invertedModelMatrix.multLocal(boneMatrixInWorldSpace);
                    Bone parent = bone.getParent();
                    if (parent != null) {
                        Matrix4f parentMatrixInModelSpace = this.toMatrix(parent.getModelSpacePosition(), parent.getModelSpaceRotation(), parent.getModelSpaceScale(), tempVars.tempMat4);
                        boneMatrixInModelSpace = parentMatrixInModelSpace.invertLocal().multLocal(boneMatrixInModelSpace);
                    }
                    bone.setBindTransforms(boneMatrixInModelSpace.toTranslationVector(), boneMatrixInModelSpace.toRotationQuat(), boneMatrixInModelSpace.toScaleVector());
                    break;
                }
                case CONSTRAINT_SPACE_PARLOCAL:
                    Matrix4f parentLocalInverseMatrix = tempVars.tempMat4;
                    if (bone.getParent() != null) {
                        this.toMatrix(bone.getParent().getLocalPosition(), bone.getParent().getLocalRotation(), bone.getParent().getLocalScale(), parentLocalInverseMatrix);
                        parentLocalInverseMatrix.invertLocal();
                    } else {
                        parentLocalInverseMatrix.loadIdentity();
                    }
                    Matrix4f m = this.toMatrix(transform.getTranslation(), transform.getRotation(), transform.getScale(), tempVars.tempMat42);
                    Matrix4f result = parentLocalInverseMatrix.multLocal(m);
                    Vector3f loc = result.toTranslationVector();
                    Quaternion rot = result.toRotationQuat().normalizeLocal().multLocal(POS_PARLOC_SPACE_QUATERNION);
                    Vector3f scl = result.toScaleVector();
                    bone.setBindTransforms(loc, rot, scl);
                    break;
                default:
                    tempVars.release();
                    throw new IllegalStateException("Invalid space type for target object: " + space.toString());
            }
            tempVars.release();
            skeleton.updateWorldVectors();
        } else {
            switch (space) {
                case CONSTRAINT_SPACE_LOCAL:
                    feature.getLocalTransform().set(transform);
                    break;
                case CONSTRAINT_SPACE_WORLD:
                    if (feature.getParent() == null) {
                        feature.setLocalTransform(transform);
                    } else {
                        Transform parentWorldTransform = feature.getParent().getWorldTransform();

                        TempVars tempVars = TempVars.get();
                        Matrix4f parentInverseMatrix = this.toMatrix(parentWorldTransform, tempVars.tempMat4).invertLocal();
                        Matrix4f m = this.toMatrix(transform, tempVars.tempMat42);
                        m = m.multLocal(parentInverseMatrix);
                        tempVars.release();

                        transform.setTranslation(m.toTranslationVector());
                        transform.setRotation(m.toRotationQuat());
                        transform.setScale(m.toScaleVector());

                        feature.setLocalTransform(transform);
                    }
                    break;
                default:
                    throw new IllegalStateException("Invalid space type for spatial object: " + space.toString());
            }
        }
    }

    /**
     * Converts given transform to the matrix.
     * 
     * @param transform
     *            the transform to be converted
     * @param store
     *            the matrix where the result will be stored
     * @return the store matrix
     */
    public Matrix4f toMatrix(Transform transform, Matrix4f store) {
        if (transform != null) {
            return this.toMatrix(transform.getTranslation(), transform.getRotation(), transform.getScale(), store);
        }
        store.loadIdentity();
        return store;
    }

    /**
     * Converts given transformation parameters into the matrix.
     * 
     * @param position
     *            the position of the feature
     * @param rotation
     *            the rotation of the feature
     * @param scale
     *            the scale of the feature
     * @param store
     *            the matrix where the result will be stored
     * @return the store matrix
     */
    private Matrix4f toMatrix(Vector3f position, Quaternion rotation, Vector3f scale, Matrix4f store) {
        store.loadIdentity();
        store.setTranslation(position);
        store.setRotationQuaternion(rotation);
        store.setScale(scale);
        return store;
    }

    /**
     * The space of target or owner transformation.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    public static enum Space {
        /** A transformation of the bone or spatial in the world space. */
        CONSTRAINT_SPACE_WORLD,
        /**
         * For spatial it is the transformation in its parent space or in WORLD space if it has no parent.
         * For bone it is a transformation in its bone parent space or in armature space if it has no parent.
         */
        CONSTRAINT_SPACE_LOCAL,
        /**
         * This space IS NOT applicable for spatials.
         * For bone it is a transformation in the blender's armature object space.
         */
        CONSTRAINT_SPACE_POSE,

        CONSTRAINT_SPACE_PARLOCAL;

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
                    throw new IllegalArgumentException("Value: " + c + " cannot be converted to Space enum instance!");
            }
        }
    }
}
