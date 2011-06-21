package com.jme3.scene.plugins.blender.helpers.v249;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.structures.AbstractInfluenceFunction;
import com.jme3.scene.plugins.blender.structures.Constraint;
import com.jme3.scene.plugins.blender.structures.Constraint.Space;
import com.jme3.scene.plugins.blender.structures.ConstraintType;
import com.jme3.scene.plugins.blender.structures.Ipo;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.Pointer;
import java.util.logging.Level;

/**
 * This class should be used for constraint calculations.
 * @author Marcin Roguski
 */
public class ConstraintHelper extends AbstractBlenderHelper {

    /**
     * A table containing implementations of influence functions for constraints. It should contain functions for
     * blender at least 249 and higher.
     */
    protected static AbstractInfluenceFunction[] influenceFunctions;
    /**
     * Constraints stored for object with the given old memory address.
     */
    protected Map<Long, Constraint[]> constraints = new HashMap<Long, Constraint[]>();

    /**
     * Helper constructor. It's main task is to generate the affection functions. These functions are common to all
     * ConstraintHelper instances. Unfortunately this constructor might grow large. If it becomes too large - I shall
     * consider refactoring. The constructor parses the given blender version and stores the result. Some
     * functionalities may differ in different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public ConstraintHelper(String blenderVersion, DataRepository dataRepository) {
        super(blenderVersion);
        this.initializeConstraintFunctions(dataRepository);
    }

	/**
	 * This method initializes constraint functions for Blender 2.49.
	 * @param dataRepository
	 *           			the data repository
	 */
    private synchronized void initializeConstraintFunctions(DataRepository dataRepository) {
    	if (influenceFunctions == null) {
            influenceFunctions = new AbstractInfluenceFunction[ConstraintType.getLastDefinedTypeValue() + 1];
            //ACTION constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_ACTION.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_ACTION, dataRepository) {
            };

            //CHILDOF constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_CHILDOF.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_CHILDOF, dataRepository) {
            };

            //CLAMPTO constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_CLAMPTO.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_CLAMPTO, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    this.validateConstraintType(constraint.getData());
                    LOGGER.log(Level.INFO, "{0} not active! Curves not yet implemented!", constraint.getName());//TODO: implement when curves are implemented
                }
            };

            //DISTLIMIT constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_DISTLIMIT.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_DISTLIMIT, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintStructure = constraint.getData();
                    this.validateConstraintType(constraintStructure);
                    Vector3f targetLocation = this.getTargetLocation(constraint);
                    BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation, constraint);
                    if (boneTrack != null) {
                        //TODO: target vertex group !!!
                        float dist = ((Number) constraintStructure.getFieldValue("dist")).floatValue();
                        int mode = ((Number) constraintStructure.getFieldValue("mode")).intValue();

                        int maxFrames = boneTrack.getTimes().length;
                        Vector3f[] translations = boneTrack.getTranslations();
                        for (int frame = 0; frame < maxFrames; ++frame) {
                            Vector3f v = translations[frame].subtract(targetLocation);
                            float currentDistance = v.length();
                            float influence = constraint.getIpo().calculateValue(frame);
                            float modifier = 0.0f;
                            switch (mode) {
                                case LIMITDIST_INSIDE:
                                    if (currentDistance >= dist) {
                                        modifier = (dist - currentDistance) / currentDistance;
                                    }
                                    break;
                                case LIMITDIST_ONSURFACE:
                                    modifier = (dist - currentDistance) / currentDistance;
                                    break;
                                case LIMITDIST_OUTSIDE:
                                    if (currentDistance <= dist) {
                                        modifier = (dist - currentDistance) / currentDistance;
                                    }
                                    break;
                                default:
                                    throw new IllegalStateException("Unknown distance limit constraint mode: " + mode);
                            }
                            translations[frame].addLocal(v.multLocal(modifier * influence));
                        }
                        boneTrack.setKeyframes(boneTrack.getTimes(), translations, boneTrack.getRotations(), boneTrack.getScales());
                    }
                }
            };

            //FOLLOWPATH constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_FOLLOWPATH.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_FOLLOWPATH, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    this.validateConstraintType(constraint.getData());
                    LOGGER.log(Level.INFO, "{0} not active! Curves not yet implemented!", constraint.getName());//TODO: implement when curves are implemented
                }
            };

            //KINEMATIC constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_KINEMATIC.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_KINEMATIC, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintStructure = constraint.getData();
                    this.validateConstraintType(constraintStructure);
                    /*Long boneOMA = constraint.getBoneOMA();
                    //IK solver is only attached to bones
                    Bone ownerBone = (Bone)dataRepository.getLoadedFeature(boneOMA, LoadedFeatureDataType.LOADED_FEATURE);
                    
                    //get the target point
                    Object targetObject = this.getTarget(constraint, LoadedFeatureDataType.LOADED_FEATURE);
                    Vector3f pt = null;//Point Target
                    if(targetObject instanceof Bone) {
                    pt = ((Bone)targetObject).getModelSpacePosition();
                    } else if(targetObject instanceof Node) {
                    pt = ((Node)targetObject).getWorldTranslation();
                    } else if(targetObject instanceof Skeleton) {
                    Structure armatureNodeStructure = (Structure)this.getTarget(constraint, LoadedFeatureDataType.LOADED_STRUCTURE);
                    ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
                    Transform transform = objectHelper.getTransformation(armatureNodeStructure);
                    pt = transform.getTranslation();
                    } else {
                    throw new IllegalStateException("Unknown target object type! Should be Node, Bone or Skeleton and there is: " + targetObject.getClass().getName());
                    }
                    //preparing data
                    int maxIterations = ((Number)constraintStructure.getFieldValue("iterations")).intValue();
                    CalculationBone[] bones = this.getBonesToCalculate(ownerBone, skeleton, boneAnimation);
                    for(int i=0;i<bones.length;++i) {
                    System.out.println(Arrays.toString(bones[i].track.getTranslations()));
                    System.out.println(Arrays.toString(bones[i].track.getRotations()));
                    System.out.println("===============================");
                    }
                    Quaternion rotation = new Quaternion();
                    int maxFrames = bones[0].track.getTimes().length;//all tracks should have the same amount of frames
                    
                    for(int frame = 0; frame < maxFrames; ++frame) {
                    float error = IK_SOLVER_ERROR;
                    int iteration = 0;
                    while(error >= IK_SOLVER_ERROR && iteration <= maxIterations) {
                    //rotating the bones
                    for(int i = 0; i < bones.length - 1; ++i) {
                    Vector3f pe = bones[i].getEndPoint();
                    Vector3f pc = bones[i + 1].getWorldTranslation().clone();
                    
                    Vector3f peSUBpc = pe.subtract(pc).normalizeLocal();
                    Vector3f ptSUBpc = pt.subtract(pc).normalizeLocal();
                    
                    float theta = FastMath.acos(peSUBpc.dot(ptSUBpc));
                    Vector3f direction = peSUBpc.cross(ptSUBpc).normalizeLocal();
                    bones[i].rotate(rotation.fromAngleAxis(theta, direction), frame);
                    }
                    error = pt.subtract(bones[0].getEndPoint()).length();
                    ++iteration;
                    }
                    System.out.println("error = " + error + "   iterations = " + iteration);
                    }
                    
                    for(CalculationBone bone : bones) {
                    bone.applyCalculatedTracks();
                    }
                    
                    System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
                    for(int i=0;i<bones.length;++i) {
                    System.out.println(Arrays.toString(bones[i].track.getTranslations()));
                    System.out.println(Arrays.toString(bones[i].track.getRotations()));
                    System.out.println("===============================");
                    }*/
                }

                /**
                 * This method returns bones used for rotation calculations.
                 * @param bone
                 *        the bone to which the constraint is applied
                 * @param skeleton
                 *        the skeleton owning the bone and its ancestors
                 * @param boneAnimation
                 *        the bone animation data that stores the traces for the skeleton's bones
                 * @return a list of bones to imitate the bone's movement during IK solving
                 */
                private CalculationBone[] getBonesToCalculate(Bone bone, Skeleton skeleton, BoneAnimation boneAnimation) {
                    List<CalculationBone> bonesList = new ArrayList<CalculationBone>();
                    Bone currentBone = bone;
                    do {
                        int boneIndex = skeleton.getBoneIndex(currentBone);
                        for (int i = 0; i < boneAnimation.getTracks().length; ++i) {
                            if (boneAnimation.getTracks()[i].getTargetBoneIndex() == boneIndex) {
                                bonesList.add(new CalculationBone(currentBone, boneAnimation.getTracks()[i]));
                                break;
                            }
                        }
                        currentBone = currentBone.getParent();
                    } while (currentBone != null);
                    //attaching children
                    CalculationBone[] result = bonesList.toArray(new CalculationBone[bonesList.size()]);
                    for (int i = result.length - 1; i > 0; --i) {
                        result[i].attachChild(result[i - 1]);
                    }
                    return result;
                }
            };

            //LOCKTRACK constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_LOCKTRACK.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_LOCKTRACK, dataRepository) {
            };

            //LOCLIKE constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_LOCLIKE.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_LOCLIKE, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintData = constraint.getData();
                    this.validateConstraintType(constraintData);
                    BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation, constraint);
                    if (boneTrack != null) {
                        Vector3f targetLocation = this.getTargetLocation(constraint);
                        int flag = ((Number) constraintData.getFieldValue("flag")).intValue();
                        Vector3f[] translations = boneTrack.getTranslations();
                        int maxFrames = translations.length;
                        for (int frame = 0; frame < maxFrames; ++frame) {
                            Vector3f offset = Vector3f.ZERO;
                            if ((flag & LOCLIKE_OFFSET) != 0) {//we add the original location to the copied location
                                offset = translations[frame].clone();
                            }

                            if ((flag & LOCLIKE_X) != 0) {
                                translations[frame].x = targetLocation.x;
                                if ((flag & LOCLIKE_X_INVERT) != 0) {
                                    translations[frame].x = -translations[frame].x;
                                }
                            } else if ((flag & LOCLIKE_Y) != 0) {
                                translations[frame].y = targetLocation.y;
                                if ((flag & LOCLIKE_Y_INVERT) != 0) {
                                    translations[frame].y = -translations[frame].y;
                                }
                            } else if ((flag & LOCLIKE_Z) != 0) {
                                translations[frame].z = targetLocation.z;
                                if ((flag & LOCLIKE_Z_INVERT) != 0) {
                                    translations[frame].z = -translations[frame].z;
                                }
                            }
                            translations[frame].addLocal(offset);//TODO: ipo influence
                        }
                        boneTrack.setKeyframes(boneTrack.getTimes(), translations, boneTrack.getRotations(), boneTrack.getScales());
                    }
                }
            };

            //LOCLIMIT constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_LOCLIMIT.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_LOCLIMIT, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintStructure = constraint.getData();
                    this.validateConstraintType(constraintStructure);
                    BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation, constraint);
                    if (boneTrack != null) {
                        int flag = ((Number) constraintStructure.getFieldValue("flag")).intValue();
                        Vector3f[] translations = boneTrack.getTranslations();
                        int maxFrames = translations.length;
                        for (int frame = 0; frame < maxFrames; ++frame) {
                            float influence = constraint.getIpo().calculateValue(frame);
                            if ((flag & LIMIT_XMIN) != 0) {
                                float xmin = ((Number) constraintStructure.getFieldValue("xmin")).floatValue();
                                if (translations[frame].x < xmin) {
                                    translations[frame].x -= (translations[frame].x - xmin) * influence;
                                }
                            }
                            if ((flag & LIMIT_XMAX) != 0) {
                                float xmax = ((Number) constraintStructure.getFieldValue("xmax")).floatValue();
                                if (translations[frame].x > xmax) {
                                    translations[frame].x -= (translations[frame].x - xmax) * influence;
                                }
                            }
                            if ((flag & LIMIT_YMIN) != 0) {
                                float ymin = ((Number) constraintStructure.getFieldValue("ymin")).floatValue();
                                if (translations[frame].y < ymin) {
                                    translations[frame].y -= (translations[frame].y - ymin) * influence;
                                }
                            }
                            if ((flag & LIMIT_YMAX) != 0) {
                                float ymax = ((Number) constraintStructure.getFieldValue("ymax")).floatValue();
                                if (translations[frame].y > ymax) {
                                    translations[frame].y -= (translations[frame].y - ymax) * influence;
                                }
                            }
                            if ((flag & LIMIT_ZMIN) != 0) {
                                float zmin = ((Number) constraintStructure.getFieldValue("zmin")).floatValue();
                                if (translations[frame].z < zmin) {
                                    translations[frame].z -= (translations[frame].z - zmin) * influence;
                                }
                            }
                            if ((flag & LIMIT_ZMAX) != 0) {
                                float zmax = ((Number) constraintStructure.getFieldValue("zmax")).floatValue();
                                if (translations[frame].z > zmax) {
                                    translations[frame].z -= (translations[frame].z - zmax) * influence;
                                }
                            }//TODO: consider constraint space !!!
                        }
                        boneTrack.setKeyframes(boneTrack.getTimes(), translations, boneTrack.getRotations(), boneTrack.getScales());
                    }
                }
            };

            //MINMAX constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_MINMAX.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_MINMAX, dataRepository) {
            };

            //NULL constraint - does nothing
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_NULL.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_NULL, dataRepository) {
            };

            //PYTHON constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_PYTHON.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_PYTHON, dataRepository) {
            };

            //RIGIDBODYJOINT constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_RIGIDBODYJOINT.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_RIGIDBODYJOINT, dataRepository) {
            };

            //ROTLIKE constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_ROTLIKE.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_ROTLIKE, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintData = constraint.getData();
                    this.validateConstraintType(constraintData);
                    BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation, constraint);
                    if (boneTrack != null) {
                        Quaternion targetRotation = this.getTargetRotation(constraint);
                        int flag = ((Number) constraintData.getFieldValue("flag")).intValue();
                        float[] targetAngles = targetRotation.toAngles(null);
                        Quaternion[] rotations = boneTrack.getRotations();
                        int maxFrames = rotations.length;
                        for (int frame = 0; frame < maxFrames; ++frame) {
                            float[] angles = rotations[frame].toAngles(null);

                            Quaternion offset = Quaternion.IDENTITY;
                            if ((flag & ROTLIKE_OFFSET) != 0) {//we add the original rotation to the copied rotation
                                offset = rotations[frame].clone();
                            }

                            if ((flag & ROTLIKE_X) != 0) {
                                angles[0] = targetAngles[0];
                                if ((flag & ROTLIKE_X_INVERT) != 0) {
                                    angles[0] = -angles[0];
                                }
                            } else if ((flag & ROTLIKE_Y) != 0) {
                                angles[1] = targetAngles[1];
                                if ((flag & ROTLIKE_Y_INVERT) != 0) {
                                    angles[1] = -angles[1];
                                }
                            } else if ((flag & ROTLIKE_Z) != 0) {
                                angles[2] = targetAngles[2];
                                if ((flag & ROTLIKE_Z_INVERT) != 0) {
                                    angles[2] = -angles[2];
                                }
                            }
                            rotations[frame].fromAngles(angles).multLocal(offset);//TODO: ipo influence
                        }
                        boneTrack.setKeyframes(boneTrack.getTimes(), boneTrack.getTranslations(), rotations, boneTrack.getScales());
                    }
                }
            };

            //ROTLIMIT constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_ROTLIMIT.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_ROTLIMIT, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintStructure = constraint.getData();
                    this.validateConstraintType(constraintStructure);
                    BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation, constraint);
                    if (boneTrack != null) {
                        int flag = ((Number) constraintStructure.getFieldValue("flag")).intValue();
                        Quaternion[] rotations = boneTrack.getRotations();
                        int maxFrames = rotations.length;
                        for (int frame = 0; frame < maxFrames; ++frame) {
                            float[] angles = rotations[frame].toAngles(null);
                            float influence = constraint.getIpo().calculateValue(frame);
                            if ((flag & LIMIT_XROT) != 0) {
                                float xmin = ((Number) constraintStructure.getFieldValue("xmin")).floatValue() * FastMath.DEG_TO_RAD;
                                float xmax = ((Number) constraintStructure.getFieldValue("xmax")).floatValue() * FastMath.DEG_TO_RAD;
                                float difference = 0.0f;
                                if (angles[0] < xmin) {
                                    difference = (angles[0] - xmin) * influence;
                                } else if (angles[0] > xmax) {
                                    difference = (angles[0] - xmax) * influence;
                                }
                                angles[0] -= difference;
                            }
                            if ((flag & LIMIT_YROT) != 0) {
                                float ymin = ((Number) constraintStructure.getFieldValue("ymin")).floatValue() * FastMath.DEG_TO_RAD;
                                float ymax = ((Number) constraintStructure.getFieldValue("ymax")).floatValue() * FastMath.DEG_TO_RAD;
                                float difference = 0.0f;
                                if (angles[1] < ymin) {
                                    difference = (angles[1] - ymin) * influence;
                                } else if (angles[1] > ymax) {
                                    difference = (angles[1] - ymax) * influence;
                                }
                                angles[1] -= difference;
                            }
                            if ((flag & LIMIT_ZROT) != 0) {
                                float zmin = ((Number) constraintStructure.getFieldValue("zmin")).floatValue() * FastMath.DEG_TO_RAD;
                                float zmax = ((Number) constraintStructure.getFieldValue("zmax")).floatValue() * FastMath.DEG_TO_RAD;
                                float difference = 0.0f;
                                if (angles[2] < zmin) {
                                    difference = (angles[2] - zmin) * influence;
                                } else if (angles[2] > zmax) {
                                    difference = (angles[2] - zmax) * influence;
                                }
                                angles[2] -= difference;
                            }
                            rotations[frame].fromAngles(angles);//TODO: consider constraint space !!!
                        }
                        boneTrack.setKeyframes(boneTrack.getTimes(), boneTrack.getTranslations(), rotations, boneTrack.getScales());
                    }
                }
            };

            //SHRINKWRAP constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_SHRINKWRAP.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_SHRINKWRAP, dataRepository) {
            };

            //SIZELIKE constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_SIZELIKE.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_SIZELIKE, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintData = constraint.getData();
                    this.validateConstraintType(constraintData);
                    Vector3f targetScale = this.getTargetLocation(constraint);
                    BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation, constraint);
                    if (boneTrack != null) {
                        int flag = ((Number) constraintData.getFieldValue("flag")).intValue();
                        Vector3f[] scales = boneTrack.getScales();
                        int maxFrames = scales.length;
                        for (int frame = 0; frame < maxFrames; ++frame) {
                            Vector3f offset = Vector3f.ZERO;
                            if ((flag & LOCLIKE_OFFSET) != 0) {//we add the original scale to the copied scale
                                offset = scales[frame].clone();
                            }

                            if ((flag & SIZELIKE_X) != 0) {
                                scales[frame].x = targetScale.x;
                            } else if ((flag & SIZELIKE_Y) != 0) {
                                scales[frame].y = targetScale.y;
                            } else if ((flag & SIZELIKE_Z) != 0) {
                                scales[frame].z = targetScale.z;
                            }
                            scales[frame].addLocal(offset);//TODO: ipo influence
                            //TODO: add or multiply???
                        }
                        boneTrack.setKeyframes(boneTrack.getTimes(), boneTrack.getTranslations(), boneTrack.getRotations(), scales);
                    }
                }
            };

            //SIZELIMIT constraint
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_SIZELIMIT.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_SIZELIMIT, dataRepository) {

                @Override
                public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
                    Structure constraintStructure = constraint.getData();
                    this.validateConstraintType(constraintStructure);
                    BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation, constraint);
                    if (boneTrack != null) {
                        int flag = ((Number) constraintStructure.getFieldValue("flag")).intValue();
                        Vector3f[] scales = boneTrack.getScales();
                        int maxFrames = scales.length;
                        for (int frame = 0; frame < maxFrames; ++frame) {
                            float influence = constraint.getIpo().calculateValue(frame);
                            if ((flag & LIMIT_XMIN) != 0) {
                                float xmin = ((Number) constraintStructure.getFieldValue("xmin")).floatValue();
                                if (scales[frame].x < xmin) {
                                    scales[frame].x -= (scales[frame].x - xmin) * influence;
                                }
                            }
                            if ((flag & LIMIT_XMAX) != 0) {
                                float xmax = ((Number) constraintStructure.getFieldValue("xmax")).floatValue();
                                if (scales[frame].x > xmax) {
                                    scales[frame].x -= (scales[frame].x - xmax) * influence;
                                }
                            }
                            if ((flag & LIMIT_YMIN) != 0) {
                                float ymin = ((Number) constraintStructure.getFieldValue("ymin")).floatValue();
                                if (scales[frame].y < ymin) {
                                    scales[frame].y -= (scales[frame].y - ymin) * influence;
                                }
                            }
                            if ((flag & LIMIT_YMAX) != 0) {
                                float ymax = ((Number) constraintStructure.getFieldValue("ymax")).floatValue();
                                if (scales[frame].y > ymax) {
                                    scales[frame].y -= (scales[frame].y - ymax) * influence;
                                }
                            }
                            if ((flag & LIMIT_ZMIN) != 0) {
                                float zmin = ((Number) constraintStructure.getFieldValue("zmin")).floatValue();
                                if (scales[frame].z < zmin) {
                                    scales[frame].z -= (scales[frame].z - zmin) * influence;
                                }
                            }
                            if ((flag & LIMIT_ZMAX) != 0) {
                                float zmax = ((Number) constraintStructure.getFieldValue("zmax")).floatValue();
                                if (scales[frame].z > zmax) {
                                    scales[frame].z -= (scales[frame].z - zmax) * influence;
                                }
                            }//TODO: consider constraint space !!!
                        }
                        boneTrack.setKeyframes(boneTrack.getTimes(), boneTrack.getTranslations(), boneTrack.getRotations(), scales);
                    }
                }
            };

            //STRETCHTO constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_STRETCHTO.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_STRETCHTO, dataRepository) {
            };

            //TRANSFORM constraint (TODO: to implement)
            influenceFunctions[ConstraintType.CONSTRAINT_TYPE_TRANSFORM.getConstraintId()] = new AbstractInfluenceFunction(ConstraintType.CONSTRAINT_TYPE_TRANSFORM, dataRepository) {
            };
        }
    }
    
    /**
     * This method reads constraints for for the given structure. The constraints are loaded only once for object/bone.
     * @param ownerOMA
     *        the owner's old memory address
     * @param objectStructure
     *        the structure we read constraint's for
     * @param dataRepository
     *        the data repository
     * @throws BlenderFileException
     */
    public void loadConstraints(Structure objectStructure, DataRepository dataRepository) throws BlenderFileException {
        // reading influence ipos for the constraints
        IpoHelper ipoHelper = dataRepository.getHelper(IpoHelper.class);
        Map<String, Map<String, Ipo>> constraintsIpos = new HashMap<String, Map<String, Ipo>>();
        Pointer pActions = (Pointer) objectStructure.getFieldValue("action");
        if (pActions.isNotNull()) {
            List<Structure> actions = pActions.fetchData(dataRepository.getInputStream());
            for (Structure action : actions) {
                Structure chanbase = (Structure) action.getFieldValue("chanbase");
                List<Structure> actionChannels = chanbase.evaluateListBase(dataRepository);
                for (Structure actionChannel : actionChannels) {
                    Map<String, Ipo> ipos = new HashMap<String, Ipo>();
                    Structure constChannels = (Structure) actionChannel.getFieldValue("constraintChannels");
                    List<Structure> constraintChannels = constChannels.evaluateListBase(dataRepository);
                    for (Structure constraintChannel : constraintChannels) {
                        Pointer pIpo = (Pointer) constraintChannel.getFieldValue("ipo");
                        if (pIpo.isNotNull()) {
                            String constraintName = constraintChannel.getFieldValue("name").toString();
                            Ipo ipo = ipoHelper.createIpo(pIpo.fetchData(dataRepository.getInputStream()).get(0), dataRepository);
                            ipos.put(constraintName, ipo);
                        }
                    }
                    String actionName = actionChannel.getFieldValue("name").toString();
                    constraintsIpos.put(actionName, ipos);
                }
            }
        }

        //loading constraints connected with the object's bones
        List<Constraint> constraintsList = new ArrayList<Constraint>();
        Pointer pPose = (Pointer) objectStructure.getFieldValue("pose");//TODO: what if the object has two armatures ????
        if (pPose.isNotNull()) {
            //getting pose channels
            List<Structure> poseChannels = ((Structure) pPose.fetchData(dataRepository.getInputStream()).get(0).getFieldValue("chanbase")).evaluateListBase(dataRepository);
            for (Structure poseChannel : poseChannels) {
                Long boneOMA = Long.valueOf(((Pointer) poseChannel.getFieldValue("bone")).getOldMemoryAddress());
                //the name is read directly from structure because bone might not yet be loaded
                String name = dataRepository.getFileBlock(boneOMA).getStructure(dataRepository).getFieldValue("name").toString();
                List<Structure> constraints = ((Structure) poseChannel.getFieldValue("constraints")).evaluateListBase(dataRepository);
                for (Structure constraint : constraints) {
                    int type = ((Number) constraint.getFieldValue("type")).intValue();
                    String constraintName = constraint.getFieldValue("name").toString();
                    Ipo ipo = constraintsIpos.get(name).get(constraintName);
                    if (ipo == null) {
                        float enforce = ((Number) constraint.getFieldValue("enforce")).floatValue();
                        ipo = ipoHelper.createIpo(enforce);
                    }
                    Space ownerSpace = Space.valueOf(((Number) constraint.getFieldValue("ownspace")).byteValue());
                    Space targetSpace = Space.valueOf(((Number) constraint.getFieldValue("tarspace")).byteValue());
                    Constraint c = new Constraint(constraint, influenceFunctions[type], boneOMA, ownerSpace, targetSpace, ipo, dataRepository);
                    constraintsList.add(c);
                }
            }
        }
        /* TODO: reading constraints for objects (implement when object's animation will be available)
        List<Structure> constraintChannels = ((Structure)objectStructure.getFieldValue("constraintChannels")).evaluateListBase(dataRepository);
        for(Structure constraintChannel : constraintChannels) {
        System.out.println(constraintChannel);
        }
        
        //loading constraints connected with the object itself (TODO: test this)
        if(!this.constraints.containsKey(objectStructure.getOldMemoryAddress())) {
        List<Structure> constraints = ((Structure)objectStructure.getFieldValue("constraints")).evaluateListBase(dataRepository);
        Constraint[] result = new Constraint[constraints.size()];
        int i = 0;
        for(Structure constraint : constraints) {
        int type = ((Number)constraint.getFieldValue("type")).intValue();
        String name = constraint.getFieldValue("name").toString();
        result[i++] = new Constraint(constraint, influenceFunctions[type], null, dataRepository);//TODO: influence ipos for object animation
        }
        this.constraints.put(objectStructure.getOldMemoryAddress(), result);
        }
         */
        if (constraintsList.size() > 0) {
            this.constraints.put(objectStructure.getOldMemoryAddress(), constraintsList.toArray(new Constraint[constraintsList.size()]));
        }
    }

    /**
     * This method returns a list of constraints of the feature's constraints. The order of constraints is important.
     * @param ownerOMA
     *        the owner's old memory address
     * @return a table of constraints for the feature specified by old memory address
     */
    public Constraint[] getConstraints(Long ownerOMA) {
        return constraints.get(ownerOMA);
    }

    @Override
    public void clearState() {
        constraints.clear();
    }

    /**
     * The purpose of this class is to imitate bone's movement when calculating inverse kinematics.
     * @author Marcin Roguski
     */
    private static class CalculationBone extends Node {

        /** The name of the bone. Only to be used in toString method. */
        private String boneName;
        /** The bone's tracks. Will be altered at the end of calculation process. */
        private BoneTrack track;
        /** The starting position of the bone. */
        private Vector3f startTranslation;
        /** The starting rotation of the bone. */
        private Quaternion startRotation;
        /** The starting scale of the bone. */
        private Vector3f startScale;
        private Vector3f[] translations;
        private Quaternion[] rotations;
        private Vector3f[] scales;

        /**
         * Constructor. Stores the track, starting transformation and sets the transformation to the starting positions.
         * @param bone
         *        the bone this class will imitate
         * @param track
         *        the bone's tracks
         */
        public CalculationBone(Bone bone, BoneTrack track) {
            this.boneName = bone.getName();
            this.track = track;
            this.startRotation = bone.getModelSpaceRotation().clone();
            this.startTranslation = bone.getModelSpacePosition().clone();
            this.startScale = bone.getModelSpaceScale().clone();
            this.translations = track.getTranslations();
            this.rotations = track.getRotations();
            this.scales = track.getScales();
            this.reset();
        }

        /**
         * This method returns the end point of the bone. If the bone has parent it is calculated from the start point
         * of parent to the start point of this bone. If the bone doesn't have a parent the end location is considered
         * to be 1 point up along Y axis (scale is applied if set to != 1.0);
         * @return the end point of this bone
         */
        //TODO: set to Z axis if user defined it this way
        public Vector3f getEndPoint() {
            if (this.getParent() == null) {
                return new Vector3f(0, this.getLocalScale().y, 0);
            } else {
                Node parent = this.getParent();
                return parent.getWorldTranslation().subtract(this.getWorldTranslation()).multLocal(this.getWorldScale());
            }
        }

        /**
         * This method resets the calculation bone to the starting position.
         */
        public void reset() {
            this.setLocalTranslation(startTranslation);
            this.setLocalRotation(startRotation);
            this.setLocalScale(startScale);
        }

        @Override
        public int attachChild(Spatial child) {
            if (this.getChildren() != null && this.getChildren().size() > 1) {
                throw new IllegalStateException(this.getClass().getName() + " class instance can only have one child!");
            }
            return super.attachChild(child);
        }

        public Spatial rotate(Quaternion rot, int frame) {
            Spatial spatial = super.rotate(rot);
            this.updateWorldTransforms();
            if (this.getChildren() != null && this.getChildren().size() > 0) {
                CalculationBone child = (CalculationBone) this.getChild(0);
                child.updateWorldTransforms();
            }
            rotations[frame].set(this.getLocalRotation());
            translations[frame].set(this.getLocalTranslation());
            if (scales != null) {
                scales[frame].set(this.getLocalScale());
            }
            return spatial;
        }

        public void applyCalculatedTracks() {
            track.setKeyframes(track.getTimes(), translations, rotations);//TODO:scales
        }

        @Override
        public String toString() {
            return boneName + ": " + this.getLocalRotation() + " " + this.getLocalTranslation();
        }
    }
}
