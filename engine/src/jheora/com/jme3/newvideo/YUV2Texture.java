package com.jme3.newvideo;

import com.fluendo.jheora.YUVBuffer;
import com.fluendo.jst.Buffer;
import com.fluendo.jst.Element;
import com.fluendo.jst.Event;
import com.fluendo.jst.Pad;
import com.jme3.app.Application;
import com.jme3.texture.Image.Format;
import java.awt.image.FilteredImageSource;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class YUV2Texture extends Element {

    private YUVConv conv = new YUVConv();
    private int width, height;
    private BlockingQueue<VideoTexture> frameQueue;
    private Application app;

    private int frame = 0;

    private YUVBuffer getYUVBuffer(Buffer buf){
        if (buf.object instanceof FilteredImageSource) {
            FilteredImageSource imgSrc = (FilteredImageSource) buf.object;
            try {
                Field srcField = imgSrc.getClass().getDeclaredField("src");
                srcField.setAccessible(true);
                return (YUVBuffer) srcField.get(imgSrc);
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        }else if (buf.object instanceof YUVBuffer){
            return (YUVBuffer) buf.object;
        }else{
            throw new RuntimeException("Expected buffer");
        }
    }

    private VideoTexture decode(YUVBuffer yuv){
        if (frameQueue == null){
            frameQueue = new ArrayBlockingQueue<VideoTexture>(20);
            for (int i = 0; i < 20; i++){
                VideoTexture img = new VideoTexture(yuv.y_width, yuv.y_height, Format.RGBA8, frameQueue);
                frameQueue.add(img);
            }
        }

        try {
            final VideoTexture videoTex = frameQueue.take();
            ByteBuffer outBuf = videoTex.getImage().getData(0);
            conv.convert(yuv, 0, 0, yuv.y_width, yuv.y_height);
            outBuf.clear();
            outBuf.asIntBuffer().put(conv.getRGBData()).clear();
            
            app.enqueue( new Callable<Void>() {
                public Void call() throws Exception {
                    videoTex.getImage().setUpdateNeeded();
                    app.getRenderer().setTexture(0, videoTex);
                    return null;
                }
            });

            return videoTex;
        } catch (InterruptedException ex) {
        }
        
        return null;
    }

    private Pad srcPad = new Pad(Pad.SRC, "src") {
        @Override
        protected boolean eventFunc(Event event) {
            return sinkPad.pushEvent(event);
        }
    };

    private Pad sinkPad = new Pad(Pad.SINK, "sink") {
        @Override
        protected boolean eventFunc(Event event) {
            return srcPad.pushEvent(event);
        }

        @Override
    protected int chainFunc (Buffer buf) {
        YUVBuffer yuv = getYUVBuffer(buf);
        buf.object = decode(yuv);
        System.out.println("DECODE: " + (frame++));
        return srcPad.push(buf);
      
    }
  };

    public YUV2Texture(Application app) {
        super("YUV2Texture");
        addPad(srcPad);
        addPad(sinkPad);
        this.app = app;
    }

    @Override
    public String getFactoryName() {
        return "yuv2tex";
    }

}
