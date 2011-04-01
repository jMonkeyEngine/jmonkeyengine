package com.jme3.bullet.control;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
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
    protected float weightThreshold = 1.0f;
    protected Spatial targetModel;
    protected Vector3f initPosition;
    protected Quaternion initRotation;
    protected Vector3f initScale;

//Normen: Think you have the system you want, with movement
//Normen: but the rootBone movement and translation is also accessible like getRootBoneLocation()
//Normen: and you can disable the applying of it
//Normen: setApplyRootBoneMovement(false)
//Normen: when you add a RigidBodyControl..
//Normen: it does this in setSpatial:
//Normen: if (spatial.getcontrol(AnimControl.class)){animControl.setApplyRootBoneMovement(false)
//Normen: and instead reads the current location and sets it to the RigidBody
//Normen: simply said
//Normen: update(){setPhysicsLocation(animControl.getRootBoneLocation())
    public RagdollControl() {
    }

    public RagdollControl(float weightThreshold) {
        this.weightThreshold = weightThreshold;
    }

    public void update(float tpf) {
        if (!enabled) {
            return;
        }

        TempVars vars = TempVars.get();
        assert vars.lock();
        Quaternion q2 = vars.quat1;
        //   skeleton.reset();
        for (PhysicsBoneLink link : boneLinks) {

//            if(link.bone==skeleton.getRoots()[0]){
//                  Vector3f loc=vars.vect1;
//                  loc.set(baseRigidBody.getMotionState().getWorldLocation());//.subtractLocal(rootBoneInit);
//                 ((Geometry)targetModel).setLocalTranslation(loc);
//                
//            }
            Vector3f p = link.rigidBody.getMotionState().getWorldLocation();
            Quaternion q = link.rigidBody.getMotionState().getWorldRotationQuat();

            q2.set(q).multLocal(link.initalWorldRotation).normalize();

            link.bone.setUserTransformsWorld(p, q2);

        }


        assert vars.unlock();

        //baseRigidBody.getMotionState().applyTransform(targetModel);

    }

    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSpatial(Spatial model) {
        targetModel = model;
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

        initPosition = model.getLocalTranslation();
        initRotation = model.getLocalRotation();
        initScale = model.getLocalScale();

        skeleton = animControl.getSkeleton();
        skeleton.resetAndUpdate();
        for (int i = 0; i < skeleton.getRoots().length; i++) {
            Bone childBone = skeleton.getRoots()[i];
            childBone.setUserControl(true);
            if (childBone.getParent() == null) {
                Vector3f parentPos = childBone.getModelSpacePosition().add(initPosition);
              //  Quaternion parentRot= childBone.getModelSpaceRotation().mult(initRotation);
                logger.log(Level.INFO, "Found root bone in skeleton {0}", skeleton);
                baseRigidBody = new PhysicsRigidBody(new BoxCollisionShape(Vector3f.UNIT_XYZ.mult(.1f)), 1);
                baseRigidBody.setPhysicsLocation(parentPos);
               // baseRigidBody.setPhysicsRotation(parentRot);
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
        //Allow bone's transformation change outside of animation
        bone.setUserControl(true);

        //get world space position of the bone
        Vector3f pos = bone.getModelSpacePosition().add(model.getLocalTranslation());
        Quaternion rot= bone.getModelSpaceRotation().mult(initRotation);

        //creating the collision shape from the bone's associated vertices
        PhysicsRigidBody shapeNode = new PhysicsRigidBody(makeShape(bone, model), 10.0f / (float) reccount);
        shapeNode.setPhysicsLocation(pos);
       // shapeNode.setPhysicsRotation(rot);

        PhysicsBoneLink link = new PhysicsBoneLink();
        link.bone = bone;
        link.rigidBody = shapeNode;
        link.initalWorldRotation = bone.getModelSpaceRotation().clone();

        //TODO: ragdoll mass 1
        if (parent != null) {
            //get joint position for parent
            Vector3f posToParent = new Vector3f();
            if (bone.getParent() != null) {
                bone.getModelSpacePosition().subtract(bone.getParent().getModelSpacePosition(), posToParent);
            }

            //Joint local position from parent
            link.pivotA = posToParent;
            //joint local position from current bone
            link.pivotB = new Vector3f(0, 0, 0f);

            ConeJoint joint = new ConeJoint(parent, shapeNode, link.pivotA, link.pivotB);
            //TODO make joints editable by user or find a way to correctly compute/import them
            joint.setLimit(FastMath.HALF_PI, FastMath.HALF_PI, 0.01f);

            link.joint = joint;
            joint.setCollisionBetweenLinkedBodys(false);
        }
        list.add(link);

        for (Iterator<Bone> it = bone.getChildren().iterator(); it.hasNext();) {
            Bone childBone = it.next();
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

    private HullCollisionShape makeShape(Bone bone, Spatial model) {
        int boneIndex = skeleton.getBoneIndex(bone);
        System.out.println(boneIndex);
        ArrayList<Float> points = new ArrayList<Float>();
        if (model instanceof Geometry) {
            Geometry g = (Geometry) model;
            points.addAll(getPoints(g.getMesh(), boneIndex, bone.getModelSpacePosition()));
        } else if (model instanceof Node) {
            Node node = (Node) model;
            for (Spatial s : node.getChildren()) {
                if (s instanceof Geometry) {
                    Geometry g = (Geometry) s;
                    points.addAll(getPoints(g.getMesh(), boneIndex, bone.getModelSpacePosition()));
                }
            }
        }
        float[] p = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            p[i] = points.get(i);
        }

        return new HullCollisionShape(p);
    }

    protected List<Float> getPoints(Mesh mesh, int boneIndex, Vector3f offset) {

        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        ByteBuffer boneIndices = (ByteBuffer) mesh.getBuffer(Type.BoneIndex).getData();
        FloatBuffer boneWeight = (FloatBuffer) mesh.getBuffer(Type.BoneWeight).getData();

        vertices.rewind();
        boneIndices.rewind();
        boneWeight.rewind();

        ArrayList<Float> results = new ArrayList<Float>();

        int vertexComponents = mesh.getVertexCount() * 3;
        for (int i = 0; i < vertexComponents; i += 3) {
            int k;
            boolean add = false;
            int start = i / 3 * 4;
            for (k = start; k < start + 4; k++) {
                if (boneIndices.get(k) == boneIndex && boneWeight.get(k) >= weightThreshold) {
                    add = true;
                    break;
                }
            }
            if (add) {
                Vector3f pos = new Vector3f();
                pos.x = vertices.get(i);
                pos.y = vertices.get(i + 1);
                pos.z = vertices.get(i + 2);
                pos.subtractLocal(offset);
                results.add(pos.x);
                results.add(pos.y);
                results.add(pos.z);
            }
        }
        return results;
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
        if (!enabled && space != null) {
            removeFromPhysicsSpace();
        } else if (enabled && space != null) {
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

    protected static class PhysicsBoneLink {

        Bone bone;
        Quaternion initalWorldRotation;
        PhysicsJoint joint;
        PhysicsRigidBody rigidBody;
        Vector3f pivotA;
        Vector3f pivotB;
    }
}
