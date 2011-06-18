package com.jme3.scene;

/**
 * <code>SceneGraphVisitorAdapter</code> is used to traverse the scene
 * graph tree. 
 * Use by calling {@link Spatial#depthFirstTraversal(com.jme3.scene.SceneGraphVisitor) }
 * or {@link Spatial#breadthFirstTraversal(com.jme3.scene.SceneGraphVisitor)}.
 */
public interface SceneGraphVisitor {
    /**
     * Called when a spatial is visited in the scene graph.
     * 
     * @param spatial The visited spatial
     */
    public void visit(Spatial spatial);
}
