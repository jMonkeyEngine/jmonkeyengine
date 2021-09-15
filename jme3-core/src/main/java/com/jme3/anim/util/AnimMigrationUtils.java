package com.jme3.anim.util;

import com.jme3.anim.*;
import com.jme3.animation.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;

import java.util.*;

public class AnimMigrationUtils {

    final private static AnimControlVisitor animControlVisitor = new AnimControlVisitor();
    final private static SkeletonControlVisitor skeletonControlVisitor = new SkeletonControlVisitor();

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private AnimMigrationUtils() {
    }

    public static Spatial migrate(Spatial source) {
        Map<Skeleton, Armature> skeletonArmatureMap = new HashMap<>();
        animControlVisitor.setMappings(skeletonArmatureMap);
        source.depthFirstTraversal(animControlVisitor);
        skeletonControlVisitor.setMappings(skeletonArmatureMap);
        source.depthFirstTraversal(skeletonControlVisitor);
        return source;
    }

    private static class AnimControlVisitor implements SceneGraphVisitor {

        Map<Skeleton, Armature> skeletonArmatureMap;

        @Override
        public void visit(Spatial spatial) {
            AnimControl control = spatial.getControl(AnimControl.class);
            if (control != null) {
                AnimComposer composer = new AnimComposer();
                Skeleton skeleton = control.getSkeleton();
                if (skeleton == null) {
                    //only bone anim for now
                    return;
                }

                Joint[] joints = new Joint[skeleton.getBoneCount()];
                for (int i = 0; i < skeleton.getBoneCount(); i++) {
                    Bone b = skeleton.getBone(i);
                    Joint j = joints[i];
                    if (j == null) {
                        j = fromBone(b);
                        joints[i] = j;
                    }
                    for (Bone bone : b.getChildren()) {
                        int index = skeleton.getBoneIndex(bone);
                        Joint joint = joints[index];
                        if (joint == null) {
                            joint = fromBone(bone);
                        }
                        j.addChild(joint);
                        joints[index] = joint;
                    }
                }

                Armature armature = new Armature(joints);
                armature.saveBindPose();
                skeletonArmatureMap.put(skeleton, armature);

                List<TransformTrack> tracks = new ArrayList<>();

                for (String animName : control.getAnimationNames()) {
                    tracks.clear();
                    Animation anim = control.getAnim(animName);
                    AnimClip clip = new AnimClip(animName);
                    Joint[] staticJoints = new Joint[joints.length];

                    System.arraycopy(joints, 0, staticJoints, 0, joints.length);
                    for (Track track : anim.getTracks()) {
                        if (track instanceof BoneTrack) {
                            BoneTrack boneTrack = (BoneTrack) track;
                            int index = boneTrack.getTargetBoneIndex();
                            Bone bone = skeleton.getBone(index);
                            Joint joint = joints[index];
                            TransformTrack jointTrack = fromBoneTrack(boneTrack, bone, joint);
                            tracks.add(jointTrack);
                            //this joint is animated let's remove it from the static joints
                            staticJoints[index] = null;
                        }
                        //TODO spatial tracks , Effect tracks, Audio tracks
                    }

                    for (int i = 0; i < staticJoints.length; i++) {
                        padJointTracks(tracks, staticJoints[i]);
                    }

                    clip.setTracks(tracks.toArray(new TransformTrack[tracks.size()]));

                    composer.addAnimClip(clip);
                }
                spatial.removeControl(control);
                spatial.addControl(composer);
            }
        }

        public void setMappings(Map<Skeleton, Armature> skeletonArmatureMap) {
            this.skeletonArmatureMap = skeletonArmatureMap;
        }
    }

    public static void padJointTracks(List<TransformTrack> tracks, Joint staticJoint) {
        Joint j = staticJoint;
        if (j != null) {
            // joint has no track , we create one with the default pose
            float[] times = new float[]{0};
            Vector3f[] translations = new Vector3f[]{j.getLocalTranslation()};
            Quaternion[] rotations = new Quaternion[]{j.getLocalRotation()};
            Vector3f[] scales = new Vector3f[]{j.getLocalScale()};
            TransformTrack track = new TransformTrack(j, times, translations, rotations, scales);
            tracks.add(track);
        }
    }

    private static class SkeletonControlVisitor implements SceneGraphVisitor {

        Map<Skeleton, Armature> skeletonArmatureMap;

        @Override
        public void visit(Spatial spatial) {
            SkeletonControl control = spatial.getControl(SkeletonControl.class);
            if (control != null) {
                Armature armature = skeletonArmatureMap.get(control.getSkeleton());
                SkinningControl skinningControl = new SkinningControl(armature);
                Map<String, List<Spatial>> attachedSpatials = new HashMap<>();
                for (int i = 0; i < control.getSkeleton().getBoneCount(); i++) {
                    Bone b = control.getSkeleton().getBone(i);
                    Node n = control.getAttachmentsNode(b.getName());
                    n.removeFromParent();
                    if (!n.getChildren().isEmpty()) {
                        attachedSpatials.put(b.getName(), n.getChildren());
                    }
                }
                spatial.removeControl(control);
                spatial.addControl(skinningControl);
                for (String name : attachedSpatials.keySet()) {
                    List<Spatial> spatials = attachedSpatials.get(name);
                    for (Spatial child : spatials) {
                        skinningControl.getAttachmentsNode(name).attachChild(child);
                    }
                }

            }
        }

        public void setMappings(Map<Skeleton, Armature> skeletonArmatureMap) {
            this.skeletonArmatureMap = skeletonArmatureMap;
        }
    }

    public static TransformTrack fromBoneTrack(BoneTrack boneTrack, Bone bone, Joint joint) {
        float[] times = new float[boneTrack.getTimes().length];
        int length = times.length;
        System.arraycopy(boneTrack.getTimes(), 0, times, 0, length);
        //translation
        Vector3f[] translations = new Vector3f[length];
        if (boneTrack.getTranslations() != null) {
            for (int i = 0; i < boneTrack.getTranslations().length; i++) {
                Vector3f oldTrans = boneTrack.getTranslations()[i];
                Vector3f newTrans = new Vector3f();
                newTrans.set(bone.getBindPosition()).addLocal(oldTrans);
                translations[i] = newTrans;
            }
        }
        //rotation
        Quaternion[] rotations = new Quaternion[length];
        if (boneTrack.getRotations() != null) {
            for (int i = 0; i < boneTrack.getRotations().length; i++) {
                Quaternion oldRot = boneTrack.getRotations()[i];
                Quaternion newRot = new Quaternion();
                newRot.set(bone.getBindRotation()).multLocal(oldRot);
                rotations[i] = newRot;
            }
        }
        //scale
        Vector3f[] scales = new Vector3f[length];
        if (boneTrack.getScales() != null) {
            for (int i = 0; i < boneTrack.getScales().length; i++) {
                Vector3f oldScale = boneTrack.getScales()[i];
                Vector3f newScale = new Vector3f();
                newScale.set(bone.getBindScale()).multLocal(oldScale);
                scales[i] = newScale;
            }
        }
        TransformTrack t = new TransformTrack(joint, times, translations, rotations, scales);
        return t;
    }

    private static Joint fromBone(Bone b) {
        Joint j = new Joint(b.getName());
        j.setLocalTranslation(b.getBindPosition());
        j.setLocalRotation(b.getBindRotation());
        j.setLocalScale(b.getBindScale());
        return j;
    }

}
