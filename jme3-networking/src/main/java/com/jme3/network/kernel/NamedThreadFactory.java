/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.network.kernel;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 *  A simple factory that delegates to java.util.concurrent's
 *  default thread factory but adds a prefix to the beginning
 *  of the thread name.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class NamedThreadFactory implements ThreadFactory
{
    private String name;
    private boolean daemon;
    private ThreadFactory delegate;
    
    public NamedThreadFactory( String name )
    {
        this( name, Executors.defaultThreadFactory() );
    }
    
    public NamedThreadFactory( String name, boolean daemon )
    {
        this( name, daemon, Executors.defaultThreadFactory() );
    }
    
    public NamedThreadFactory( String name, ThreadFactory delegate )
    {
        this( name, false, delegate );
    }

    public NamedThreadFactory( String name, boolean daemon, ThreadFactory delegate )
    {
        this.name = name;
        this.daemon = daemon;
        this.delegate = delegate;
    }
    
    @Override
    public Thread newThread( Runnable r )
    {
        Thread result = delegate.newThread(r);
        String s = result.getName();
        result.setName( name + "[" + s + "]" );
        result.setDaemon(daemon);
        return result;
    } 
}

