package com.jme3.input.xr;

import java.util.ArrayList;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindowXr;

public class XrHmd
{
	public static final float DEFAULT_X_DIST = 0.4f;
	public static final float DEFAULT_X_ROT = 0.028f;
	public static final float DEFAULT_POS_MULT = 10.0f;
	Quaternion tmpQ = new Quaternion();
	float[] tmpArr = new float[3];
	Quaternion tmpQdistRotL = new Quaternion().fromAngles(0, -DEFAULT_X_ROT, 0);
	Quaternion tmpQdistRotR = new Quaternion().fromAngles(0, DEFAULT_X_ROT, 0);
	SimpleApplication app;
	Eye leftEye;
	Eye rightEye;
	ArrayList<XrListener.OrientationListener> hmdListeners = new ArrayList<XrListener.OrientationListener>();
	ArrayList<XrListener.ButtonPressedListener> contr1Listeners = new ArrayList<XrListener.ButtonPressedListener>();
	ArrayList<XrListener.ButtonPressedListener> contr2Listeners = new ArrayList<XrListener.ButtonPressedListener>();
	
	public XrHmd(SimpleApplication app)
	{
		this.app = app;
		leftEye = new Eye(app);
		rightEye = new Eye(app);
		setXDistance(DEFAULT_X_DIST);
		hmdListeners.add((p,r) -> doMoveRotate(p,r));
	}
	
	Vector3f initPos;
	Vector3f diffPos = new Vector3f();
	private void doMoveRotate(Vector3f p, Quaternion r)
	{
		if (initPos == null) { initPos = new Vector3f(p.getX(), p.getY(), p.getZ()); }
		initPos.subtract(p,diffPos);
		leftEye.moveAbs(diffPos);
		rightEye.moveAbs(diffPos);
		tmpQ.set(r);
		tmpQ.inverseLocal();
		tmpQ.set(tmpQ.getX(), tmpQ.getY(), -tmpQ.getZ(), tmpQ.getW());
		leftEye.rotateAbs(tmpQ.multLocal(tmpQdistRotL));
		tmpQ.set(r);
		tmpQ.inverseLocal();
		tmpQ.set(tmpQ.getX(), tmpQ.getY(), -tmpQ.getZ(), tmpQ.getW());
		rightEye.rotateAbs(tmpQ.multLocal(tmpQdistRotR));
	}

	public Eye getLeftEye() { return leftEye; }
	public Eye getRightEye() { return rightEye; }
	
	public ArrayList<XrListener.OrientationListener> getHmdOrientationListeners() { return hmdListeners; }
	public ArrayList<XrListener.ButtonPressedListener> getContr1ButtonPressedListeners() { return contr1Listeners; }
	public ArrayList<XrListener.ButtonPressedListener> getContr2ButtonPressedListeners() { return contr2Listeners; }
	
	Vector3f multPos = new Vector3f();
	public void onUpdateHmdOrientation(Vector3f viewPos, Quaternion viewRot)
	{
		viewPos.mult(DEFAULT_POS_MULT, multPos);
		multPos.setX(0.0f-multPos.getX());
		multPos.setY(0.0f-multPos.getY());
		for (XrListener.OrientationListener l : hmdListeners) { l.onUpdateOrientation(multPos, viewRot); }
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
		XrHmd xrHmd = new XrHmd(app);
		((LwjglWindowXr)app.getContext()).getXr().setHmd(xrHmd);
		return xrHmd;
	}

    /** Gets the distance between the eyes. Default is DEFAULT_X_DIST */
    public float getXDistance() { return rightEye.getPosX() * 2f; }
    
    /** Sets the distance between the eyes. Default is DEFAULT_X_DIST */
    public void setXDistance(float xDist)
    {
    	rightEye.setPosX(xDist / 2f);
    	leftEye.setPosX(xDist / -2f);
    }
    
    /** Gets the rotation angle between the eyes. Default is DEFAULT_X_ROT */
    public float getXRotation() { return tmpQdistRotR.toAngles(tmpArr)[1]; }
    
    /** Sets the rotation angle between the eyes. Default is DEFAULT_X_ROT */
    public void setXRotation(float xRot)
    {
    	tmpQdistRotR.fromAngles(0, xRot, 0);
    	tmpQdistRotL.fromAngles(0, xRot, 0);
    }
}
