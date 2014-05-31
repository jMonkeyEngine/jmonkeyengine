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
package com.jme3.network;


/**
 *  The source of a received message and the common abstract interface
 *  of client-&gt;server and server-&gt;client objects. 
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public interface MessageConnection
{
    /**
     *  Indicates the default reliable channel that is used
     *  when calling the channel-less send() with a reliable
     *  message.  This channel number can be used in the send(channel, msg)
     *  version of send.
     *
     *  <p>Normally, callers should just call the regular non-channel
     *  send message but these channel numbers are useful for extensions
     *  that allow the user to specify a channel and want to still
     *  support the default channels.</p>
     */
    public static final int CHANNEL_DEFAULT_RELIABLE = -2;
    
    /**
     *  Indicates the default unreliable channel that is used
     *  when calling the channel-less send() with a reliable=false
     *  message.  This channel number can be used in the send(channel, msg)
     *  version of send.
     *
     *  <p>Normally, callers should just call the regular non-channel
     *  send message but these channel numbers are useful for extensions
     *  that allow the user to specify a channel and want to still
     *  support the default channels.</p>
     */
    public static final int CHANNEL_DEFAULT_UNRELIABLE = -1;

    /**
     *  Sends a message to the other end of the connection.
     */   
    public void send( Message message );
    
    /**
     *  Sends a message to the other end of the connection using
     *  the specified alternate channel.
     */   
    public void send( int channel, Message message );
}    

