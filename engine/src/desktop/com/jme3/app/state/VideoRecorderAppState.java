package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.NanoTimer;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Video recording AppState that records the screen output into an AVI file with
 * M-JPEG content. The file should be playable on any OS in any video player.<br/>
 * The video recording starts when the state is attached and stops when it is detached
 * or the application is quit. You can set the fileName of the file to be written when the
 * state is detached, else the old file will be overwritten. If you specify no file
 * the AppState will attempt to write a file into the user home directory, made unique
 * by a timestamp.
 * @author normenhansen, Robert McIntyre
 */
public class VideoRecorderAppState extends AbstractAppState {

    private int framerate = 30;
    private VideoProcessor processor;
    private File file;
    private Application app;
    private ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {

        public Thread newThread(Runnable r) {
            Thread th = new Thread(r);
            th.setName("jME Video Processing Thread");
            th.setDaemon(true);
            return th;
        }
    });
    private int numCpus = Runtime.getRuntime().availableProcessors();
    private ViewPort lastViewPort;

    public VideoRecorderAppState() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "JME3 VideoRecorder running on {0} CPU's", numCpus);
    }

    public VideoRecorderAppState(File file) {
        this.file = file;
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "JME3 VideoRecorder running on {0} CPU's", numCpus);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        if (isInitialized()) {
            throw new IllegalStateException("Cannot set file while attached!");
        }
        this.file = file;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = app;
        app.setTimer(new IsoTimer(framerate));
        if (file == null) {
            String filename = System.getProperty("user.home") + File.separator + "jMonkey-" + System.currentTimeMillis() / 1000 + ".avi";
            file = new File(filename);
        }
        processor = new VideoProcessor();
        List<ViewPort> vps = app.getRenderManager().getPostViews();
        lastViewPort = vps.get(vps.size()-1);
        lastViewPort.addProcessor(processor);
    }

    @Override
    public void cleanup() {
        lastViewPort.removeProcessor(processor);
        app.setTimer(new NanoTimer());
        initialized = false;
        file = null;
        super.cleanup();
    }

    private class WorkItem {

        ByteBuffer buffer;
        BufferedImage image;
        byte[] data;

        public WorkItem(int width, int height) {
            image = new BufferedImage(width, height,
                    BufferedImage.TYPE_4BYTE_ABGR);
            buffer = BufferUtils.createByteBuffer(width * height * 4);
        }
    }

    private class VideoProcessor implements SceneProcessor {

        private Camera camera;
        private int width;
        private int height;
        private RenderManager renderManager;
        private boolean isInitilized = false;
        private LinkedBlockingQueue<WorkItem> freeItems;
        private LinkedBlockingQueue<WorkItem> usedItems = new LinkedBlockingQueue<WorkItem>();
        private MjpegFileWriter writer;

        public void addImage(Renderer renderer, FrameBuffer out) {
            if (freeItems == null) {
                return;
            }
            try {
                final WorkItem item = freeItems.take();
                usedItems.add(item);
                item.buffer.clear();
                renderer.readFrameBuffer(out, item.buffer);
                executor.submit(new Callable<Void>() {

                    public Void call() throws Exception {
                        Screenshots.convertScreenShot(item.buffer, item.image);
                        item.data = writer.writeImageToBytes(item.image);
                        while (usedItems.peek() != item) {
                            Thread.sleep(1);
                        }
                        writer.addImage(item.data);
                        usedItems.poll();
                        freeItems.add(item);
                        return null;
                    }
                });
            } catch (InterruptedException ex) {
                Logger.getLogger(VideoRecorderAppState.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void initialize(RenderManager rm, ViewPort viewPort) {
            this.camera = viewPort.getCamera();
            this.width = camera.getWidth();
            this.height = camera.getHeight();
            this.renderManager = rm;
            this.isInitilized = true;
            if (freeItems == null) {
                freeItems = new LinkedBlockingQueue<WorkItem>();
                for (int i = 0; i < numCpus; i++) {
                    freeItems.add(new WorkItem(width, height));
                }
            }
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
                    Logger.getLogger(VideoRecorderAppState.class.getName()).log(Level.SEVERE, "Error creating file writer: {0}", ex);
                }
            }
        }

        public void postQueue(RenderQueue rq) {
        }

        public void postFrame(FrameBuffer out) {
            addImage(renderManager.getRenderer(), out);
        }

        public void cleanup() {
            try {
                while (freeItems.size() < numCpus) {
                    Thread.sleep(10);
                }
                writer.finishAVI();
            } catch (Exception ex) {
                Logger.getLogger(VideoRecorderAppState.class.getName()).log(Level.SEVERE, "Error closing video: {0}", ex);
            }
            writer = null;
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
