/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.debug;

import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.RenderObject;
import com.jme3.renderer.framegraph.RenderResource;
import com.jme3.texture.FrameBuffer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.function.Supplier;

/**
 *
 * @author codex
 */
public class GraphEventCapture {
    
    private final File target;
    private final LinkedList<Event> events = new LinkedList<>();
    private int frame = 0;
    private boolean includeNanos = true;
    private long userNanos = 0;

    public GraphEventCapture(File target) {
        this.target = target;
    }
    
    private void add(Event c) {
        events.addLast(c);
    }
    public void addUserEvent(String operation, Object... arguments) {
        add(new Event("USEREVENT", operation, arguments));
    }
    public void value(String name, Object value) {
        add(new Value(name, value));
    }
    
    public void startRenderFrame() {
        add(new Event("SUPEREVENT", "StartRenderFrame", frame));
    }
    public void endRenderFrame() {
        add(new Event("SUPEREVENT", "EndRenderFrame", frame++));
    }
    public void renderViewPort(ViewPort vp) {
        Camera cam = vp.getCamera();
        add(new Event("SUPEREVENT", "StartViewPort", vp.getName(), cam.getWidth(), cam.getHeight()));
    }
    public void prepareRenderPass(int index, String name) {
        events.add(new Event("PrepareRenderPass", index, name));
    }
    public void executeRenderPass(int index, String name) {
        add(new Event("ExecuteRenderPass", index, name));
    }
    public void createFrameBuffer(FrameBuffer fb) {
        add(new Event("CreateFrameBuffer", fb.getWidth(), fb.getHeight(), fb.getSamples()));
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
    public void setObjectDirect(long id, int index, String type) {
        add(new Event("SetObjectDirect", id, index, type));
    }
    public void reallocateObject(long id, int index, String type) {
        add(new Event("ReallocateObject", id, index, type));
    }
    public void attemptReallocation(long id, int index) {
        add(new Event("AttemptSpecificReallocation", id, index));
    }
    public void allocateSpecificFailed(RenderObject object, RenderResource resource) {
        add(new Failure("AllocateSpecific",
            new Check("nullObject", () -> object != null, true),
            new Check("acquired", () -> !object.isAcquired()),
            new Check("constant", () -> !object.isConstant()),
            new Check("conflicting", () -> object.isReservedAt(resource.getLifeTime().getStartIndex())
                    || !object.isReservedWithin(resource.getLifeTime()))
        ));
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
    
    public void startNanos(String subject) {
        userNanos = System.nanoTime();
        add(new Event("PROFILE", "StartNanos", subject));
    }
    public long lapNanos(String subject) {
        return breakNanos(subject, subject);
    }
    public long breakNanos(String endSubject, String startSubject) {
        long duration = endNanos(endSubject);
        startNanos(startSubject);
        return duration;
    }
    public long endNanos(String subject) {
        long duration = Math.abs(System.nanoTime()-userNanos);
        add(new Event("PROFILE", "EndNanos", subject, duration+"ns", (duration/1000000)+"ms"));
        return duration;
    }
    
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
    
    public void setIncludeNanos(boolean includeNanos) {
        this.includeNanos = includeNanos;
    }
    public boolean isIncludeNanos() {
        return includeNanos;
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
    private static class Failure extends Event {
        
        public Failure(String operation, Check... checks) {
            super(operation, new Object[checks.length]);
            int i = 0;
            for (; i < checks.length; i++) {
                if (!checks[i].run()) {
                    arguments[i] = checks[i].name;
                    if (checks[i].terminal) break;
                } else {
                    arguments[i] = '-';
                }
            }
            for (; i < checks.length; i++) {
                arguments[i] = '?';
            }
        }
        
        @Override
        public String getEventType() {
            return "FAILURE";
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
