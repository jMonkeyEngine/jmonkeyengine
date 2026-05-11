package org.jmonkeyengine.gradle.nativemetadata.internal;

import com.oracle.svm.core.jdk.RuntimeSupport;

final class JmeNativeRuntimeDefaultsHook implements RuntimeSupport.Hook {

    @Override
    public void execute(boolean firstIsolate) {
        JmeNativeRuntimeDefaults.apply();
    }
}
