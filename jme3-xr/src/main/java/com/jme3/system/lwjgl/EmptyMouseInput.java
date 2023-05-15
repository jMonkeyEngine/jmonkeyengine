package com.jme3.system.lwjgl;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;

public class EmptyMouseInput implements MouseInput {

	public EmptyMouseInput() {  }
    @Override
    public void initialize() {}
    
    @Override
    public boolean isInitialized() {return false;}
	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}
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
	public void setCursorVisible(boolean visible) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getButtonCount() {
		// TODO Auto-generated method stub
		return 3;
	}
	@Override
	public void setNativeCursor(JmeCursor cursor) {
		// TODO Auto-generated method stub
	}
	
	public void resetContext() {}
}
