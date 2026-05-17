package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.SimpleApplication;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * This interface is to allow the desktop vs android specificness to be passed down into the
 * main test on run
 */
public interface AppRunner {

    void runApplicationUntilScenarioCompletes(App application, CountDownLatch applicationFinishedLatch);

    Path getChangedImagesDirectory();

}
