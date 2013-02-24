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
package com.jme3.util;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * See thread http://jmonkeyengine.org/forum/topic/monitor-direct-memory-usage-in-your-app/#post-205999
 * @author Paul Speed
 */
public class MemoryUtils {
    private static MBeanServer mbeans = ManagementFactory.getPlatformMBeanServer();
    private static ObjectName directPool;
    static {
        try {
            // Create the name reference for the direct buffer pool’s MBean
            directPool = new ObjectName("java.nio:type=BufferPool,name=direct");
        } catch (MalformedObjectNameException ex) {
            Logger.getLogger(MemoryUtils.class.getName()).log(Level.SEVERE, "Error creating direct pool ObjectName", ex);
        }
    }

    /**
     * 
     * @return the direct memory used in byte.
     */
    public static long getDirectMemoryUsage() {
        try {
            Long value = (Long)mbeans.getAttribute(directPool, "MemoryUsed");
            return value == null ? -1 : value;
        } catch (JMException ex) {
            Logger.getLogger(MemoryUtils.class.getName()).log(Level.SEVERE, "Error retrieving ‘MemoryUsed’", ex);
            return -1;
        }
    }

    /**
     * 
     * @return the number of direct buffer used
     */
    public static long getDirectMemoryCount() {
        try {
            Long value = (Long)mbeans.getAttribute(directPool, "Count");
            return value == null ? -1 : value;
        } catch (JMException ex) {
            Logger.getLogger(MemoryUtils.class.getName()).log(Level.SEVERE, "Error retrieving ‘Count’", ex);
            return -1;
        }
    }

    /**
     * 
     * @return Should return the total direct memory available, result seem off
     * see post http://jmonkeyengine.org/forum/topic/monitor-direct-memory-usage-in-your-app/#post-205999
     */
    public static long getDirectMemoryTotalCapacity() {
        try {
            Long value = (Long)mbeans.getAttribute(directPool, "TotalCapacity");
            return value == null ? -1 : value;
        } catch (JMException ex) {
            Logger.getLogger(MemoryUtils.class.getName()).log(Level.SEVERE, "Error retrieving ‘TotalCapacity’", ex);
            return -1;
        }
    }
}