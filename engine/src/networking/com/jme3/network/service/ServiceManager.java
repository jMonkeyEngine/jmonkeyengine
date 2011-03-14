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

package com.jme3.network.service;

import com.jme3.network.connection.Client;
import com.jme3.network.connection.Server;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class can extend Service Manager to support services.
 *
 * @author Lars Wesselius
 */
public class ServiceManager {
    private Logger log = Logger.getLogger(ServiceManager.class.getName());
    private final List<Service> services = new ArrayList<Service>();

    private boolean client;

    public static final boolean CLIENT = true;
    public static final boolean SERVER = false;

    public ServiceManager(boolean client) {
        this.client = client;
    }

    public <T> T getService(Class cls) {
        for (Service service : services) {
            if (service.getClass() == cls) return (T)service;
        }

        try {
            if (!Service.class.isAssignableFrom(cls))
                return null;

            Constructor ctor;
            if (client) {
                try {
                    ctor = cls.getConstructor(new Class[]{Client.class});
                } catch (NoSuchMethodException nsme) {
                    log.log(Level.WARNING, "[ServiceManager][???] The service {0} does not support client mode.", cls);
                    return null;
                }
            } else {
                try {
                    ctor = cls.getConstructor(new Class[]{Server.class});
                } catch (NoSuchMethodException nsme) {
                    log.log(Level.WARNING, "[ServiceManager][???] The service {0} does not support server mode.", cls);
                    return null;
                }
            }

            T inst = (T)ctor.newInstance(this);

            services.add((Service)inst);
            return inst;
        } catch (Exception e) {
            log.log(Level.SEVERE, "[ServiceManager][???] Instantiaton of service failed.", e);
        }
        return null;
    }
}
