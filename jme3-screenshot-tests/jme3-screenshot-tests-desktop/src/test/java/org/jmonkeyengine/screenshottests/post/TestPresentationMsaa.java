package org.jmonkeyengine.screenshottests.post;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jmonkeyengine.screenshottests.testframework.TestContainingApp;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class TestPresentationMsaa {
    @Test
    void rendersWithAndWithoutPostProcessingMsaa() throws Exception {
        for (int filterSamples : new int[]{-1, 0, 4}) {
            runScenario(4, filterSamples);
        }
        runScenario(0, -1);
        runScenario(0, 4);
    }

    private void runScenario(int samples, int filterSamples) throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        CountDownLatch finished = new CountDownLatch(1);
        TestContainingApp app = new TestContainingApp() {
            private int frames;

            @Override
            public void simpleInitApp() {
                setDisplayStatView(false);
                setDisplayFps(false);
                viewPort.setBackgroundColor(ColorRGBA.Black);
                cam.setLocation(new Vector3f(0, 0, 8));
                cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
                Geometry box = new Geometry("box", new Box(1.5f, 1.5f, 1.5f));
                Material white = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                white.setColor("Color", ColorRGBA.White);
                box.setMaterial(white);
                box.rotate(0.35f, 0.55f, 0.1f);
                rootNode.attachChild(box);
                if (filterSamples >= 0) {
                    FilterPostProcessor processor = new FilterPostProcessor(assetManager);
                    if (filterSamples > 1) {
                        processor.setNumSamples(filterSamples);
                    }
                    processor.addFilter(new ColorOverlayFilter(new ColorRGBA(1, 1, 1, 0)));
                    viewPort.addProcessor(processor);
                }
            }

            @Override
            public void simpleUpdate(float tpf) {
                if (++frames != 5) {
                    return;
                }
                try {
                    LwjglWindow window = (LwjglWindow) getContext();
                    FrameBuffer render = framebuffer(window, "blitFramebuffer");
                    FrameBuffer resolve = framebuffer(window, "blitResolveFramebuffer");
                    assertEquals(samples > 1 ? samples : 1, render.getSamples());
                    if (renderer.getCaps().contains(Caps.OpenGLES20)) {
                        assertFalse(renderer.getCaps().contains(Caps.TextureMultisample));
                    }
                    FrameBuffer readable = null;
                    if (samples > 1 && !renderer.getCaps().contains(Caps.TextureMultisample)) {
                        assertNotNull(resolve);
                        assertNull(render.getColorTarget().getTexture());
                        assertEquals(Image.Format.RGBA8, render.getColorTarget().getFormat());
                        assertEquals(render.getWidth(), resolve.getWidth());
                        assertEquals(render.getHeight(), resolve.getHeight());
                        readable = resolve;
                    } else if (samples > 1) {
                        assertNull(resolve);
                        assertEquals(4, render.getColorTarget().getTexture().getImage().getMultiSamples());
                    } else {
                        assertNull(resolve);
                        assertEquals(1, render.getColorTarget().getTexture().getImage().getMultiSamples());
                        readable = render;
                    }
                    if (readable != null) {
                        boolean floating = readable.getColorTarget().getFormat() == Image.Format.RGBA16F;
                        int pixelBytes = floating ? 16 : 4;
                        ByteBuffer pixels = BufferUtils.createByteBuffer(readable.getWidth() * readable.getHeight() * pixelBytes);
                        renderer.setViewPort(0, 0, readable.getWidth(), readable.getHeight());
                        renderer.readFrameBufferWithFormat(readable, pixels,
                                floating ? Image.Format.RGBA32F : Image.Format.RGBA8);
                        renderer.setFrameBuffer(render);
                        int lit = 0;
                        int partial = 0;
                        for (int offset = 0; offset < pixels.capacity(); offset += pixelBytes) {
                            float red = floating ? pixels.getFloat(offset) : (pixels.get(offset) & 0xff) / 255f;
                            lit += red > 0.5f ? 1 : 0;
                            partial += red > 0f && red < 1f ? 1 : 0;
                        }
                        assertTrue(lit > 100, "presentation scene is blank");
                        if ((samples > 1 && filterSamples == -1) || filterSamples == 4) {
                            assertTrue(partial > 0, "no partially covered pixels in presentation scene");
                        }
                    }
                    result.complete(null);
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                } finally {
                    finished.countDown();
                }
            }

            @Override
            public void handleError(String message, Throwable failure) {
                result.completeExceptionally(failure == null ? new AssertionError(message) : failure);
                finished.countDown();
            }

        };
        AppSettings settings = new AppSettings(true);
        settings.setResolution(96, 96);
        settings.setAudioRenderer(null);
        settings.setUseInput(false);
        settings.setRenderer(System.getProperty("jme.screenshot.renderer", AppSettings.LWJGL_OPENGL45));
        settings.setSamples(samples);
        settings.setGammaCorrection(true);
        settings.setDisplayScaleMode(2f);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
        try {
            assertTrue(finished.await(45, TimeUnit.SECONDS), "rendering timed out");
            result.get(1, TimeUnit.SECONDS);
        } finally {
            app.stop(false);
            // Application.destroy() runs before the window/context and SDL
            // subsystem are released. Wait for the entire render thread so the
            // next scenario cannot initialize SDL during the previous teardown.
            Field threadField = LwjglWindow.class.getDeclaredField("mainThread");
            threadField.setAccessible(true);
            Thread renderThread = (Thread) threadField.get(app.getContext());
            assertNotNull(renderThread, "render thread was not created");
            renderThread.join(TimeUnit.SECONDS.toMillis(10));
            assertFalse(renderThread.isAlive(), "render thread did not shut down");
        }
    }

    private static FrameBuffer framebuffer(LwjglWindow window, String name) throws Exception {
        Field field = LwjglWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return (FrameBuffer) field.get(window);
    }
}
