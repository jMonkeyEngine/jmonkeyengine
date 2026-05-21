package com.jme3.vulkan.slang;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class Session {

    private final Map<String, String> anonymousModules = new HashMap<>();
    private final AtomicLong anonymousId = new AtomicLong(0);

    public Module loadModule(String name) {}

    public Module loadAnonymousModule(String code) {
        String moduleName = createAnonymousModuleName(code);
        return loadModuleFromString(moduleName, moduleName + ".slang", code);
    }

    public String createAnonymousModuleName(String code) {
        return anonymousModules.computeIfAbsent(code, k -> "ANON" + anonymousId.getAndIncrement());
    }

    public ComponentType createComposite(ComponentType... components) {}

    public Module loadModuleFromString(String moduleName, String fileName, String code) {}

}
