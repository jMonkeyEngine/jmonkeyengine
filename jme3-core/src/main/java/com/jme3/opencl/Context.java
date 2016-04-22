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
import com.jme3.opencl.Image.ImageDescriptor;
import com.jme3.opencl.Image.ImageFormat;
import com.jme3.opencl.Image.ImageType;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The central OpenCL context. Every actions start from here.
 *
 * @author Sebastian Weiss
 */
public abstract class Context {
    private static final Logger LOG = Logger.getLogger(Context.class.getName());

    public abstract List<? extends Device> getDevices();

    public CommandQueue createQueue() {
        return createQueue(getDevices().get(0));
    }
	public abstract CommandQueue createQueue(Device device);

    public abstract Buffer createBuffer(long size, MemoryAccess access);
    public Buffer createBuffer(long size) {
        return createBuffer(size, MemoryAccess.READ_WRITE);
    }

    public abstract Buffer createBufferFromHost(ByteBuffer data, MemoryAccess access);
    public Buffer createBufferFromHost(ByteBuffer data) {
        return createBufferFromHost(data, MemoryAccess.READ_WRITE);
    }

    public abstract Image createImage(MemoryAccess access, ImageFormat format, ImageDescriptor descr, ByteBuffer hostPtr);
	//TODO: add simplified methods for 1D, 2D, 3D textures
    public abstract ImageFormat[] querySupportedFormats(MemoryAccess access, ImageType type);
    
	//Interop
    public abstract Buffer bindVertexBuffer(VertexBuffer vb, MemoryAccess access);

    public abstract Image bindImage(com.jme3.texture.Image image, Texture.Type textureType, int miplevel, MemoryAccess access);
    public Image bindImage(Texture texture, int miplevel, MemoryAccess access) {
        return bindImage(texture.getImage(), texture.getType(), miplevel, access);
    }
    public Image bindImage(Texture texture, MemoryAccess access) {
        return bindImage(texture, 0, access);
    }
    public Image bindRenderBuffer(FrameBuffer.RenderBuffer buffer, MemoryAccess access) {
        if (buffer.getTexture() == null) {
            return bindPureRenderBuffer(buffer, access);
        } else {
            return bindImage(buffer.getTexture(), access);
        }
    }
    protected abstract Image bindPureRenderBuffer(FrameBuffer.RenderBuffer buffer, MemoryAccess access);

    public abstract Program createProgramFromSourceCode(String sourceCode);
    
    public Program createProgramFromSourceFilesWithInclude(AssetManager assetManager, String include, String... resources) {
        return createProgramFromSourceFilesWithInclude(assetManager, include, Arrays.asList(resources));
    }

    public Program createProgramFromSourceFilesWithInclude(AssetManager assetManager, String include, List<String> resources) {
        StringBuilder str = new StringBuilder();
        str.append(include);
        for (String res : resources) {
            AssetInfo info = assetManager.locateAsset(new AssetKey<String>(res));
            if (info == null) {
                LOG.log(Level.WARNING, "unable to load source file ''{0}''", res);
                continue;
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
        return createProgramFromSourceCode(str.toString());
    }

    public Program createProgramFromSourceFiles(AssetManager assetManager, String... resources) {
        return createProgramFromSourceFilesWithInclude(assetManager, "", resources);
    }

    public Program createProgramFromSourceFiles(AssetManager assetManager, List<String> resources) {
        return createProgramFromSourceFilesWithInclude(assetManager, "", resources);
    }
}
