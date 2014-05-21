package com.jme3.scene.plugins.blender.animations;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * An implementation of bone envelope. Used when assigning bones to the mesh by envelopes.
 * 
 * @author Marcin Roguski
 */
public class BoneEnvelope {
    /** A defined distance that will be included in the envelope space. */
    private float    distance;
    /** The bone's weight. */
    private float    weight;
    /** The radius of the bone's head. */
    private float    boneHeadRadius;
    /** The radius of the bone's tail. */
    private float    boneTailRadius;
    /** Head position in rest pose in world space. */
    private Vector3f head;
    /** Tail position in rest pose in world space. */
    private Vector3f tail;

    /**
     * The constructor of bone envelope. It reads all the needed data. Take notice that the positions of head and tail
     * are computed in the world space and that the points' positions given for computations should be in world space as well.
     * 
     * @param boneStructure
     *            the blender bone structure
     * @param armatureWorldMatrix
     *            the world matrix of the armature object
     * @param fixUpAxis
     *            a variable that tells if we use the Y-is up axis orientation
     */
    @SuppressWarnings("unchecked")
    public BoneEnvelope(Structure boneStructure, Matrix4f armatureWorldMatrix, boolean fixUpAxis) {
        distance = ((Number) boneStructure.getFieldValue("dist")).floatValue();
        weight = ((Number) boneStructure.getFieldValue("weight")).floatValue();
        boneHeadRadius = ((Number) boneStructure.getFieldValue("rad_head")).floatValue();
        boneTailRadius = ((Number) boneStructure.getFieldValue("rad_tail")).floatValue();

        DynamicArray<Number> headArray = (DynamicArray<Number>) boneStructure.getFieldValue("arm_head");
        head = new Vector3f(headArray.get(0).floatValue(), headArray.get(1).floatValue(), headArray.get(2).floatValue());
        if (fixUpAxis) {
            float z = head.z;
            head.z = -head.y;
            head.y = z;
        }
        armatureWorldMatrix.mult(head, head);// move the head point to global space

        DynamicArray<Number> tailArray = (DynamicArray<Number>) boneStructure.getFieldValue("arm_tail");
        tail = new Vector3f(tailArray.get(0).floatValue(), tailArray.get(1).floatValue(), tailArray.get(2).floatValue());
        if (fixUpAxis) {
            float z = tail.z;
            tail.z = -tail.y;
            tail.y = z;
        }
        armatureWorldMatrix.mult(tail, tail);// move the tail point to global space
    }

    /**
     * The method verifies if the given point is inside the envelope.
     * @param point
     *            the point in 3D space (MUST be in a world coordinate space)
     * @return <b>true</b> if the point is inside the envelope and <b>false</b> otherwise
     */
    public boolean isInEnvelope(Vector3f point) {
        Vector3f v = tail.subtract(head);
        float boneLength = v.length();
        v.normalizeLocal();

        // computing a plane that contains 'point' and v is its normal vector
        // the plane's equation is: Ax + By + Cz + D = 0, where v = [A, B, C]
        float D = -v.dot(point);

        // computing a point where a line that contains head and tail crosses the plane
        float temp = -(v.dot(head) + D) / v.dot(v);
        Vector3f p = head.add(v.x * temp, v.y * temp, v.z * temp);

        // determining if the point p is on the same or other side of head than the tail point
        Vector3f headToPointOnLineVector = p.subtract(head);
        float headToPointLength = headToPointOnLineVector.length();
        float cosinus = headToPointOnLineVector.dot(v) / headToPointLength;// the length of v is already = 1; cosinus should be either 1, 0 or -1
        if (cosinus < 0 && headToPointLength > boneHeadRadius || headToPointLength > boneLength + boneTailRadius) {
            return false;// the point is outside the anvelope
        }

        // now check if the point is inside and envelope
        float pointDistanceFromLine = point.subtract(p).length(), maximumDistance = 0;
        if (cosinus < 0) {
            // checking if the distance from p to point is inside the half sphere defined by head envelope
            // compute the distance from the line to the half sphere border
            maximumDistance = boneHeadRadius;
        } else if (headToPointLength < boneLength) {
            // compute the maximum available distance
            if (boneTailRadius > boneHeadRadius) {
                // compute the distance from head to p
                float headToPDistance = p.subtract(head).length();
                // from tangens function we have
                float x = headToPDistance * ((boneTailRadius - boneHeadRadius) / boneLength);
                maximumDistance = x + boneHeadRadius;
            } else if (boneTailRadius < boneHeadRadius) {
                // compute the distance from head to p
                float tailToPDistance = p.subtract(tail).length();
                // from tangens function we have
                float x = tailToPDistance * ((boneHeadRadius - boneTailRadius) / boneLength);
                maximumDistance = x + boneTailRadius;
            } else {
                maximumDistance = boneTailRadius;
            }
        } else {
            // checking if the distance from p to point is inside the half sphere defined by tail envelope
            maximumDistance = boneTailRadius;
        }

        return pointDistanceFromLine <= maximumDistance + distance;
    }

    /**
     * @return the weight of the bone
     */
    public float getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "BoneEnvelope [d=" + distance + ", w=" + weight + ", hr=" + boneHeadRadius + ", tr=" + boneTailRadius + ", (" + head + ") -> (" + tail + ")]";
    }
}
