package com.jme3.input.xr;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public interface XrListener {
    public interface OrientationListener
    {
    	public void onUpdateOrientation(Vector3f pos, Quaternion rot);
    }
    
    public interface ButtonPressedListener
    {
    	public void onButtonPressed(int num);
    }
}
