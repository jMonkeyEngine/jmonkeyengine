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

package jme3test.network;

import com.jme3.network.*;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;

public class TestLatency {

    private static long startTime;
    private static Client client;
    private static MovingAverage average = new MovingAverage(100);

    static {
        startTime = System.currentTimeMillis();
    }

    private static long getTime(){
        return System.currentTimeMillis() - startTime;
    }

    @Serializable
    public static class TimestampMessage extends AbstractMessage {

        long timeSent     = 0;
        long timeReceived = 0;

        public TimestampMessage(){
            setReliable(false);
        }

        public TimestampMessage(long timeSent, long timeReceived){
            setReliable(false);
            this.timeSent = timeSent;
            this.timeReceived = timeReceived;
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException{
        Serializer.registerClass(TimestampMessage.class);

        Server server = Network.createServer(5110);
        server.start();

        client = Network.connectToServer("localhost", 5110);
        client.start();
        
        client.addMessageListener(new MessageListener<Client>(){
            public void messageReceived(Client source, Message m) {
                TimestampMessage timeMsg = (TimestampMessage) m;

                long curTime = getTime();
                //System.out.println("Time sent: " + timeMsg.timeSent);
                //System.out.println("Time received by server: " + timeMsg.timeReceived);
                //System.out.println("Time recieved by client: " + curTime);

                long latency = (curTime - timeMsg.timeSent);
                System.out.println("Latency: " + (latency) + " ms");
                //long timeOffset = ((timeMsg.timeSent + curTime) / 2) - timeMsg.timeReceived;
                //System.out.println("Approximate timeoffset: "+ (timeOffset) + " ms");

                average.add(latency);
                System.out.println("Average latency: " + average.getAverage());

                long latencyOffset = latency - average.getAverage();
                System.out.println("Latency offset: " + latencyOffset);

                client.send(new TimestampMessage(getTime(), 0));
            }
        }, TimestampMessage.class);

        server.addMessageListener(new MessageListener<HostedConnection>(){
            public void messageReceived(HostedConnection source, Message m) {
                TimestampMessage timeMsg = (TimestampMessage) m;
                TimestampMessage outMsg = new TimestampMessage(timeMsg.timeSent, getTime());
                source.send(outMsg);
            }
        }, TimestampMessage.class);

        Thread.sleep(1);

        client.send(new TimestampMessage(getTime(), 0));
        
        Object obj = new Object();
        synchronized(obj){
            obj.wait();
        }
    }

}
