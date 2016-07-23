/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.opencl.Image.ImageDescriptor;
import com.jme3.opencl.Image.ImageFormat;
import com.jme3.opencl.Image.ImageType;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The central OpenCL context. Every action starts from here.
 * The context can be obtained by {@link com.jme3.system.JmeContext#getOpenCLContext() }.
 * <p>
 * The context is used to:
 * <ul>
 *  <li>Query the available devices</li>
 *  <li>Create a command queue</li>
 *  <li>Create buffers and images</li>
 *  <li>Created buffers and images shared with OpenGL vertex buffers, textures and renderbuffers</li>
 *  <li>Create program objects from source code and source files</li>
 * </ul>
 * @author shaman
 */
public abstract class Context extends AbstractOpenCLObject {
    private static final Logger LOG = Logger.getLogger(Context.class.getName());

    protected Context(ObjectReleaser releaser) {
        super(releaser);
    }

	@Override
	public Context register() {
		super.register();
		return this;
	}

    /**
     * Returns all available devices for this context.
     * These devices all belong to the same {@link Platform}.
     * They are used to create a command queue sending commands to a particular
     * device, see {@link #createQueue(com.jme3.opencl.Device) }.
     * Also, device capabilities, like the supported OpenCL version, extensions,
     * memory size and so on, are queried over the Device instances.
     * <br>
     * The available devices were specified by a {@link PlatformChooser}.
     * @return 
     */
    public abstract List<? extends Device> getDevices();

    /**
     * Alternative version of {@link #createQueue(com.jme3.opencl.Device) },
     * just uses the first device returned by {@link #getDevices() }.
     * @return the command queue
     */
    public CommandQueue createQueue() {
        return createQueue(getDevices().get(0));
    }
    /**
     * Creates a command queue sending commands to the specified device.
     * The device must be an entry of {@link #getDevices() }.
     * @param device the target device
     * @return the command queue
     */
	public abstract CommandQueue createQueue(Device device);

    /**
     * Allocates a new buffer of the specific size and access type on the device.
     * @param size the size of the buffer in bytes
     * @param access the allowed access of this buffer from kernel code
     * @return the new buffer
     */
    public abstract Buffer createBuffer(long size, MemoryAccess access);
    /**
     * Alternative version of {@link #createBuffer(long, com.jme3.opencl.MemoryAccess) },
     * creates a buffer with read and write access.
     * @param size the size of the buffer in bytes
     * @return the new buffer
     */
    public Buffer createBuffer(long size) {
        return createBuffer(size, MemoryAccess.READ_WRITE);
    }

    /**
     * Creates a new buffer wrapping the specific host memory. This host memory
     * specified by a ByteBuffer can then be used directly by kernel code,
     * although the access might be slower than with native buffers
     * created by {@link #createBuffer(long, com.jme3.opencl.MemoryAccess) }.
     * @param data the host buffer to use
     * @param access the allowed access of this buffer from kernel code
     * @return the new buffer
     */
    public abstract Buffer createBufferFromHost(ByteBuffer data, MemoryAccess access);
    /**
     * Alternative version of {@link #createBufferFromHost(java.nio.ByteBuffer, com.jme3.opencl.MemoryAccess) },
     * creates a buffer with read and write access.
     * @param data the host buffer to use
     * @return the new buffer
     */
    public Buffer createBufferFromHost(ByteBuffer data) {
        return createBufferFromHost(data, MemoryAccess.READ_WRITE);
    }

    /**
     * Creates a new 1D, 2D, 3D image.<br>
     * {@code ImageFormat} specifies the element type and order, like RGBA of floats.<br>
     * {@code ImageDescriptor} specifies the dimension of the image.<br>
     * Furthermore, a ByteBuffer can be specified in the ImageDescriptor together
     * with row and slice pitches. This buffer is then used to store the image.
     * If no ByteBuffer is specified, a new buffer is allocated (this is the
     * normal behaviour).
     * @param access the allowed access of this image from kernel code
     * @param format the image format
     * @param descr the image descriptor
     * @return the new image object
     */
    public abstract Image createImage(MemoryAccess access, ImageFormat format, ImageDescriptor descr);
	//TODO: add simplified methods for 1D, 2D, 3D textures
    
    /**
     * Queries all supported image formats for a specified memory access and
     * image type.
     * <br>
     * Note that the returned array may contain {@code ImageFormat} objects
     * where {@code ImageChannelType} or {@code ImageChannelOrder} are {@code null}
     * (or both). This is the case when the device supports new formats that
     * are not included in this wrapper yet.
     * @param access the memory access type
     * @param type the image type (1D, 2D, 3D, ...)
     * @return an array of all supported image formats
     */
    public abstract ImageFormat[] querySupportedFormats(MemoryAccess access, ImageType type);
    
	//Interop
    /**
     * Creates a shared buffer from a VertexBuffer. 
     * The returned buffer and the vertex buffer operate on the same memory, 
     * changes in one view are visible in the other view.
     * This can be used to modify meshes directly from OpenCL (e.g. for particle systems).
     * <br>
     * <b>Note:</b> The vertex buffer must already been uploaded to the GPU,
     * i.e. it must be used at least once for drawing.
     * <p>
     * Before the returned buffer can be used, it must be acquried explicitly
     * by {@link Buffer#acquireBufferForSharingAsync(com.jme3.opencl.CommandQueue) }
     * and after modifying it, released by {@link Buffer#releaseBufferForSharingAsync(com.jme3.opencl.CommandQueue) }.
     * This is needed so that OpenGL and OpenCL operations do not interfer with each other.
     * @param vb the vertex buffer to share
     * @param access the memory access for the kernel
     * @return the new buffer
     */
    public abstract Buffer bindVertexBuffer(VertexBuffer vb, MemoryAccess access);

    /**
     * Creates a shared image object from a jME3-image.
     * The returned image shares the same memory with the jME3-image, changes
     * in one view are visible in the other view.
     * This can be used to modify textures and images directly from OpenCL
     * (e.g. for post processing effects and other texture effects).
     * <br>
     * <b>Note:</b> The image must already been uploaded to the GPU,
     * i.e. it must be used at least once for drawing.
     * <p>
     * Before the returned image can be used, it must be acquried explicitly
     * by {@link Image#acquireImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * and after modifying it, released by {@link Image#releaseImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * This is needed so that OpenGL and OpenCL operations do not interfer with each other.
     * 
     * @param image the jME3 image object
     * @param textureType the texture type (1D, 2D, 3D), since this is not stored in the image
     * @param miplevel the mipmap level that should be shared
     * @param access the allowed memory access for kernels
     * @return the OpenCL image
     */
    public abstract Image bindImage(com.jme3.texture.Image image, Texture.Type textureType, int miplevel, MemoryAccess access);
    /**
     * Creates a shared image object from a jME3 texture.
     * The returned image shares the same memory with the jME3 texture, changes
     * in one view are visible in the other view.
     * This can be used to modify textures and images directly from OpenCL
     * (e.g. for post processing effects and other texture effects).
     * <br>
     * <b>Note:</b> The image must already been uploaded to the GPU,
     * i.e. it must be used at least once for drawing.
     * <p>
     * Before the returned image can be used, it must be acquried explicitly
     * by {@link Image#acquireImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * and after modifying it, released by {@link Image#releaseImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * This is needed so that OpenGL and OpenCL operations do not interfer with each other.
     * <p>
     * This method is equivalent to calling
     * {@code bindImage(texture.getImage(), texture.getType(), miplevel, access)}.
     * 
     * @param texture the jME3 texture
     * @param miplevel the mipmap level that should be shared
     * @param access the allowed memory access for kernels
     * @return the OpenCL image
     */
    public Image bindImage(Texture texture, int miplevel, MemoryAccess access) {
        return bindImage(texture.getImage(), texture.getType(), miplevel, access);
    }
    /**
     * Alternative version to {@link #bindImage(com.jme3.texture.Texture, int, com.jme3.opencl.MemoryAccess) },
     * uses {@code miplevel=0}. 
     * @param texture the jME3 texture
     * @param access the allowed memory access for kernels
     * @return the OpenCL image
     */
    public Image bindImage(Texture texture, MemoryAccess access) {
        return bindImage(texture, 0, access);
    }
    /**
     * Creates a shared image object from a jME3 render buffer.
     * The returned image shares the same memory with the jME3 render buffer, changes
     * in one view are visible in the other view.
     * <br>
     * This can be used as an alternative to post processing effects
     * (e.g. reduce sum operations, needed e.g. for tone mapping).
     * <br>
     * <b>Note:</b> The renderbuffer must already been uploaded to the GPU,
     * i.e. it must be used at least once for drawing.
     * <p>
     * Before the returned image can be used, it must be acquried explicitly
     * by {@link Image#acquireImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * and after modifying it, released by {@link Image#releaseImageForSharingAsync(com.jme3.opencl.CommandQueue) }
     * This is needed so that OpenGL and OpenCL operations do not interfer with each other.
     * 
     * @param buffer
     * @param access
     * @return 
     */
    public Image bindRenderBuffer(FrameBuffer.RenderBuffer buffer, MemoryAccess access) {
        if (buffer.getTexture() == null) {
            return bindPureRenderBuffer(buffer, access);
        } else {
            return bindImage(buffer.getTexture(), access);
        }
    }
    protected abstract Image bindPureRenderBuffer(FrameBuffer.RenderBuffer buffer, MemoryAccess access);

    /**
     * Creates a program object from the provided source code.
     * The program still needs to be compiled using {@link Program#build() }.
     * 
     * @param sourceCode the source code
     * @return the program object
     */
    public abstract Program createProgramFromSourceCode(String sourceCode);
    
    /**
     * Resolves dependencies (using {@code #include } in the source code)
     * and delegates the combined source code to
     * {@link #createProgramFromSourceCode(java.lang.String) }.
     * Important: only absolute paths are allowed.
     * @param sourceCode the original source code
     * @param assetManager the asset manager to load the files
     * @return the created program object
     * @throws AssetNotFoundException if a dependency could not be loaded
     */
    public Program createProgramFromSourceCodeWithDependencies(String sourceCode, AssetManager assetManager) {
        StringBuilder builder = new StringBuilder(sourceCode.length());
        BufferedReader reader = new BufferedReader(new StringReader(sourceCode));
        try {
            buildSourcesRec(reader, builder, assetManager);
        } catch (IOException ex) {
            throw new AssetNotFoundException("Unable to read a dependency file", ex);
        }
        return createProgramFromSourceCode(builder.toString());
    }
    private void buildSourcesRec(BufferedReader reader, StringBuilder builder, AssetManager assetManager) throws IOException {
        String ln;
        while ((ln = reader.readLine()) != null) {
            if (ln.trim().startsWith("#import ")) {
                ln = ln.trim().substring(8).trim();
                if (ln.startsWith("\"")) {
                    ln = ln.substring(1);
                }
                if (ln.endsWith("\"")) {
                    ln = ln.substring(0, ln.length()-1);
                }
                AssetInfo info = assetManager.locateAsset(new AssetKey<String>(ln));
                if (info == null) {
                    throw new AssetNotFoundException("Unable to load source file \""+ln+"\"");
                }
                try (BufferedReader r = new BufferedReader(new InputStreamReader(info.openStream()))) {
                    builder.append("//-- begin import ").append(ln).append(" --\n");
                    buildSourcesRec(r, builder, assetManager);
                    builder.append("//-- end import ").append(ln).append(" --\n");
                }
            } else {
                builder.append(ln).append('\n');
            }
        }
    }
    
    /**
     * Creates a program object from the provided source code and files.
     * The source code is made up from the specified include string first, 
     * then all files specified by the resource array (array of asset paths)
     * are loaded by the provided asset manager and appended to the source code.
     * <p>
     * The typical use case is:
     * <ul>
     *  <li>The include string contains some compiler constants like the grid size </li>
     *  <li>Some common OpenCL files used as libraries (Convention: file names end with {@code .clh}</li>
     *  <li>One main OpenCL file containing the actual kernels (Convention: file name ends with {@code .cl})</li>
     * </ul>
     * 
     * After the files were combined, additional include statements are resolved
     * by {@link #createProgramFromSourceCodeWithDependencies(java.lang.String, com.jme3.asset.AssetManager) }.
     * 
     * @param assetManager the asset manager used to load the files
     * @param include an additional include string
     * @param resources an array of asset paths pointing to OpenCL source files
     * @return the new program objects
     * @throws AssetNotFoundException if a file could not be loaded
     */
    public Program createProgramFromSourceFilesWithInclude(AssetManager assetManager, String include, String... resources) {
        return createProgramFromSourceFilesWithInclude(assetManager, include, Arrays.asList(resources));
    }

    /**
     * Creates a program object from the provided source code and files.
     * The source code is made up from the specified include string first, 
     * then all files specified by the resource array (array of asset paths)
     * are loaded by the provided asset manager and appended to the source code.
     * <p>
     * The typical use case is:
     * <ul>
     *  <li>The include string contains some compiler constants like the grid size </li>
     *  <li>Some common OpenCL files used as libraries (Convention: file names end with {@code .clh}</li>
     *  <li>One main OpenCL file containing the actual kernels (Convention: file name ends with {@code .cl})</li>
     * </ul>
     * 
     * After the files were combined, additional include statements are resolved
     * by {@link #createProgramFromSourceCodeWithDependencies(java.lang.String, com.jme3.asset.AssetManager) }.
     * 
     * @param assetManager the asset manager used to load the files
     * @param include an additional include string
     * @param resources an array of asset paths pointing to OpenCL source files
     * @return the new program objects
     * @throws AssetNotFoundException if a file could not be loaded
     */
    public Program createProgramFromSourceFilesWithInclude(AssetManager assetManager, String include, List<String> resources) {
        StringBuilder str = new StringBuilder();
        str.append(include);
        for (String res : resources) {
            AssetInfo info = assetManager.locateAsset(new AssetKey<String>(res));
            if (info == null) {
                throw new AssetNotFoundException("Unable to load source file \""+res+"\"");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(info.openStream()))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    str.append(line).append('\n');
                }
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "unable to load source file '"+res+"'", ex);
            }
        }
        return createProgramFromSourceCodeWithDependencies(str.toString(), assetManager);
    }

    /**
     * Alternative version of {@link #createProgramFromSourceFilesWithInclude(com.jme3.asset.AssetManager, java.lang.String, java.lang.String...) }
     * with an empty include string
     * @throws AssetNotFoundException if a file could not be loaded
     */
    public Program createProgramFromSourceFiles(AssetManager assetManager, String... resources) {
        return createProgramFromSourceFilesWithInclude(assetManager, "", resources);
    }

    /**
     * Alternative version of {@link #createProgramFromSourceFilesWithInclude(com.jme3.asset.AssetManager, java.lang.String, java.util.List) }
     * with an empty include string
     * @throws AssetNotFoundException if a file could not be loaded
     */
    public Program createProgramFromSourceFiles(AssetManager assetManager, List<String> resources) {
        return createProgramFromSourceFilesWithInclude(assetManager, "", resources);
    }
    
    /**
     * Creates a program from the specified binaries.
     * The binaries are created by {@link Program#getBinary(com.jme3.opencl.Device) }.
     * The returned program still needs to be build using
     * {@link Program#build(java.lang.String, com.jme3.opencl.Device...) }.
     * <b>Important:</b>The device passed to {@code Program.getBinary(..)},
     * this method and {@code Program#build(..)} must be the same.
     * 
     * The binaries are used to build a program cache across multiple launches
     * of the application. The programs build mach faster from binaries than
     * from sources.
     * 
     * @param binaries the binaries
     * @param device the device to use
     * @return the new program
     */
    public abstract Program createProgramFromBinary(ByteBuffer binaries, Device device);

	@Override
	public String toString() {
		return "Context (" + getDevices() + ')';
	}

}
