package jme3test.ios;

import com.jme3.app.Application;
import com.jme3.app.IosApplicationLauncher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IosTestChooserLauncher extends IosApplicationLauncher {
    private static final String CLASS_LIST_RESOURCE = "/jme3test/test-classes.txt";
    private static final String IOS_INITIAL_EXAMPLE_CLASS = "jme3test.ios.IosInitialExample";
    private static List<String> testClasses;
    private static IosTestChooserLauncher activeLauncher;

    private String pendingClass;

    @Override
    public void start() {
        activeLauncher = this;
        pendingClass = initialExampleClass();
        super.start();
    }

    @Override
    protected Application createApplication() {
        return new IosTestChooser();
    }

    @Override
    public void update() {
        if (startPendingClass()) {
            return;
        }
        super.update();
    }

    @Override
    public void stop(boolean waitFor) {
        super.stop(waitFor);
        if (activeLauncher == this) {
            activeLauncher = null;
        }
    }

    static void selectForNextLaunch(String className) {
        if (!testClasses().contains(className)) {
            throw new IllegalArgumentException("Unsupported iOS test: " + className);
        }
        if (activeLauncher != null) {
            activeLauncher.selectForCurrentRun(className);
            return;
        }
        throw new IllegalStateException("No active iOS test chooser launcher");
    }

    static List<String> testClasses() {
        if (testClasses == null) {
            testClasses = Collections.unmodifiableList(loadAvailableTestClasses());
        }
        return testClasses;
    }

    private static List<String> loadAvailableTestClasses() {
        List<String> classes = new ArrayList<>();
        try (InputStream stream = IosTestChooserLauncher.class.getResourceAsStream(CLASS_LIST_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing iOS test class list resource: " + CLASS_LIST_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String className = line.trim();
                    if (!className.isEmpty() && isAvailableApplicationClass(className)) {
                        classes.add(className);
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load iOS test class list", exception);
        }
        return classes;
    }

    private static boolean isAvailableApplicationClass(String className) {
        try {
            Class<?> clazz = Class.forName(className, false, IosTestChooserLauncher.class.getClassLoader());
            return Application.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static Application instantiate(String className) {
        try {
            Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
            if (!(instance instanceof Application)) {
                throw new IllegalStateException(className + " is not a jME Application");
            }
            return (Application) instance;
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not start selected iOS test: " + className, exception);
        }
    }

    private void selectForCurrentRun(String className) {
        pendingClass = className;
    }

    private boolean startPendingClass() {
        String className = pendingClass;
        if (className == null) {
            return false;
        }
        pendingClass = null;
        stopApplicationForHandoff();
        app = instantiate(className);
        app.start();
        return true;
    }

    private static String initialExampleClass() {
        String className = generatedInitialExampleClassName();
        if (className == null || className.isEmpty()) {
            return null;
        }
        if (!isAvailableApplicationClass(className)) {
            throw new IllegalStateException("Configured iOS test is not available: " + className);
        }
        return className;
    }

    private static String generatedInitialExampleClassName() {
        try {
            Class<?> clazz = Class.forName(IOS_INITIAL_EXAMPLE_CLASS, false,
                    IosTestChooserLauncher.class.getClassLoader());
            java.lang.reflect.Method method = clazz.getDeclaredMethod("className");
            method.setAccessible(true);
            return (String) method.invoke(null);
        } catch (ClassNotFoundException exception) {
            return null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not read generated iOS initial example", exception);
        }
    }

    private void stopApplicationForHandoff() {
        Application previous = app;
        app = null;
        if (previous == null) {
            return;
        }
        if (previous instanceof IosTestChooser) {
            ((IosTestChooser) previous).prepareForHandoff();
        }
        previous.stop(false);
    }
}
