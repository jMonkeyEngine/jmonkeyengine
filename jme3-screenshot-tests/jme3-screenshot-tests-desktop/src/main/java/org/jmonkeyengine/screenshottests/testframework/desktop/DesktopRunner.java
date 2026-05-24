package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Image;

import org.jmonkeyengine.screenshottests.testframework.TestContainingApp;
import org.jmonkeyengine.screenshottests.testframework.AppRunner;
import org.junit.jupiter.api.Assertions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DesktopRunner implements AppRunner {

    private static final Logger logger = Logger.getLogger(DesktopRunner.class.getName());


    private static final Executor executor = Executors.newSingleThreadExecutor( (r) -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void runApplicationUntilScenarioCompletes(TestContainingApp application, CountDownLatch applicationFinishedLatch) {
        executor.execute(() -> application.start(JmeContext.Type.Display));

        application.onError = error -> {
            logger.log(Level.WARNING, "Error in test application", error);
            applicationFinishedLatch.countDown();
        };

        int maxWaitTimeMilliseconds = 45000;

        try {
            boolean exitedProperly = applicationFinishedLatch.await(maxWaitTimeMilliseconds, TimeUnit.MILLISECONDS);

            if (!exitedProperly) {
                logger.warning("Test driver did not exit in " + maxWaitTimeMilliseconds + "ms. Timed out");
            }
            application.stop(true);
            Thread.sleep(1000); //give time for openGL is fully released before starting a new test (get random JVM crashes without this)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getChangedImagesDirectory() {
        return Paths.get("build/changed-images/");
    }

    public Path getReportsDirectory() {
        return Paths.get("build/reports/");
    }

    @Override
    public <V> V fail(String s) {
        return Assertions.fail(s);
    }

    @Override
    public void saveGeneratedImageToChangedImages(Image generatedImage, String fileName) {
        Path savedImage = getChangedImagesDirectory().resolve(fileName);
        try {
            Files.createDirectories(savedImage.getParent());
            try (FileOutputStream fileOutBuf = new FileOutputStream(savedImage.toFile())) {
                JmeSystem.writeImageFile(fileOutBuf, "png",generatedImage.getData(0), generatedImage.getWidth(), generatedImage.getHeight());
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
