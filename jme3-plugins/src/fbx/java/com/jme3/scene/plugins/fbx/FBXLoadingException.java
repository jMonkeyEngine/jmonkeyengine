package com.jme3.scene.plugins.fbx;

public class FBXLoadingException extends RuntimeException {
	
	private static final long serialVersionUID = -1641318378648889782L;

	public FBXLoadingException() {
	}
	
	public FBXLoadingException(String message) {
		super(message);
	}
	
	public FBXLoadingException(Throwable cause) {
		super(cause);
	}
	
	public FBXLoadingException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public FBXLoadingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
}
