package com.jme3.vulkan.commands;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Commands {

    private Commands() {}

    private static final Map<Thread, Deque<Session>> buffers = new ConcurrentHashMap<>();

    public static Session start(CommandBuffer cmd) {
        return new Session(Thread.currentThread(), cmd);
    }

    public static CommandBuffer get() {
        Deque<Session> stack = buffers.get(Thread.currentThread());
        if (stack == null || stack.isEmpty()) {
            throw new NullPointerException("No command session available.");
        }
        return stack.peek().get();
    }

    public static class Session implements AutoCloseable {

        private final Thread origin;
        private final CommandBuffer cmd;

        private Session(Thread origin, CommandBuffer cmd) {
            this.origin = origin;
            this.cmd = cmd;
            buffers.computeIfAbsent(origin, t -> new ArrayDeque<>()).push(this);
        }

        public CommandBuffer get() {
            return cmd;
        }

        @Override
        public void close() {
            if (Thread.currentThread() != origin) {
                throw new IllegalStateException("Session closed on a thread that did not open it.");
            }
            if (buffers.get(origin).poll() != this) {
                throw new IllegalStateException("Command sessions closed out of order.");
            }
        }

    }

}
