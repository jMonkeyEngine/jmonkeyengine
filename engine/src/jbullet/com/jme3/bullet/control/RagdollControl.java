package com.jme3.bullet.control;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RagdollControl implements PhysicsControl {

    protected static final Logger logger = Logger.getLogger(RagdollControl.class.getName());
    protected List<PhysicsBoneLink> boneLinks = new LinkedList<PhysicsBoneLink>();
    protected Skeleton skeleton;
    protected PhysicsSpace space;
    protected boolean enabled = true;
    protected boolean debug = false;
    protected Quaternion tmp_jointRotation = new Quaternion();
    protected PhysicsRigidBody baseRigidBody;

    public RagdollControl() {
    }

    public void update(float tpf) {
        if (!enabled) {
            return;
        }
        TempVars vars = TempVars.get();
        assert vars.lock();

        skeleton.reset();
        for (PhysicsBoneLink link : boneLinks) {
            Vector3f p = link.rigidBody.getMotionState().getWorldLocation();
            Quaternion q = link.rigidBody.getMotionState().getWorldRotationQuat();

            q.toAxes(vars.tri);

            Vector3f dir = vars.tri[2];
            float len = link.length;

            Vector3f parentPos = new Vector3f(p).subtractLocal(dir.mult(len / 2f));
            Vector3f childPos = new Vector3f(p).addLocal(dir.mult(len / 2f));

            Quaternion q2 = q.clone();
            Quaternion rot = new Quaternion();
            rot.fromAngles(FastMath.HALF_PI, 0, 0);
            q2.multLocal(rot);
            q2.normalize();

            link.parentBone.setUserTransformsWorld(parentPos, q2);
            if (link.childBone.getChildren().size() == 0) {
                link.childBone.setUserTransformsWorld(childPos, q2.clone());
            }
        }
        assert vars.unlock();
    }

    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSpatial(Spatial model) {
        removeFromPhysicsSpace();
        clearData();
        // put into bind pose and compute bone transforms in model space
        // maybe dont reset to ragdoll out of animations?
        scanSpatial(model);

        logger.log(Level.INFO, "Create physics ragdoll for skeleton {0}", skeleton);
    }

    public void addBoneName(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void scanSpatial(Spatial model) {
        AnimControl animControl = model.getControl(AnimControl.class);

        skeleton = animControl.getSkeleton();
        skeleton.resetAndUpdate();
        for (int i = 0; i < skeleton.getBoneCount(); i++) {
            Bone childBone = skeleton.getBone(i);
            childBone.setUserControl(true);
            if (childBone.getParent() == null) {
                Vector3f parentPos = childBone.getModelSpacePosition().add(model.getWorldTranslation());
                logger.log(Level.INFO, "Found root bone in skeleton {0}", skeleton);
                baseRigidBody = new PhysicsRigidBody(new BoxCollisionShape(Vector3f.UNIT_XYZ.mult(.1f)), 1);
                baseRigidBody.setPhysicsLocation(parentPos);
                boneLinks = boneRecursion(model, childBone, baseRigidBody, boneLinks, 1);
                return;
            }

        }
//        BoneAnimation myAnimation = new BoneAnimation("boneAnimation", 1000000);
//        myAnimation.setTracks(new BoneTrack[0]);
//        animControl.addAnim(myAnimation);
//        animControl.createChannel().setAnim("boneAnimation");

    }

    private List<PhysicsBoneLink> boneRecursion(Spatial model, Bone bone, PhysicsRigidBody parent, List<PhysicsBoneLink> list, int reccount) {
        ArrayList<Bone> children = bone.getChildren();
        bone.setUserControl(true);
        for (Iterator<Bone> it = children.iterator(); it.hasNext();) {
            Bone childBone = it.next();
            Bone parentBone = bone;
            Vector3f parentPos = parentBone.getModelSpacePosition().add(model.getWorldTranslation());
            Vector3f childPos = childBone.getModelSpacePosition().add(model.getWorldTranslation());
            //get location between the two bones (physicscapsule center)
            Vector3f jointCenter = parentPos.add(childPos).multLocal(0.5f);
            tmp_jointRotation.lookAt(childPos.subtract(parentPos), Vector3f.UNIT_Y);
            // length of the joint
            float height = parentPos.distance(childPos);

            // TODO: joints act funny when bone is too thin??
            float radius = height > 2f ? 0.4f : height * .2f;
            CapsuleCollisionShape shape = new CapsuleCollisionShape(radius, height - (radius), 2);

            PhysicsRigidBody shapeNode = new PhysicsRigidBody(shape, 10.0f / (float) reccount);
            shapeNode.setPhysicsLocation(jointCenter);
            shapeNode.setPhysicsRotation(tmp_jointRotation.toRotationMatrix());

            PhysicsBoneLink link = new PhysicsBoneLink();
            link.parentBone = parentBone;
            link.childBone = childBone;
            link.rigidBody = shapeNode;
            link.length = height;

            //TODO: ragdoll mass 1
            if (parent != null) {
                //get length of parent
                float parentHeight = 0.0f;
                if (bone.getParent() != null) {
                    parentHeight = bone.getParent().getModelSpacePosition().add(model.getWorldTranslation()).distance(parentPos);
                }
                //local position from parent
                link.pivotA = new Vector3f(0, 0, (parentHeight * .5f));
                //local position from child
                link.pivotB = new Vector3f(0, 0, -(height * .5f));

                ConeJoint joint = new ConeJoint(parent, shapeNode, link.pivotA, link.pivotB);
                joint.setLimit(FastMath.HALF_PI, FastMath.HALF_PI, 0.01f);

                link.joint = joint;
                joint.setCollisionBetweenLinkedBodys(false);
            }
            list.add(link);
            boneRecursion(model, childBone, shapeNode, list, reccount++);
        }
        return list;
    }

    private void clearData() {
        boneLinks.clear();
        baseRigidBody = null;
    }

    private void addToPhysicsSpace() {
        if (baseRigidBody != null) {
            space.add(baseRigidBody);
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.rigidBody != null) {
                space.add(physicsBoneLink.rigidBody);
            }
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.joint != null) {
                space.add(physicsBoneLink.joint);
            }
        }
    }

    private void removeFromPhysicsSpace() {
        if (baseRigidBody != null) {
            space.remove(baseRigidBody);
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.joint != null) {
                space.remove(physicsBoneLink.joint);
            }
        }
        for (Iterator<PhysicsBoneLink> it = boneLinks.iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            if (physicsBoneLink.rigidBody != null) {
                space.remove(physicsBoneLink.rigidBody);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if(!enabled&&space!=null){
            removeFromPhysicsSpace();
        }else if(enabled && space!=null){
            addToPhysicsSpace();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void attachDebugShape(AssetManager manager) {
        for (Iterator<PhysicsBoneLink> it = boneLinks.iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            physicsBoneLink.rigidBody.attachDebugShape(manager);
        }
        debug = true;
    }

    public void detachDebugShape() {
        for (Iterator<PhysicsBoneLink> it = boneLinks.iterator(); it.hasNext();) {
            PhysicsBoneLink physicsBoneLink = it.next();
            physicsBoneLink.rigidBody.detachDebugShape();
        }
        debug = false;
    }

    public void render(RenderManager rm, ViewPort vp) {
        if (enabled && space != null && space.getDebugManager() != null) {
            if (!debug) {
                attachDebugShape(space.getDebugManager());
            }
            for (Iterator<PhysicsBoneLink> it = boneLinks.iterator(); it.hasNext();) {
                PhysicsBoneLink physicsBoneLink = it.next();
                Spatial debugShape = physicsBoneLink.rigidBody.debugShape();
                if (debugShape != null) {
                    debugShape.setLocalTranslation(physicsBoneLink.rigidBody.getMotionState().getWorldLocation());
                    debugShape.setLocalRotation(physicsBoneLink.rigidBody.getMotionState().getWorldRotationQuat());
                    debugShape.updateGeometricState();
                    rm.renderScene(debugShape, vp);
                }
            }
        }
    }

    public void setPhysicsSpace(PhysicsSpace space) {
        if (space == null) {
            removeFromPhysicsSpace();
            this.space = space;
        } else {
            if (this.space == space) {
                return;
            }
            this.space = space;
            addToPhysicsSpace();
        }
    }

    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class PhysicsBoneLink {

        Bone childBone;
        Bone parentBone;
        PhysicsJoint joint;
        PhysicsRigidBody rigidBody;
        Vector3f pivotA;
        Vector3f pivotB;
        float length;
    }
}
