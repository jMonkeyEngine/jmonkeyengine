package org.jmonkeyengine.jme3androidexamples;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest {
    @Test
    public void testApplicationPackage() {
        String packageName = InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getPackageName();
        assertEquals("org.jmonkeyengine.jme3androidexamples", packageName);
    }
}
