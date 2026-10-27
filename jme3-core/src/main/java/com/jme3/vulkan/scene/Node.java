package com.jme3.vulkan.scene;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.material.experimental.Material;
import com.jme3.vulkan.scene.components.Component;
import com.jme3.vulkan.scene.components.Property;

import java.util.Set;

public class Node {

    private final Scene scene;
    private final int id;

    public Node(Scene scene, int id) {
        this.scene = scene;
        this.id = id;
    }

    private void requireSameScene(Node n) {
        assert scene == n.scene : "Nodes must belong to the same scene.";
    }

    public Scene getScene() {
        return scene;
    }

    public int getId() {
        return id;
    }

    public int getGeometricId() {
        return scene.getGeometricId(id);
    }

    public void addMaterial(Material material) {
        scene.addMaterial(id, material);
    }

    public void removeMaterial(Material material) {
        scene.removeMaterial(id, material);
    }

    public void removeMaterial(Class<? extends Material> type) {
        scene.removeMaterial(id, type);
    }

    public void clearMaterials() {
        scene.clearMaterials(id);
    }

    public <T> T getProperty(Class<? extends Property<T>> type) {
        return scene.getComponent(type).get(id);
    }

    /**
     * Gets this node's value for the property type {@code type}, or {@code defaultValue}
     * if this node has no value for it (it was never set, or was cleared).
     *
     * @param type property component type
     * @param defaultValue value to return if this node has no value for {@code type}
     * @return this node's value for {@code type}, or {@code defaultValue}
     * @param <T> property value type
     */
    public <T> T getProperty(Class<? extends Property<T>> type, T defaultValue) {
        return scene.getProperty(type, id, defaultValue);
    }

    public Mesh getMesh() {
        return scene.getComponent(Scene.MeshType.class, id);
    }

    /*=================*\
    |     TRANSFORM     |
    \*=================*/

    public void setTranslation(Vector3f translation) {
        scene.setTranslation(id, translation);
    }

    public void setTranslation(float x, float y, float z) {
        scene.setTranslation(id, x, y, z);
    }

    public void move(Vector3f translation) {
        scene.move(id, translation);
    }

    public void move(float x, float y, float z) {
        scene.move(id, x, y, z);
    }

    public void setRotation(Quaternion rotation) {
        scene.setRotation(id, rotation);
    }

    public void rotate(Quaternion rotation) {
        scene.rotate(id, rotation);
    }

    public void setScale(Vector3f scale) {
        scene.setScale(id, scale);
    }

    public void setScale(float x, float y, float z) {
        scene.setScale(id, x, y, z);
    }

    public void setScale(float scale) {
        scene.setScale(id, scale);
    }

    public void scale(Vector3f scale) {
        scene.scale(id, scale);
    }

    public void scale(float x, float y, float z) {
        scene.scale(id, x, y, z);
    }

    public void scale(float scale) {
        scene.scale(id, scale);
    }

    public Vector3f getLocalTranslation() {
        return scene.getLocalTranslation(id, null);
    }

    public Vector3f getLocalTranslation(Vector3f store) {
        return scene.getLocalTranslation(id, store);
    }

    public Quaternion getLocalRotation() {
        return scene.getLocalRotation(id, null);
    }

    public Quaternion getLocalRotation(Quaternion store) {
        return scene.getLocalRotation(id, store);
    }

    public Vector3f getLocalScale() {
        return scene.getLocalScale(id, null);
    }

    public Vector3f getLocalScale(Vector3f store) {
        return scene.getLocalScale(id, store);
    }

    public Transform getLocalTransform() {
        return scene.getLocalTransform(id, null);
    }

    public Transform getLocalTransform(Transform store) {
        return scene.getLocalTransform(id, store);
    }

    public Vector3f getWorldTranslation() {
        return scene.getWorldTranslation(id, null);
    }

    public Vector3f getWorldTranslation(Vector3f store) {
        return scene.getWorldTranslation(id, store);
    }

    public Quaternion getWorldRotation() {
        return scene.getWorldRotation(id, null);
    }

    public Quaternion getWorldRotation(Quaternion store) {
        return scene.getWorldRotation(id, store);
    }

    public Vector3f getWorldScale() {
        return scene.getWorldScale(id, null);
    }

    public Vector3f getWorldScale(Vector3f store) {
        return scene.getWorldScale(id, store);
    }

    public Transform getWorldTransform() {
        return scene.getWorldTransform(id, null);
    }

    public Transform getWorldTransform(Transform store) {
        return scene.getWorldTransform(id, store);
    }

    public BoundingBox getWorldBounds() {
        return scene.getWorldBounds(id, null);
    }

    public BoundingBox getWorldBounds(BoundingBox store) {
        return scene.getWorldBounds(id, store);
    }

    /*=================*\
    |      LIGHTS       |
    \*=================*/

    public void addLight() {}

    /*=================*\
    |       FLAGS       |
    \*=================*/

    public void setIgnoreParentTransform(boolean ignore) {
        scene.setIgnoreParentTransform(id, ignore);
    }

    public boolean isIgnoreParentTransform() {
        return scene.isIgnoreParentTransform(id);
    }

    public void setIgnoreNonLocalLights(boolean ignore) {
        scene.setIgnoreNonLocalLights(id, ignore);
    }

    public boolean isIgnoreNonLocalLights() {
        return scene.isIgnoreNonLocalLights(id);
    }

    /*=================*\
    |       GRAPH       |
    \*=================*/

    public void attachChild(Node node) {
        requireSameScene(node);
        scene.attachChild(id, node.id);
    }

    public void detachAllChildren() {
        scene.detachAllChildren(id);
    }

    public void makeRoot() {
        scene.makeRoot(id);
    }

    public void detach() {
        scene.detach(id);
    }

    public Set<Integer> children() {
        return scene.children(id);
    }

    public boolean isRoot() {
        return scene.isRoot(id);
    }

    public Scene.DepthFirstIterator depthFirst(boolean skipOrigin) {
        return scene.depthFirst(id, skipOrigin);
    }

    public Scene.DepthFirstIterator depthFirst() {
        return scene.depthFirst(id);
    }

}
