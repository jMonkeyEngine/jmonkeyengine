/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.awt.Image;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.actions.DeleteAction;
import org.openide.loaders.DataObject;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service=SceneExplorerNode.class)
public class JmeRigidBodyControl extends AbstractSceneExplorerNode {

    private static Image smallImage =
            ImageUtilities.loadImage("com/jme3/gde/core/sceneexplorer/nodes/icons/physicscontrol.gif");
    private RigidBodyControl geom;

    public JmeRigidBodyControl() {
    }

    public JmeRigidBodyControl(RigidBodyControl spatial, DataObject dataObject) {
        super(dataObject);
        getLookupContents().add(spatial);
        getLookupContents().add(this);
        this.geom = spatial;
        setName("PhysicsControl");
    }

    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new SystemAction[]{
                    //                    SystemAction.get(CopyAction.class),
                    //                    SystemAction.get(CutAction.class),
                    //                    SystemAction.get(PasteAction.class),
                    SystemAction.get(DeleteAction.class)
                };
    }

    @Override
    public boolean canDestroy() {
        return !readOnly;
    }

    @Override
    public void destroy() throws IOException {
        super.destroy();
        final Spatial spat=getParentNode().getLookup().lookup(Spatial.class);
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    spat.removeControl(geom);
                    return null;
                }
            }).get();
            ((AbstractSceneExplorerNode)getParentNode()).refresh(true);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("RigidBodyControl");
        set.setName(RigidBodyControl.class.getName());
        RigidBodyControl obj = geom;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }

        set.put(makeProperty(obj, Vector3f.class, "getPhysicsLocation", "setPhysicsLocation", "Physics Location"));
        set.put(makeProperty(obj, Quaternion.class, "getPhysicsRotation", "setPhysicsRotation", "Physics Rotation"));

        set.put(makeProperty(obj, CollisionShape.class, "getCollisionShape", "setCollisionShape", "Collision Shape"));
        set.put(makeProperty(obj, int.class, "getCollisionGroup", "setCollisionGroup", "Collision Group"));
        set.put(makeProperty(obj, int.class, "getCollideWithGroups", "setCollideWithGroups", "Collide With Groups"));
        
        set.put(makeProperty(obj, float.class, "getFriction", "setFriction", "Friction"));
        set.put(makeProperty(obj, float.class, "getMass", "setMass", "Mass"));
        set.put(makeProperty(obj, boolean.class, "isKinematic", "setKinematic", "Kinematic"));
        set.put(makeProperty(obj, Vector3f.class, "getGravity", "setGravity", "Gravity"));
        set.put(makeProperty(obj, float.class, "getLinearDamping", "setLinearDamping", "Linear Damping"));
        set.put(makeProperty(obj, float.class, "getAngularDamping", "setAngularDamping", "Angular Damping"));
        set.put(makeProperty(obj, float.class, "getRestitution", "setRestitution", "Restitution"));

        set.put(makeProperty(obj, float.class, "getLinearSleepingThreshold", "setLinearSleepingThreshold", "Linear Sleeping Threshold"));
        set.put(makeProperty(obj, float.class, "getAngularSleepingThreshold", "setAngularSleepingThreshold", "Angular Sleeping Threshold"));

        sheet.put(set);
        return sheet;

    }

    public Class getExplorerObjectClass() {
        return RigidBodyControl.class;
    }

    public Class getExplorerNodeClass() {
        return JmeRigidBodyControl.class;
    }

    public org.openide.nodes.Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        return new org.openide.nodes.Node[]{new JmeRigidBodyControl((RigidBodyControl) key, key2).setReadOnly(cookie)};
    }
}
