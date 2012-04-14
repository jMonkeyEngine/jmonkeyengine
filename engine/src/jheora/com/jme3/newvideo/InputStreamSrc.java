/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.newvideo;

import com.fluendo.jst.Buffer;
import com.fluendo.jst.Caps;
import com.fluendo.jst.Element;
import com.fluendo.jst.ElementFactory;
import com.fluendo.jst.Event;
import com.fluendo.jst.Message;
import com.fluendo.jst.Pad;
import com.fluendo.utils.Debug;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamSrc extends Element {
    private InputStream input;
    private long contentLength;
    private long offset = 0;
    private long offsetLastMessage = 0;
    private long skipBytes = 0;
    private String mime;
    private Caps outCaps;
    private boolean discont = true;
    private static final int DEFAULT_READSIZE = 4096;
    private int readSize = DEFAULT_READSIZE;

    private Pad srcpad = new Pad(Pad.SRC, "src") {
        @Override
        protected void taskFunc() {
          int ret;
          int toRead;
          long left;

          // Skip to the target offset if required
          if (skipBytes > 0) {
              Debug.info("Skipping " + skipBytes + " input bytes");
              try {
                  offset += input.skip(skipBytes);
              } catch (IOException e) {
                  Debug.error("input.skip error: " + e);
                  postMessage(Message.newError(this, "File read error"));
                  return;
              }
              skipBytes = 0;
          }

          // Calculate the read size
          if (contentLength != -1) {
              left = contentLength - offset;
          } else {
              left = -1;
          }

          if (left != -1 && left < readSize) {
              toRead = (int) left;
          } else {
              toRead = readSize;
          }

          // Perform the read
          Buffer data = Buffer.create();
          data.ensureSize(toRead);
          data.offset = 0;
          try {
              if (toRead > 0) {
                  data.length = input.read(data.data, 0, toRead);
              } else {
                  data.length = -1;
              }
          } catch (Exception e) {
              e.printStackTrace();
              data.length = 0;
          }
          if (data.length <= 0) {
              /* EOS */

              postMessage(Message.newBytePosition(this, offset));
              offsetLastMessage = offset;

              try {
                  input.close();
              } catch (Exception e) {
                  e.printStackTrace();
              }
              data.free();
              Debug.log(Debug.INFO, this + " reached EOS");
              pushEvent(Event.newEOS());
              postMessage(Message.newStreamStatus(this, false, Pad.UNEXPECTED, "reached EOS"));
              pauseTask();
              return;
          }

          offset += data.length;
          if (offsetLastMessage > offset) {
              offsetLastMessage = 0;
          }
          if (offset - offsetLastMessage > contentLength / 100) {
              postMessage(Message.newBytePosition(this, offset));
              offsetLastMessage = offset;
          }

          // Negotiate capabilities
          if (srcpad.getCaps() == null) {
              String typeMime;

              typeMime = ElementFactory.typeFindMime(data.data, data.offset, data.length);
              Debug.log(Debug.INFO, "using typefind contentType: " + typeMime);
              mime = typeMime;
              
              outCaps = new Caps(mime);
              srcpad.setCaps(outCaps);
          }

          data.caps = outCaps;
          data.setFlag(com.fluendo.jst.Buffer.FLAG_DISCONT, discont);
          discont = false;

          // Push the data to the peer
          if ((ret = push(data)) != OK) {
              if (isFlowFatal(ret) || ret == Pad.NOT_LINKED) {
                  postMessage(Message.newError(this, "error: " + getFlowName(ret)));
                  pushEvent(Event.newEOS());
              }
              postMessage(Message.newStreamStatus(this, false, ret, "reason: " + getFlowName(ret)));
              pauseTask();
          }
      }

        @Override
        protected boolean activateFunc(int mode) {
            switch (mode) {
                case MODE_NONE:
                    postMessage(Message.newStreamStatus(this, false, Pad.WRONG_STATE, "stopping"));
                    input = null;
                    outCaps = null;
                    mime = null;
                    return stopTask();
                case MODE_PUSH:
                    contentLength = -1;
                    // until we can determine content length from IS?
//                    if (contentLength != -1) {
//                        postMessage(Message.newDuration(this, Format.BYTES, contentLength));
//                    }
                    if (input == null)
                        return false;
                    
                    postMessage(Message.newStreamStatus(this, true, Pad.OK, "activating"));
                    return startTask("JmeVideo-Src-Stream-" + Debug.genId());
                default:
                    return false;
            }
        }
    };

    public String getFactoryName() {
        return "inputstreamsrc";
    }

    public InputStreamSrc(String name) {
        super(name);
        addPad(srcpad);
    }

    @Override
    public synchronized boolean setProperty(String name, java.lang.Object value) {
        if (name.equals("inputstream")){
            input = (InputStream) value;
        }else if (name.equals("readSize")) {
            readSize = Integer.parseInt((String) value);
        } else {
            return false;
        }
        return true;
    }
}
