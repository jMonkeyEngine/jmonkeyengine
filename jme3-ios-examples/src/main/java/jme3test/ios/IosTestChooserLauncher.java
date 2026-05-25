package jme3test.ios;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.SystemListener;
import com.jme3.system.ios.IGLESContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IosTestChooserLauncher {
    private static final String CLASS_LIST_RESOURCE = "/jme3test/test-classes.txt";
    private static final String IOS_INITIAL_EXAMPLE_CLASS = "jme3test.ios.IosInitialExample";
    private static List<String> testClasses;
    private static IosTestChooserLauncher activeLauncher;

    private Application delegate;
    private String pendingClass;

    public void start() {
        activeLauncher = this;
        delegate = new IosTestChooser();
        pendingClass = initialExampleClass();
        startDelegate(delegate);
    }

    public JmeContext getContext() {
        return null;
    }

    public void update() {
        if (startPendingClass()) {
            return;
        }
        runDelegateFrame();
    }

    public void reshape(int width, int height) {
        if (delegate == null) {
            return;
        }
        JmeContext context = delegate.getContext();
        if (context instanceof IGLESContext) {
            ((IGLESContext) context).resizeFramebuffer(width, height);
            return;
        }
        if (delegate instanceof SystemListener) {
            ((SystemListener) delegate).reshape(width, height);
            return;
        }
        invokeIfPresent(delegate, "reshape", new Class<?>[]{int.class, int.class}, width, height);
    }

    public void stop(boolean waitFor) {
        if (delegate != null) {
            delegate.stop(waitFor);
            delegate = null;
        }
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

    private void startDelegate(Application application) {
        invokeSetShowSettings(application);
        configureIosSettings(application);
        application.start();
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
        stopDelegateForHandoff();
        delegate = instantiate(className);
        startDelegate(delegate);
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

    private void runDelegateFrame() {
        if (delegate == null) {
            return;
        }
        JmeContext context = delegate.getContext();
        if (context instanceof IGLESContext) {
            ((IGLESContext) context).runFrame();
            return;
        }
        if (delegate instanceof SystemListener) {
            ((SystemListener) delegate).update();
            return;
        }
        invokeIfPresent(delegate, "update", new Class<?>[0]);
    }

    private void stopDelegateForHandoff() {
        Application previous = delegate;
        delegate = null;
        if (previous == null) {
            return;
        }
        if (previous instanceof IosTestChooser) {
            ((IosTestChooser) previous).prepareForHandoff();
        }
        previous.stop(false);
    }

    private static void invokeSetShowSettings(Application application) {
        try {
            application.getClass().getMethod("setShowSettings", boolean.class).invoke(application, false);
        } catch (NoSuchMethodException ignored) {
            // Some Application subclasses do not expose settings dialogs.
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not disable settings dialog", exception);
        }
    }

    private static void configureIosSettings(Application application) {
        AppSettings settings = new AppSettings(true);
        settings.setUseJoysticks(true);
        settings.setOnDeviceJoystickRumble(true);
        invokeConfigureSettings(application, settings);
        application.setSettings(settings);
    }

    private static void invokeConfigureSettings(Application application, AppSettings settings) {
        try {
            java.lang.reflect.Method method = application.getClass().getMethod("configureSettings", AppSettings.class);
            Object target = java.lang.reflect.Modifier.isStatic(method.getModifiers()) ? null : application;
            method.invoke(target, settings);
        } catch (NoSuchMethodException ignored) {
            // Most examples rely on default settings.
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not configure iOS settings for "
                    + application.getClass().getName(), exception);
        }
    }

    private static Object invokeIfPresent(Object target, String name, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return MissingMethod.INSTANCE;
        }
        try {
            return target.getClass().getMethod(name, parameterTypes).invoke(target, args);
        } catch (NoSuchMethodException ignored) {
            return MissingMethod.INSTANCE;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not invoke " + name, exception);
        }
    }

    private enum MissingMethod {
        INSTANCE
    }
}
