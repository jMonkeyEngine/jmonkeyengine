package org.jmonkeyengine.gradle.nativeimage.internal;

import com.oracle.svm.core.jdk.RuntimeSupport;
import org.graalvm.nativeimage.hosted.Feature;

/**
 * Configures runtime defaults for jME and LWJGL native library locations in native images.
 */
public final class JmeNativeRuntimeDefaultsFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        RuntimeSupport.getRuntimeSupport().addStartupHook(new JmeNativeRuntimeDefaultsHook());
    }
}
