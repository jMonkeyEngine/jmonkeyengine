/*
 * To change this template;choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.vehiclecreator;

import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;

/**
 *
 * @author normenhansen
 */
public class SuspensionSettings {

    private float boundingScale = 0;
    private boolean createNode = true;
    private Vector3f direction = new Vector3f(0, -1, 0);
    private Vector3f axle = new Vector3f(-1, 0, 0);
    private boolean frontWheel = false;
    private float radius = 0;
    private float restLength = 1;
    private float maxForce = 6000;
    private float stiffness = 50;
    private float rollInfluence = 1.0f;
    private float compression = 0.83f;
    private float release = 0.88f;
    private float friction = 10.5f;

    public SuspensionSettings() {
    }

    public SuspensionSettings(VehicleWheel wheel) {
        this.direction.set(wheel.getDirection());
        this.axle.set(wheel.getAxle());
        this.frontWheel = wheel.isFrontWheel();
        this.radius = wheel.getRadius();
        this.restLength = wheel.getRestLength();
        this.maxForce = wheel.getMaxSuspensionForce();
        this.stiffness = wheel.getSuspensionStiffness();
        this.rollInfluence = wheel.getRollInfluence();
        this.compression = wheel.getWheelsDampingCompression();
        this.release = wheel.getWheelsDampingRelaxation();
        this.friction = wheel.getFrictionSlip();
    }

    public void applyData(VehicleWheel wheel){
        wheel.setRadius(getRadius());
        wheel.setFrictionSlip(getFriction());
        wheel.setRollInfluence(getRollInfluence());
        wheel.setMaxSuspensionForce(getMaxForce());
        wheel.setSuspensionStiffness(getStiffness());
        wheel.setWheelsDampingCompression(getCompression());
        wheel.setWheelsDampingRelaxation(getRelease());
    }

    public float getBoundingScale() {
        return boundingScale;
    }

    public void setBoundingScale(float boundingScale) {
        this.boundingScale = boundingScale;
    }

    /**
     * @return the createNode
     */
    public boolean isCreateNode() {
        return createNode;
    }

    /**
     * @param createNode the createNode to set
     */
    public void setCreateNode(boolean createNode) {
        this.createNode = createNode;
    }

    /**
     * @return the direction
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    /**
     * @return the axle
     */
    public Vector3f getAxle() {
        return axle;
    }

    /**
     * @param axle the axle to set
     */
    public void setAxle(Vector3f axle) {
        this.axle = axle;
    }

    /**
     * @return the frontWheel
     */
    public boolean isFrontWheel() {
        return frontWheel;
    }

    /**
     * @param frontWheel the frontWheel to set
     */
    public void setFrontWheel(boolean frontWheel) {
        this.frontWheel = frontWheel;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getRestLength() {
        return restLength;
    }

    public void setRestLength(float restLength) {
        this.restLength = restLength;
    }

    public float getMaxForce() {
        return maxForce;
    }

    public void setMaxForce(float maxForce) {
        this.maxForce = maxForce;
    }

    public float getStiffness() {
        return stiffness;
    }

    public void setStiffness(float stiffness) {
        this.stiffness = stiffness;
    }

    public float getRollInfluence() {
        return rollInfluence;
    }

    public void setRollInfluence(float rollInfluence) {
        this.rollInfluence = rollInfluence;
    }

    public float getCompression() {
        return compression;
    }

    public void setCompression(float compression) {
        this.compression = compression;
    }

    public float getRelease() {
        return release;
    }

    public void setRelease(float release) {
        this.release = release;
    }

    public float getFriction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }
}
