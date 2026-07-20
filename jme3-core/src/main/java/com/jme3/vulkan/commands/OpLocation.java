package com.jme3.vulkan.commands;

public enum OpLocation {

    Host(true, false),
    Device(false, true),
    PreferHost(true, false),
    PreferDevice(false, true),
    DontCare(false, false);

    private final boolean host, device;

    OpLocation(boolean host, boolean device) {
        this.host = host;
        this.device = device;
    }

    public boolean isHostBiased() {
        return host;
    }

    public boolean isDeviceBiased() {
        return device;
    }

}
