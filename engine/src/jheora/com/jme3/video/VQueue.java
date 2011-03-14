/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.video;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VQueue extends ArrayBlockingQueue<VFrame> {

//    private final ArrayList<VFrame> returnedFrames;
    private final ArrayBlockingQueue<VFrame> returnedFrames;
    
    public VQueue(int bufferedFrames){
        super(bufferedFrames);
//        returnedFrames = new ArrayList<VFrame>(remainingCapacity());
        returnedFrames = new ArrayBlockingQueue<VFrame>(bufferedFrames * 3);
    }

    public VFrame nextReturnedFrame(boolean waitForIt){
        //        synchronized (returnedFrames){
        //            while (returnedFrames.size() == 0){
        //                if (!waitForIt)
        //                    return null;
        //
        //                try {
        //                    returnedFrames.wait();
        //                } catch (InterruptedException ex) {
        //                }
        //            }
        //        }
        //        }

        try {
            return returnedFrames.take();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void returnFrame(VFrame frame){
        returnedFrames.add(frame);

//        synchronized (returnedFrames){
//            returnedFrames.add(frame);
//            returnedFrames.notifyAll();
//        }
    }
}
