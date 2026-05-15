package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.jme3.app.SimpleApplication;
import com.jme3.system.JmeContext;

import org.jmonkeyengine.screenshottests.testframework.AppRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DesktopRunner implements AppRunner {

    private static final Logger logger = Logger.getLogger(DesktopRunner.class.getName());


    private static final Executor executor = Executors.newSingleThreadExecutor( (r) -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void runApplicationUntilScenarioCompletes(SimpleApplication application, CountDownLatch applicationFinishedLatch) {
        executor.execute(() -> application.start(JmeContext.Type.Display));

        int maxWaitTimeMilliseconds = 45000;

        try {
            boolean exitedProperly = applicationFinishedLatch.await(maxWaitTimeMilliseconds, TimeUnit.MILLISECONDS);

            if (!exitedProperly) {
                logger.warning("Test driver did not exit in " + maxWaitTimeMilliseconds + "ms. Timed out");
                application.stop(true);
            }

            Thread.sleep(1000); //give time for openGL is fully released before starting a new test (get random JVM crashes without this)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }


    }
}
