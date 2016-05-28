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

import com.jme3.system.JmeSystem;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a simple cache system for program objects.
 * The program objects are saved persistently with {@link #saveToCache(java.lang.String, com.jme3.opencl.Program) }.
 * On the next run, the stored programs can then be loaded
 * with {@link #loadFromCache(java.lang.String, java.lang.String) }.
 * <br>
 * The programs are identified by a unique id. The following format is recommended:
 * {@code id = <full name of the class using the program>.<unique identifier within that class>}.
 * 
 * @author shaman
 */
public class ProgramCache {
    private static final Logger LOG = Logger.getLogger(ProgramCache.class.getName());
    private static final String FILE_EXTENSION = ".clbin";
    
    private final Context context;
    private final Device device;
    private final File tmpFolder;

    /**
     * Creates a "disabled" program cache, no caching is done.
     * {@link #loadFromCache(java.lang.String) } will always return {@code null}
     * and {@link #saveToCache(java.lang.String, com.jme3.opencl.Program) } does
     * nothing.<br>
     * Use this during development if you still modify your kernel code.
     * (Otherwise, you don't see the changes because you are still use the 
     * cached version of your program)
     */
    public ProgramCache() {
        this.context = null;
        this.device = null;
        this.tmpFolder = null;
    }
    
    /**
     * Creates a new program cache associated with the specified context and
     * devices.
     * The cached programs are built against the specified device and also
     * only the binaries linked to that device are stored.
     * @param context the OpenCL context
     * @param device the OpenCL device
     */
    public ProgramCache(Context context, Device device) {
        this.context = context;
        this.device = device;
        if (JmeSystem.isLowPermissions()) {
            tmpFolder = null;
        } else {
            tmpFolder = JmeSystem.getStorageFolder();
        }
    }
    
    protected String getCleanFileName(String id) {
        //http://stackoverflow.com/a/35591188/4053176
        return id.replaceAll("[^a-zA-Z0-9.-]", "") + FILE_EXTENSION;
    }

    /**
     * Creates a new program cache using the first device from the specified 
     * context.
     * @param context the context
     * @see #ProgramCache(com.jme3.opencl.Context, com.jme3.opencl.Device) 
     */
    public ProgramCache(Context context) {
        this(context, context.getDevices().get(0));
    }

    /**
     * Loads the program from the cache and builds it against the current device.
     * You can pass additional build arguments with the parameter {@code buildArgs}.
     * <p>
     * The cached program is identified by the specified id. 
     * This id must be unique, otherwise collisions within the cache occur.
     * Therefore, the following naming schema is recommended:
     * {@code id = <full name of the class using the program>.<unique identifier within that class>}.
     * <p>
     * If the program can't be loaded, built or any other exception happened,
     * {@code null} is returned.
     * 
     * @param id the unique identifier of this program
     * @param buildArgs additional build arguments, can be {@code null}
     * @return the loaded and built program, or {@code null}
     * @see #saveToCache(java.lang.String, com.jme3.opencl.Program) 
     */
    public Program loadFromCache(String id, String buildArgs) {
        if (tmpFolder == null) {
            return null; //low permissions
        }
        //get file
        File file = new File(tmpFolder, getCleanFileName(id));
        if (!file.exists()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Cache file {0} does not exist", file.getAbsolutePath());
            }
            return null;
        }
        //load from file
        ByteBuffer bb;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            bb = BufferUtils.createByteBuffer(bytes);
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Unable to read cache file", ex);
            return null;
        }
        //create program
        Program program;
        try {
            program = context.createProgramFromBinary(bb, device);
        } catch (OpenCLException ex) {
            LOG.log(Level.FINE, "Unable to create program from binary", ex);
            return null;
        }
        //build program
        try {
            program.build(buildArgs, device);
        } catch (OpenCLException ex) {
            LOG.log(Level.FINE, "Unable to build program", ex);
            return null;
        }
        //done
        return program;
    }
    
    /**
     * Calls {@link #loadFromCache(java.lang.String, java.lang.String) }
     * with the additional build arguments set to {@code ""}.
     * @param id a unique identifier of the program
     * @return the loaded and built program or {@code null} if this
     * program could not be loaded from the cache
     * @see #loadFromCache(java.lang.String, java.lang.String) 
     */
    public Program loadFromCache(String id) {
        return loadFromCache(id, "");
    }
    
    /**
     * Saves the specified program in the cache.
     * The parameter {@code id} denotes the name of the program. Under this id,
     * the program is then loaded again by {@link #loadFromCache(java.lang.String, java.lang.String) }.
     * <br>
     * The id must be unique, otherwise collisions within the cache occur.
     * Therefore, the following naming schema is recommended:
     * {@code id = <full name of the class using the program>.<unique identifier within that class>}.
     * 
     * @param id the program id
     * @param program the program to store in the cache
     */
    public void saveToCache(String id, Program program) {
        if (tmpFolder == null) {
            return; //low permissions
        }
        //get file
        File file = new File(tmpFolder, getCleanFileName(id));
        //get binaries
        ByteBuffer bb;
        try {
            bb = program.getBinary(device);
        } catch (UnsupportedOperationException | OpenCLException ex) {
            LOG.log(Level.WARNING, "Unable to retrieve the program binaries", ex);
            return;
        }
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        //save
        try {
            Files.write(file.toPath(), bytes);
        } catch (IOException ex) {
           LOG.log(Level.WARNING, "Unable to save program binaries to the cache", ex);
        }
    }
    
    /**
     * Clears the cache.
     * All saved program binaries are deleted.
     */
    public void clearCache() {
        if (tmpFolder == null) {
            return; //low permissions
        }
        for (File file : tmpFolder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(FILE_EXTENSION)) {
                file.delete();
            }
        }
    }
}
