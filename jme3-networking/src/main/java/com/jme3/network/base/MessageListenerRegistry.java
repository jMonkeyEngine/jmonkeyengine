/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.network.base;

import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Keeps track of message listeners registered to specific
 *  types or to any type.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class MessageListenerRegistry<S> implements MessageListener<S>
{
    private static final Logger log = Logger.getLogger(MessageListenerRegistry.class.getName());
    
    private final List<MessageListener<? super S>> listeners = new CopyOnWriteArrayList<>();
    private final Map<Class,List<MessageListener<? super S>>> typeListeners 
                    = new ConcurrentHashMap<>(); 

    public MessageListenerRegistry()
    {
    }
 
    @Override
    public void messageReceived( S source, Message m )
    {
        boolean delivered = false;
        boolean trace = log.isLoggable(Level.FINER);
        
        for( MessageListener<? super S> l : listeners ) {
            if( trace ) {
                log.log(Level.FINER, "Delivering {0} to:{1}", new Object[]{m, l});
            }
            l.messageReceived( source, m );
            delivered = true;
        }
        
        for( MessageListener<? super S> l : getListeners(m.getClass(),false) ) {
            if( trace ) {
                log.log(Level.FINER, "Delivering {0} to:{1}", new Object[]{m, l});
            }
            l.messageReceived( source, m );
            delivered = true;
        }
        
        if( !delivered ) {
            log.log( Level.FINE, "Received message had no registered listeners: {0}", m );
        }
    }
 
    protected List<MessageListener<? super S>> getListeners( Class c, boolean create )
    {
        List<MessageListener<? super S>> result = typeListeners.get(c);
        if( result == null && create ) {
            result = new CopyOnWriteArrayList<MessageListener<? super S>>();
            typeListeners.put( c, result );
        }
        
        if( result == null ) {
            result = Collections.emptyList();
        }
        return result;   
    }
    
    public void addMessageListener( MessageListener<? super S> listener )
    {
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null." );
        listeners.add(listener);
    } 

    public void removeMessageListener( MessageListener<? super S> listener )
    {
        listeners.remove(listener);
    } 

    public void addMessageListener( MessageListener<? super S> listener, Class... classes )
    {
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null." );
        for( Class c : classes ) {
            getListeners(c, true).add(listener);
        }
    } 

    public void removeMessageListener( MessageListener<? super S> listener, Class... classes )
    {
        for( Class c : classes ) {
            getListeners(c, false).remove(listener);
        }
    }
}
