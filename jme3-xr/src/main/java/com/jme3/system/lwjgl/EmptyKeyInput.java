package com.jme3.system.lwjgl;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;

public class EmptyKeyInput implements KeyInput {

	public EmptyKeyInput() {  }
    @Override
    public void initialize() {}
    
    @Override
    public boolean isInitialized() {return false;}
    
    @Override
    public void update() {}
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setInputListener(RawInputListener listener) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public long getInputTimeNanos() {
        return (long) (glfwGetTime() * 1000000000);
	}
	@Override
	public String getKeyName(int key) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void resetContext() {}
}
