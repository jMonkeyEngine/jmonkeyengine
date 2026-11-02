package com.jme3.vulkan.shadergen;

import java.util.Objects;

public class Location {

    public static final int CONSTANTS_SET = -1;
    public static final int MACROS_SET = -2;
    public static final int PUSH_CONSTANTS_SET = -3;

    public static Location constant(int id) {
        return new Location(CONSTANTS_SET, id);
    }

    public static Location macro(int id) {
        return new Location(MACROS_SET, id);
    }

    /**
     * Gets the location of a shader's push constants. A shader has only one push constant block,
     * so there is only one such location.
     *
     * @return push constant location
     */
    public static Location pushConstants() {
        return new Location(PUSH_CONSTANTS_SET, 0);
    }

    private final int set, binding;

    public Location(int binding) {
        this(0, binding);
    }

    public Location(int set, int binding) {
        this.set = set;
        this.binding = binding;
    }

    public int getSet() {
        return set;
    }

    public int getBinding() {
        return binding;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Location location1 = (Location) o;
        return set == location1.set && binding == location1.binding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(set, binding);
    }

    @Override
    public String toString() {
        return "Location{" +
                "set=" + set +
                ", binding=" + binding +
                '}';
    }

    public boolean isDescriptor() {
        return set >= 0;
    }

    public boolean isConstant() {
        return set == CONSTANTS_SET;
    }

    public boolean isMacro() {
        return set == MACROS_SET;
    }

    public boolean isPushConstants() {
        return set == PUSH_CONSTANTS_SET;
    }

    public static boolean isDescriptorSet(int set) {
        return set >= 0;
    }

}
