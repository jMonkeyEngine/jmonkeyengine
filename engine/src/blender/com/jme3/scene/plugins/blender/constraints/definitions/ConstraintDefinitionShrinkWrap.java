package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Shrink wrap' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionShrinkWrap extends ConstraintDefinition {

    public ConstraintDefinitionShrinkWrap(Structure constraintData, BlenderContext blenderContext) {
        super(constraintData, blenderContext);
    }

    @Override
    public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
        // loading mesh points (blender ensures that the target is a mesh-object)
        /*
         * List<Vector3f> pts = new ArrayList<Vector3f>();
         * Node target = (Node) this.target.getObject();
         * for(Spatial spatial : target.getChildren()) {
         * if(spatial instanceof Geometry) {
         * Mesh mesh = ((Geometry) spatial).getMesh();
         * FloatBuffer floatBuffer = mesh.getFloatBuffer(Type.Position);
         * for(int i=0;i<floatBuffer.limit();i+=3) {
         * pts.add(new Vector3f(floatBuffer.get(i), floatBuffer.get(i + 1), floatBuffer.get(i + 2)));
         * }
         * }
         * }
         * AnimData animData = blenderContext.getAnimData(this.owner.getOma());
         * if(animData != null) {
         * Object owner = this.owner.getObject();
         * for(Animation animation : animData.anims) {
         * BlenderTrack track = this.getTrack(owner, animData.skeleton, animation);
         * Vector3f[] translations = track.getTranslations();
         * Quaternion[] rotations = track.getRotations();
         * int maxFrames = translations.length;
         * for (int frame = 0; frame < maxFrames; ++frame) {
         * Vector3f currentTranslation = translations[frame];
         * //looking for minimum distanced point
         * Vector3f minDistancePoint = null;
         * float distance = Float.MAX_VALUE;
         * for(Vector3f p : pts) {
         * float temp = currentTranslation.distance(p);
         * if(temp < distance) {
         * distance = temp;
         * minDistancePoint = p;
         * }
         * }
         * translations[frame] = minDistancePoint.clone();
         * }
         * track.setKeyframes(track.getTimes(), translations, rotations, track.getScales());
         * }
         * }
         */

        // TODO: static constraint for spatials
    }
}
