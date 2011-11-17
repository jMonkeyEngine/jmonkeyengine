package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.NanoTimer;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author normenhansen, Robert McIntyre
 */
public class VideoRecorderAppState extends AbstractAppState {

    private int framerate = 30;
    private VideoProcessor processor;
    private MjpegFileWriter writer;
    private File file;
    private Application app;
    private ExecutorService executor;
    private Future fut;

    public VideoRecorderAppState(File file) {
        this.file = file;
        executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread th = new Thread(r);
                th.setName("jME Video processing Thread");
                th.setDaemon(true);
                return th;
            }
        });
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = app;
        app.setTimer(new IsoTimer(framerate));
        processor = new VideoProcessor();
        app.getViewPort().addProcessor(processor);
    }

    @Override
    public void cleanup() {
        app.getViewPort().removeProcessor(processor);
        super.cleanup();
    }

    public class VideoProcessor implements SceneProcessor {

        private Camera camera;
        private int width;
        private int height;
        private FrameBuffer frameBuffer;
        private RenderManager renderManager;
        private ByteBuffer byteBuffer;
        private BufferedImage rawFrame;
        private boolean isInitilized = false;

        public void initialize(RenderManager rm, ViewPort viewPort) {
            this.camera = viewPort.getCamera();
            this.width = camera.getWidth();
            this.height = camera.getHeight();
            rawFrame = new BufferedImage(width, height,
                    BufferedImage.TYPE_4BYTE_ABGR);
            byteBuffer = BufferUtils.createByteBuffer(width * height * 4);
            this.renderManager = rm;
            this.isInitilized = true;
        }

        public void reshape(ViewPort vp, int w, int h) {
        }

        public boolean isInitialized() {
            return this.isInitilized;
        }

        public void preFrame(float tpf) {
            if (null == writer) {
                try {
                    writer = new MjpegFileWriter(file, width, height, framerate);
                } catch (Exception ex) {
                    Logger.getLogger(VideoRecorderAppState.class.getName()).log(Level.SEVERE, "Error creating file writer {0}", ex);
                }
            }
        }

        public void postQueue(RenderQueue rq) {
        }

        public void postFrame(FrameBuffer out) {
            try {
                if (fut != null) {
                    fut.get();
                }
                fut = null;
            } catch (InterruptedException ex) {
            } catch (ExecutionException ex) {
            }
            byteBuffer.clear();
            renderManager.getRenderer().readFrameBuffer(out, byteBuffer);
            fut = executor.submit(new Callable<Void>() {

                public Void call() throws Exception {
                    Screenshots.convertScreenShot(byteBuffer, rawFrame);
                    try {
                        writer.addImage(rawFrame);
                    } catch (Exception ex) {
                        Logger.getLogger(VideoRecorderAppState.class.getName()).log(Level.SEVERE, "Error writing frame: {0}", ex);
                    }
                    return null;
                }
            });
        }

        public void cleanup() {
            try {
                if (fut != null) {
                    fut.get();
                    fut = null;
                }
            } catch (InterruptedException ex) {
            } catch (ExecutionException ex) {
            }
            app.setTimer(new NanoTimer());
            try {
                writer.finishAVI();
            } catch (Exception ex) {
                Logger.getLogger(VideoRecorderAppState.class.getName()).log(Level.SEVERE, "Error closing video: {0}", ex);
            }
        }
    }

    public static final class IsoTimer extends com.jme3.system.Timer {

        private float framerate;
        private int ticks;
        private long lastTime = 0;

        public IsoTimer(float framerate) {
            this.framerate = framerate;
            this.ticks = 0;
        }

        public long getTime() {
            return (long) (this.ticks * (1.0f / this.framerate) * 1000f);
        }

        public long getResolution() {
            return 1000000000L;
        }

        public float getFrameRate() {
            return this.framerate;
        }

        public float getTimePerFrame() {
            return (float) (1.0f / this.framerate);
        }

        public void update() {
            long time = System.currentTimeMillis();
            long difference = time - lastTime;
            lastTime = time;
            if (difference < (1.0f / this.framerate) * 1000.0f) {
                try {
                    Thread.sleep(difference);
                } catch (InterruptedException ex) {
                }
            }
            this.ticks++;
        }

        public void reset() {
            this.ticks = 0;
        }
    }
}
