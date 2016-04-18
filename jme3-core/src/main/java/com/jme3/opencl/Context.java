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

import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexByteBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * The central OpenCL context. Every actions start from here.
 *
 * @author Sebastian Weiss
 */
public final class Context extends NativeCLObject {

    private final long context;

    public Context(long context) {
        this.context = context;
    }

    public List<? extends Device> getDevices() {
        throw new UnsupportedOperationException("not supported yet");
    }

    public CommandQueue createQueue() {
        throw new UnsupportedOperationException("not supported yet");
    }
	//TODO: constructor with specific device and properties

    public Buffer createBuffer(int size, MemoryAccess access) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Buffer createBuffer(int size) {
        return createBuffer(size, MemoryAccess.READ_WRITE);
    }

    public Buffer useHostBuffer(ByteBuffer data, int size, MemoryAccess access) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Buffer useHostBuffer(ByteBuffer data, int size) {
        return useHostBuffer(data, size, MemoryAccess.READ_WRITE);
    }

    @Override
    public void deleteObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static enum ImageChannelOrder {

        R, Rx, A,
        INTENSITY,
        LUMINANCE,
        RG, RGx, RA,
        RGB, RGBx,
        RGBA,
        ARGB, BGRA
    }

    public static enum ImageChannelType {

        SNORM_INT8,
        SNORM_INT16,
        UNORM_INT8,
        UNROM_INT16,
        UNORM_SHORT_565,
        UNROM_SHORT_555,
        UNORM_INT_101010,
        SIGNED_INT8,
        SIGNED_INT16,
        SIGNED_INT32,
        UNSIGNED_INT8,
        UNSIGNED_INT16,
        UNSIGNED_INT32,
        HALF_FLOAT,
        FLOAT
    }

    public static class ImageFormat { //Struct

        public ImageChannelOrder channelOrder;
        public ImageChannelType channelType;
    }

    public static enum ImageType {

        IMAGE_1D,
        IMAGE_1D_BUFFER,
        IMAGE_2D,
        IMAGE_3D,
        IMAGE_1D_ARRAY,
        IMAGE_2D_ARRAY
    }

    public static class ImageDescriptor { //Struct

        public ImageType type;
        public int width;
        public int height;
        public int depth;
        public int arraySize;
        public int rowPitch;
        public int slicePitch;
        public int numMipLevels;
        public int numSamples;
        public Buffer buffer;
    }

    public Buffer createImage(MemoryAccess access, ImageFormat format, ImageDescriptor descr, ByteBuffer hostPtr) {
        throw new UnsupportedOperationException("not supported yet");
    }
	//TODO: add simplified methods for 1D, 2D, 3D textures

	//Interop
    public Buffer bindVertexBuffer(VertexBuffer vb) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Buffer bindIndexBuffer(IndexBuffer ib) {
        if (!(ib instanceof IndexByteBuffer)
                && !(ib instanceof IndexShortBuffer)
                && !(ib instanceof IndexIntBuffer)) {
            throw new IllegalArgumentException("Index buffer must be an IndexByteBuffer, IndexShortBuffer or IndexIntBuffer");
        }
        throw new UnsupportedOperationException("not supported yet");
    }

    public Buffer bindImage(Image image) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Program createProgramFromSourceCode(String sourceCode) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Program createProgramFromSourceFilesWithInclude(
            String include, String... resources) {
        //TODO: load resources
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Program createProgramFormSourcesWithInclude(String include, List<String> resources) {
        return createProgramFromSourceFilesWithInclude(include, resources.toArray(new String[resources.size()]));
    }

    public Program createProgramFromSources(String... resources) {
        return createProgramFromSourceFilesWithInclude(null, resources);
    }

    public Program createProgramFromSources(List<String> resources) {
        return createProgramFormSourcesWithInclude(null, resources);
    }

}
