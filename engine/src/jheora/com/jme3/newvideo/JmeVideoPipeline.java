package com.jme3.newvideo;

import com.fluendo.jst.Caps;
import com.fluendo.jst.CapsListener;
import com.fluendo.jst.Clock;
import com.fluendo.jst.Element;
import com.fluendo.jst.ElementFactory;
import com.fluendo.jst.Format;
import com.fluendo.jst.Message;
import com.fluendo.jst.Pad;
import com.fluendo.jst.PadListener;
import com.fluendo.jst.Pipeline;
import com.fluendo.jst.Query;
import com.fluendo.utils.Debug;
import com.jme3.app.Application;
import com.jme3.texture.Texture2D;
import java.io.InputStream;

public class JmeVideoPipeline extends Pipeline implements PadListener, CapsListener {

    private boolean enableAudio;
    private boolean enableVideo;

    private int bufferSize = -1;
    private int bufferLow = -1;
    private int bufferHigh = -1;

    private Element inputstreamsrc;
    private Element buffer;
    private Element demux;
    private Element videodec;
    private Element audiodec;
    private Element videosink;
    private Element audiosink;
    private Element yuv2tex;
    private Element v_queue, v_queue2, a_queue = null;
    private Pad asinkpad, ovsinkpad;
    private Pad apad, vpad;
    public boolean usingJavaX = false;

    public InputStream inputStream;
    private Application app;

    public JmeVideoPipeline(Application app) {
        super("pipeline");

        enableAudio = true;
        enableVideo = true;
        this.app = app;
    }

    private void noSuchElement(String elemName) {
        postMessage(Message.newError(this, "no such element: " + elemName));
    }

    public void padAdded(Pad pad) {
        Caps caps = pad.getCaps();

        if (caps == null) {
            Debug.log(Debug.INFO, "pad added without caps: " + pad);
            return;
        }

        Debug.log(Debug.INFO, "pad added " + pad);
        String mime = caps.getMime();

        if (mime.equals("audio/x-vorbis")) {
            if (true)
                return;
            
            if (a_queue != null) {
                Debug.log(Debug.INFO, "More than one audio stream detected, ignoring all except first one");
                return;
            }

            a_queue = ElementFactory.makeByName("queue", "a_queue");
            if (a_queue == null) {
                noSuchElement("queue");
                return;
            }

            // if we already have a video queue: We want smooth audio playback
            // over frame completeness, so make the video queue leaky
            if (v_queue != null) {
                v_queue.setProperty("leaky", "2"); // 2 == Queue.LEAK_DOWNSTREAM
            }

            audiodec = ElementFactory.makeByName("vorbisdec", "audiodec");
            if (audiodec == null) {
                noSuchElement("vorbisdec");
                return;
            }

            a_queue.setProperty("maxBuffers", "100");

            add(a_queue);
            add(audiodec);

            pad.link(a_queue.getPad("sink"));
            a_queue.getPad("src").link(audiodec.getPad("sink"));
            if (!audiodec.getPad("src").link(asinkpad)) {
                postMessage(Message.newError(this, "audiosink already linked"));
                return;
            }

            apad = pad;

            audiodec.setState(PAUSE);
            a_queue.setState(PAUSE);
        } else if (enableVideo && mime.equals("video/x-theora")) {
            // Constructs a chain of the form
            // oggdemux -> v_queue -> theoradec -> v_queue2 -> videosink
            v_queue = ElementFactory.makeByName("queue", "v_queue");
            v_queue2 = ElementFactory.makeByName("queue", "v_queue2");
            yuv2tex = new YUV2Texture(app);
            if (v_queue == null) {
                noSuchElement("queue");
                return;
            }

            videodec = ElementFactory.makeByName("theoradec", "videodec");
            if (videodec == null) {
                noSuchElement("theoradec");
                return;
            }
            add(videodec);

            // if we have audio: We want smooth audio playback
            // over frame completeness
            if (a_queue != null) {
                v_queue.setProperty("leaky", "2"); // 2 == Queue.LEAK_DOWNSTREAM
            }
            
            v_queue.setProperty("maxBuffers", "5");
            v_queue2.setProperty("maxBuffers", "5");
            v_queue2.setProperty("isBuffer", Boolean.FALSE);

            add(v_queue);
            add(v_queue2);
            add(yuv2tex);

            pad.link(v_queue.getPad("sink"));
            v_queue.getPad("src").link(videodec.getPad("sink"));

            // WITH YUV2TEX
            videodec.getPad("src").link(yuv2tex.getPad("sink"));
            yuv2tex.getPad("src").link(v_queue2.getPad("sink"));
            v_queue2.getPad("src").link(videosink.getPad("sink")); 

            // WITHOUT YUV2TEX
//            videodec.getPad("src").link(v_queue2.getPad("sink"));

            if (!v_queue2.getPad("src").link(ovsinkpad)) {
                postMessage(Message.newError(this, "videosink already linked"));
                return;
            }

            vpad = pad;

            videodec.setState(PAUSE);
            v_queue.setState(PAUSE);
            v_queue2.setState(PAUSE);
            yuv2tex.setState(PAUSE);
        }
    }

    public void padRemoved(Pad pad) {
        pad.unlink();
        if (pad == vpad) {
            Debug.log(Debug.INFO, "video pad removed " + pad);
            ovsinkpad.unlink();
            vpad = null;
        } else if (pad == apad) {
            Debug.log(Debug.INFO, "audio pad removed " + pad);
            asinkpad.unlink();
            apad = null;
        }
    }

    @Override
    public void noMorePads() {
        boolean changed = false;

        Debug.log(Debug.INFO, "all streams detected");

        if (apad == null && enableAudio) {
            Debug.log(Debug.INFO, "file has no audio, remove audiosink");
            audiosink.setState(STOP);
            remove(audiosink);
            audiosink = null;
            changed = true;
            if (videosink != null) {
//                videosink.setProperty("max-lateness", Long.toString(Long.MAX_VALUE));
                videosink.setProperty("max-lateness", ""+Clock.SECOND);
            }
        }
        if (vpad == null && enableVideo) {
            Debug.log(Debug.INFO, "file has no video, remove videosink");
            videosink.setState(STOP);

            remove(videosink);
            videosink = null;
            changed = true;
        }
        if (changed) {
            scheduleReCalcState();
        }
    }

    public Texture2D getTexture(){
        if (videosink != null){
            return (Texture2D) videosink.getProperty("texture");
        }
        return null;
    }

    public boolean buildOggPipeline() {
        demux = ElementFactory.makeByName("oggdemux", "OggFileDemuxer");
        if (demux == null) {
            noSuchElement("oggdemux");
            return false;
        }

        buffer = ElementFactory.makeByName("queue", "BufferQueue");
        if (buffer == null) {
            demux = null;
            noSuchElement("queue");
            return false;
        }
        
        buffer.setProperty("isBuffer", Boolean.TRUE);
        if (bufferSize != -1) {
            buffer.setProperty("maxSize", new Integer(bufferSize * 1024));
        }
        if (bufferLow != -1) {
            buffer.setProperty("lowPercent", new Integer(bufferLow));
        }
        if (bufferHigh != -1) {
            buffer.setProperty("highPercent", new Integer(bufferHigh));
        }

        add(demux);
        add(buffer);

        // Link input stream source with bufferqueue's sink
        inputstreamsrc.getPad("src").link(buffer.getPad("sink"));
        
        // Link bufferqueue's source with the oggdemuxer's sink
        buffer.getPad("src").link(demux.getPad("sink"));

        // Receive pad events from OggDemuxer
        demux.addPadListener(this);

        buffer.setState(PAUSE);
        demux.setState(PAUSE);

        return true;
    }

    public void capsChanged(Caps caps) {
        String mime = caps.getMime();
        if (mime.equals("application/ogg")) {
            buildOggPipeline();
        } else {
            postMessage(Message.newError(this, "Unknown MIME type: " + mime));
        }
    }

    private boolean openFile() {
        inputstreamsrc = new InputStreamSrc("InputStreamSource");
        inputstreamsrc.setProperty("inputstream", inputStream);
        add(inputstreamsrc);

        // Receive caps from InputStream source
        inputstreamsrc.getPad("src").addCapsListener(this);

        audiosink = newAudioSink();
        if (audiosink == null) {
            enableAudio = false; 
        } else {
            asinkpad = audiosink.getPad("sink");
            add(audiosink);
        }

        if (enableVideo) {
            videosink = new TextureVideoSink("TextureVideoSink");
            videosink.setProperty("max-lateness", ""+Clock.SECOND);
//                    Long.toString(enableAudio ? Clock.MSECOND * 20 : Long.MAX_VALUE));
            add(videosink);

            ovsinkpad = videosink.getPad("sink");
        }
        if (audiosink == null && videosink == null) {
            postMessage(Message.newError(this, "Both audio and video are disabled, can't play anything"));
            return false;
        }

        return true;
    }

    protected Element newAudioSink() {
        com.fluendo.plugin.AudioSink s;
        try {
            s = (com.fluendo.plugin.AudioSink) ElementFactory.makeByName("audiosinkj2", "audiosink");
            Debug.log(Debug.INFO, "using high quality javax.sound backend");
        } catch (Throwable e) {
            s = null;
            noSuchElement ("audiosink");
            return null;
        }
        if (!s.test()) {
            return null;
        } else {
            return s;
        }
    }

    private boolean cleanup() {
        Debug.log(Debug.INFO, "cleanup");
        if (inputstreamsrc != null) {
            remove(inputstreamsrc);
            inputstreamsrc = null;
        }
        if (audiosink != null) {
            remove(audiosink);
            audiosink = null;
            asinkpad = null;
        }
        if (videosink != null) {
            remove(videosink);
            videosink = null;
        }
        if (buffer != null) {
            remove(buffer);
            buffer = null;
        }
        if (demux != null) {
            demux.removePadListener(this);
            remove(demux);
            demux = null;
        }
        if (v_queue != null) {
            remove(v_queue);
            v_queue = null;
        }
        if (v_queue2 != null) {
            remove(v_queue2);
            v_queue2 = null;
        }
        if (yuv2tex != null){
            remove(yuv2tex);
            yuv2tex = null;
        }
        if (a_queue != null) {
            remove(a_queue);
            a_queue = null;
        }
        if (videodec != null) {
            remove(videodec);
            videodec = null;
        }
        if (audiodec != null) {
            remove(audiodec);
            audiodec = null;
        }

        return true;
    }

    @Override
    protected int changeState(int transition) {
        int res;
        switch (transition) {
            case STOP_PAUSE:
                if (!openFile()) {
                    return FAILURE;
                }
                break;
            default:
                break;
        }

        res = super.changeState(transition);
        
        switch (transition) {
            case PAUSE_STOP:
                cleanup();
                break;
            default:
                break;
        }

        return res;
    }

    @Override
    protected boolean doSendEvent(com.fluendo.jst.Event event) {
        return false; // no seek support
    }

    protected long getPosition() {
        Query q;
        long result = 0;

        q = Query.newPosition(Format.TIME);
        if (super.query(q)){
            result = q.parsePositionValue();
        }
        return result;
    }

}
