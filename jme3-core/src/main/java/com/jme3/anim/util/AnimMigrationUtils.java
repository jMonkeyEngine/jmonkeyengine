package com.jme3.anim.util;

import com.jme3.animation.AnimControl;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;

public class AnimMigrationUtils {

    public static Spatial migrate(Spatial source) {
        //source.depthFirstTraversal();
        return source;
    }

    private class AnimControlVisitor implements SceneGraphVisitor {

        @Override
        public void visit(Spatial spatial) {
            AnimControl control = spatial.getControl(AnimControl.class);
            if (control != null) {

            }
        }
    }
}
