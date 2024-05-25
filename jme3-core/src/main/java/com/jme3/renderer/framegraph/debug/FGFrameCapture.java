/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.debug;

import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class FGFrameCapture {
    
    private final File target;
    private final LinkedList<Event> commands = new LinkedList<>();
    private int frame = 0;
    private boolean includeNanos = true;

    public FGFrameCapture(File target) {
        this.target = target;
    }
    
    private void add(Event c) {
        commands.addLast(c);
    }
    
    public void renderFrame() {
        add(new Event("StartRenderFrame", frame++));
    }
    public void renderViewPort(ViewPort vp) {
        Camera cam = vp.getCamera();
        add(new Event("StartViewPort", vp.getName(), cam.getWidth(), cam.getHeight()));
    }
    public void prepareRenderPass(int index, String name) {
        commands.add(new Event("PrepareRenderPass", index, name));
    }
    public void executeRenderPass(int index, String name) {
        add(new Event("ExecuteRenderPass", index, name));
    }
    
    public void declareResource(int index, String ticket) {
        add(new Event("DeclareResource", index, ticket));
    }
    public void referenceResource(int index, String ticket) {
        add(new Event("ReferenceResource", index, ticket));
    }
    public void acquireResource(int index, String ticket) {
        add(new Event("AcquireResource", index, ticket));
    }
    public void setResourceUndefined(int index, String ticket) {
        add(new Event("SetResourceUndefined", index, ticket));
    }
    public void releaseResource(int index, String ticket) {
        add(new Event("ReleaseResource", index, ticket));
    }
    public void clearResources(int size) {
        add(new Event("ClearResources", size));
    }
    public void bindTexture(int index, String ticket) {
        add(new Event("BindTexture", index, ticket));
    }
    
    public void reserveObject(long id, int index) {
        add(new Event("ReserveObject", id, index));
    }
    public void createObject(long id, int index, String type) {
        add(new Event("CreateObject", id, index, type));
    }
    public void reallocateObject(long id, int index, String type) {
        add(new Event("ReallocateObject", id, index, type));
    }
    public void acquireSpecificFailed(long id, int index) {
        add(new Event("AcquireSpecificFailed", id, index));
    }
    public void setObjectConstant(long id) {
        add(new Event("SetObjectConstant", id));
    }
    public void releaseObject(long id) {
        add(new Event("ReleaseObject", id));
    }
    public void disposeObject(long id) {
        add(new Event("DisposeObject", id));
    }
    public void flushObjects(int size) {
        add(new Event("FlushObjects", size));
    }
    
    public void value(String name, Object value) {
        add(new ValueEvent(name, value));
    }
    
    public void export() throws IOException {
        if (target.exists()) {
            target.delete();
        }
        target.createNewFile();
        FileWriter writer = new FileWriter(target);
        writer.write(commands.size()+" framegraph events over "+frame+" frames\n");
        for (Event e : commands) {
            writer.write(e.toExportString(includeNanos));
            writer.write('\n');
        }
        writer.close();
    }
    
    public void setIncludeNanos(boolean includeNanos) {
        this.includeNanos = includeNanos;
    }
    
    public boolean isIncludeNanos() {
        return includeNanos;
    }
    
    private class Event {
        
        protected final String operation;
        protected final Object[] arguments;
        protected final long startNanos;

        public Event(String operation, Object... arguments) {
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
            builder.append("EVENT ")
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
        
    }
    private class ValueEvent extends Event {
        
        public ValueEvent(String field, Object value) {
            super(field, value);
        }
        
        @Override
        public String toExportString(boolean nanos) {
            StringBuilder builder = new StringBuilder();
            return builder.append("VALUE ")
                    .append(operation).append('=')
                    .append(arguments[0]).toString();
        }
        
    }
    
}
