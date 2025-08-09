package com.jme3.vulkan.devices;

import java.util.Objects;
import java.util.Set;

public class DeviceExtension {

    private final String name;
    private final Float success, fail;

    public DeviceExtension(String name) {
        this(name, 1f, null);
    }

    public DeviceExtension(String name, Float success) {
        this(name, success, null);
    }

    public DeviceExtension(String name, Float success, Float fail) {
        this.name = name;
        this.success = success;
        this.fail = fail;
    }

    public Float evaluate(Set<String> extensions) {
        return extensions.contains(name) ? success : fail;
    }

    public String getName() {
        return name;
    }

    public Float getSuccessWeight() {
        return success;
    }

    public Float getFailWeight() {
        return fail;
    }

    public boolean isRejectOnFailure() {
        return fail == null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DeviceExtension that = (DeviceExtension) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public static DeviceExtension critical(String name) {
        return new DeviceExtension(name, 1f, null);
    }

    public static DeviceExtension optional(String name, float successWeight) {
        return new DeviceExtension(name, successWeight);
    }

}
