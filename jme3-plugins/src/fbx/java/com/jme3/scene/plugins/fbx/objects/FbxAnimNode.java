package com.jme3.scene.plugins.fbx.objects;

import java.util.Collection;

import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxAnimNode extends FbxObject {

    public Vector3f value;
    public FbxAnimCurve xCurve;
    public FbxAnimCurve yCurve;
    public FbxAnimCurve zCurve;
    public long layerId;

    public FbxAnimNode(SceneLoader scene, FbxElement element) {
        super(scene, element);
        if(type.equals("")) {
            Double x = null, y = null, z = null;
            for(FbxElement e2 : element.getFbxProperties()) {
                String propName = (String) e2.properties.get(0);
                switch(propName) {
                case "d|X":
                    x = (Double) e2.properties.get(4);
                    break;
                case "d|Y":
                    y = (Double) e2.properties.get(4);
                    break;
                case "d|Z":
                    z = (Double) e2.properties.get(4);
                    break;
                }
            }
            // Load only T R S curve nodes
            if(x != null && y != null && z != null)
                value = new Vector3f(x.floatValue(), y.floatValue(), z.floatValue());
        }
    }

    @Override
    public void link(FbxObject otherObject, String propertyName) {
        if(otherObject instanceof FbxAnimCurve) {
            FbxAnimCurve curve = (FbxAnimCurve) otherObject;
            switch(propertyName) {
            case "d|X":
                xCurve = curve;
                break;
            case "d|Y":
                yCurve = curve;
                break;
            case "d|Z":
                zCurve = curve;
                break;
            }
        }
    }

    @Override
    public void link(FbxObject otherObject) {
        layerId = otherObject.id;
    }

    public boolean haveAnyChannel() {
        return xCurve != null || yCurve != null || zCurve != null;
    }

    public void exportTimes(Collection<Long> stamps) {
        if(xCurve != null)
            for(long t : xCurve.keyTimes)
                stamps.add(t);
        if(yCurve != null)
            for(long t : yCurve.keyTimes)
                stamps.add(t);
        if(zCurve != null)
            for(long t : zCurve.keyTimes)
                stamps.add(t);
    }

    public Vector3f getValue(long time, Vector3f defaultValue) {
        float xValue = (xCurve != null) ? xCurve.getValue(time) : defaultValue.x;
        float yValue = (yCurve != null) ? yCurve.getValue(time) : defaultValue.y;
        float zValue = (zCurve != null) ? zCurve.getValue(time) : defaultValue.z;
        return new Vector3f(xValue, yValue, zValue);
    }
}
