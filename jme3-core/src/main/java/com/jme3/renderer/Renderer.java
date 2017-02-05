/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer;

import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.util.NativeObject;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * The <code>Renderer</code> is responsible for taking rendering commands and
 * executing them on the underlying video hardware.
 * 
 * @author Kirill Vainer
 */
public interface Renderer {

    /**
     * Detects available capabilities of the GPU. 
     * 
     * Must be called prior to any other Renderer methods.
     */
    public void initialize();
    
    /**
     * Get the capabilities of the renderer.
     * @return The capabilities of the renderer.
     */
    public EnumSet<Caps> getCaps();

    /**
     * Get the limits of the renderer.
     *
     * @return The limits of the renderer.
     */
    public EnumMap<Limits, Integer> getLimits();

    /**
     * The statistics allow tracking of how data
     * per frame, such as number of objects rendered, number of triangles, etc.
     * These are updated when the Renderer's methods are used, make sure
     * to call {@link Statistics#clearFrame() } at the appropriate time
     * to get accurate info per frame.
     */
    public Statistics getStatistics();

    /**
     * Invalidates the current rendering state. Should be called after
     * the GL state was changed manually or through an external library.
     */
    public void invalidateState();

    /**
     * Clears certain channels of the currently bound framebuffer.
     *
     * @param color True if to clear colors (RGBA)
     * @param depth True if to clear depth/z
     * @param stencil True if to clear stencil buffer (if available, otherwise
     * ignored)
     */
    public void clearBuffers(boolean color, boolean depth, boolean stencil);

    /**
     * Sets the background (aka clear) color.
     * 
     * @param color The background color to set
     */
    public void setBackgroundColor(ColorRGBA color);

    /**
     * Applies the given {@link RenderState}, making the necessary
     * GL calls so that the state is applied.
     */
    public void applyRenderState(RenderState state);

    /**
     * Set the range of the depth values for objects. All rendered
     * objects will have their depth clamped to this range.
     * 
     * @param start The range start
     * @param end The range end
     */
    public void setDepthRange(float start, float end);

    /**
     * Called when a new frame has been rendered.
     * 
     * Currently, this will simply delete any OpenGL objects from the GPU
     * which have been garbage collected by the GC.
     */
    public void postFrame();

    /**
     * Set the viewport location and resolution on the screen.
     * 
     * @param x The x coordinate of the viewport
     * @param y The y coordinate of the viewport
     * @param width Width of the viewport
     * @param height Height of the viewport
     */
    public void setViewPort(int x, int y, int width, int height);

    /**
     * Specifies a clipping rectangle.
     * For all future rendering commands, no pixels will be allowed
     * to be rendered outside of the clip rectangle.
     * 
     * @param x The x coordinate of the clip rect
     * @param y The y coordinate of the clip rect
     * @param width Width of the clip rect
     * @param height Height of the clip rect
     */
    public void setClipRect(int x, int y, int width, int height);

    /**
     * Clears the clipping rectangle set with 
     * {@link #setClipRect(int, int, int, int) }.
     */
    public void clearClipRect();

    /**
     * Sets the shader to use for rendering.
     * If the shader has not been uploaded yet, it is compiled
     * and linked. If it has been uploaded, then the 
     * uniform data is updated and the shader is set.
     * 
     * @param shader The shader to use for rendering.
     */
    public void setShader(Shader shader);

    /**
     * Deletes a shader. This method also deletes
     * the attached shader sources.
     * 
     * @param shader Shader to delete.
     * @see #deleteShaderSource(com.jme3.shader.Shader.ShaderSource) 
     */
    public void deleteShader(Shader shader);

    /**
     * Deletes the provided shader source.
     * 
     * @param source The ShaderSource to delete.
     */
    public void deleteShaderSource(ShaderSource source);

    /**
     * Copies contents from src to dst, scaling if necessary.
     * set copyDepth to false to only copy the color buffers.
     */
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth);

    /**
     * Sets the framebuffer that will be drawn to.
     * 
     * If the framebuffer has not been initialized yet, it will be created
     * and its render surfaces and attached textures will be allocated.
     * 
     * @param fb The framebuffer to set
     */
    public void setFrameBuffer(FrameBuffer fb);
    
    /**
     * Set the framebuffer that will be set instead of the main framebuffer
     * when a call to setFrameBuffer(null) is made.
     * 
     * @param fb The framebuffer to override the main framebuffer.
     */
    public void setMainFrameBufferOverride(FrameBuffer fb);

    /**
     * Reads the pixels currently stored in the specified framebuffer
     * into the given ByteBuffer object. 
     * Only color pixels are transferred, the format is RGBA with 8 bits 
     * per component. The given byte buffer should have at least
     * fb.getWidth() * fb.getHeight() * 4 bytes remaining.
     * 
     * @param fb The framebuffer to read from
     * @param byteBuf The bytebuffer to transfer color data to
     */
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf);
    
    /**
     * Reads the pixels currently stored in the specified framebuffer
     * into the given ByteBuffer object. 
     * Only color pixels are transferred, witht hte given format. 
     * The given byte buffer should have at least
     * fb.getWidth() * fb.getHeight() * 4 bytes remaining.
     * 
     * @param fb The framebuffer to read from
     * @param byteBuf The bytebuffer to transfer color data to
     * @param format the image format to use when reading the frameBuffer.
     */
    public void readFrameBufferWithFormat(FrameBuffer fb, ByteBuffer byteBuf, Image.Format format);

    /**
     * Deletes a framebuffer and all attached renderbuffers
     */
    public void deleteFrameBuffer(FrameBuffer fb);

    /**
     * Sets the texture to use for the given texture unit.
     */
    public void setTexture(int unit, Texture tex);

    /**
     * Modify the given Texture with the given Image. 
     * The image will be put at x and y into the texture.
     * 
     * NOTE: this is only supported for uncompressed 2D images without mipmaps.
     *
     * @param tex the Texture that will be modified
     * @param pixels the source Image data to copy data from
     * @param x the x position to put the image into the texture
     * @param y the y position to put the image into the texture
     */
    public void modifyTexture(Texture tex, Image pixels, int x, int y);

    /**
     * Deletes a texture from the GPU.
     */
    public void deleteImage(Image image);

    /**
     * Uploads a vertex buffer to the GPU.
     * 
     * @param vb The vertex buffer to upload
     */
    public void updateBufferData(VertexBuffer vb);

    /**
     * Deletes a vertex buffer from the GPU.
     * @param vb The vertex buffer to delete
     */
    public void deleteBuffer(VertexBuffer vb);

    /**
     * Renders <code>count</code> meshes, with the geometry data supplied and
     * per-instance data supplied.
     * The shader which is currently set with <code>setShader</code> is
     * responsible for transforming the input vertices into clip space
     * and shading it based on the given vertex attributes.
     * The integer variable gl_InstanceID can be used to access the current
     * instance of the mesh being rendered inside the vertex shader.
     * If the instance data is non-null, then it is submitted as a
     * per-instance vertex attribute to the shader.
     *
     * @param mesh The mesh to render
     * @param lod The LOD level to use, see {@link Mesh#setLodLevels(com.jme3.scene.VertexBuffer[]) }.
     * @param count Number of mesh instances to render
     * @param instanceData When count is greater than 1, these buffers provide
     *                     the per-instance attributes.
     */
    public void renderMesh(Mesh mesh, int lod, int count, VertexBuffer[] instanceData);

    /**
     * Resets all previously used {@link NativeObject Native Objects} on this Renderer.
     * The state of the native objects is reset in such way, that using
     * them again will cause the renderer to reupload them.
     * Call this method when you know the GL context is going to shutdown.
     * 
     * @see NativeObject#resetObject() 
     */
    public void resetGLObjects();

    /**
     * Deletes all previously used {@link NativeObject Native Objects} on this Renderer, and
     * then resets the native objects.
     * 
     * @see #resetGLObjects() 
     * @see NativeObject#deleteObject(java.lang.Object) 
     */
    public void cleanup();

    /**
     * Set the default anisotropic filter level for textures.
     *
     * If the
     * {@link Texture#setAnisotropicFilter(int) texture anisotropic filter} is
     * set to 0, then the default level is used. Otherwise if the texture level
     * is 1 or greater, then the texture's value overrides the default value.
     *
     * @param level The default anisotropic filter level to use. Default: 1.
     *
     * @throws IllegalArgumentException If level is less than 1.
     */
    public void setDefaultAnisotropicFilter(int level);

    /**
     * Sets the alpha to coverage state.
     * <p>
     * When alpha coverage and multi-sampling is enabled, 
     * each pixel will contain alpha coverage in all
     * of its subsamples, which is then combined when
     * other future alpha-blended objects are rendered.
     * </p>
     * <p>
     * Alpha-to-coverage is useful for rendering transparent objects
     * without having to worry about sorting them.
     * </p>
     */
    public void setAlphaToCoverage(boolean value);
    
      /**
      * If enabled, color values rendered to the main framebuffer undergo 
      * linear -&gt; sRGB conversion.
      * 
      * This is identical to {@link FrameBuffer#setSrgb(boolean)} except it is toggled
      * for the main framebuffer instead of an offscreen buffer.
      *
      * This should be set together with {@link Renderer#setLinearizeSrgbImages(boolean)}
      *
      * As a shorthand, the user can set {@link AppSettings#setGammaCorrection(boolean)} to true
      * to toggle both {@link Renderer#setLinearizeSrgbImages(boolean)} and
      * {@link Renderer#setMainFrameBufferSrgb(boolean)} if the 
      * {@link Caps#Srgb} is supported by the GPU.
      *
      * @throws RendererException If the GPU hardware does not support sRGB.
      *
      * @see FrameBuffer#setSrgb(boolean)
      * @see Caps#Srgb
      */
     public void setMainFrameBufferSrgb(boolean srgb);
     
       /**
      * If enabled, all {@link Image images} with the {@link Image#setColorSpace(com.jme3.texture.image.ColorSpace) sRGB flag}
      * set shall undergo an sRGB to linear RGB color conversion when read by a shader.
      *
      * The conversion is performed for the following formats:
      *  - {@link Image.Format#RGB8}
      *  - {@link Image.Format#RGBA8}
      *  - {@link Image.Format#Luminance8}
      *  - {@link Image.Format#Luminance8Alpha8}
      *  - {@link Image.Format#DXT1}
      *  - {@link Image.Format#DXT1A}
      *  - {@link Image.Format#DXT3}
      *  - {@link Image.Format#DXT5}
      * 
      * For all other formats, no conversion is performed.
      *
      * If this option is toggled at runtime, textures must be reloaded for the change to take effect.
      *
      * @throws RendererException If the GPU hardware does not support sRGB.
      *
      * @param linearize If sRGB images undergo sRGB -&gt; linear conversion prior to rendering.
      *
      * @see Caps#Srgb
      */
     public void setLinearizeSrgbImages(boolean linearize);


    /**
     * Generates a pool of gpu queries meant to use as profiling tasks
     *
     * @param numTasks the number of task ids to generate
     * @return an array of tasks ids.
     */
    public int[] generateProfilingTasks(int numTasks);

    /**
     * Starts a time profiling task on the GPU.
     * This will profile all operations called between startProfiling and stopProfiling
     *
     * @param taskId the id of the task to start profiling.
     */
    public void startProfiling(int taskId);

    /**
     * Will stop the last profiling task started with startProfiling
     */
    public void stopProfiling();

    /**
     * Returns the time in nano seconds elapsed for the task with the given id.
     * Note that the result may not be available right after stopProfiling has been called.
     * You need to check if the result is available with isTaskResultAvailable.
     * Also note that it's guaranteed that the result will be available on next frame.
     * If you use getProfilingTime on the next frame you called stopProfiling, you don't need to check the result availability with isTaskResultAvailable
     *
     * @param taskId the id of the task given by startProfiling.
     * @return the time in nano second of the profiling task with the given id.
     */
    public long getProfilingTime(int taskId);

    /**
     * Check if the profiling results are available
     *
     * @param taskId the id of the task provided by startProfiling
     * @return true if the resulst of the task with the given task id are available.
     */
    public boolean isTaskResultAvailable(int taskId);

}
