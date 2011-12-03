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
import java.util.*;

public class TestSerialization implements MessageListener<HostedConnection> {

    @Serializable
    public static class SomeObject {

        private int val;

        public SomeObject(){
        }

        public SomeObject(int val){
            this.val = val;
        }

        public int getVal(){
            return val;
        }

        @Override
        public String toString(){
            return "SomeObject[val="+val+"]";
        }
    }

    public enum Status {
        High,
        Middle,
        Low;
    }

    @Serializable
    public static class TestSerializationMessage extends AbstractMessage {

        boolean z;
        byte b;
        char c;
        short s;
        int i;
        float f;
        long l;
        double d;
        
        int[] ia;
        List<Object> ls;
        Map<String, SomeObject> mp;

        Status status1;
        Status status2;

        Date date;

        public TestSerializationMessage(){
            super(true);
        }

        public TestSerializationMessage(boolean initIt){
            super(true);
            if (initIt){
                z = true;
                b = -88;
                c = 'Y';
                s = 9999;
                i = 123;
                f = -75.4e8f;
                l = 9438345072805034L;
                d = -854834.914703e88;
                ia = new int[]{ 456, 678, 999 };

                ls = new ArrayList<Object>();
                ls.add("hello");
                ls.add(new SomeObject(-22));

                mp = new HashMap<String, SomeObject>();
                mp.put("abc", new SomeObject(555));

                status1 = Status.High;
                status2 = Status.Middle;

                date = new Date(System.currentTimeMillis());
            }
        }
    }

    public void messageReceived(HostedConnection source, Message m) {
        TestSerializationMessage cm = (TestSerializationMessage) m;
        System.out.println(cm.z);
        System.out.println(cm.b);
        System.out.println(cm.c);
        System.out.println(cm.s);
        System.out.println(cm.i);
        System.out.println(cm.f);
        System.out.println(cm.l);
        System.out.println(cm.d);
        System.out.println(Arrays.toString(cm.ia));
        System.out.println(cm.ls);
        System.out.println(cm.mp);
        System.out.println(cm.status1);
        System.out.println(cm.status2);
        System.out.println(cm.date);
    }

    public static void main(String[] args) throws IOException, InterruptedException{
        Serializer.registerClass(SomeObject.class);
        Serializer.registerClass(TestSerializationMessage.class);

        Server server = Network.createServer( 5110 );
        server.start();

        Client client = Network.connectToServer( "localhost", 5110 ); 
        client.start();

        server.addMessageListener(new TestSerialization(), TestSerializationMessage.class);
        client.send(new TestSerializationMessage(true));
        
        Thread.sleep(10000);
    }

}
