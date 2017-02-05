package com.jme3.renderer.jogl;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL3;

import java.nio.*;

import com.jme3.renderer.opengl.GL4;
import com.jogamp.opengl.GLContext;

public class JoglGL implements GL, GL2, GL3, GL4 {
    
	private static int getLimitBytes(ByteBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit();
    }

    private static int getLimitBytes(ShortBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 2;
    }

    private static int getLimitBytes(FloatBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 4;
    }

    private static int getLimitCount(Buffer buffer, int elementSize) {
        checkLimit(buffer);
        return buffer.limit() / elementSize;
    }
	
    private static void checkLimit(Buffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.limit() == 0) {
            throw new RendererException("Attempting to upload empty buffer (limit = 0), that's an error");
        }
        if (buffer.remaining() == 0) {
            throw new RendererException("Attempting to upload empty buffer (remaining = 0), that's an error");
        }
    }
    
    @Override
	public void resetStats() {
    }
    
    @Override
	public void glActiveTexture(int param1) {
        GLContext.getCurrentGL().glActiveTexture(param1);
    }

    @Override
	public void glAlphaFunc(int param1, float param2) {
        GLContext.getCurrentGL().getGL2ES1().glAlphaFunc(param1, param2);
    }

    @Override
	public void glAttachShader(int param1, int param2) {
        GLContext.getCurrentGL().getGL2ES2().glAttachShader(param1, param2);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        GLContext.getCurrentGL().getGL2ES2().glBeginQuery(target, query);
    }

    @Override
    public void glBindBuffer(int param1, int param2) {
        GLContext.getCurrentGL().glBindBuffer(param1, param2);
    }

    @Override
	public void glBindTexture(int param1, int param2) {
        GLContext.getCurrentGL().glBindTexture(param1, param2);
    }

    @Override
    public void glBlendEquationSeparate(int colorMode, int alphaMode){
        GLContext.getCurrentGL().glBlendEquationSeparate(colorMode, alphaMode);
    }

    @Override
	public void glBlendFunc(int param1, int param2) {
        GLContext.getCurrentGL().glBlendFunc(param1, param2);
    }

    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GLContext.getCurrentGL().glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    @Override
	public void glBufferData(int param1, long param2, int param3) {
        GLContext.getCurrentGL().glBufferData(param1, param2, null, param3);
    }
    
    @Override
	public void glBufferData(int param1, FloatBuffer param2, int param3) {
        checkLimit(param2);
        GLContext.getCurrentGL().glBufferData(param1, getLimitBytes(param2), param2, param3);
    }

    @Override
	public void glBufferData(int param1, ShortBuffer param2, int param3) {
        checkLimit(param2);
        GLContext.getCurrentGL().glBufferData(param1, getLimitBytes(param2), param2, param3);
    }

    @Override
	public void glBufferData(int param1, ByteBuffer param2, int param3) {
        checkLimit(param2);
        GLContext.getCurrentGL().glBufferData(param1, getLimitBytes(param2), param2, param3);
    }

    @Override
	public void glBufferSubData(int param1, long param2, FloatBuffer param3) {
        checkLimit(param3);
        GLContext.getCurrentGL().glBufferSubData(param1, param2, getLimitBytes(param3), param3);
    }

    @Override
	public void glBufferSubData(int param1, long param2, ShortBuffer param3) {
        checkLimit(param3);
        GLContext.getCurrentGL().glBufferSubData(param1, param2, getLimitBytes(param3), param3);
    }

    @Override
	public void glBufferSubData(int param1, long param2, ByteBuffer param3) {
        checkLimit(param3);
        GLContext.getCurrentGL().glBufferSubData(param1, param2, getLimitBytes(param3), param3);
    }

    @Override
	public void glClear(int param1) {
        GLContext.getCurrentGL().glClear(param1);
    }

    @Override
	public void glClearColor(float param1, float param2, float param3, float param4) {
        GLContext.getCurrentGL().glClearColor(param1, param2, param3, param4);
    }

    @Override
	public void glColorMask(boolean param1, boolean param2, boolean param3, boolean param4) {
        GLContext.getCurrentGL().glColorMask(param1, param2, param3, param4);
    }

    @Override
	public void glCompileShader(int param1) {
        GLContext.getCurrentGL().getGL2ES2().glCompileShader(param1);
    }

    @Override
	public void glCompressedTexImage2D(int param1, int param2, int param3, int param4, int param5, int param6, ByteBuffer param7) {
        checkLimit(param7);
        GLContext.getCurrentGL().glCompressedTexImage2D(param1, param2, param3, param4, param5, param6, getLimitBytes(param7), param7);
    }

    @Override
	public void glCompressedTexImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, ByteBuffer param8) {
        checkLimit(param8);
        GLContext.getCurrentGL().getGL2ES2().glCompressedTexImage3D(param1, param2, param3, param4, param5, param6, param7, getLimitBytes(param8), param8);
    }

    @Override
	public void glCompressedTexSubImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, ByteBuffer param8) {
        checkLimit(param8);
        GLContext.getCurrentGL().glCompressedTexSubImage2D(param1, param2, param3, param4, param5, param6, param7, getLimitBytes(param8), param8);
    }

    @Override
	public void glCompressedTexSubImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, ByteBuffer param10) {
        checkLimit(param10);
        GLContext.getCurrentGL().getGL2ES2().glCompressedTexSubImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, getLimitBytes(param10), param10);
    }

    @Override
	public int glCreateProgram() {
        return GLContext.getCurrentGL().getGL2ES2().glCreateProgram();
    }

    @Override
	public int glCreateShader(int param1) {
        return GLContext.getCurrentGL().getGL2ES2().glCreateShader(param1);
    }

    @Override
	public void glCullFace(int param1) {
        GLContext.getCurrentGL().glCullFace(param1);
    }

    @Override
	public void glDeleteBuffers(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glDeleteBuffers(param1.limit(), param1);
    }

    @Override
	public void glDeleteProgram(int param1) {
        GLContext.getCurrentGL().getGL2ES2().glDeleteProgram(param1);
    }

    @Override
	public void glDeleteShader(int param1) {
        GLContext.getCurrentGL().getGL2ES2().glDeleteShader(param1);
    }

    @Override
	public void glDeleteTextures(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glDeleteTextures(param1.limit() ,param1);
    }

    @Override
	public void glDepthFunc(int param1) {
        GLContext.getCurrentGL().glDepthFunc(param1);
    }

    @Override
	public void glDepthMask(boolean param1) {
        GLContext.getCurrentGL().glDepthMask(param1);
    }

    @Override
	public void glDepthRange(double param1, double param2) {
        GLContext.getCurrentGL().glDepthRange(param1, param2);
    }

    @Override
	public void glDetachShader(int param1, int param2) {
        GLContext.getCurrentGL().getGL2ES2().glDetachShader(param1, param2);
    }

    @Override
	public void glDisable(int param1) {
        GLContext.getCurrentGL().glDisable(param1);
    }

    @Override
	public void glDisableVertexAttribArray(int param1) {
        GLContext.getCurrentGL().getGL2ES2().glDisableVertexAttribArray(param1);
    }

    @Override
	public void glDrawArrays(int param1, int param2, int param3) {
        GLContext.getCurrentGL().glDrawArrays(param1, param2, param3);
    }

    @Override
	public void glDrawBuffer(int param1) {
        GLContext.getCurrentGL().getGL2GL3().glDrawBuffer(param1);
    }
    
    @Override
	public void glDrawRangeElements(int param1, int param2, int param3, int param4, int param5, long param6) {
        GLContext.getCurrentGL().getGL2ES3().glDrawRangeElements(param1, param2, param3, param4, param5, param6);
    }

    @Override
	public void glEnable(int param1) {
        GLContext.getCurrentGL().glEnable(param1);
    }

    @Override
	public void glEnableVertexAttribArray(int param1) {
        GLContext.getCurrentGL().getGL2ES2().glEnableVertexAttribArray(param1);
    }

    @Override
    public void glEndQuery(int target) {
        GLContext.getCurrentGL().getGL2ES2().glEndQuery(target);
    }

    @Override
    public void glGenBuffers(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glGenBuffers(param1.limit(), param1);
    }

    @Override
    public void glGenQueries(int num, IntBuffer buff) {
        GLContext.getCurrentGL().getGL2ES2().glGenQueries(num, buff);
    }

    @Override
    public void glGenTextures(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().glGenTextures(param1.limit(), param1);
    }

    @Override
	public void glGetBoolean(int param1, ByteBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().glGetBooleanv(param1, param2);
    }
    
    @Override
	public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        checkLimit(data);
        GLContext.getCurrentGL().getGL2GL3().glGetBufferSubData(target, offset, getLimitBytes(data), data);
    }

    @Override
	public int glGetError() {
        return GLContext.getCurrentGL().glGetError();
    }
    
    @Override
	public void glGetInteger(int param1, IntBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().glGetIntegerv(param1, param2);
    }

    @Override
	public void glGetProgram(int param1, int param2, IntBuffer param3) {
        checkLimit(param3);
        GLContext.getCurrentGL().getGL2ES2().glGetProgramiv(param1, param2, param3);
    }

    @Override
	public void glGetShader(int param1, int param2, IntBuffer param3) {
        checkLimit(param3);
        GLContext.getCurrentGL().getGL2ES2().glGetShaderiv(param1, param2, param3);
    }

    @Override
	public String glGetString(int param1) {
        return GLContext.getCurrentGL().glGetString(param1);
    }
    
    @Override
	public String glGetString(int param1, int param2) {
        return GLContext.getCurrentGL().getGL2ES3().glGetStringi(param1, param2);
    }

    @Override
	public boolean glIsEnabled(int param1) {
        return GLContext.getCurrentGL().glIsEnabled(param1);
    }

    @Override
	public void glLineWidth(float param1) {
        GLContext.getCurrentGL().glLineWidth(param1);
    }

    @Override
	public void glLinkProgram(int param1) {
        GLContext.getCurrentGL().getGL2ES2().glLinkProgram(param1);
    }

    @Override
	public void glPixelStorei(int param1, int param2) {
        GLContext.getCurrentGL().glPixelStorei(param1, param2);
    }

    @Override
	public void glPointSize(float param1) {
        GLContext.getCurrentGL().getGL2ES1().glPointSize(param1);
    }

    @Override
	public void glPolygonMode(int param1, int param2) {
        GLContext.getCurrentGL().getGL2().glPolygonMode(param1, param2);
    }

    @Override
	public void glPolygonOffset(float param1, float param2) {
        GLContext.getCurrentGL().glPolygonOffset(param1, param2);
    }

    @Override
	public void glReadBuffer(int param1) {
        GLContext.getCurrentGL().getGL2ES3().glReadBuffer(param1);
    }

    @Override
	public void glReadPixels(int param1, int param2, int param3, int param4, int param5, int param6, ByteBuffer param7) {
        checkLimit(param7);
        GLContext.getCurrentGL().glReadPixels(param1, param2, param3, param4, param5, param6, param7);
    }
    
    @Override
	public void glReadPixels(int param1, int param2, int param3, int param4, int param5, int param6, long param7) {
        GLContext.getCurrentGL().glReadPixels(param1, param2, param3, param4, param5, param6, param7);
    }

    @Override
	public void glScissor(int param1, int param2, int param3, int param4) {
        GLContext.getCurrentGL().glScissor(param1, param2, param3, param4);
    }

    @Override
	public void glStencilFuncSeparate(int param1, int param2, int param3, int param4) {
        GLContext.getCurrentGL().getGL2ES2().glStencilFuncSeparate(param1, param2, param3, param4);
    }

    @Override
	public void glStencilOpSeparate(int param1, int param2, int param3, int param4) {
        GLContext.getCurrentGL().getGL2ES2().glStencilOpSeparate(param1, param2, param3, param4);
    }

    @Override
	public void glTexImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, ByteBuffer param9) {
        checkLimit(param9);
        GLContext.getCurrentGL().glTexImage2D(param1, param2, param3, param4, param5, param6, param7, param8, param9);
    }

    @Override
	public void glTexImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, ByteBuffer param10) {
        checkLimit(param10);
        GLContext.getCurrentGL().getGL2ES2().glTexImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10);
    }

    @Override
	public void glTexParameterf(int param1, int param2, float param3) {
        GLContext.getCurrentGL().glTexParameterf(param1, param2, param3);
    }

    @Override
	public void glTexParameteri(int param1, int param2, int param3) {
        GLContext.getCurrentGL().glTexParameteri(param1, param2, param3);
    }

    @Override
	public void glTexSubImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, ByteBuffer param9) {
        checkLimit(param9);
        GLContext.getCurrentGL().glTexSubImage2D(param1, param2, param3, param4, param5, param6, param7, param8, param9);
    }

    @Override
	public void glTexSubImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, int param10, ByteBuffer param11) {
        checkLimit(param11);
        GLContext.getCurrentGL().getGL2ES2().glTexSubImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10, param11);
    }

    @Override
	public void glUniform1(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform1fv(param1, getLimitCount(param2, 1), param2);
    }

    @Override
	public void glUniform1(int param1, IntBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform1iv(param1, getLimitCount(param2, 1), param2);
    }

    @Override
	public void glUniform1f(int param1, float param2) {
        GLContext.getCurrentGL().getGL2ES2().glUniform1f(param1, param2);
    }

    @Override
	public void glUniform1i(int param1, int param2) {
        GLContext.getCurrentGL().getGL2ES2().glUniform1i(param1, param2);
    }

    @Override
	public void glUniform2(int param1, IntBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform2iv(param1, getLimitCount(param2, 2), param2);
    }

    @Override
	public void glUniform2(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform2fv(param1, getLimitCount(param2, 2), param2);
    }

    @Override
	public void glUniform2f(int param1, float param2, float param3) {
        GLContext.getCurrentGL().getGL2ES2().glUniform2f(param1, param2, param3);
    }

    @Override
	public void glUniform3(int param1, IntBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform3iv(param1, getLimitCount(param2, 3), param2);
    }

    @Override
	public void glUniform3(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform3fv(param1, getLimitCount(param2, 3), param2);
    }

    @Override
	public void glUniform3f(int param1, float param2, float param3, float param4) {
        GLContext.getCurrentGL().getGL2ES2().glUniform3f(param1, param2, param3, param4);
    }

    @Override
	public void glUniform4(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform4fv(param1, getLimitCount(param2, 4), param2);
    }

    @Override
	public void glUniform4(int param1, IntBuffer param2) {
        checkLimit(param2);
        GLContext.getCurrentGL().getGL2ES2().glUniform4iv(param1, getLimitCount(param2, 4), param2);
    }

    @Override
	public void glUniform4f(int param1, float param2, float param3, float param4, float param5) {
        GLContext.getCurrentGL().getGL2ES2().glUniform4f(param1, param2, param3, param4, param5);
    }

    @Override
	public void glUniformMatrix3(int param1, boolean param2, FloatBuffer param3) {
        checkLimit(param3);
        GLContext.getCurrentGL().getGL2ES2().glUniformMatrix3fv(param1, getLimitCount(param3, 3 * 3), param2, param3);
    }

    @Override
	public void glUniformMatrix4(int param1, boolean param2, FloatBuffer param3) {
        checkLimit(param3);
        GLContext.getCurrentGL().getGL2ES2().glUniformMatrix4fv(param1, getLimitCount(param3, 4 * 4), param2, param3);
    }

    @Override
	public void glUseProgram(int param1) {
        GLContext.getCurrentGL().getGL2ES2().glUseProgram(param1);
    }

    @Override
	public void glVertexAttribPointer(int param1, int param2, int param3, boolean param4, int param5, long param6) {
        GLContext.getCurrentGL().getGL2ES2().glVertexAttribPointer(param1, param2, param3, param4, param5, param6);
    }

    @Override
	public void glViewport(int param1, int param2, int param3, int param4) {
        GLContext.getCurrentGL().glViewport(param1, param2, param3, param4);
    }

    @Override
	public int glGetAttribLocation(int param1, String param2) {
    	// JOGL 2.0 doesn't need a null-terminated string
        return GLContext.getCurrentGL().getGL2ES2().glGetAttribLocation(param1, param2);
    }

    @Override
	public int glGetUniformLocation(int param1, String param2) {
    	// JOGL 2.0 doesn't need a null-terminated string
        return GLContext.getCurrentGL().getGL2ES2().glGetUniformLocation(param1, param2);
    }

    @Override
	public void glShaderSource(int param1, String[] param2, IntBuffer param3) {
        checkLimit(param3);
        
        int param3pos = param3.position();
        try {
        	for (final String param2string : param2) {
        		param3.put(Math.max(param2string.length(), param2string.getBytes().length));
        	}
        } finally {
        	param3.position(param3pos);
        }
        GLContext.getCurrentGL().getGL2ES2().glShaderSource(param1, param2.length, param2, param3);
    }

    @Override
	public String glGetProgramInfoLog(int program, int maxSize) {
    	ByteBuffer buffer = ByteBuffer.allocateDirect(maxSize);
		buffer.order(ByteOrder.nativeOrder());
		ByteBuffer tmp = ByteBuffer.allocateDirect(4);
		tmp.order(ByteOrder.nativeOrder());
		IntBuffer intBuffer = tmp.asIntBuffer();

		GLContext.getCurrentGL().getGL2ES2().glGetProgramInfoLog(program, maxSize, intBuffer, buffer);
		int numBytes = intBuffer.get(0);
		byte[] bytes = new byte[numBytes];
		buffer.get(bytes);
		return new String(bytes);
    }

    @Override
    public long glGetQueryObjectui64(int query, int target) {
        LongBuffer buff = LongBuffer.allocate(1);
        GLContext.getCurrentGL().getGL2ES2().glGetQueryObjectui64v(query, target, buff);
        return buff.get(0);
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        IntBuffer buff = IntBuffer.allocate(1);
        GLContext.getCurrentGL().getGL2ES2().glGetQueryObjectiv(query, pname, buff);
        return buff.get(0);
    }

    @Override
	public String glGetShaderInfoLog(int shader, int maxSize) {
    	ByteBuffer buffer = ByteBuffer.allocateDirect(maxSize);
		buffer.order(ByteOrder.nativeOrder());
		ByteBuffer tmp = ByteBuffer.allocateDirect(4);
		tmp.order(ByteOrder.nativeOrder());
		IntBuffer intBuffer = tmp.asIntBuffer();

		GLContext.getCurrentGL().getGL2ES2().glGetShaderInfoLog(shader, maxSize, intBuffer, buffer);
		int numBytes = intBuffer.get(0);
		byte[] bytes = new byte[numBytes];
		buffer.get(bytes);
		return new String(bytes);
    }

    @Override
	public void glBindFragDataLocation(int param1, int param2, String param3) {
        GLContext.getCurrentGL().getGL2GL3().glBindFragDataLocation(param1, param2, param3);
    }

    @Override
	public void glBindVertexArray(int param1) {
        GLContext.getCurrentGL().getGL2ES3().glBindVertexArray(param1);
    }

    @Override
	public void glGenVertexArrays(IntBuffer param1) {
        checkLimit(param1);
        GLContext.getCurrentGL().getGL2ES3().glGenVertexArrays(param1.limit(), param1);
    }

    @Override
	public void glPatchParameter(int count) {
        GLContext.getCurrentGL().getGL3().glPatchParameteri(com.jogamp.opengl.GL3.GL_PATCH_VERTICES, count);
    }
    
    @Override
	public void glDeleteVertexArrays(IntBuffer arrays) {
        checkLimit(arrays);
        GLContext.getCurrentGL().getGL2ES3().glDeleteVertexArrays(arrays.limit(), arrays);
    }

    @Override
    public void glFramebufferTextureLayer(int param1, int param2, int param3, int param4, int param5) {
        GLContext.getCurrentGL().getGL3().glFramebufferTextureLayer(param1, param2, param3, param4, param5);
    }

}
