/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.gizmo.light;

import com.jme3.light.Light;
import com.jme3.math.Vector3f;
import com.jme3.scene.control.BillboardControl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.openide.util.Exceptions;

/**
 * Updates the marker's position whenever the light has moved. It is also a
 * BillboardControl, so this marker always faces the camera
 */
public class LightGizmoControl extends BillboardControl {

    private final Vector3f lastPos = new Vector3f();
    private Vector3f lightPos;

    LightGizmoControl(Light light) {
        super();

        try {
            Method getPosition = light.getClass().getMethod("getPosition");
            lightPos = (Vector3f) getPosition.invoke(light);
        } catch (NoSuchMethodException ex) {
            //light type doesn't have a get position method, silancing the exception
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    @Override
    protected void controlUpdate(float f) {
        super.controlUpdate(f);

        if (!lightPos.equals(lastPos)) {
            if (getSpatial() != null) {
                lastPos.set(lightPos);
                getSpatial().setLocalTranslation(lastPos);
            }
        }

    }

}
