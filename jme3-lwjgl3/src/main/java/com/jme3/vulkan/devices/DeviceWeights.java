package com.jme3.vulkan.devices;

public class DeviceWeights {

    public static final Float SUCCESS = 1f;
    public static final Float FAILURE = -1f;
    public static final Float REJECTION = null;

    public static Float success(float weight) {
        return weight;
    }

    public static Float failure(float weight) {
        return -weight;
    }

    public static Float successOrReject(boolean success) {
        return successOrReject(success, SUCCESS);
    }

    public static Float successOrReject(boolean success, float weight) {
        return success ? Float.valueOf(weight) : REJECTION;
    }

    public static Float successOrFail(boolean success) {
        return successOrFail(success, SUCCESS, FAILURE);
    }

    public static Float successOrFail(boolean success, float weight) {
        return success ? weight : -weight;
    }

    public static Float successOrFail(boolean success, float successWeight, float failWeight) {
        return success ? successWeight : -failWeight;
    }

    public static boolean isSuccess(Float weight) {
        return weight != null && weight >= 0f;
    }

    public static boolean isFailure(Float weight) {
        return weight == null || weight < 0f;
    }

    public static boolean isRejection(Float weight) {
        return weight == null;
    }

}
