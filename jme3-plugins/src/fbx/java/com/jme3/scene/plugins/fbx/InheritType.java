package com.jme3.scene.plugins.fbx;

public enum InheritType {
	
	RrSs, RSrs, 
	/**
	 * <p>Segment Scale Compensate
	 * <p>Resets scale of node to (1.0, 1.0, 1.0)
	 */
	Rrs;
	
	public static final InheritType[] values = values();
}
