package com.jme3.glhelper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.jme3.math.ColorRGBA;
import com.jme3.shader.Shader;

/**
 * OpenGL helper that does not rely on a specific binding. Its main purpose is to allow 
 * to move the binding-agnostic OpenGL logic from the source code of the both renderers 
 * to a single abstract renderer in order to ease the maintenance
 * 
 * @author Julien Gouesse
 *
 */
public interface Helper {
	
	public static final int TEXTURE0 = 33984;
	
	//TODO: add Array, Format, TargetBuffer, TextureType, ShaderType, ShadeModel, BlendMode, CullFace, FillMode, DepthFunc, AlphaFunc
	
	public enum MatrixMode{
			
		MODELVIEW(0x1700),PROJECTION(0x1701);
		
		private final int glConstant;
		
		private MatrixMode(int glConstant){
			this.glConstant = glConstant;
		}
		
		public final int getGLConstant(){
			return glConstant;
		}
	};
	
	public enum BufferBit{
		COLOR_BUFFER(16384),DEPTH_BUFFER(256),STENCIL_BUFFER(1024),ACCUM_BUFFER(512);
		
		private final int glConstant;
		
		private BufferBit(int glConstant){
			this.glConstant = glConstant;
		}
		
		public final int getGLConstant(){
			return glConstant;
		}
	}
	
	public enum Filter{
		NEAREST(9728),LINEAR(9729);
		
        private final int glConstant;
		
		private Filter(int glConstant){
			this.glConstant = glConstant;
		}
		
		public final int getGLConstant(){
			return glConstant;
		}
	}

	public void useProgram(int program);
	
	public void setMatrixMode(MatrixMode matrixMode);
	
	public void loadMatrixf(FloatBuffer m);
	
	public void multMatrixf(FloatBuffer m);
	
	public void setViewPort(int x, int y, int width, int height);
	
	public void setBackgroundColor(ColorRGBA color);
	
	public void clear(BufferBit bufferBit);
	
	public void setDepthRange(float start, float end);
	
	public void setScissor(int x, int y, int width, int height);
	
	public int getUniformLocation(Shader shader,String name,ByteBuffer nameBuffer);
}
