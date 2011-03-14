package com.jme3.scene;

/**
 * Traverse and visit Geometry/Node
 * by calling Spatial.depthFirstTraversal() or Spatial.breadthFirstTraversal().
 *
 */
public class SceneGraphVisitorAdapter implements SceneGraphVisitor {
    
    public void visit(Geometry geom) {}
    
    public void visit(Node geom) {}

    @Override
    public final void visit(Spatial spatial) {
        if (spatial instanceof Geometry) {
            visit((Geometry)spatial);
        } else {
            visit((Node)spatial);
        }
    }
}
