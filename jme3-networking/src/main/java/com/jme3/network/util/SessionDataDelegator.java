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

import com.jme3.network.HostedConnection;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  A MessageListener implementation that will forward messages to methods
 *  of a delegate specified as a HostedConnection session attribute.  This is
 *  useful for handling connection-specific messages from clients that must
 *  delegate to client-specific data objects.
 *  The delegate methods can be automapped or manually specified.  
 *
 *  @author    Paul Speed
 */
public class SessionDataDelegator extends AbstractMessageDelegator<HostedConnection> {
 
    private static final Logger log = Logger.getLogger(SessionDataDelegator.class.getName());
    
    private String attributeName;
 
    /**
     *  Creates a MessageListener that will forward mapped message types
     *  to methods of an object specified as a HostedConnection attribute.
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
    public SessionDataDelegator( Class delegateType, String attributeName, boolean automap ) {
        super(delegateType, automap);
        this.attributeName = attributeName;
    }
 
    /**
     *  Returns the attribute name that will be used to look up the 
     *  delegate object.
     */   
    public String getAttributeName() {
        return attributeName;
    }
 
    /**
     *  Called internally when there is no session object
     *  for the current attribute name attached to the passed source
     *  HostConnection.  Default implementation logs a warning.
     */
    protected void miss( HostedConnection source ) {
        log.log(Level.WARNING, "Session data is null for:{0} on connection:{1}", new Object[]{attributeName, source});
    }
 
    /**
     *  Returns the attributeName attribute of the supplied source
     *  HostConnection.  If there is no value at that attribute then
     *  the miss() method is called.
     */   
    @Override
    protected Object getSourceDelegate( HostedConnection source ) {
        Object result = source.getAttribute(attributeName);
        if( result == null ) {
            miss(source);
        }
        return result;
    }
}

