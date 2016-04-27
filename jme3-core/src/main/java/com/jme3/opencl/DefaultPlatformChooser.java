/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A default implementation of {@link PlatformChooser}.
 * It favors GPU devices with OpenGL sharing, then any devices with OpenGL sharing,
 * then any possible device.
 * @author shaman
 */
public class DefaultPlatformChooser implements PlatformChooser {
    private static final Logger LOG = Logger.getLogger(DefaultPlatformChooser.class.getName());

    @Override
    public List<? extends Device> chooseDevices(List<? extends Platform> platforms) {
        ArrayList<Device> result = new ArrayList<Device>();
        for (Platform p : platforms) {
            if (!p.hasOpenGLInterop()) {
                continue; //must support interop
            }
            for (Device d : p.getDevices()) {
                if (d.hasOpenGLInterop() && d.getDeviceType()==Device.DeviceType.GPU) {
                    result.add(d); //GPU prefered
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        //no GPU devices found, try all
        for (Platform p : platforms) {
            if (!p.hasOpenGLInterop()) {
                continue; //must support interop
            }
            for (Device d : p.getDevices()) {
                if (d.hasOpenGLInterop()) {
                    result.add(d); //just interop needed
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        //still no one found, try without interop
        LOG.warning("No device with OpenCL-OpenGL-interop found, try without");
        for (Platform p : platforms) {
            for (Device d : p.getDevices()) {
                result.add(d);
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        //no devices available at all!
        return result; //result is empty
    }
    
}
