/*
 * Copyright (c) 2015-2021 jMonkeyEngine
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

package com.jme3.network.util;

import com.jme3.network.MessageConnection;


/**
 *  A MessageListener implementation that will forward messages to methods
 *  of a specified delegate object.  These methods can be automapped or manually
 *  specified.  
 *
 *  @author    Paul Speed
 */
public class ObjectMessageDelegator<S extends MessageConnection> extends AbstractMessageDelegator<S> {
 
    private Object delegate;
    
    /**
     *  Creates a MessageListener that will forward mapped message types
     *  to methods of the specified object.
     *  If automap is true then all methods with the proper signature will
     *  be mapped.
     *  <p>Methods of the following signatures are allowed:
     *  <ul>
     *  <li>void someName(S conn, SomeMessage msg)
     *  <li>void someName(Message msg)
     *  </ul>
     *  Where S is the type of MessageConnection and SomeMessage is some
     *  specific concrete Message subclass.
     */   
    public ObjectMessageDelegator( Object delegate, boolean automap ) {
        super(delegate.getClass(), automap);
        this.delegate = delegate;
    }
 
    @Override
    protected Object getSourceDelegate( MessageConnection source ) {
        return delegate;
    }
}

