package com.jme3.renderer.camera;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.vulkan.util.SceneStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class ParallelCamera extends ExtensionCamera {

    protected static final int LEFT = 0;
    protected static final int RIGHT = 1;
    protected static final int BOTTOM = 2;
    protected static final int TOP = 3;
    protected static final int FAR = 4;
    protected static final int NEAR = 5;
    protected static final int NUM_PLANES = 6;

    protected final Frustum frustum = new Frustum();
    protected final Matrix4f viewMatrix = new Matrix4f();
    protected final Matrix4f projectionMatrix = new Matrix4f();
    protected final Matrix4f viewProjectionMatrix = new Matrix4f();

    protected boolean viewChanged = true;
    protected boolean projectionChanged = true;
    protected boolean viewProjChanged = true;

    public ParallelCamera() {
        super();
    }

    public ParallelCamera(Camera base) {
        super(base);
    }

    @Override
    public void setLocation(float x, float y, float z) {
        base.setLocation(x, y, z);
        viewChanged = true;
    }

    @Override
    public void setRotation(Quaternion rotation) {
        base.setRotation(rotation);
        viewChanged = true;
    }

    @Override
    public Matrix4f getViewMatrix() {
        updateView();
        return viewMatrix;
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        updateProjection();
        return projectionMatrix;
    }

    @Override
    public Matrix4f getViewProjectionMatrix() {
        updateViewProjection();
        return viewProjectionMatrix;
    }

    @Override
    public Camera.FrustumIntersect contains(BoundingVolume volume) {
        if (volume == null) {
            return Camera.FrustumIntersect.Inside;
        }
        int planeState = 0;
        updateView();
        Camera.FrustumIntersect result = Camera.FrustumIntersect.Inside;
        for (int i = 0; i < frustum.planes.length; i++) {
            int mask = 1 << i;
            if ((planeState & mask) == 0) switch (volume.whichSide(frustum.planes[i])) {
                case Positive: planeState |= mask; break;
                case Negative: {
                    volume.setCheckPlane(i);
                    return Camera.FrustumIntersect.Outside;
                }
                case None: result = Camera.FrustumIntersect.Intersects; break;
            }
        }
        return result;
    }

    @Override
    public SceneStack<FrustumIntersect> createCullStack() {
        return new Cull();
    }

    @Override
    public void setNearDistance(float near) {
        base.setNearDistance(near);
        projectionChanged = true;
    }

    @Override
    public void setFarDistance(float far) {
        base.setFarDistance(far);
        projectionChanged = true;
    }

    public void updateViewProjection() {
        updateView();
        if (viewProjChanged) {
            viewProjectionMatrix.set(getProjectionMatrix()).multLocal(getViewMatrix());
            viewProjChanged = false;
        }
    }

    public void updateProjection() {

        if (!projectionChanged) {
            return;
        }

        frustum.coeffLeft[0] = 1;
        frustum.coeffLeft[1] = 0;
        frustum.coeffRight[0] = -1;
        frustum.coeffRight[1] = 0;
        frustum.coeffBottom[0] = 1;
        frustum.coeffBottom[1] = 0;
        frustum.coeffTop[0] = -1;
        frustum.coeffTop[1] = 0;

        projectionMatrix.fromFrustum(getNearDistance(), getFarDistance(),
                frustum.left, frustum.right, frustum.top, frustum.bottom, true);
        projectionChanged = false;

        // for vulkan rendering
        projectionMatrix.flipYScalarForVulkan();

        // The frame is affected by the frustum values, update it as well
        viewChanged = true;
        viewProjChanged = true;
    }

    public void updateView() {

        updateProjection();

        if (!viewChanged) {
            return;
        }

        TempVars vars = TempVars.get();

        Vector3f left = getLeft(vars.vect1);
        Vector3f direction = getDirection(vars.vect2);
        Vector3f up = getUp(vars.vect3);

        float dirDotLocation = direction.dot(getLocation());

        // left plane
        Vector3f leftPlaneNormal = frustum.planes[LEFT].getNormal();
        leftPlaneNormal.x = left.x * frustum.coeffLeft[0];
        leftPlaneNormal.y = left.y * frustum.coeffLeft[0];
        leftPlaneNormal.z = left.z * frustum.coeffLeft[0];
        leftPlaneNormal.addLocal(direction.x * frustum.coeffLeft[1], direction.y
                * frustum.coeffLeft[1], direction.z * frustum.coeffLeft[1]);
        frustum.planes[LEFT].setConstant(getLocation().dot(leftPlaneNormal));

        // right plane
        Vector3f rightPlaneNormal = frustum.planes[RIGHT].getNormal();
        rightPlaneNormal.x = left.x * frustum.coeffRight[0];
        rightPlaneNormal.y = left.y * frustum.coeffRight[0];
        rightPlaneNormal.z = left.z * frustum.coeffRight[0];
        rightPlaneNormal.addLocal(direction.x * frustum.coeffRight[1], direction.y
                * frustum.coeffRight[1], direction.z * frustum.coeffRight[1]);
        frustum.planes[RIGHT].setConstant(getLocation().dot(rightPlaneNormal));

        // bottom plane
        Vector3f bottomPlaneNormal = frustum.planes[BOTTOM].getNormal();
        bottomPlaneNormal.x = up.x * frustum.coeffBottom[0];
        bottomPlaneNormal.y = up.y * frustum.coeffBottom[0];
        bottomPlaneNormal.z = up.z * frustum.coeffBottom[0];
        bottomPlaneNormal.addLocal(direction.x * frustum.coeffBottom[1], direction.y
                * frustum.coeffBottom[1], direction.z * frustum.coeffBottom[1]);
        frustum.planes[BOTTOM].setConstant(getLocation().dot(bottomPlaneNormal));

        // top plane
        Vector3f topPlaneNormal = frustum.planes[TOP].getNormal();
        topPlaneNormal.x = up.x * frustum.coeffTop[0];
        topPlaneNormal.y = up.y * frustum.coeffTop[0];
        topPlaneNormal.z = up.z * frustum.coeffTop[0];
        topPlaneNormal.addLocal(direction.x * frustum.coeffTop[1], direction.y
                * frustum.coeffTop[1], direction.z * frustum.coeffTop[1]);
        frustum.planes[TOP].setConstant(getLocation().dot(topPlaneNormal));

        frustum.planes[LEFT].setConstant(frustum.planes[LEFT].getConstant() + frustum.left);
        frustum.planes[RIGHT].setConstant(frustum.planes[RIGHT].getConstant() - frustum.right);
        frustum.planes[TOP].setConstant(frustum.planes[TOP].getConstant() - frustum.top);
        frustum.planes[BOTTOM].setConstant(frustum.planes[BOTTOM].getConstant() + frustum.bottom);

        // far plane
        frustum.planes[FAR].setNormal(left);
        frustum.planes[FAR].setNormal(-direction.x, -direction.y, -direction.z);
        frustum.planes[FAR].setConstant(-(dirDotLocation + getFarDistance()));

        // near plane
        frustum.planes[NEAR].setNormal(direction.x, direction.y, direction.z);
        frustum.planes[NEAR].setConstant(dirDotLocation + getNearDistance());

        viewMatrix.fromFrame(getLocation(), direction, up, left);
        viewChanged = false;

        vars.release();
        viewProjChanged = true;
    }

    public Frustum getFrustum() {
        return frustum;
    }

    public class Frustum {

        protected float left = -0.5f;
        protected float right = 0.5f;
        protected float top = 0.5f;
        protected float bottom = -0.5f;

        protected final Plane[] planes = new Plane[NUM_PLANES];
        protected int planeState = 0;

        protected final float[] coeffLeft = new float[2];
        protected final float[] coeffRight = new float[2];
        protected final float[] coeffBottom = new float[2];
        protected final float[] coeffTop = new float[2];
        
        private Frustum() {
            for (int i = 0; i < planes.length; i++) {
                planes[i] = new Plane();
            }
        }

        public void set(float near, float far, float left, float right, float top, float bottom) {
            base.setNearDistance(near);
            base.setFarDistance(far);
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            projectionChanged = true;
        }

        public void setHorizontal(float horizontal) {
            this.right = horizontal;
            this.left = -horizontal;
            projectionChanged = true;
        }

        public void setVertical(float vertical) {
            this.top = vertical;
            this.bottom = -vertical;
            projectionChanged = true;
        }

        public void setNear(float near) {
            setNearDistance(near);
        }

        public void setFar(float far) {
            setFarDistance(far);
        }

        public void setLeft(float left) {
            this.left = left;
            projectionChanged = true;
        }

        public void setRight(float right) {
            this.right = right;
            projectionChanged = true;
        }

        public void setTop(float top) {
            this.top = top;
            projectionChanged = true;
        }

        public void setBottom(float bottom) {
            this.bottom = bottom;
            projectionChanged = true;
        }

        public float getNear() {
            return base.getNearDistance();
        }

        public float getFar() {
            return base.getFarDistance();
        }

        public float getLeft() {
            return left;
        }

        public float getRight() {
            return right;
        }

        public float getTop() {
            return top;
        }

        public float getBottom() {
            return bottom;
        }

    }

    public class Cull implements SceneStack<Camera.FrustumIntersect> {

        private final Deque<SpatialCullState> intersect = new ArrayDeque<>();

        public Cull() {
            intersect.push(new SpatialCullState(FrustumIntersect.Intersects, Spatial.CullHint.Dynamic, 0));
        }

        @Override
        public FrustumIntersect push(Spatial spatial) {
            SpatialCullState parent = intersect.peek();
            Spatial.CullHint hint = spatial.getCullHint();
            if (hint == Spatial.CullHint.Inherit) {
                hint = parent.cullHint;
            }
            if (hint == Spatial.CullHint.Never) {
                return push(new SpatialCullState(FrustumIntersect.Intersects, hint, parent.planeState));
            } else if (hint == Spatial.CullHint.Always) {
                return push(new SpatialCullState(FrustumIntersect.Outside, hint, parent.planeState));
            } else if (parent.intersect != FrustumIntersect.Intersects) {
                return push(new SpatialCullState(parent.intersect, hint, parent.planeState));
            }
            updateView();
            BoundingVolume volume = spatial.getWorldBound();
            Camera.FrustumIntersect result = Camera.FrustumIntersect.Inside;
            int planeState = parent.planeState;
            planeLoop: for (int i = 0; i < frustum.planes.length; i++) {
                int mask = 1 << i;
                if ((planeState & mask) == 0) switch (volume.whichSide(frustum.planes[i])) {
                    case Positive: planeState |= mask; break;
                    case Negative: {
                        volume.setCheckPlane(i);
                        result = Camera.FrustumIntersect.Outside;
                        break planeLoop;
                    }
                    case None: result = Camera.FrustumIntersect.Intersects; break;
                }
            }
            intersect.push(new SpatialCullState(result, hint, planeState));
            return result;
        }

        @Override
        public FrustumIntersect pop() {
            return intersect.pop().intersect;
        }

        @Override
        public FrustumIntersect peek() {
            return intersect.isEmpty() ? null : intersect.peek().intersect;
        }

        @Override
        public void clear() {
            intersect.clear();
        }

        private FrustumIntersect push(SpatialCullState state) {
            intersect.push(state);
            return state.intersect;
        }

    }

    private static class SpatialCullState {

        public final FrustumIntersect intersect;
        public final Spatial.CullHint cullHint;
        public final int planeState;

        private SpatialCullState(FrustumIntersect intersect, Spatial.CullHint cullHint, int planeState) {
            this.intersect = intersect;
            this.cullHint = cullHint;
            this.planeState = planeState;
        }

    }

}
