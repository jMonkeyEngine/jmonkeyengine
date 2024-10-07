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
package com.jme3.network.kernel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Base implementation of the Kernel interface providing several
 *  useful default implementations of some methods.  This implementation
 *  assumes that the kernel will be managing its own internal threads
 *  and queuing any results for the caller to retrieve on their own
 *  thread.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public abstract class AbstractKernel implements Kernel
{
    private static final Logger log = Logger.getLogger(AbstractKernel.class.getName());

    private AtomicLong nextId = new AtomicLong(1);

    /**
     *  Contains the pending endpoint events waiting for the caller
     *  to retrieve them.
     */
    private ConcurrentLinkedQueue<EndpointEvent> endpointEvents = new ConcurrentLinkedQueue<>();

    /**
     *  Contains the pending envelopes waiting for the caller to
     *  retrieve them.
     */
    private LinkedBlockingQueue<Envelope> envelopes = new LinkedBlockingQueue<>();

    protected AbstractKernel()
    {
    }

    protected void reportError( Exception e )
    {
        // Should really be queued up so the outer thread can
        // retrieve them.  For now we'll just log it.  FIXME
        log.log( Level.SEVERE, "Unhandled kernel error", e );
    }

    protected void wakeupReader() {
        // If there are no pending messages then add one so that the
        // kernel-user knows to wake up if it is only listening for
        // envelopes.
        if( !hasEnvelopes() ) {
            // Note: this is not really a race condition.  At worst, our
            // event has already been handled, and it does no harm
            // to check again.
            addEnvelope( EVENTS_PENDING );
        }
    }

    protected long nextEndpointId()
    {
        return nextId.getAndIncrement();
    }

    /**
     *  Returns true if there are waiting envelopes.
     */
    @Override
    public boolean hasEnvelopes()
    {
        return !envelopes.isEmpty();
    }

    /**
     *  Removes one envelope from the received messages queue or
     *  blocks until one is available.
     */
    @Override
    public Envelope read() throws InterruptedException
    {
        return envelopes.take();
    }

    /**
     *  Removes and returns one endpoint event from the event queue or
     *  null if there are no endpoint events.
     */
    @Override
    public EndpointEvent nextEvent()
    {
        return endpointEvents.poll();
    }

    protected void addEvent( EndpointEvent e )
    {
        endpointEvents.add( e );
    }

    protected void addEnvelope( Envelope env )
    {
        if( !envelopes.offer( env ) ) {
            throw new KernelException( "Critical error, could not enqueue envelope." );
        }            
    }
}
