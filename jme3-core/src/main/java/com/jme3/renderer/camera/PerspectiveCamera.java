package com.jme3.renderer.camera;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.util.TempVars;

public class PerspectiveCamera extends ParallelCamera {

    public PerspectiveCamera() {
        super();
    }

    public PerspectiveCamera(Camera base) {
        super(base);
    }

    @Override
    public void updateProjection() {

        if (!projectionChanged) {
            return;
        }

        float near = getNearDistance();
        float nearSquared = near * near;
        float leftSquared = frustum.left * frustum.left;
        float rightSquared = frustum.right * frustum.right;
        float bottomSquared = frustum.bottom * frustum.bottom;
        float topSquared = frustum.top * frustum.top;

        float inverseLength = FastMath.invSqrt(nearSquared + leftSquared);
        frustum.coeffLeft[0] = -near * inverseLength;
        frustum.coeffLeft[1] = -frustum.left * inverseLength;

        inverseLength = FastMath.invSqrt(nearSquared + rightSquared);
        frustum.coeffRight[0] = near * inverseLength;
        frustum.coeffRight[1] = frustum.right * inverseLength;

        inverseLength = FastMath.invSqrt(nearSquared + bottomSquared);
        frustum.coeffBottom[0] = near * inverseLength;
        frustum.coeffBottom[1] = -frustum.bottom * inverseLength;

        inverseLength = FastMath.invSqrt(nearSquared + topSquared);
        frustum.coeffTop[0] = -near * inverseLength;
        frustum.coeffTop[1] = frustum.top * inverseLength;

        projectionMatrix.fromFrustum(near, getFarDistance(),
                frustum.left, frustum.right, frustum.top, frustum.bottom, false);
        projectionChanged = false;

        // for vulkan rendering
        projectionMatrix.flipYScalarForVulkan();

        // The frame is affected by the frustum values, update it as well
        viewChanged = true;
        viewProjChanged = true;
    }

    @Override
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

    public void setFov(float fov, float aspect) {
        if (fov <= 0) {
            throw new IllegalArgumentException("Field of view must be greater than 0");
        }
        float h = FastMath.tan(fov * FastMath.DEG_TO_RAD * .5f) * getNearDistance();
        float w = h * aspect;
        frustum.left = -w;
        frustum.right = w;
        frustum.bottom = -h;
        frustum.top = h;
        projectionChanged = true;
    }

    public float getFov() {
        float fovY = frustum.top / getNearDistance();
        fovY = FastMath.atan(fovY);
        fovY /= 0.5F * FastMath.DEG_TO_RAD;
        return fovY;
    }

    public void setPerspective(float fov, float aspect, float near, float far) {
        float h = FastMath.tan(fov * FastMath.DEG_TO_RAD * .5f) * near;
        float w = h * aspect;
        frustum.left = -w;
        frustum.right = w;
        frustum.bottom = -h;
        frustum.top = h;
        setNearDistance(near);
        setFarDistance(far);
        projectionChanged = true;
    }

}
