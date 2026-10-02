/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.renderer.opengl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.RendererException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.NativeObjectManager;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;

/** Shared recording-GL fixture; does not require or emulate a native GPU. */
abstract class FrameBufferTestSupport {
    static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    RecordingGL driver;
    GLRenderer renderer;

    @BeforeEach
    void initializeRenderer() {
        initializeRenderer(0);
    }

    void initializeRenderer(int defaultFramebuffer) {
        driver = new RecordingGL(defaultFramebuffer);
        renderer = new GLRenderer(driver.proxy(GL2.class),
                driver.proxy(GLExt.class), driver.proxy(GLFbo.class));
        renderer.initialize();
    }

    static Texture2D texture() {
        return new Texture2D(8, 8, Image.Format.RGBA8);
    }

    static FrameBuffer framebuffer(Texture2D... textures) {
        FrameBuffer fb = new FrameBuffer(8, 8, 1);
        for (Texture2D texture : textures) {
            fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
        }
        return fb;
    }

    static FrameBuffer bufferedFramebuffer(int colorCount) {
        FrameBuffer fb = new FrameBuffer(8, 8, 1);
        for (int i = 0; i < colorCount; ++i) {
            fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        }
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth));
        return fb;
    }

    Reference<?> registeredReference(FrameBuffer framebuffer) throws ReflectiveOperationException {
        Field managerField = GLRenderer.class.getDeclaredField("objManager");
        managerField.setAccessible(true);
        NativeObjectManager manager = (NativeObjectManager) managerField.get(renderer);
        Field mapField = NativeObjectManager.class.getDeclaredField("refMap");
        mapField.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) mapField.get(manager);
        return (Reference<?>) map.get(framebuffer.getUniqueId());
    }

    static final class RecordingGL implements InvocationHandler {
        final int defaultFramebuffer;
        final Map<Integer, Map<Integer, Integer>> attachments = new HashMap<>();
        final Set<Integer> liveRenderbuffers = new HashSet<>();
        final Set<Integer> liveFramebuffers = new HashSet<>();
        final Set<Integer> liveTextures = new HashSet<>();
        final List<Integer> deletedRenderbuffers = new ArrayList<>();
        final List<Integer> renderbufferStorageCalls = new ArrayList<>();
        final Map<Integer, Integer> renderbufferFormats = new HashMap<>();
        final Map<Integer, List<Integer>> drawBuffers = new HashMap<>();
        final List<Integer> mipmapGenerations = new ArrayList<>();
        final Map<Integer, Integer> boundTextures = new HashMap<>();
        int nextObjectId = 100;
        int reuseRenderbufferId = -1;
        int reuseFramebufferId = -1;
        int boundRenderbuffer;
        int drawFramebuffer;
        int readFramebuffer;
        int blitDrawFramebuffer = -1;
        int blitReadFramebuffer = -1;
        int activeTextureUnit;
        int framebufferStatus = GLFbo.GL_FRAMEBUFFER_COMPLETE_EXT;
        boolean failNextStorage;

        RecordingGL(int defaultFramebuffer) {
            this.defaultFramebuffer = defaultFramebuffer;
            drawFramebuffer = defaultFramebuffer;
            readFramebuffer = defaultFramebuffer;
        }

        <T> T proxy(Class<T> api) {
            return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api}, this));
        }

        int attachment(FrameBuffer fb, int colorSlot) {
            return attachmentAt(fb, GLFbo.GL_COLOR_ATTACHMENT0_EXT + colorSlot);
        }

        int attachmentAt(FrameBuffer fb, int glSlot) {
            return attachments.get(fb.getId()).getOrDefault(glSlot, 0);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "glGetString":
                    return getString((Integer) args[0]);
                case "glGetInteger":
                    ((IntBuffer) args[1]).put(0, getInteger((Integer) args[0]));
                    return null;
                case "glGenTextures":
                    int texture = nextObjectId++;
                    liveTextures.add(texture);
                    ((IntBuffer) args[0]).put(0, texture);
                    return null;
                case "glDeleteTextures":
                    liveTextures.remove(((IntBuffer) args[0]).get(0));
                    return null;
                case "glActiveTexture":
                    activeTextureUnit = (Integer) args[0] - GL.GL_TEXTURE0;
                    return null;
                case "glBindTexture":
                    int textureName = (Integer) args[1];
                    assertTrue(textureName >= 0, "Cannot bind an unallocated image name");
                    boundTextures.put(activeTextureUnit, textureName);
                    return null;
                case "glGenerateMipmapEXT":
                    int generated = boundTextures.getOrDefault(activeTextureUnit, 0);
                    assertTrue(liveTextures.contains(generated), "Mipmap generation needs allocated storage");
                    mipmapGenerations.add(generated);
                    return null;
                case "glGenFramebuffersEXT":
                    int framebuffer = reuseFramebufferId >= 0 ? reuseFramebufferId : nextObjectId++;
                    reuseFramebufferId = -1;
                    liveFramebuffers.add(framebuffer);
                    attachments.put(framebuffer, new HashMap<>());
                    ((IntBuffer) args[0]).put(0, framebuffer);
                    return null;
                case "glGenRenderbuffersEXT":
                    int renderbuffer = reuseRenderbufferId >= 0 ? reuseRenderbufferId : nextObjectId++;
                    reuseRenderbufferId = -1;
                    liveRenderbuffers.add(renderbuffer);
                    ((IntBuffer) args[0]).put(0, renderbuffer);
                    return null;
                case "glBindRenderbufferEXT":
                    boundRenderbuffer = (Integer) args[1];
                    return null;
                case "glRenderbufferStorageEXT":
                case "glRenderbufferStorageMultisampleEXT":
                    assertTrue(liveRenderbuffers.contains(boundRenderbuffer),
                            "Storage requires a live, bound renderbuffer");
                    if (failNextStorage) {
                        failNextStorage = false;
                        throw new RendererException("Simulated storage allocation failure");
                    }
                    renderbufferStorageCalls.add(boundRenderbuffer);
                    int formatIndex = method.getName().equals("glRenderbufferStorageEXT") ? 1 : 2;
                    renderbufferFormats.put(boundRenderbuffer, (Integer) args[formatIndex]);
                    return null;
                case "glDeleteRenderbuffersEXT":
                    int deleted = ((IntBuffer) args[0]).get(0);
                    assertTrue(liveRenderbuffers.remove(deleted),
                            "Deleting a stale renderbuffer name " + deleted);
                    deletedRenderbuffers.add(deleted);
                    if (boundRenderbuffer == deleted) {
                        boundRenderbuffer = 0;
                    }
                    return null;
                case "glDeleteFramebuffersEXT":
                    int deletedFramebuffer = ((IntBuffer) args[0]).get(0);
                    assertTrue(liveFramebuffers.remove(deletedFramebuffer),
                            "Deleting a stale framebuffer name");
                    attachments.remove(deletedFramebuffer);
                    return null;
                case "glBindFramebufferEXT":
                    int target = (Integer) args[0];
                    int id = (Integer) args[1];
                    if (target == GLFbo.GL_FRAMEBUFFER_EXT || target == GLFbo.GL_DRAW_FRAMEBUFFER_EXT) {
                        drawFramebuffer = id;
                    }
                    if (target == GLFbo.GL_FRAMEBUFFER_EXT || target == GLFbo.GL_READ_FRAMEBUFFER_EXT) {
                        readFramebuffer = id;
                    }
                    return null;
                case "glFramebufferTexture2DEXT":
                case "glFramebufferRenderbufferEXT":
                    int slot = (Integer) args[1];
                    int attachment = (Integer) args[3];
                    if (slot == GL3.GL_DEPTH_STENCIL_ATTACHMENT) {
                        attachments.get(drawFramebuffer).put(GLFbo.GL_DEPTH_ATTACHMENT_EXT, attachment);
                        attachments.get(drawFramebuffer).put(GL_STENCIL_ATTACHMENT, attachment);
                    } else {
                        attachments.get(drawFramebuffer).put(slot, attachment);
                    }
                    return null;
                case "glDrawBuffer":
                    drawBuffers.put(drawFramebuffer, java.util.Collections.singletonList((Integer) args[0]));
                    return null;
                case "glDrawBuffers":
                    List<Integer> targets = new ArrayList<>();
                    IntBuffer source = ((IntBuffer) args[0]).duplicate();
                    while (source.hasRemaining()) {
                        targets.add(source.get());
                    }
                    drawBuffers.put(drawFramebuffer, targets);
                    return null;
                case "glCheckFramebufferStatusEXT":
                    return framebufferStatus;
                case "glBlitFramebufferEXT":
                    blitDrawFramebuffer = drawFramebuffer;
                    blitReadFramebuffer = readFramebuffer;
                    return null;
                default:
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    throw new AssertionError("Unexpected GL query: " + method.getName());
            }
        }

        private static String getString(int name) {
            switch (name) {
                case GL.GL_VERSION:
                    return "2.1";
                case GL.GL_SHADING_LANGUAGE_VERSION:
                    return "1.20";
                case GL.GL_EXTENSIONS:
                    return "GL_EXT_framebuffer_object GL_EXT_framebuffer_blit GL_EXT_packed_depth_stencil "
                            + "GL_EXT_texture_sRGB GL_ARB_framebuffer_sRGB";
                case GL.GL_VENDOR:
                case GL.GL_RENDERER:
                    return "Recording GL";
                default:
                    throw new AssertionError("Unexpected GL string: " + name);
            }
        }

        private int getInteger(int name) {
            switch (name) {
                case GL.GL_FRAMEBUFFER_BINDING:
                    return defaultFramebuffer;
                case GL.GL_MAX_TEXTURE_SIZE:
                case GL.GL_MAX_CUBE_MAP_TEXTURE_SIZE:
                case GLFbo.GL_MAX_RENDERBUFFER_SIZE_EXT:
                    return 1024;
                case GL.GL_MAX_TEXTURE_IMAGE_UNITS:
                case GL.GL_MAX_VERTEX_ATTRIBS:
                    return 16;
                case GLFbo.GL_MAX_COLOR_ATTACHMENTS_EXT:
                case GLExt.GL_MAX_DRAW_BUFFERS_ARB:
                    return 4;
                case GL2.GL_DRAW_BUFFER:
                case GL2.GL_READ_BUFFER:
                    return GL.GL_BACK;
                default:
                    return 0;
            }
        }
    }
}
