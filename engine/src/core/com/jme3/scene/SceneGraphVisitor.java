package com.jme3.scene;

/**
 * Interface to traverse and visit scene graph 
 * by calling Spatial.depthFirstTraversal() or Spatial.breadthFirstTraversal().
 */
public interface SceneGraphVisitor {
    public void visit(Spatial spatial);
}
