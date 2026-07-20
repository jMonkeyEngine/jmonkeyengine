package com.jme3.vulkan.scene;

public class Node {

    private final Scene scene;
    private final int id;

    public Node(Scene scene) {
        this.scene = scene;
        this.id = scene.createNode();
    }

    public int getNodeId() {
        return id;
    }

    public void attachChild(Node child) {
        scene.attachChild(id, child.getNodeId());
    }

    public void detachChild(Node child) {
        if (scene.getParentId(child.getNodeId()) != id) {
            return;
        }
        scene.detach(child.getNodeId());
    }

    public Scene.ChildIterator children() {
        return scene.children(id);
    }

    public Scene.DepthFirstIterator depthFirst() {
        return scene.depthFirst(id);
    }

}
