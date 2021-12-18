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

package com.jme3.network.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  The base service manager class from which the HostedServiceManager
 *  and ClientServiceManager classes are derived.  This manages
 *  the underlying services and their life cycles.
 *
 *  @author    Paul Speed
 */
public abstract class ServiceManager<T> {

    private static final Logger log = Logger.getLogger(ServiceManager.class.getName());
    
    private final List<Service<T>> services = new CopyOnWriteArrayList<>();
    private volatile boolean started = false;
    
    protected ServiceManager() {
    }

    /**
     *  Retrieves the 'parent' of this service manager, usually
     *  a more specifically typed version of 'this' but it can be
     *  anything the services are expecting.
     */
    protected abstract T getParent();
 
    /**
     *  Returns the complete list of services managed by this
     *  service manager.  This list is thread safe following the
     *  CopyOnWriteArrayList semantics.
     */   
    protected List<Service<T>> getServices() {
        return services;
    }
    
    /**
     *  Starts this service manager and all services that it contains.
     *  Any services added after the service manager has started will have
     *  their start() methods called.
     */   
    public void start() {
        if( started ) {
            return;
        }
        for( Service<T> s : services ) {
            if( log.isLoggable(Level.FINE) ) {
                log.log(Level.FINE, "Starting service:{0}", s);
            }
            s.start();
        }
        started = true;
    }

    /**
     *  Returns true if this service manager has been started.
     */
    public boolean isStarted() {
        return started;
    }
 
    /**
     *  Stops all services and puts the service manager into a stopped state.
     */   
    public void stop() {
        if( !started ) {
            throw new IllegalStateException(getClass().getSimpleName() + " not started.");
        }
        for( Service<T> s : services ) {
            if( log.isLoggable(Level.FINE) ) {
                log.log(Level.FINE, "Stopping service:{0}", s);
            }
            s.stop();
        }
        started = false;
    }
 
    /**
     *  Adds the specified service and initializes it.  If the service manager
     *  has already been started then the service will also be started.
     */   
    public <S extends Service<T>> void addService( S s ) {
        if( log.isLoggable(Level.FINE) ) {
            log.log(Level.FINE, "addService({0})", s);
        }
        services.add(s);
        if( log.isLoggable(Level.FINE) ) {
            log.log(Level.FINE, "Initializing service:{0}", s);
        }
        s.initialize(getParent());
        if( started ) {
            if( log.isLoggable(Level.FINE) ) {
                log.log(Level.FINE, "Starting service:{0}", s);
            }
            s.start();
        }
    }

    /**
     *  Removes the specified service from this service manager, stopping
     *  and terminating it as required.  If this service manager is in a
     *  started state then the service will be stopped.  After removal,
     *  the service will be terminated.
     */
    public <S extends Service<T>> void removeService( S s ) {
        if( log.isLoggable(Level.FINE) ) {
            log.log(Level.FINE, "removeService({0})", s);
        }
        if( started ) {
            if( log.isLoggable(Level.FINE) ) {
                log.log(Level.FINE, "Stopping service:{0}", s);
            }
            s.stop();
        }
        services.remove(s);
        if( log.isLoggable(Level.FINE) ) {
            log.log(Level.FINE, "Terminating service:{0}", s);
        }
        s.terminate(getParent());        
    }
 
    /**
     *  Terminates all services.  If the service manager has not been
     *  stopped yet, then it will be stopped.
     */
    public void terminate() {
        if( started ) {
            stop();
        }
        for( Service<T> s : services ) {
            s.terminate(getParent());
        }               
    }
 
    /**
     *  Retrieves the first service of the specified type.
     */    
    public <S extends Service<T>> S getService( Class<S> type ) {
        for( Service s : services ) {
            if( type.isInstance(s) ) {
                return type.cast(s);
            }
        }
        return null;
    }
 
    @Override   
    public String toString() {
        return getClass().getName() + "[services=" + services + "]";
    }
}

