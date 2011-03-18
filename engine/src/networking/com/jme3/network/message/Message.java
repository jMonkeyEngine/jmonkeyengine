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

package com.jme3.network.message;

import com.jme3.network.connection.Client;
import com.jme3.network.connection.Connection;
import com.jme3.network.serializing.Serializable;

/**
 * Message represents data being sent to the other side. This can be anything,
 *  and it will be serialized field by field. Extend this class if you wish to
 *  provide objects with common fields to the other side.
 *
 * @author Lars Wesselius
 */
@Serializable()
public class Message implements com.jme3.network.Message {
    // The connector this message is meant for.
    private transient Client        connector;
    private transient Connection    connection;
    private transient boolean       reliable = true;

    public Message() {}

    public Message(boolean reliable) {
        this.reliable = reliable;
    }

    public boolean isReliable() {
        return reliable;
    }

    public Message setReliable(boolean reliable) {
        this.reliable = reliable;
        return this;
    }

    /**
     *  @deprecated This method always returns null in the new API.
     */
    @Deprecated
    public Client getClient() {
        return connector;
    }

    @Deprecated
    public void setClient(Client connector) {
        this.connector = connector;
    }

    /**
     *  @deprecated This method always returns null in the new API.
     */
    @Deprecated
    public Connection getConnection() {
        return connection;
    }

    @Deprecated
    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
