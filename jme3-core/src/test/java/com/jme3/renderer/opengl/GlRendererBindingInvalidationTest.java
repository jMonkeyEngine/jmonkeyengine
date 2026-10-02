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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.renderer.RenderContext;
import com.jme3.renderer.RendererException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Test-only model of native bindings; intentionally separate from RenderContext. */
public class GlRendererBindingInvalidationTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void textureZeroAfterExternalActiveUnit(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        driver.activeUnit = 3;
        driver.textureBindings[0] = 16;
        driver.renderer.invalidateState();
        driver.renderer.setTexture(0, texture(17));
        assertEquals(17, driver.textureBindings[0]);
        assertEquals(0, driver.activeUnit);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void repeatedTextureZeroAfterInvalidation(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        Texture2D texture = texture(17);
        driver.renderer.setTexture(0, texture);
        driver.activeUnit = 3;
        driver.textureBindings[0] = 16;
        driver.renderer.invalidateState();
        driver.renderer.setTexture(0, texture);
        assertEquals(17, driver.textureBindings[0]);
        driver.calls.clear();
        driver.renderer.setTexture(0, texture);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void nonzeroTextureUnitControl(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        driver.activeUnit = 3;
        driver.renderer.invalidateState();
        driver.renderer.setTexture(1, texture(17));
        assertEquals(17, driver.textureBindings[1]);
        assertEquals(1, driver.activeUnit);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void freshTextureAndRepeatControl(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        Texture2D texture = texture(17);
        driver.renderer.setTexture(0, texture);
        assertEquals(17, driver.textureBindings[0]);
        assertFalse(driver.calls.contains("glActiveTexture"));
        driver.calls.clear();
        driver.renderer.setTexture(0, texture);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    public void defaultFramebufferAfterExternalBinding(int defaultId) throws Exception {
        Recording driver = new Recording(false);
        driver.setDefaultFramebuffer(defaultId);
        driver.setNativeFramebuffer(9);
        driver.renderer.invalidateState();
        driver.calls.clear();
        driver.renderer.setFrameBuffer(null);
        assertEquals(defaultId, driver.drawFramebuffer);
        assertEquals(1, driver.calls.stream().filter("glBindFramebufferEXT"::equals).count());
        driver.calls.clear();
        driver.renderer.setFrameBuffer(null);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    public void explicitFramebufferControl(int defaultId) throws Exception {
        Recording driver = new Recording(false);
        driver.setDefaultFramebuffer(defaultId);
        driver.setNativeFramebuffer(9);
        FrameBuffer target = new FrameBuffer(4, 4, 1);
        target.setId(12);
        target.clearUpdateNeeded();
        driver.renderer.invalidateState();
        driver.renderer.setFrameBuffer(target);
        assertEquals(12, driver.drawFramebuffer);
        driver.calls.clear();
        driver.renderer.setFrameBuffer(target);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void invalidationDoesNotMutateBindings(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        driver.activeUnit = 3;
        driver.setNativeFramebuffer(9);
        driver.renderer.invalidateState();
        assertEquals(3, driver.activeUnit);
        assertEquals(9, driver.drawFramebuffer);
        assertFalse(driver.calls.contains("glActiveTexture"));
        assertFalse(driver.calls.contains("glBindFramebufferEXT"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void freshDefaultFramebufferElidesBinding(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        driver.renderer.setFrameBuffer(null);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void nullFramebufferDoesNotRequireFramebufferSupport(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        driver.renderer.getCaps().remove(Caps.FrameBuffer);
        driver.renderer.setFrameBuffer(null);
        driver.renderer.invalidateState();
        driver.calls.clear();
        driver.renderer.setFrameBuffer(null);
        driver.renderer.setFrameBuffer(null);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
        assertThrows(RendererException.class, () -> driver.renderer.setFrameBuffer(framebuffer(12)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void uploadAfterInvalidationUsesRequestedUnit(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        Texture2D texture = texture(17);
        texture.getImage().setUpdateNeeded();
        driver.activeUnit = 3;
        driver.renderer.invalidateState();
        driver.renderer.setTexture(0, texture);
        assertEquals(0, driver.activeUnit);
        assertEquals(17, driver.textureBindings[0]);
        assertEquals(17, driver.lastUploadedTexture);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void parameterChangeAfterRepeatedInvalidationUsesRequestedUnit(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        Texture2D texture = texture(17);
        driver.renderer.setTexture(0, texture);
        driver.activeUnit = 3;
        driver.textureBindings[3] = 19;
        driver.renderer.invalidateState();
        driver.renderer.invalidateState();
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        driver.renderer.setTexture(0, texture);
        assertEquals(0, driver.activeUnit);
        assertEquals(17, driver.lastParameterTexture);
        assertEquals(19, driver.textureBindings[3]);
        driver.calls.clear();
        driver.renderer.setTexture(0, texture);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @CsvSource({"0, 9, 9", "0, 9, 10", "7, 9, 9", "7, 9, 10"})
    public void unknownCopyRestoresNativeBindingsWithoutValidatingCache(
            int defaultId, int readId, int drawId) throws Exception {
        Recording driver = new Recording(false);
        driver.setDefaultFramebuffer(defaultId);
        driver.readFramebuffer = readId;
        driver.drawFramebuffer = drawId;
        driver.renderer.invalidateState();
        driver.calls.clear();
        driver.renderer.copyFrameBuffer(framebuffer(12), framebuffer(13), true, false);
        assertEquals(readId, driver.readFramebuffer);
        assertEquals(drawId, driver.drawFramebuffer);
        assertFalse(driver.context.isFrameBufferBindingValid());
        assertEquals(2, driver.countCalls("glGetInteger"));
        assertEquals(List.of("glGetInteger", "glGetInteger"), driver.calls.subList(0, 2));
        driver.calls.clear();
        driver.renderer.setFrameBuffer(null);
        assertEquals(defaultId, driver.readFramebuffer);
        assertEquals(defaultId, driver.drawFramebuffer);
        assertEquals(1, driver.countCalls("glBindFramebufferEXT"));
        driver.calls.clear();
        driver.renderer.setFrameBuffer(null);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void unknownCopySnapshotsBindingsBeforeAttachmentUpdates(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        driver.readFramebuffer = 9;
        driver.drawFramebuffer = 10;
        driver.renderer.invalidateState();
        FrameBuffer source = new FrameBuffer(4, 4, 1);
        source.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        FrameBuffer destination = new FrameBuffer(4, 4, 1);
        destination.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        driver.calls.clear();
        driver.renderer.copyFrameBuffer(source, destination, true, false);
        assertTrue(source.getId() > 0);
        assertTrue(destination.getId() > 0);
        assertEquals(9, driver.readFramebuffer);
        assertEquals(10, driver.drawFramebuffer);
        assertFalse(driver.context.isFrameBufferBindingValid());
        assertEquals(List.of("glGetInteger", "glGetInteger"), driver.calls.subList(0, 2));
        driver.renderer.setFrameBuffer(null);
        assertEquals(0, driver.drawFramebuffer);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void knownCopyUsesCachedBindingAndPreservesElision(boolean desktop) throws Exception {
        Recording driver = new Recording(desktop);
        FrameBuffer previous = framebuffer(14);
        driver.renderer.setFrameBuffer(previous);
        driver.calls.clear();
        driver.renderer.copyFrameBuffer(framebuffer(12), framebuffer(13), true, false);
        assertEquals(14, driver.readFramebuffer);
        assertEquals(14, driver.drawFramebuffer);
        assertTrue(driver.context.isFrameBufferBindingValid());
        assertSame(previous, driver.renderer.getCurrentFrameBuffer());
        assertEquals(0, driver.countCalls("glGetInteger"));
        assertEquals(3, driver.countCalls("glBindFramebufferEXT"));
        driver.calls.clear();
        driver.renderer.setFrameBuffer(previous);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    public void unknownOverrideDefersBindingUntilNullSelection(int defaultId) throws Exception {
        Recording driver = new Recording(false);
        driver.setDefaultFramebuffer(defaultId);
        driver.setNativeFramebuffer(9);
        driver.renderer.invalidateState();
        driver.calls.clear();
        FrameBuffer override = framebuffer(12);
        driver.renderer.setMainFrameBufferOverride(override);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
        assertEquals(9, driver.drawFramebuffer);
        driver.renderer.setFrameBuffer(null);
        assertEquals(12, driver.drawFramebuffer);
        assertTrue(driver.context.isFrameBufferBindingValid());
        driver.calls.clear();
        driver.renderer.setFrameBuffer(null);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
    }

    @Test
    public void knownMainOverrideRetainsImmediateBinding() throws Exception {
        Recording driver = new Recording(false);
        driver.renderer.setMainFrameBufferOverride(framebuffer(12));
        assertEquals(12, driver.drawFramebuffer);
        assertTrue(driver.context.isFrameBufferBindingValid());
    }

    @ParameterizedTest
    @CsvSource({"0, 9", "0, 10", "7, 9", "7, 10"})
    public void deletingWhileUnknownDoesNotValidateOrEagerlyRebind(
            int defaultId, int deletedId) throws Exception {
        Recording driver = new Recording(false);
        driver.setDefaultFramebuffer(defaultId);
        driver.setNativeFramebuffer(9);
        driver.renderer.invalidateState();
        driver.calls.clear();
        driver.renderer.deleteFrameBuffer(framebuffer(deletedId));
        assertEquals(deletedId == 9 ? 0 : 9, driver.drawFramebuffer);
        assertEquals(0, driver.countCalls("glBindFramebufferEXT"));
        assertFalse(driver.context.isFrameBufferBindingValid());
        driver.renderer.setFrameBuffer(null);
        assertEquals(defaultId, driver.drawFramebuffer);
        assertTrue(driver.context.isFrameBufferBindingValid());
    }

    @Test
    public void unknownCopyRestorationDoesNotAssumeMainSrgbTarget() throws Exception {
        Recording driver = new Recording(false);
        driver.renderer.getCaps().add(Caps.Srgb);
        driver.renderer.getCaps().add(Caps.SrgbWriteControl);
        driver.setNativeFramebuffer(9);
        driver.renderer.invalidateState();
        driver.renderer.setMainFrameBufferSrgb(true);
        driver.calls.clear();
        driver.renderer.copyFrameBuffer(framebuffer(12), framebuffer(13), true, false);
        assertEquals(9, driver.drawFramebuffer);
        assertEquals(0, driver.countCalls("glEnable"));
        assertFalse(driver.context.isFrameBufferBindingValid());
        driver.renderer.setFrameBuffer(null);
        assertEquals(1, driver.countCalls("glEnable"));
    }

    @Test
    public void mainSrgbConfigurationDoesNotToggleAnUnknownTarget() throws Exception {
        Recording driver = new Recording(false);
        driver.renderer.getCaps().add(Caps.Srgb);
        driver.renderer.getCaps().add(Caps.SrgbWriteControl);
        driver.setNativeFramebuffer(9);
        driver.renderer.invalidateState();
        driver.calls.clear();
        driver.renderer.setMainFrameBufferSrgb(true);
        assertTrue(driver.calls.isEmpty(), driver.calls.toString());
        driver.renderer.setFrameBuffer(null);
        assertEquals(1, driver.countCalls("glEnable"));
        driver.calls.clear();
        driver.renderer.setMainFrameBufferSrgb(false);
        assertEquals(1, driver.countCalls("glDisable"));
    }

    private static Texture2D texture(int id) {
        Image image = new Image(Image.Format.RGBA8, 1, 1, (ByteBuffer) null, ColorSpace.Linear);
        image.setId(id);
        Texture2D texture = new Texture2D(image);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        image.clearUpdateNeeded();
        return texture;
    }

    private static FrameBuffer framebuffer(int id) {
        FrameBuffer framebuffer = new FrameBuffer(4, 4, 1);
        framebuffer.setId(id);
        framebuffer.clearUpdateNeeded();
        return framebuffer;
    }

    private static final class Recording implements InvocationHandler {
        private int activeUnit;
        private int readFramebuffer;
        private int drawFramebuffer;
        private int nextObject = 100;
        private int lastUploadedTexture;
        private int lastParameterTexture;
        private final int[] textureBindings = new int[16];
        private final List<String> calls = new ArrayList<>();
        private final GLRenderer renderer;
        private final RenderContext context;

        private Recording(boolean desktop) throws ReflectiveOperationException {
            Class<?>[] types = {desktop ? GL2.class : GL.class, GLExt.class, GLFbo.class};
            Object api = Proxy.newProxyInstance(GL.class.getClassLoader(), types, this);
            renderer = new GLRenderer((GL) api, (GLExt) api, (GLFbo) api);
            renderer.getCaps().add(Caps.FrameBuffer);
            renderer.getCaps().add(Caps.FrameBufferBlit);
            renderer.getCaps().add(desktop ? Caps.OpenGL20 : Caps.OpenGLES20);
            renderer.getLimits().put(Limits.RenderBufferSize, 1024);
            renderer.getLimits().put(Limits.FrameBufferAttachments, 16);
            renderer.getLimits().put(Limits.TextureSize, 1024);
            Field contextField = GLRenderer.class.getDeclaredField("context");
            contextField.setAccessible(true);
            context = (RenderContext) contextField.get(renderer);
            Field textureUtilField = GLRenderer.class.getDeclaredField("texUtil");
            textureUtilField.setAccessible(true);
            TextureUtil textureUtil = (TextureUtil) textureUtilField.get(renderer);
            textureUtil.initialize(renderer.getCaps());
        }

        private void setDefaultFramebuffer(int value) throws ReflectiveOperationException {
            Field field = GLRenderer.class.getDeclaredField("defaultFBO");
            field.setAccessible(true);
            field.setInt(renderer, value);
        }

        private void setNativeFramebuffer(int value) {
            readFramebuffer = value;
            drawFramebuffer = value;
        }

        private long countCalls(String name) {
            return calls.stream().filter(name::equals).count();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            calls.add(name);
            if (name.equals("glActiveTexture")) {
                activeUnit = (Integer) args[0] - GL.GL_TEXTURE0;
            } else if (name.equals("glBindTexture")) {
                textureBindings[activeUnit] = (Integer) args[1];
            } else if (name.equals("glTexImage2D")) {
                lastUploadedTexture = textureBindings[activeUnit];
            } else if (name.equals("glTexParameteri")) {
                lastParameterTexture = textureBindings[activeUnit];
            } else if (name.equals("glBindFramebufferEXT")) {
                int target = (Integer) args[0];
                int framebuffer = (Integer) args[1];
                assertTrue(framebuffer >= 0, "Unknown framebuffer ID passed to GL");
                if (target != GLFbo.GL_DRAW_FRAMEBUFFER_EXT) {
                    readFramebuffer = framebuffer;
                }
                if (target != GLFbo.GL_READ_FRAMEBUFFER_EXT) {
                    drawFramebuffer = framebuffer;
                }
            } else if (name.equals("glGetInteger")) {
                int parameter = (Integer) args[0];
                int result = parameter == GLFbo.GL_READ_FRAMEBUFFER_BINDING_EXT ? readFramebuffer
                        : parameter == GLFbo.GL_DRAW_FRAMEBUFFER_BINDING_EXT ? drawFramebuffer : 0;
                ((IntBuffer) args[1]).put(0, result);
            } else if (name.equals("glGenFramebuffersEXT") || name.equals("glGenRenderbuffersEXT")
                    || name.equals("glGenTextures")) {
                ((IntBuffer) args[0]).put(0, nextObject++);
            } else if (name.equals("glDeleteFramebuffersEXT")) {
                int framebuffer = ((IntBuffer) args[0]).get(0);
                if (readFramebuffer == framebuffer) {
                    readFramebuffer = 0;
                }
                if (drawFramebuffer == framebuffer) {
                    drawFramebuffer = 0;
                }
            } else if (name.equals("glCheckFramebufferStatusEXT")) {
                return GLFbo.GL_FRAMEBUFFER_COMPLETE_EXT;
            }
            if (method.getReturnType() == Integer.TYPE) {
                return 0;
            }
            if (method.getReturnType() == Boolean.TYPE) {
                return false;
            }
            return null;
        }
    }
}
