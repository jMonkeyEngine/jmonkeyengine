package com.jme3.input.xr;

import java.util.ArrayList;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindowXr;

public class XrHmd
{
	Quaternion tmpQ = new Quaternion();
	SimpleApplication app;
	Eye leftEye;
	Eye rightEye;
	ArrayList<XrListener.OrientationListener> hmdListeners = new ArrayList<XrListener.OrientationListener>();
	ArrayList<XrListener.ButtonPressedListener> contr1Listeners = new ArrayList<XrListener.ButtonPressedListener>();
	ArrayList<XrListener.ButtonPressedListener> contr2Listeners = new ArrayList<XrListener.ButtonPressedListener>();
	
	public XrHmd(SimpleApplication app, float distX)
	{
		this.app = app;
		leftEye = new Eye(app, -distX/2.0f);
		rightEye = new Eye(app, distX/2.0f);
		hmdListeners.add((p,r) -> doMoveRotate(p,r));
	}
	
	private void doMoveRotate(Vector3f p, Quaternion r)
	{
		tmpQ.set(r);
		tmpQ.inverseLocal();
		leftEye.rotateAbs(tmpQ);
		rightEye.rotateAbs(tmpQ);
	}

	public Eye getLeftEye() { return leftEye; }
	public Eye getRightEye() { return rightEye; }
	
	public ArrayList<XrListener.OrientationListener> getHmdOrientationListeners() { return hmdListeners; }
	public ArrayList<XrListener.ButtonPressedListener> getContr1ButtonPressedListeners() { return contr1Listeners; }
	public ArrayList<XrListener.ButtonPressedListener> getContr2ButtonPressedListeners() { return contr2Listeners; }
	
	public void onUpdateHmdOrientation(Vector3f viewPos, Quaternion viewRot)
	{
		for (XrListener.OrientationListener l : hmdListeners) { l.onUpdateOrientation(viewPos, viewRot); }
	}
	
	/** Must be called in main function before init.
	 * @param s The appSettings that must be used with app.setSettings(s). */
	public static void setRendererForSettings(AppSettings s)
	{
		s.setRenderer("CUSTOM" + com.jme3.system.lwjgl.LwjglWindowXr.class.getName()); //see JmeDesktopSystem.newContext(...)
	}
	
	/** Must be called in simpleInitApp-function of SimpleApplication.
	 * @return The head-mounted-device object. */
	public static XrHmd initHmd(SimpleApplication app)
	{
		XrHmd xrHmd = new XrHmd(app, 0.8f);
		((LwjglWindowXr)app.getContext()).getXr().setHmd(xrHmd);
		return xrHmd;
	}
}
