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

import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.light.Light;
import com.jme3.math.Vector3f;
import java.awt.Image;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.actions.DeleteAction;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service=SceneExplorerNode.class)
public class JmeVehicleWheel extends AbstractSceneExplorerNode{

    private VehicleWheel wheel;
    private VehicleControl vehicle;
    private static Image smallImage = IconList.wheel.getImage();

    public JmeVehicleWheel() {
    }

    public JmeVehicleWheel(VehicleControl vehicle, VehicleWheel wheel) {
        super(Children.LEAF);
        this.vehicle=vehicle;
        this.wheel = wheel;
        getLookupContents().add(wheel);
        getLookupContents().add(this);
        setName("Wheel");
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
    protected Sheet createSheet() {
        //TODO: multithreading..
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("VehicleWheel");
        set.setName(Light.class.getName());
        VehicleWheel obj = wheel;
        if (obj == null) {
            return sheet;
        }

        set.put(makeProperty(obj, Vector3f.class, "getLocation", "Location"));
        set.put(makeProperty(obj, Vector3f.class, "getAxle", "Axis"));
        set.put(makeProperty(obj, Vector3f.class, "getDirection", "Direction"));
        set.put(makeProperty(obj, boolean.class, "isFrontWheel", "setFrontWheel", "Front Wheel"));
        set.put(makeProperty(obj, float.class, "getFrictionSlip", "setFrictionSlip", "Friction Slip"));
        set.put(makeProperty(obj, float.class, "getMaxSuspensionForce", "setMaxSuspensionForce", "Max Suspension Force"));
        set.put(makeProperty(obj, float.class, "getMaxSuspensionTravelCm", "setMaxSuspensionTravelCm", "Max Suspension Travel"));
        set.put(makeProperty(obj, float.class, "getRadius", "setRadius", "Radius"));
        set.put(makeProperty(obj, float.class, "getRestLength", "setRestLength", "Rest Length"));
        set.put(makeProperty(obj, float.class, "getRollInfluence", "setRollInfluence", "Roll Influence"));
        set.put(makeProperty(obj, float.class, "getSuspensionStiffness", "setSuspensionStiffness", "Suspension Stiffness"));
        set.put(makeProperty(obj, float.class, "getWheelsDampingCompression", "setWheelsDampingCompression", "Damping Compression"));
        set.put(makeProperty(obj, float.class, "getWheelsDampingRelaxation", "setWheelsDampingRelaxation", "Damping Relaxation"));

        sheet.put(set);
        return sheet;

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
        return true;
    }

    @Override
    public void destroy() throws IOException {
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    for (int i= 0; i < vehicle.getNumWheels(); i++) {
                        if(vehicle.getWheel(i)==wheel){
                            vehicle.removeWheel(i);
                            return null;
                        }
                    }
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

    public Class getExplorerObjectClass() {
        return VehicleWheel.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeVehicleWheel.class;
    }

}
