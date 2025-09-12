package com.jme3.vulkan.sync;

import java.util.concurrent.Future;

public interface TaskQueue {

    void submit(Future task);

}
