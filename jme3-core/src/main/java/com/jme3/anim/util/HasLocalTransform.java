package com.jme3.anim.util;

import com.jme3.export.Savable;
import com.jme3.math.Transform;

public interface HasLocalTransform extends Savable {
    public void setLocalTransform(Transform transform);

    public Transform getLocalTransform();
}
