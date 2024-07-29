/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer.framegraph.debug;

import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.PassIndex;
import com.jme3.texture.FrameBuffer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * Logs a list of FrameGraph events that occures over a number of frames.
 * <p>
 * Event logs can be written to a file, so that it can be analyzed by external
 * programs.
 * 
 * @author codex
 */
public class GraphEventCapture {
    
    private final File target;
    private final LinkedList<Event> events = new LinkedList<>();
    private int frame = 0, targetFrames = 10;
    private boolean includeNanos = true;
    private long userNanos = 0;
    
    /**
     * Graph event capture that exports the the file.
     * 
     * @param target 
     */
    public GraphEventCapture(File target) {
        this.target = target;
    }
    
    private void add(Event c) {
        events.addLast(c);
    }
    /**
     * Adds an event triggered by the user.
     * 
     * @param operation
     * @param arguments 
     */
    public void addUserEvent(String operation, Object... arguments) {
        add(new Event("USEREVENT", operation, arguments));
    }
    /**
     * Logs the object.
     * <p>
     * Changes to object afterward affect this.
     * 
     * @param name
     * @param value 
     */
    public void value(String name, Object value) {
        add(new Value(name, value));
    }
    
    /**
     * Logs a render frame start event.
     */
    public void beginRenderFrame() {
        add(new Event("SUPEREVENT", "BeginRenderFrame", frame));
    }
    /**
     * Logs a render frame end event.
     */
    public void endRenderFrame() {
        add(new Event("SUPEREVENT", "EndRenderFrame", frame++));
    }
    /**
     * Logs a viewport render event.
     * 
     * @param vp 
     */
    public void renderViewPort(ViewPort vp) {
        Camera cam = vp.getCamera();
        add(new Event("SUPEREVENT", "StartViewPort", vp.getName(), cam.getWidth(), cam.getHeight()));
    }
    /**
     * Logs a RenderPass preperation event.
     * 
     * @param index
     * @param name 
     */
    public void prepareRenderPass(PassIndex index, String name) {
        events.add(new Event("PrepareRenderPass", index.clone(), name));
    }
    /**
     * Logs a RenderPass execution event.
     * 
     * @param index
     * @param name 
     */
    public void executeRenderPass(PassIndex index, String name) {
        add(new Event("ExecuteRenderPass", index.clone(), name));
    }
    /**
     * Logs a FrameBuffer creation event.
     * 
     * @param fb 
     */
    public void createFrameBuffer(FrameBuffer fb) {
        add(new Event("CreateFrameBuffer", fb.getWidth(), fb.getHeight(), fb.getSamples()));
    }
    
    /**
     * Logs a ResourceView declaration event.
     * 
     * @param index
     * @param ticket 
     */
    public void declareResource(int index, String ticket) {
        add(new Event("DeclareResource", index, ticket));
    }
    /**
     * Logs a ResourceView reference event.
     * 
     * @param index
     * @param ticket 
     */
    public void referenceResource(int index, String ticket) {
        add(new Event("ReferenceResource", index, ticket));
    }
    /**
     * Logs a resource acquire event.
     * 
     * @param index
     * @param ticket 
     */
    public void acquireResource(int index, String ticket) {
        add(new Event("AcquireResource", index, ticket));
    }
    /**
     * Logs an event in which a ResourceView is marked as undefined.
     * 
     * @param index
     * @param ticket 
     */
    public void setResourceUndefined(int index, String ticket) {
        add(new Event("SetResourceUndefined", index, ticket));
    }
    /**
     * Logs a ResourceView release event.
     * 
     * @param index
     * @param ticket 
     */
    public void releaseResource(int index, String ticket) {
        add(new Event("ReleaseResource", index, ticket));
    }
    /**
     * Logs a ResourceView clearing event.
     * 
     * @param size number of resources cleared
     */
    public void clearResources(int size) {
        add(new Event("ClearResources", size));
    }
    /**
     * Logs a texture bind event.
     * 
     * @param index
     * @param ticket 
     */
    public void bindTexture(int index, String ticket) {
        add(new Event("BindTexture", index, ticket));
    }
    
    /**
     * Logs a RenderObject reservation event.
     * 
     * @param id
     * @param index 
     */
    public void reserveObject(long id, PassIndex index) {
        add(new Event("ReserveObject", id, index.clone()));
    }
    /**
     * Logs a ResourceObject creation event.
     * 
     * @param id
     * @param index
     * @param type 
     */
    public void createObject(long id, int index, String type) {
        add(new Event("CreateObject", id, index, type));
    }
    /**
     * Logs a RenderObject reallocation event.
     * 
     * @param id
     * @param index
     * @param type 
     */
    public void reallocateObject(long id, int index, String type) {
        add(new Event("ReallocateObject", id, index, type));
    }
    /**
     * Logs an attempt at reallocating a specific RenderObject.
     * 
     * @param id
     * @param index 
     */
    public void attemptReallocation(long id, int index) {
        add(new Event("AttemptSpecificReallocation", id, index));
    }
    /**
     * Logs an event in which a RenderObject is marked as constant.
     * 
     * @param id 
     */
    public void setObjectConstant(long id) {
        add(new Event("SetObjectConstant", id));
    }
    /**
     * Logs a RenderObject release event.
     * 
     * @param id 
     */
    public void releaseObject(long id) {
        add(new Event("ReleaseObject", id));
    }
    /**
     * Logs a RenderObject dispose event.
     * 
     * @param id 
     */
    public void disposeObject(long id) {
        add(new Event("DisposeObject", id));
    }
    /**
     * Logs an event in which RenderObjectMap is flushed.
     * 
     * @param size number of RenderObjects in the map.
     */
    public void flushObjects(int size) {
        add(new Event("FlushObjects", size));
    }
    
    /**
     * Begins a nanos count.
     * 
     * @param subject 
     */
    public void startNanos(String subject) {
        userNanos = System.nanoTime();
        add(new Event("PROFILE", "StartNanos", subject));
    }
    /**
     * Marks the beginning and end of a nanos "lap".
     * 
     * @param subject
     * @return 
     */
    public long lapNanos(String subject) {
        return breakNanos(subject, subject);
    }
    /**
     * Ends the current nanos count and begins a new nanos count.
     * 
     * @param endSubject
     * @param startSubject
     * @return 
     */
    public long breakNanos(String endSubject, String startSubject) {
        long duration = endNanos(endSubject);
        startNanos(startSubject);
        return duration;
    }
    /**
     * Ends the current nanos count.
     * 
     * @param subject
     * @return 
     */
    public long endNanos(String subject) {
        long duration = Math.abs(System.nanoTime()-userNanos);
        add(new Event("PROFILE", "EndNanos", subject, duration+"ns", (duration/1000000)+"ms"));
        return duration;
    }
    
    /**
     * Exports the current event log to this capture's target file.
     * 
     * @throws IOException 
     */
    public void export() throws IOException {
        if (target.exists()) {
            target.delete();
        }
        target.createNewFile();
        FileWriter writer = new FileWriter(target);
        writer.write(events.size()+" framegraph events over "+frame+" frames\n");
        for (Event e : events) {
            writer.write(e.toExportString(includeNanos));
            writer.write('\n');
        }
        writer.close();
    }
    
    /**
     * 
     * @param targetFrames 
     */
    public void setTargetFrames(int targetFrames) {
        this.targetFrames = targetFrames;
    }
    /**
     * Set the export file to include nanos with each event.
     * 
     * @param includeNanos 
     */
    public void setIncludeNanos(boolean includeNanos) {
        this.includeNanos = includeNanos;
    }
    
    public int getTargetFrames() {
        return targetFrames;
    }
    /**
     * 
     * @return 
     */
    public boolean isIncludeNanos() {
        return includeNanos;
    }
    
    public boolean isComplete() {
        return frame >= targetFrames;
    }
    
    public void resetFrameCount() {
        frame = 0;
    }
    
    private static class Event {
        
        protected String eventType;
        protected final String operation;
        protected final Object[] arguments;
        protected final long startNanos;
        
        public Event(String operation, Object... arguments) {
            this("EVENT", operation, arguments);
        }
        public Event(String eventType, String operation, Object... arguments) {
            this.eventType = eventType;
            this.operation = operation;
            this.arguments = arguments;
            this.startNanos = System.nanoTime();
        }
        
        public String getOperation() {
            return operation;
        }
        public Object[] getArguments() {
            return arguments;
        }
        
        public String toExportString(boolean nanos) {
            StringBuilder builder = new StringBuilder();
            builder.append(getEventType())
                   .append(" ")
                   .append(operation)
                   .append('(');
            if (nanos) {
                builder.append(nanos);
            }
            for (Object a : arguments) {
                if (nanos) {
                    builder.append(',');
                }
                builder.append(a.toString());
                nanos = true;
            }
            return builder.append(')').toString();
        }
        public String getEventType() {
            return eventType;
        }
        
    }
    private static class Value extends Event {
        
        public Value(String field, Object value) {
            super(field, value);
        }
        
        @Override
        public String toExportString(boolean nanos) {
            StringBuilder builder = new StringBuilder();
            return builder.append(getEventType()).append(" ")
                    .append(operation).append('=')
                    .append(arguments[0]).toString();
        }
        @Override
        public String getEventType() {
            return "VALUE";
        }
        
    }
    
    private static class Check {
        
        private final String name;
        private final Supplier<Boolean> check;
        private final boolean terminal;

        public Check(String name, Supplier<Boolean> check) {
            this(name, check, false);
        }
        public Check(String name, Supplier<Boolean> check, boolean terminal) {
            this.name = name;
            this.check = check;
            this.terminal = terminal;
        }
        
        public boolean run() {
            return check.get();
        }
        public String asArgument(boolean b) {
            return name+"="+b;
        }
        
    }
    
}
