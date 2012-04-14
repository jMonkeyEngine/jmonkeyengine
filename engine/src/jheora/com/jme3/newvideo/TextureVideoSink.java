package com.jme3.newvideo;

import com.fluendo.jst.Buffer;
import com.fluendo.jst.Caps;
import com.fluendo.jst.Pad;
import com.fluendo.jst.Sink;
import com.fluendo.utils.Debug;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class TextureVideoSink extends Sink {

    private Texture2D outTex;
    private int width, height;

    private int frame = 0;

    public TextureVideoSink(String name) {
        super();
        setName(name);
    }

    @Override
    protected boolean setCapsFunc(Caps caps) {
        String mime = caps.getMime();
        if (!mime.equals("video/raw")) {
            return false;
        }

        width = caps.getFieldInt("width", -1);
        height = caps.getFieldInt("height", -1);

        if (width == -1 || height == -1) {
            return false;
        }

//        aspectX = caps.getFieldInt("aspect_x", 1);
//        aspectY = caps.getFieldInt("aspect_y", 1);
//
//        if (!ignoreAspect) {
//            Debug.log(Debug.DEBUG, this + " dimension: " + width + "x" + height + ", aspect: " + aspectX + "/" + aspectY);
//
//            if (aspectY > aspectX) {
//                height = height * aspectY / aspectX;
//            } else {
//                width = width * aspectX / aspectY;
//            }
//            Debug.log(Debug.DEBUG, this + " scaled source: " + width + "x" + height);
//        }

        outTex = new Texture2D();

        return true;
    }

    @Override
    protected int preroll(Buffer buf) {
        return render(buf);
    }

    @Override
    protected int render(Buffer buf) {
        if (buf.duplicate)
            return Pad.OK;

        Debug.log(Debug.DEBUG, this.getName() + " starting buffer " + buf);
        if (buf.object instanceof Image){
            synchronized (outTex){
                outTex.setImage( (Image) buf.object );
                outTex.notifyAll();
                System.out.println("PUSH  : " + (frame++));
            }
        } else {
            System.out.println(this + ": unknown buffer received " + buf.object);
            return Pad.ERROR;
        }

        if (outTex == null) {
            return Pad.NOT_NEGOTIATED;
        }

        Debug.log(Debug.DEBUG, this.getName() + " done with buffer " + buf);
        return Pad.OK;
    }

    public String getFactoryName() {
        return "texturevideosink";
    }

    @Override
    public java.lang.Object getProperty(String name) {
        if (name.equals("texture")) {
            return outTex;
        } else {
            return super.getProperty(name);
        }
    }
}
