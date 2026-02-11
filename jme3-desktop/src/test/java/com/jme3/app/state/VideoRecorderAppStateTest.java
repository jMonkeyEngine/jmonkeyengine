/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for VideoRecorderAppState, specifically testing the
 * per-resolution worker pattern that handles window resizing.
 * 
 * @author GitHub Copilot
 */
public class VideoRecorderAppStateTest {
    
    private VideoRecorderAppState videoRecorder;
    private File testFile;
    private MockApplication app;
    
    @Before
    public void setUp() {
        testFile = new File(System.getProperty("java.io.tmpdir"), "test-video-" + System.currentTimeMillis() + ".avi");
        videoRecorder = new VideoRecorderAppState(testFile, 0.8f, 30);
        app = new MockApplication();
    }
    
    @After
    public void tearDown() {
        if (videoRecorder != null && videoRecorder.isInitialized()) {
            videoRecorder.cleanup();
        }
        
        // Clean up test files
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
        
        // Clean up any resolution-specific files that may have been created
        File parentDir = testFile.getParentFile();
        if (parentDir != null && parentDir.exists()) {
            File[] files = parentDir.listFiles((dir, name) -> 
                name.startsWith("test-video-") && name.endsWith(".avi"));
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }
    
    /**
     * Test that the VideoRecorderAppState can be initialized.
     */
    @Test
    public void testInitialization() {
        AppStateManager stateManager = new AppStateManager(app);
        videoRecorder.initialize(stateManager, app);
        
        assertTrue("VideoRecorderAppState should be initialized", videoRecorder.isInitialized());
    }
    
    /**
     * Test that reshape creates a new worker for a different resolution.
     */
    @Test
    public void testReshapeCreatesNewWorker() throws Exception {
        AppStateManager stateManager = new AppStateManager(app);
        videoRecorder.initialize(stateManager, app);
        
        // Access the VideoProcessor using reflection
        Field processorField = VideoRecorderAppState.class.getDeclaredField("processor");
        processorField.setAccessible(true);
        Object processor = processorField.get(videoRecorder);
        
        // Get the workers map
        Field workersField = processor.getClass().getDeclaredField("workers");
        workersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> workers = (Map<String, Object>) workersField.get(processor);
        
        // Initial state: should have one worker for 800x600
        assertEquals("Should have one worker initially", 1, workers.size());
        assertTrue("Should have worker for 800x600", workers.containsKey("800x600"));
        
        // Simulate reshape to 1024x768
        ViewPort vp = app.getRenderManager().getMainView("default");
        Camera cam = vp.getCamera();
        cam.resize(1024, 768, true);
        
        Method reshapeMethod = processor.getClass().getDeclaredMethod("reshape", ViewPort.class, int.class, int.class);
        reshapeMethod.setAccessible(true);
        reshapeMethod.invoke(processor, vp, 1024, 768);
        
        // Should now have two workers (old one not yet evicted)
        assertEquals("Should have two workers after reshape", 2, workers.size());
        assertTrue("Should have worker for 1024x768", workers.containsKey("1024x768"));
        assertTrue("Should still have worker for 800x600", workers.containsKey("800x600"));
    }
    
    /**
     * Test that old workers are evicted when fully drained.
     */
    @Test
    public void testWorkerEvictionWhenDrained() throws Exception {
        AppStateManager stateManager = new AppStateManager(app);
        videoRecorder.initialize(stateManager, app);
        
        // Access the VideoProcessor using reflection
        Field processorField = VideoRecorderAppState.class.getDeclaredField("processor");
        processorField.setAccessible(true);
        Object processor = processorField.get(videoRecorder);
        
        // Get the workers map
        Field workersField = processor.getClass().getDeclaredField("workers");
        workersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> workers = (Map<String, Object>) workersField.get(processor);
        
        // Reshape to create a new worker
        ViewPort vp = app.getRenderManager().getMainView("default");
        Method reshapeMethod = processor.getClass().getDeclaredMethod("reshape", ViewPort.class, int.class, int.class);
        reshapeMethod.setAccessible(true);
        reshapeMethod.invoke(processor, vp, 1024, 768);
        
        assertEquals("Should have two workers after reshape", 2, workers.size());
        
        // Simulate preFrame which should evict the old worker (since it's fully drained in our mock)
        Method preFrameMethod = processor.getClass().getDeclaredMethod("preFrame", float.class);
        preFrameMethod.setAccessible(true);
        preFrameMethod.invoke(processor, 0.016f);
        
        // The old worker (800x600) should be evicted
        assertEquals("Should have one worker after eviction", 1, workers.size());
        assertTrue("Should have worker for 1024x768", workers.containsKey("1024x768"));
        assertFalse("Should not have worker for 800x600", workers.containsKey("800x600"));
    }
    
    /**
     * Test that isFullyDrained correctly identifies when a worker is drained.
     */
    @Test
    public void testIsFullyDrained() throws Exception {
        AppStateManager stateManager = new AppStateManager(app);
        videoRecorder.initialize(stateManager, app);
        
        // Access the VideoProcessor using reflection
        Field processorField = VideoRecorderAppState.class.getDeclaredField("processor");
        processorField.setAccessible(true);
        Object processor = processorField.get(videoRecorder);
        
        // Get the current worker
        Field currentWorkerField = processor.getClass().getDeclaredField("currentWorker");
        currentWorkerField.setAccessible(true);
        Object currentWorker = currentWorkerField.get(processor);
        
        // Get the freeItems and usedItems queues
        Field freeItemsField = currentWorker.getClass().getDeclaredField("freeItems");
        freeItemsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedBlockingQueue<Object> freeItems = (LinkedBlockingQueue<Object>) freeItemsField.get(currentWorker);
        
        Field usedItemsField = currentWorker.getClass().getDeclaredField("usedItems");
        usedItemsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedBlockingQueue<Object> usedItems = (LinkedBlockingQueue<Object>) usedItemsField.get(currentWorker);
        
        // Initially, worker should be fully drained (all items free, none used)
        Method isFullyDrainedMethod = currentWorker.getClass().getDeclaredMethod("isFullyDrained");
        isFullyDrainedMethod.setAccessible(true);
        assertTrue("Worker should be fully drained initially", 
                   (Boolean) isFullyDrainedMethod.invoke(currentWorker));
        
        // Simulate taking an item
        Object item = freeItems.take();
        usedItems.add(item);
        
        // Worker should not be fully drained now
        assertFalse("Worker should not be fully drained with used items", 
                    (Boolean) isFullyDrainedMethod.invoke(currentWorker));
        
        // Return the item
        usedItems.remove(item);
        freeItems.add(item);
        
        // Worker should be fully drained again
        assertTrue("Worker should be fully drained after returning item", 
                   (Boolean) isFullyDrainedMethod.invoke(currentWorker));
    }
    
    /**
     * Test that reshape is non-blocking and doesn't throw exceptions.
     */
    @Test
    public void testReshapeIsNonBlocking() throws Exception {
        AppStateManager stateManager = new AppStateManager(app);
        videoRecorder.initialize(stateManager, app);
        
        // Access the VideoProcessor using reflection
        Field processorField = VideoRecorderAppState.class.getDeclaredField("processor");
        processorField.setAccessible(true);
        Object processor = processorField.get(videoRecorder);
        
        ViewPort vp = app.getRenderManager().getMainView("default");
        Method reshapeMethod = processor.getClass().getDeclaredMethod("reshape", ViewPort.class, int.class, int.class);
        reshapeMethod.setAccessible(true);
        
        // Measure time - should be very fast (< 100ms)
        long startTime = System.currentTimeMillis();
        reshapeMethod.invoke(processor, vp, 1920, 1080);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue("Reshape should be non-blocking (< 100ms), took " + duration + "ms", duration < 100);
    }
    
    /**
     * Test that getWorker returns the same worker for the same resolution.
     */
    @Test
    public void testGetWorkerReturnsExistingWorker() throws Exception {
        AppStateManager stateManager = new AppStateManager(app);
        videoRecorder.initialize(stateManager, app);
        
        // Access the VideoProcessor using reflection
        Field processorField = VideoRecorderAppState.class.getDeclaredField("processor");
        processorField.setAccessible(true);
        Object processor = processorField.get(videoRecorder);
        
        // Get the getWorker method
        Method getWorkerMethod = processor.getClass().getDeclaredMethod("getWorker", int.class, int.class);
        getWorkerMethod.setAccessible(true);
        
        // Call getWorker twice with the same resolution
        Object worker1 = getWorkerMethod.invoke(processor, 800, 600);
        Object worker2 = getWorkerMethod.invoke(processor, 800, 600);
        
        assertSame("Should return the same worker instance for the same resolution", worker1, worker2);
    }
    
    /**
     * Mock application for testing.
     */
    private static class MockApplication extends Application {
        private RenderManager renderManager;
        private ViewPort viewPort;
        
        public MockApplication() {
            super();
            this.renderManager = new MockRenderManager();
            this.viewPort = renderManager.createMainView("default", new Camera(800, 600));
        }
        
        @Override
        public void start() {
            // Not needed for tests
        }
        
        @Override
        public void restart() {
            // Not needed for tests
        }
        
        @Override
        public void setTimer(com.jme3.system.Timer timer) {
            this.timer = timer;
        }
        
        @Override
        public com.jme3.system.Timer getTimer() {
            return timer;
        }
        
        @Override
        public RenderManager getRenderManager() {
            return renderManager;
        }
    }
    
    /**
     * Mock render manager for testing.
     */
    private static class MockRenderManager extends RenderManager {
        private ViewPort mainView;
        
        public MockRenderManager() {
            super(new MockRenderer());
        }
        
        public ViewPort createMainView(String name, Camera cam) {
            mainView = new ViewPort(name, cam);
            return mainView;
        }
        
        public ViewPort getMainView(String name) {
            return mainView;
        }
    }
    
    /**
     * Mock renderer for testing.
     */
    private static class MockRenderer implements Renderer {
        @Override
        public void initialize() {
        }
        
        @Override
        public void setMainFrameBufferOverride(FrameBuffer fb) {
        }
        
        @Override
        public void setFrameBuffer(FrameBuffer fb) {
        }
        
        @Override
        public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        }
        
        @Override
        public void setBackgroundColor(com.jme3.math.ColorRGBA color) {
        }
        
        @Override
        public void applyRenderState(com.jme3.material.RenderState state) {
        }
        
        @Override
        public void setDepthRange(float start, float end) {
        }
        
        @Override
        public void onFrame() {
        }
        
        @Override
        public void setWorldMatrix(com.jme3.math.Matrix4f worldMatrix) {
        }
        
        @Override
        public void setViewProjectionMatrices(com.jme3.math.Matrix4f viewMatrix, com.jme3.math.Matrix4f projMatrix) {
        }
        
        @Override
        public void setCamera(Camera cam, boolean ortho) {
        }
        
        @Override
        public void renderMesh(com.jme3.scene.Mesh mesh, int lod, int count, com.jme3.scene.VertexBuffer[] instanceData) {
        }
        
        @Override
        public void resetGLObjects() {
        }
        
        @Override
        public void cleanup() {
        }
        
        @Override
        public void setShader(com.jme3.shader.Shader shader) {
        }
        
        @Override
        public void deleteShader(com.jme3.shader.Shader shader) {
        }
        
        @Override
        public void deleteShaderSource(com.jme3.shader.Shader.ShaderSource source) {
        }
        
        @Override
        public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
        }
        
        @Override
        public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyColor) {
        }
        
        @Override
        public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyColor, boolean copyDepth) {
        }
        
        @Override
        public void setTexture(int unit, com.jme3.texture.Texture tex) {
        }
        
        @Override
        public void updateBufferData(com.jme3.scene.VertexBuffer vb) {
        }
        
        @Override
        public void deleteBuffer(com.jme3.scene.VertexBuffer vb) {
        }
        
        @Override
        public void renderMesh(com.jme3.scene.Mesh mesh, int lod, int count) {
        }
        
        @Override
        public void updateTexImageData(com.jme3.texture.Image image, com.jme3.texture.Texture.Type type, int unit) {
        }
        
        @Override
        public void deleteImage(com.jme3.texture.Image image) {
        }
        
        @Override
        public int convertShaderType(com.jme3.shader.Shader.ShaderType type) {
            return 0;
        }
        
        @Override
        public void updateFrameBuffer(FrameBuffer fb) {
        }
        
        @Override
        public void deleteFrameBuffer(FrameBuffer fb) {
        }
        
        @Override
        public void setLinearizeSrgbImages(boolean linearize) {
        }
        
        @Override
        public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
        }
        
        @Override
        public void readFrameBufferWithFormat(FrameBuffer fb, ByteBuffer byteBuf, Image.Format format) {
            // Mock implementation - just fill with zeros
            byteBuf.clear();
            while (byteBuf.hasRemaining()) {
                byteBuf.put((byte) 0);
            }
            byteBuf.flip();
        }
        
        @Override
        public void setMainFrameBufferSrgb(boolean srgb) {
        }
        
        @Override
        public void setLinearizeSrgbImages(boolean linearize, boolean force) {
        }
        
        @Override
        public com.jme3.renderer.Statistics getStatistics() {
            return null;
        }
        
        @Override
        public EnumSet<com.jme3.renderer.Caps> getCaps() {
            return EnumSet.noneOf(com.jme3.renderer.Caps.class);
        }
        
        @Override
        public com.jme3.renderer.Limits getLimits() {
            return null;
        }
    }
}
