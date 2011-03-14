package com.jme3.glhelper.jogl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

import com.jme3.glhelper.Helper;
import com.jme3.math.ColorRGBA;
import com.jme3.shader.Shader;

/**
 * OpenGL helper that relies on a specific binding. The main purposes of this helper is to contain
 * the type-safe OpenGL logic that is specific to a particular binding and to support several kinds
 * of hardware by delegating tasks supported by only some "profiles" to separate helpers 
 * (desktop shader-based, desktop fixed pipeline, embedded shader-based, embedded fixed pipeline)
 * 
 * @author Julien Gouesse
 *
 */
public class JoglHelper implements Helper {
	
	
	public JoglHelper(){
		//TODO: get the current GL and choose the best fitted delegate depending on the hardware (Desktop, Embedded)
	    GL gl = GLContext.getCurrentGL();
		if(gl.isGL2ES1()){
			//embedded fixed pipeline
		}
		else{
			if(gl.isGL2ES2()){
				//embedded shader-based
			}
			else{
				if(gl.isGL3()&&!gl.isGL3bc()||gl.isGL4()&&!gl.isGL4bc()){
					//desktop shader-based (forward compatible)
				}
				else{
					//if GLSL is supported, use desktop shader-based (backward compatible)
					//otherwise use desktop fixed pipeline
				}
			}
		}
	}

	@Override
    public void useProgram(int program) {
        GLContext.getCurrentGL().getGL2().glUseProgram(program);
    }
	
	@Override
	public void setMatrixMode(MatrixMode matrixMode){
		GLContext.getCurrentGL().getGL2().glMatrixMode(matrixMode.getGLConstant());
	}
	
	@Override
	public void loadMatrixf(FloatBuffer m){
		GLContext.getCurrentGL().getGL2().glLoadMatrixf(m);
	}
	
	@Override
	public void multMatrixf(FloatBuffer m){
		GLContext.getCurrentGL().getGL2().glMultMatrixf(m);
	}
	
	@Override
	public void setViewPort(int x, int y, int width, int height){
		GLContext.getCurrentGL().glViewport(x, y, width, height);
	}
	
	@Override
	public void setBackgroundColor(ColorRGBA color){
		GLContext.getCurrentGL().glClearColor(color.r, color.g, color.b, color.a);
	}
	
	@Override
	public void clear(BufferBit bufferBit){
		GLContext.getCurrentGL().glClear(bufferBit.getGLConstant());
	}
	
	@Override
	public void setDepthRange(float start, float end) {
        GLContext.getCurrentGL().glDepthRange(start, end);
    }
	
	@Override
	public void setScissor(int x, int y, int width, int height){
		GLContext.getCurrentGL().glScissor(x, y, width, height);
	}
	
	@Override
	public int getUniformLocation(Shader shader,String name,ByteBuffer nameBuffer){
		return GLContext.getCurrentGL().getGL2ES2().glGetUniformLocation(shader.getId(),name);
	}
}
