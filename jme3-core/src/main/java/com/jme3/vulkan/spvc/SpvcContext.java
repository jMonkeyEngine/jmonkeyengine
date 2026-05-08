package com.jme3.vulkan.spvc;

import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.spvc.Spvc;

public class SpvcContext implements Disposable {

    private final DisposableReference ref;
    private final long context;

    public SpvcContext() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer ptr = stack.mallocPointer(1);
            Spvc.spvc_context_create(ptr);
            context = ptr.get(0);
        }
        ref = DisposableManager.reference(this);
    }

    @Override
    public Runnable createDestroyer() {
        return () -> Spvc.spvc_context_destroy(context);
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    public long get() {
        return context;
    }

}
