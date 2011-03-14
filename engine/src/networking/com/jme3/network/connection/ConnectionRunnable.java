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

package com.jme3.network.connection;

import com.jme3.system.JmeSystem;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The connection runnable takes the UDP and TCP connections
 *  and updates them accordingly.
 *
 * @author Lars Wesselius
 */
public class ConnectionRunnable implements Runnable {
    protected Logger log = Logger.getLogger(Server.class.getName());

    private TCPConnection tcp;
    private UDPConnection udp;
    private int delay = 2;
    private boolean keepAlive = true;
    private boolean alive = true;

    public ConnectionRunnable(TCPConnection tcp, UDPConnection udp, int delay) {
        this.tcp = tcp;
        this.udp = udp;
        this.delay = delay;
    }

    public ConnectionRunnable(TCPConnection tcp, UDPConnection udp) {
        this.tcp = tcp;
        this.udp = udp;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isRunning() {
        return alive;
    }

    public void run() {
        if (!JmeSystem.isLowPermissions()){
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable thrown) {
                    log.log(Level.SEVERE, "Uncaught exception thrown in "+thread.toString(), thrown);
                }
            });
        }

        while (keepAlive)
        {
            // Run while one of the connections is still live.
            tcp.run();
            udp.run();

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else { Thread.yield(); }
        }
        try
        {
            tcp.cleanup();
            udp.cleanup();
        } catch (IOException e) {
            log.log(Level.WARNING, "[???][???] Could not clean up the connection.", e);
            return;
        }
        alive = false;
        log.log(Level.FINE, "[???][???] Cleaned up TCP/UDP.");
    }
}
