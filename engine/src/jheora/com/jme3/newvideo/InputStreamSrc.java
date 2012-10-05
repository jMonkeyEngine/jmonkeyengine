/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
