/*
 * Copyright (c) 2011 jMonkeyEngine
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

//import com.jme3.network.connection.OldClient;
//import com.jme3.network.connection.OldServer;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageConnection;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
//import com.jme3.network.events.MessageAdapter;
//import com.jme3.network.message.Message;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;

public class TestThroughput implements MessageListener<MessageConnection> { //extends MessageAdapter {

    private static long lastTime = -1;
    private static long counter = 0;
    private static long total = 0;
//    private static OldClient client;
    private static Client client;

    private boolean isOnServer;

    public TestThroughput( boolean isOnServer ) {
        this.isOnServer = isOnServer;
    } 

    @Override
    public void messageReceived( MessageConnection source, Message msg){
    
        if( !isOnServer ) {
            // It's local to the client so we got it back
            counter++;
            total++;
            long time = System.currentTimeMillis();
//System.out.println( "total:" + total + "  counter:" + counter + "  lastTime:" + lastTime + "  time:" + time );
            if( lastTime < 0 ) {
                lastTime = time;
            } else if( time - lastTime > 1000 ) {
                long delta = time - lastTime;
                double scale = delta / 1000.0;
                double pps = counter / scale;
                System.out.println( "messages per second:" + pps + "  total messages:" + total );
                counter = 0;
                lastTime = time;
            }
        } else {
            if( source == null ) {
System.out.println( "Received a message from a not fully connected source, msg:"+ msg );
            } else {
//System.out.println( "sending:" + msg + " back to client:" + source );
                source.send(msg);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException{
    
        Serializer.registerClass(TestMessage.class);

        //OldServer server = new OldServer(5110, 5110);
        //server.start();
        Server server = Network.createServer( 5110 );
        server.start();

        //client = new OldClient("localhost", 5110, 5110);
        //client.start();
        Client client = Network.connectToServer( "hydra", 5110, 5000 );
        client.start();

        client.addMessageListener(new TestThroughput(false), TestMessage.class);
        server.addMessageListener(new TestThroughput(true), TestMessage.class);

        Thread.sleep(1);

        TestMessage test = new TestMessage();
//        for( int i = 0; i < 10; i++ ) {
        while( true ) {
//System.out.println( "sending." );
            client.send(test);
        }            
 
        //Thread.sleep(5000);
    }

    @Serializable
    public static class TestMessage extends com.jme3.network.message.Message {

        public TestMessage(){
            setReliable(true);
        }
    }

}
