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

import com.jme3.network.Message;
import com.jme3.network.AbstractMessage;
import com.jme3.network.HostedConnection;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;


/**
 *  A simple test chat server.  When SM implements a set
 *  of standard chat classes this can become a lot simpler.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class TestChatServer
{
    // Normally these and the initialized method would
    // be in shared constants or something.
    public static final String NAME = "Test Chat Server";
    public static final int VERSION = 1;

    public static final int PORT = 5110;

    public static void initializeClasses()
    {
        // Doing it here means that the client code only needs to
        // call our initialize. 
        Serializer.registerClass(ChatMessage.class);
    }
    
    public static void main( String... args ) throws Exception
    {
        initializeClasses();
    
        // Use this to test the client/server name version check
        Server server = Network.createServer( NAME, VERSION, PORT, PORT );
        server.start();

        ChatHandler handler = new ChatHandler();
        server.addMessageListener( handler, ChatMessage.class );
 
        // Keep running basically forever
        synchronized( NAME ) {
            NAME.wait();
        }           
    }
    
    private static class ChatHandler implements MessageListener<HostedConnection>
    {
        public ChatHandler() 
        {
        }
    
        public void messageReceived( HostedConnection source, Message m )
        {
            if( m instanceof ChatMessage ) {
                // Keep track of the name just in case we 
                // want to know it for some other reason later and it's
                // a good example of session data
                source.setAttribute( "name", ((ChatMessage)m).getName() );
 
                System.out.println( "Broadcasting:" + m + "  reliable:" + m.isReliable() );
                                   
                // Just rebroadcast... the reliable flag will stay the
                // same so if it came in on UDP it will go out on that too
                source.getServer().broadcast( m ); 
            } else {
                System.err.println( "Received odd message:" + m );
            }            
        }
    }

    @Serializable
    public static class ChatMessage extends AbstractMessage 
    {
        private String name;
        private String message;
 
        public ChatMessage()
        {
        }

        public ChatMessage( String name, String message )
        {
            setName(name);
            setMessage(message);
        }
        
        public void setName( String name )
        {
            this.name = name;
        }
        
        public String getName()
        {
            return name;
        }
        
        public void setMessage( String s )
        {
            this.message = s;
        }
        
        public String getMessage()
        {
            return message;
        }
        
        public String toString()
        {
            return name + ":" + message;
        }
    }
}

