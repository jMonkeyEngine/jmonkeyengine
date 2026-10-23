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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.TextureUnitException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises real framebuffer and image lifecycles against a recording GL driver.
 * In particular, redefining a null-data texture after rendering discards its
 * contents even though the Image has no CPU data to upload.
 */
public class GlRendererFrameBufferMipmapTest {

    private RecordingGL driver;
    private GLRenderer renderer;

    @BeforeEach
    public void initializeRenderer() {
        driver = new RecordingGL();
        renderer = new GLRenderer(driver.proxy(GL2.class),
                driver.proxy(GLExt.class), driver.proxy(GLFbo.class));
        renderer.initialize();
    }

    @Test
    public void testFirstAttachmentAllocatesStorageWithoutGeneratingMipmaps() {
        Texture2D texture = renderTexture();
        Image image = texture.getImage();
        FrameBuffer framebuffer = framebuffer(texture);

        assertNull(image.getData(0));
        assertTrue(image.isGeneratedMipmapsRequired());
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(framebuffer);

        assertAll(
                () -> assertEquals(1, driver.imageDefinitions.size()),
                () -> assertEquals(1, driver.definitionCount(image.getId())),
                () -> assertNull(driver.uploadedData.get(0)),
                () -> assertTrue(driver.mipmapGenerations.isEmpty()),
                () -> assertFalse(image.isUpdateNeeded()),
                () -> assertFalse(image.isMipmapsGenerated()),
                () -> assertFalse(framebuffer.isUpdateNeeded()));
    }

    @Test
    public void testFirstDepartureGeneratesMipmapsWithoutRedefiningStorage() {
        Texture2D texture = renderTexture();
        renderer.setFrameBuffer(framebuffer(texture));
        renderer.clearBuffers(true, false, false);

        renderer.setFrameBuffer(null);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId()),
                        "Leaving the framebuffer must preserve its rendered level zero"),
                () -> assertEquals(1, driver.generationCount(texture.getImage().getId())),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()),
                () -> assertFalse(texture.getImage().isUpdateNeeded()));
    }

    @Test
    public void testRepeatedSamplingReusesFramebufferStorageAndMipmaps() throws TextureUnitException {
        Texture2D texture = renderTexture();
        renderer.setFrameBuffer(framebuffer(texture));
        renderer.setFrameBuffer(null);
        final int definitionsBeforeSampling = driver.imageDefinitions.size();
        final int generationsBeforeSampling = driver.mipmapGenerations.size();

        renderer.setTexture(0, texture);
        renderer.setTexture(1, texture);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(definitionsBeforeSampling, driver.imageDefinitions.size(),
                        "Sampling a rendered texture must not upload null data"),
                () -> assertEquals(generationsBeforeSampling, driver.mipmapGenerations.size()),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testLaterFramesRegenerateMipmapsWithoutAllocatingStorage() throws TextureUnitException {
        Texture2D texture = renderTexture();
        FrameBuffer framebuffer = framebuffer(texture);

        for (int frame = 0; frame < 3; frame++) {
            renderer.setFrameBuffer(framebuffer);
            renderer.clearBuffers(true, false, false);
            renderer.setFrameBuffer(null);
            renderer.setTexture(0, texture);
        }
        // Remaining on the main framebuffer is not another rendering pass.
        renderer.setFrameBuffer(null);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId())),
                () -> assertEquals(3, driver.generationCount(texture.getImage().getId()),
                        "Each rendered frame needs fresh mipmaps, even after the first generation"),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testContextResetReallocatesAndRegeneratesFramebufferTexture() throws TextureUnitException {
        Texture2D texture = renderTexture();
        Image image = texture.getImage();
        FrameBuffer framebuffer = framebuffer(texture);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        final int previousTextureId = image.getId();
        final int previousFramebufferId = framebuffer.getId();

        renderer.resetGLObjects();

        assertAll(
                () -> assertEquals(-1, image.getId()),
                () -> assertEquals(-1, framebuffer.getId()),
                () -> assertTrue(image.isUpdateNeeded()),
                () -> assertTrue(framebuffer.isUpdateNeeded()),
                () -> assertFalse(image.isMipmapsGenerated()));

        renderer.setFrameBuffer(framebuffer);
        assertNotEquals(previousTextureId, image.getId());
        assertNotEquals(previousFramebufferId, framebuffer.getId());
        assertFalse(image.isMipmapsGenerated());
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(previousTextureId)),
                () -> assertEquals(1, driver.definitionCount(image.getId())),
                () -> assertEquals(1, driver.generationCount(previousTextureId)),
                () -> assertEquals(1, driver.generationCount(image.getId())),
                () -> assertTrue(image.isMipmapsGenerated()));
    }

    @Test
    public void testMultipleColorAttachmentsGenerateTheirOwnMipmaps() throws TextureUnitException {
        Texture2D first = renderTexture();
        Texture2D second = renderTexture();
        FrameBuffer framebuffer = framebuffer(first, second);
        framebuffer.setMultiTarget(true);

        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        renderer.setTexture(1, first);
        renderer.setTexture(2, second);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);

        assertNotEquals(first.getImage().getId(), second.getImage().getId());
        assertAll(
                () -> assertEquals(1, driver.definitionCount(first.getImage().getId())),
                () -> assertEquals(1, driver.definitionCount(second.getImage().getId())),
                () -> assertEquals(2, driver.generationCount(first.getImage().getId())),
                () -> assertEquals(2, driver.generationCount(second.getImage().getId())),
                () -> assertTrue(first.getImage().isMipmapsGenerated()),
                () -> assertTrue(second.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testCubemapAttachmentGeneratesTheWholeCubeWithoutRedefiningFaces()
            throws TextureUnitException {
        TextureCubeMap texture = new TextureCubeMap(8, 8, Image.Format.RGBA8);
        texture.setMinFilter(Texture.MinFilter.Trilinear);
        FrameBuffer framebuffer = new FrameBuffer(8, 8, 1);
        framebuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(
                texture, TextureCubeMap.Face.NegativeZ));

        renderer.setFrameBuffer(framebuffer);
        assertEquals(6, driver.definitionCount(texture.getImage().getId()));
        assertEquals(GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, driver.framebufferTextureTargets.get(0));
        for (int face = 0; face < 6; face++) {
            assertEquals(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, driver.definitionTargets.get(face));
            assertNull(driver.uploadedData.get(face));
        }
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(6, driver.definitionCount(texture.getImage().getId()),
                        "Generating cubemap mipmaps must preserve all six rendered face allocations"),
                () -> assertEquals(1, driver.generationCount(texture.getImage().getId())),
                () -> assertEquals(GL.GL_TEXTURE_CUBE_MAP, driver.generationTargets.get(0)),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testGenerationActivatesTheUnitContainingTheFramebufferTexture() throws TextureUnitException {
        Texture2D attachment = renderTexture();
        Texture2D otherTexture = renderTexture();
        FrameBuffer framebuffer = framebuffer(attachment);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);

        renderer.setFrameBuffer(framebuffer);
        // Unit zero still contains the attachment, but unit one is now active.
        renderer.setTexture(1, otherTexture);
        renderer.setFrameBuffer(null);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(attachment.getImage().getId())),
                () -> assertEquals(1, driver.definitionCount(otherTexture.getImage().getId())),
                () -> assertEquals(2, driver.generationCount(attachment.getImage().getId())),
                () -> assertEquals(0, driver.generationCount(otherTexture.getImage().getId()),
                        "Generation must not operate on the texture in the previously active unit"));
    }

    @Test
    public void testLateMipmapFilterOnMultisampleTextureIsStillRejected() {
        Texture2D texture = new Texture2D(8, 8, 4, Image.Format.RGBA8);
        FrameBuffer framebuffer = new FrameBuffer(8, 8, 4);
        framebuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
        renderer.setFrameBuffer(framebuffer);
        texture.setMinFilter(Texture.MinFilter.Trilinear);

        RendererException departureError = assertThrows(RendererException.class,
                () -> renderer.setFrameBuffer(null));
        RendererException samplingError = assertThrows(RendererException.class,
                () -> renderer.setTexture(0, texture));

        assertAll(
                () -> assertEquals("Multisample textures do not support mipmaps",
                        departureError.getMessage()),
                () -> assertEquals("Multisample textures do not support mipmaps", samplingError.getMessage()),
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId())),
                () -> assertTrue(driver.mipmapGenerations.isEmpty()));
    }

    @Test
    public void testDisabledHintPreservesStorageUntilGenerationIsEnabled() throws TextureUnitException {
        Texture2D texture = renderTexture();
        FrameBuffer framebuffer = framebuffer(texture);
        framebuffer.setMipMapsGenerationHint(false);

        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertTrue(driver.mipmapGenerations.isEmpty()),
                () -> assertFalse(texture.getImage().isMipmapsGenerated()),
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId()),
                        "Disabling automatic mipmaps must not make sampling discard the render target"));

        framebuffer.setMipMapsGenerationHint(true);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId())),
                () -> assertEquals(1, driver.generationCount(texture.getImage().getId())),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testEnabledHintOverridesDisabledRendererDefault() {
        Texture2D texture = renderTexture();
        FrameBuffer framebuffer = framebuffer(texture);
        renderer.setGenerateMipmapsForFrameBuffer(false);
        framebuffer.setMipMapsGenerationHint(true);

        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId())),
                () -> assertEquals(1, driver.generationCount(texture.getImage().getId())),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testNullHintFollowsChangesToRendererDefault() {
        Texture2D texture = renderTexture();
        FrameBuffer framebuffer = framebuffer(texture);
        assertNull(framebuffer.getMipMapsGenerationHint());
        renderer.setGenerateMipmapsForFrameBuffer(false);

        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        assertTrue(driver.mipmapGenerations.isEmpty());
        assertFalse(texture.getImage().isMipmapsGenerated());

        renderer.setGenerateMipmapsForFrameBuffer(true);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId())),
                () -> assertEquals(1, driver.generationCount(texture.getImage().getId())),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testEnablingMipmapFilterAfterAllocationReopensGeneratedMipRange()
            throws TextureUnitException {
        Texture2D texture = new Texture2D(8, 8, Image.Format.RGBA8);
        FrameBuffer framebuffer = framebuffer(texture);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        int textureId = texture.getImage().getId();
        assertEquals(0, driver.maxLevels.get(textureId));

        texture.setMinFilter(Texture.MinFilter.Trilinear);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(textureId)),
                () -> assertEquals(1, driver.generationCount(textureId)),
                () -> assertEquals(3, driver.generatedMaxLevels.get(textureId),
                        "The full mip range must be opened before generation"),
                () -> assertEquals(3, driver.maxLevels.get(textureId)),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testExplicitMipChainKeepsItsRangeWithHintsOffAndOn() throws TextureUnitException {
        Texture2D texture = renderTexture();
        // Allocate only levels zero and one; later levels are intentionally absent.
        texture.getImage().setMipMapSizes(new int[] {8 * 8 * 4, 4 * 4 * 4});
        FrameBuffer framebuffer = framebuffer(texture);
        framebuffer.setMipMapsGenerationHint(false);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);
        int textureId = texture.getImage().getId();

        assertAll(
                () -> assertEquals(2, driver.definitionCount(textureId)),
                () -> assertTrue(driver.mipmapGenerations.isEmpty()),
                () -> assertEquals(1, driver.maxLevels.get(textureId)));

        framebuffer.setMipMapsGenerationHint(true);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(2, driver.definitionCount(textureId)),
                () -> assertEquals(1, driver.generationCount(textureId)),
                () -> assertEquals(1, driver.generatedMaxLevels.get(textureId),
                        "Automatic generation must respect an explicit mip-chain range"),
                () -> assertEquals(1, driver.maxLevels.get(textureId)),
                () -> assertTrue(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testTextureWithoutMipmapFilterDoesNotGenerateMipmaps() throws TextureUnitException {
        Texture2D texture = new Texture2D(8, 8, Image.Format.RGBA8);
        renderer.setFrameBuffer(framebuffer(texture));
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(texture.getImage().getId())),
                () -> assertTrue(driver.mipmapGenerations.isEmpty()),
                () -> assertFalse(texture.getImage().isMipmapsGenerated()));
    }

    @Test
    public void testCpuBackedTextureStillUploadsAndGeneratesMipmapsWhenDirty() throws TextureUnitException {
        ByteBuffer pixels = ByteBuffer.allocateDirect(8 * 8 * 4);
        Image image = new Image(Image.Format.RGBA8, 8, 8, pixels, ColorSpace.Linear);
        Texture2D texture = new Texture2D(image);
        texture.setMinFilter(Texture.MinFilter.Trilinear);

        renderer.setTexture(0, texture);
        renderer.setTexture(1, texture);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(image.getId())),
                () -> assertSame(pixels, driver.uploadedData.get(0)),
                () -> assertEquals(1, driver.generationCount(image.getId())),
                () -> assertEquals(3, driver.generatedMaxLevels.get(image.getId())),
                () -> assertTrue(image.isMipmapsGenerated()),
                () -> assertFalse(image.isUpdateNeeded()));

        image.setUpdateNeeded();
        assertFalse(image.isMipmapsGenerated());
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(2, driver.definitionCount(image.getId())),
                () -> assertSame(pixels, driver.uploadedData.get(1)),
                () -> assertEquals(2, driver.generationCount(image.getId())),
                () -> assertTrue(image.isMipmapsGenerated()),
                () -> assertFalse(image.isUpdateNeeded()));
    }

    @Test
    public void testLateMipmapFilterPreservesRenderedContentOfCpuBackedTexture() throws TextureUnitException {
        ByteBuffer pixels = ByteBuffer.allocateDirect(8 * 8 * 4);
        Image image = new Image(Image.Format.RGBA8, 8, 8, pixels, ColorSpace.Linear);
        Texture2D texture = new Texture2D(image);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        FrameBuffer framebuffer = framebuffer(texture);
        renderer.setFrameBuffer(framebuffer);
        renderer.setFrameBuffer(null);
        assertSame(pixels, driver.uploadedData.get(0));
        assertEquals(0, driver.maxLevels.get(image.getId()));

        texture.setMinFilter(Texture.MinFilter.Trilinear);
        renderer.setFrameBuffer(framebuffer);
        renderer.clearBuffers(true, false, false);
        renderer.setFrameBuffer(null);
        renderer.setTexture(0, texture);

        assertAll(
                () -> assertEquals(1, driver.definitionCount(image.getId()),
                        "Leaving a framebuffer must not overwrite rendered content with old CPU data"),
                () -> assertEquals(1, driver.generationCount(image.getId())),
                () -> assertEquals(3, driver.generatedMaxLevels.get(image.getId())),
                () -> assertTrue(image.isMipmapsGenerated()));
    }

    private static Texture2D renderTexture() {
        Texture2D texture = new Texture2D(8, 8, Image.Format.RGBA8);
        texture.setMinFilter(Texture.MinFilter.Trilinear);
        return texture;
    }

    private static FrameBuffer framebuffer(Texture2D... textures) {
        FrameBuffer framebuffer = new FrameBuffer(8, 8, 1);
        for (Texture2D texture : textures) {
            framebuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
        }
        return framebuffer;
    }

    /**
     * Supplies a minimal desktop GL 2.1 context with framebuffer support, and
     * records the actual texture receiving each storage or mipmap operation.
     * The renderer itself initializes capabilities, formats, and object state.
     */
    private static final class RecordingGL implements InvocationHandler {
        private final List<Integer> imageDefinitions = new ArrayList<>();
        private final List<ByteBuffer> uploadedData = new ArrayList<>();
        private final List<Integer> definitionTargets = new ArrayList<>();
        private final List<Integer> framebufferTextureTargets = new ArrayList<>();
        private final List<Integer> generationTargets = new ArrayList<>();
        private final List<Integer> mipmapGenerations = new ArrayList<>();
        private final Map<Integer, Integer> textureBindings = new HashMap<>();
        private final Map<Integer, Integer> maxLevels = new HashMap<>();
        private final Map<Integer, Integer> generatedMaxLevels = new HashMap<>();
        private int activeTextureUnit;
        private int nextObjectId = 1;

        <T> T proxy(Class<T> api) {
            return api.cast(Proxy.newProxyInstance(api.getClassLoader(),
                    new Class<?>[] {api}, this));
        }

        int definitionCount(int textureId) {
            return count(imageDefinitions, textureId);
        }

        int generationCount(int textureId) {
            return count(mipmapGenerations, textureId);
        }

        private static int count(List<Integer> calls, int textureId) {
            int count = 0;
            for (int calledTextureId : calls) {
                if (calledTextureId == textureId) {
                    count++;
                }
            }
            return count;
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
                case "glGenFramebuffersEXT":
                    ((IntBuffer) args[0]).put(0, nextObjectId++);
                    return null;
                case "glCheckFramebufferStatusEXT":
                    return GLFbo.GL_FRAMEBUFFER_COMPLETE_EXT;
                case "glActiveTexture":
                    activeTextureUnit = (Integer) args[0] - GL.GL_TEXTURE0;
                    return null;
                case "glBindTexture":
                    assertTrue((Integer) args[0] == GL.GL_TEXTURE_2D
                            || (Integer) args[0] == GLExt.GL_TEXTURE_2D_MULTISAMPLE
                            || (Integer) args[0] == GL.GL_TEXTURE_CUBE_MAP);
                    textureBindings.put(activeTextureUnit, (Integer) args[1]);
                    return null;
                case "glTexImage2D":
                    int target = (Integer) args[0];
                    assertTrue(target == GL.GL_TEXTURE_2D
                            || (target >= GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X
                            && target <= GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z));
                    definitionTargets.add(target);
                    int level = (Integer) args[1];
                    assertEquals(Math.max(1, 8 >> level), args[3]);
                    assertEquals(Math.max(1, 8 >> level), args[4]);
                    uploadedData.add((ByteBuffer) args[8]);
                    imageDefinitions.add(boundTexture());
                    return null;
                case "glFramebufferTexture2DEXT":
                    framebufferTextureTargets.add((Integer) args[2]);
                    return null;
                case "glReadPixels":
                    throw new AssertionError(
                            "Framebuffer mipmap generation must not read pixels back to the CPU");
                case "glTexImage2DMultisample":
                    assertEquals(GLExt.GL_TEXTURE_2D_MULTISAMPLE, args[0]);
                    assertEquals(4, args[1]);
                    assertEquals(8, args[3]);
                    assertEquals(8, args[4]);
                    imageDefinitions.add(boundTexture());
                    return null;
                case "glTexParameteri":
                    if ((Integer) args[1] == GL2.GL_TEXTURE_MAX_LEVEL) {
                        maxLevels.put(boundTexture(), (Integer) args[2]);
                    }
                    return null;
                case "glGenerateMipmapEXT":
                    assertTrue((Integer) args[0] == GL.GL_TEXTURE_2D
                            || (Integer) args[0] == GL.GL_TEXTURE_CUBE_MAP,
                            "Mipmaps require a texture target, not an individual cubemap face");
                    generationTargets.add((Integer) args[0]);
                    int textureId = boundTexture();
                    mipmapGenerations.add(textureId);
                    generatedMaxLevels.put(textureId, maxLevels.get(textureId));
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

        private int boundTexture() {
            Integer textureId = textureBindings.get(activeTextureUnit);
            assertTrue(textureId != null && textureId > 0,
                    "Storage and mipmap operations require a bound texture");
            return textureId;
        }

        private static String getString(int name) {
            switch (name) {
                case GL.GL_VERSION:
                    return "2.1";
                case GL.GL_SHADING_LANGUAGE_VERSION:
                    return "1.20";
                case GL.GL_EXTENSIONS:
                    return "GL_EXT_framebuffer_object GL_ARB_texture_multisample";
                case GL.GL_VENDOR:
                case GL.GL_RENDERER:
                    return "Recording GL";
                default:
                    throw new AssertionError("Unexpected GL string: " + name);
            }
        }

        private static int getInteger(int name) {
            switch (name) {
                case GL.GL_MAX_TEXTURE_SIZE:
                case GL.GL_MAX_CUBE_MAP_TEXTURE_SIZE:
                case GLFbo.GL_MAX_RENDERBUFFER_SIZE_EXT:
                    return 1024;
                case GL.GL_MAX_TEXTURE_IMAGE_UNITS:
                case GL.GL_MAX_VERTEX_ATTRIBS:
                    return 16;
                case GLFbo.GL_MAX_COLOR_ATTACHMENTS_EXT:
                case GLExt.GL_MAX_DRAW_BUFFERS_ARB:
                case GLExt.GL_MAX_COLOR_TEXTURE_SAMPLES:
                case GLExt.GL_MAX_DEPTH_TEXTURE_SAMPLES:
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
