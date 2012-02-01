package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Bone;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.constraints.Constraint.Space;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents either owner or target of the constraint. It has the
 * common methods that take the evalueation space of the feature.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class Feature {
	/** The evalueation space. */
	protected Space				space;
	/** Old memory address of the feature. */
	protected Long				oma;
	/** The spatial that is hold by the Feature. */
	protected Spatial			spatial;
	/** The bone that is hold by the Feature. */
	protected Bone				bone;
	/** The blender context. */
	protected BlenderContext	blenderContext;

	/**
	 * Constructs the feature. This object should be loaded later
	 * when it is read from the blender file.
	 * The update method should be called before the feature is used.
	 * 
	 * @param space
	 *            the spatial's evaluation space
	 * @param oma
	 *            the spatial's old memory address
	 * @param blenderContext
	 *            the blender context
	 */
	public Feature(Space space, Long oma, BlenderContext blenderContext) {
		this.space = space;
		this.oma = oma;
		this.blenderContext = blenderContext;
	}
	
	/**
	 * Constructs the feature based on spatial.
	 * 
	 * @param spatial
	 *            the spatial
	 * @param space
	 *            the spatial's evaluation space
	 * @param oma
	 *            the spatial's old memory address
	 * @param blenderContext
	 *            the blender context
	 */
	public Feature(Spatial spatial, Space space, Long oma, BlenderContext blenderContext) {
		this(space, oma, blenderContext);
		this.blenderContext = blenderContext;
	}

	/**
	 * Constructs the feature based on bone.
	 * 
	 * @param bone
	 *            the bone
	 * @param space
	 *            the bone evaluation space
	 * @param oma
	 *            the bone old memory address
	 * @param blenderContext
	 *            the blender context
	 */
	public Feature(Bone bone, Space space, Long oma, BlenderContext blenderContext) {
		this(space, oma, blenderContext);
		this.bone = bone;
	}
	
	/**
	 * This method should be called before the feature is used.
	 * It may happen that the object this feature refers to was not yet loaded from blend file
	 * when the instance of this class was created.
	 */
	public void update() {
		Object owner = blenderContext.getLoadedFeature(oma, LoadedFeatureDataType.LOADED_FEATURE);
		if(owner instanceof Spatial) {
			this.spatial = (Spatial) owner;
		} else if(owner instanceof Bone) {
			this.bone = (Bone) owner;
		} else {
			throw new IllegalStateException("Unknown type of owner: " + owner.getClass());
		}
	}
	
	/**
	 * @return the feature's old memory address
	 */
	public Long getOma() {
		return oma;
	}

	/**
	 * @return the object held by the feature (either bone or spatial)
	 */
	public Object getObject() {
		if (spatial != null) {
			return spatial;
		}
		return bone;
	}

	/**
	 * @return the feature's transform depending on the evaluation space
	 */
	@SuppressWarnings("unchecked")
	public Transform getTransform() {
		if (spatial != null) {
			switch (space) {
				case CONSTRAINT_SPACE_LOCAL:
					Structure targetStructure = (Structure) blenderContext.getLoadedFeature(oma, LoadedFeatureDataType.LOADED_STRUCTURE);

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
				case CONSTRAINT_SPACE_WORLD:
					return spatial.getWorldTransform();
				default:
					throw new IllegalStateException("Invalid space type for target object: " + space.toString());
			}
		}
		// Bone
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
	 * This method applies the given transform to the feature in the proper
	 * evaluation space.
	 * 
	 * @param transform
	 *            the transform to be applied
	 */
	public void applyTransform(Transform transform) {
		if (spatial != null) {
			switch (space) {
				case CONSTRAINT_SPACE_LOCAL:
					Transform ownerLocalTransform = spatial.getLocalTransform();
					ownerLocalTransform.getTranslation().addLocal(transform.getTranslation());
					ownerLocalTransform.getRotation().multLocal(transform.getRotation());
					ownerLocalTransform.getScale().multLocal(transform.getScale());
					break;
				case CONSTRAINT_SPACE_WORLD:
					Matrix4f m = this.getParentWorldTransformMatrix();
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
		} else {// Bone
			switch (space) {
				case CONSTRAINT_SPACE_LOCAL:
					bone.setBindTransforms(transform.getTranslation(), transform.getRotation(), transform.getScale());
					break;
				case CONSTRAINT_SPACE_WORLD:
					Matrix4f m = this.getParentWorldTransformMatrix();
//					m.invertLocal();
					transform.setTranslation(m.mult(transform.getTranslation()));
					transform.setRotation(m.mult(transform.getRotation(), null));
					transform.setScale(transform.getScale());
					bone.setBindTransforms(transform.getTranslation(), transform.getRotation(), transform.getScale());
//					float x = FastMath.HALF_PI/2;
//					float y = -FastMath.HALF_PI;
//					float z = -FastMath.HALF_PI/2;
//					bone.setBindTransforms(new Vector3f(0,0,0), new Quaternion().fromAngles(x, y, z), new Vector3f(1,1,1));
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
	}

	/**
	 * @return world transform matrix of the feature
	 */
	public Matrix4f getWorldTransformMatrix() {
		if (spatial != null) {
			Matrix4f result = new Matrix4f();
			Transform t = spatial.getWorldTransform();
			result.setTransform(t.getTranslation(), t.getScale(), t.getRotation().toRotationMatrix());
			return result;
		}
		// Bone
		Matrix4f result = new Matrix4f();
		result.setTransform(bone.getWorldBindPosition(), bone.getWorldBindScale(), bone.getWorldBindRotation().toRotationMatrix());
		return result;
	}

	/**
	 * @return world transform matrix of the feature's parent or identity matrix
	 *         if the feature has no parent
	 */
	public Matrix4f getParentWorldTransformMatrix() {
		Matrix4f result = new Matrix4f();
		if (spatial != null) {
			if (spatial.getParent() != null) {
				Transform t = spatial.getParent().getWorldTransform();
				result.setTransform(t.getTranslation(), t.getScale(), t.getRotation().toRotationMatrix());
			}
		} else {// Bone
			Bone parent = bone.getParent();
			if (parent != null) {
				result.setTransform(parent.getWorldBindPosition(), parent.getWorldBindScale(), parent.getWorldBindRotation().toRotationMatrix());
			}
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
	protected Matrix4f toMatrix(Transform transform) {
		Matrix4f result = Matrix4f.IDENTITY;
		if (transform != null) {
			result = new Matrix4f();
			result.setTranslation(transform.getTranslation());
			result.setRotationQuaternion(transform.getRotation());
			result.setScale(transform.getScale());
		}
		return result;
	}
}
