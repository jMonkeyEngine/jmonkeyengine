package com.jme3.audio.openal;

import static com.jme3.audio.openal.AL.*;

public final class ALUtil {

    private ALUtil() {
    }
    
    public static String getALErrorMessage(int errorCode) {
        String errorText;
        switch (errorCode) {
            case AL_NO_ERROR:
                errorText = "No Error";
                break;
            case AL_INVALID_NAME:
                errorText = "Invalid Name";
                break;
            case AL_INVALID_ENUM:
                errorText = "Invalid Enum";
                break;
            case AL_INVALID_VALUE:
                errorText = "Invalid Value";
                break;
            case AL_INVALID_OPERATION:
                errorText = "Invalid Operation";
                break;
            case AL_OUT_OF_MEMORY:
                errorText = "Out of Memory";
                break;
            default:
                errorText = "Unknown Error Code: " + String.valueOf(errorCode);
        }
        return errorText;
    }
    
    public static void checkALError(AL al) {
        int err = al.alGetError();
        if (err != AL_NO_ERROR) {
            throw new RuntimeException("OpenAL Error: " + getALErrorMessage(err));
        }
    }
}
