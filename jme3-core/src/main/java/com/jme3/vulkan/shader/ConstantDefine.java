package com.jme3.vulkan.shader;

public class ConstantDefine implements Define {

    private final String name;
    private final String value;

    public ConstantDefine(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getDefineName() {
        return name;
    }

    @Override
    public String getDefineValue() {
        return value;
    }

    @Override
    public boolean isDefineActive() {
        return true;
    }

    @Override
    public long getVersionNumber() {
        return 0L;
    }

}
