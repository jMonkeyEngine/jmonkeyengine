/*
 * Copyright (c) 2009-2025 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test;

import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.system.JmeContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command-line test chooser for running example classes without any AWT/Swing usage.
 */
public class TestChooserCli {

    private static final Logger logger = Logger.getLogger(TestChooserCli.class.getName());
    private static final String CLASS_LIST_RESOURCE = "/jme3test/test-classes.txt";
    private static final long WAIT_INTERVAL_MILLIS = Math.max(10L,
            Long.getLong("jme3test.cli.waitIntervalMillis", 100L));
    private static final long START_TIMEOUT_MILLIS = Math.max(WAIT_INTERVAL_MILLIS,
            Long.getLong("jme3test.cli.startTimeoutMillis", 30000L));
    private static final long RUN_TIMEOUT_MILLIS = Long.getLong("jme3test.cli.runTimeoutMillis",
            30L * 60L * 1000L);

    public static void main(String[] args) {
        new TestChooserCli().start(args);
    }

    private void start(String[] args) {
        if (args.length > 0) {
            launchFromArgument(args);
            return;
        }

        List<String> fallbackClassNames = loadClassNamesFromResource();
        Set<Class<?>> discovered = new LinkedHashSet<Class<?>>();
        addDisplayedClasses(discovered);

        List<Class<?>> sorted = new ArrayList<Class<?>>(discovered);
        Collections.sort(sorted, (a, b) -> a.getName().compareTo(b.getName()));

        if (sorted.isEmpty()) {
            if (!fallbackClassNames.isEmpty()) {
                printClassNameMenu(fallbackClassNames);
                String selectedClassName = chooseClassName(fallbackClassNames);
                if (selectedClassName != null) {
                    launchFromArgument(new String[]{selectedClassName});
                }
                return;
            }
            logger.warning("No test classes discovered. Pass a class name explicitly, for example: jme3test.light.TestManyLights");
            return;
        }

        printMenu(sorted);
        Class<?> target = chooseClass(sorted);
        if (target != null) {
            launchClass(target, new String[0]);
        }
    }

    private void launchFromArgument(String[] args) {
        String requested = args[0];
        String[] appArgs = Arrays.copyOfRange(args, 1, args.length);

        for (String listedClassName : loadClassNamesFromResource()) {
            if (listedClassName.equals(requested) || simpleClassName(listedClassName).equals(requested)) {
                Class<?> listedClass = loadFromClassName(listedClassName);
                if (listedClass != null) {
                    launchClass(listedClass, appArgs);
                    return;
                }
            }
        }

        Class<?> target;
        try {
            target = Class.forName(requested);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot find test class: " + requested, e);
            return;
        }

        launchClass(target, appArgs);
    }

    private Class<?> chooseClass(List<Class<?>> classes) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Choose test index or class name (empty to exit): ");
        try {
            String input = reader.readLine();
            if (input == null || input.trim().isEmpty()) {
                return null;
            }
            input = input.trim();
            try {
                int index = Integer.parseInt(input);
                if (index >= 1 && index <= classes.size()) {
                    return classes.get(index - 1);
                }
                System.err.println("Invalid index: " + index);
                return null;
            } catch (NumberFormatException ignored) {
                for (Class<?> c : classes) {
                    if (c.getName().equals(input) || c.getSimpleName().equals(input)) {
                        return c;
                    }
                }
                try {
                    return Class.forName(input);
                } catch (ClassNotFoundException e) {
                    logger.log(Level.SEVERE, "Cannot find class: " + input, e);
                    return null;
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read input", e);
            return null;
        }
    }

    private void printMenu(List<Class<?>> classes) {
        System.out.println("Available jME tests:");
        for (int i = 0; i < classes.size(); i++) {
            System.out.println(String.format("%3d. %s", i + 1, classes.get(i).getName()));
        }
    }

    private void printClassNameMenu(List<String> classNames) {
        System.out.println("Available jME tests (from resource list):");
        for (int i = 0; i < classNames.size(); i++) {
            System.out.println(String.format("%3d. %s", i + 1, classNames.get(i)));
        }
    }

    private String chooseClassName(List<String> classNames) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Choose test index or class name (empty to exit): ");
        try {
            String input = reader.readLine();
            if (input == null || input.trim().isEmpty()) {
                return null;
            }
            input = input.trim();
            try {
                int index = Integer.parseInt(input);
                if (index >= 1 && index <= classNames.size()) {
                    return classNames.get(index - 1);
                }
                System.err.println("Invalid index: " + index);
                return null;
            } catch (NumberFormatException ignored) {
                for (String className : classNames) {
                    if (className.equals(input) || simpleClassName(className).equals(input)) {
                        return className;
                    }
                }
                return input;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read input", e);
            return null;
        }
    }

    private void launchClass(Class<?> clazz, String[] appArgs) {
        try {
            if (LegacyApplication.class.isAssignableFrom(clazz)) {
                LegacyApplication app = (LegacyApplication) clazz.getDeclaredConstructor().newInstance();
                if (app instanceof SimpleApplication) {
                    ((SimpleApplication) app).setShowSettings(false);
                }
                app.start();

                JmeContext context = waitForContext(app, clazz.getName());
                waitForContextCreated(app, context, clazz.getName());
                waitForContextDestroyed(app, context, clazz.getName());
            } else {
                Method mainMethod = clazz.getMethod("main", String[].class);
                mainMethod.invoke(null, new Object[]{appArgs});
            }
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Cannot access constructor: " + clazz.getName(), e);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "main() had illegal argument: " + clazz.getName(), e);
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "main() method had exception: " + clazz.getName(), e);
        } catch (InstantiationException e) {
            logger.log(Level.SEVERE, "Failed to create app: " + clazz.getName(), e);
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Test class does not have required method: " + clazz.getName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Interrupted while waiting for app context", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot start test: " + clazz.getName(), e);
        }
    }

    private JmeContext waitForContext(LegacyApplication app, String className) throws InterruptedException {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MILLIS;
        JmeContext context = app.getContext();
        while (context == null) {
            if (System.currentTimeMillis() >= deadline) {
                requestStop(app);
                throw new IllegalStateException("Timed out waiting for application context: " + className);
            }
            Thread.sleep(WAIT_INTERVAL_MILLIS);
            context = app.getContext();
        }
        return context;
    }

    private void waitForContextCreated(LegacyApplication app, JmeContext context, String className)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MILLIS;
        while (!context.isCreated()) {
            if (System.currentTimeMillis() >= deadline) {
                requestStop(app);
                throw new IllegalStateException("Timed out waiting for context creation: " + className);
            }
            Thread.sleep(WAIT_INTERVAL_MILLIS);
        }
    }

    private void waitForContextDestroyed(LegacyApplication app, JmeContext context, String className)
            throws InterruptedException {
        long deadline = RUN_TIMEOUT_MILLIS > 0L ? System.currentTimeMillis() + RUN_TIMEOUT_MILLIS : Long.MAX_VALUE;
        while (context.isCreated()) {
            if (System.currentTimeMillis() >= deadline) {
                requestStop(app);
                throw new IllegalStateException("Timed out waiting for application to exit: " + className);
            }
            Thread.sleep(WAIT_INTERVAL_MILLIS);
        }
    }

    private void requestStop(LegacyApplication app) {
        JmeContext context = app.getContext();
        if (context == null) {
            return;
        }
        try {
            app.stop(false);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to stop timed-out application", e);
        }
    }

    private void find(String packageName, boolean recursive, Set<Class<?>> classes) {
        String name = packageName;
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        name = name.replace('.', '/');

        packageName = packageName + ".";
        URI uri;
        FileSystem fileSystem = null;
        boolean closeFileSystem = false;
        try {
            URL packageUrl = this.getClass().getResource(name);
            if (packageUrl == null) {
                return;
            }
            uri = packageUrl.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load demo classes.", e);
        }

        if ("jar".equalsIgnoreCase(uri.getScheme())) {
            try {
                fileSystem = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                try {
                    fileSystem = FileSystems.newFileSystem(uri, Collections.<String, String>emptyMap());
                    closeFileSystem = true;
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to load demo classes from JAR.", ex);
                }
            }
        }

        try {
            Path directory = Paths.get(uri);
            addAllFilesInDirectory(directory, classes, packageName, recursive);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find classes", e);
        } finally {
            if (fileSystem != null && closeFileSystem) {
                try {
                    fileSystem.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to close JAR.", e);
                }
            }
        }
    }

    private void addAllFilesInDirectory(
            final Path directory,
            final Set<Class<?>> allClasses,
            final String packageName,
            final boolean recursive) {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, getFileFilter())) {
            for (Path file : stream) {
                if (Files.isDirectory(file)) {
                    if (recursive) {
                        String dirName = String.valueOf(file.getFileName());
                        if (dirName.endsWith("/")) {
                            dirName = dirName.substring(0, dirName.length() - 1);
                        }
                        addAllFilesInDirectory(file, allClasses, packageName + dirName + ".", true);
                    }
                } else {
                    Class<?> result = load(packageName + file.getFileName());
                    if (result != null && !allClasses.contains(result)) {
                        allClasses.add(result);
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not search the folder", ex);
        }
    }

    private static DirectoryStream.Filter<Path> getFileFilter() {
        return new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                String fileName = entry.getFileName().toString();
                return ((fileName.endsWith(".class") && fileName.contains("Test") && !fileName.contains("$"))
                        || (!fileName.startsWith(".") && Files.isDirectory(entry)));
            }
        };
    }

    private Class<?> load(String name) {
        String classname = name.substring(0, name.length() - ".class".length());
        if (classname.startsWith("/")) {
            classname = classname.substring(1);
        }
        classname = classname.replace('/', '.');

        return loadFromClassName(classname);
    }

    private Class<?> loadFromClassName(String classname) {

        if (classname.equals(TestChooser.class.getName()) || classname.equals(TestChooserCli.class.getName())) {
            return null;
        }

        try {
            final Class<?> cls = Class.forName(classname, false, TestChooserCli.class.getClassLoader());
            cls.getMethod("main", String[].class);
            return cls;
        } catch (NoClassDefFoundError e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (UnsupportedClassVersionError e) {
            return null;
        }
    }

    private List<String> loadClassNamesFromResource() {
        InputStream stream = TestChooserCli.class.getResourceAsStream(CLASS_LIST_RESOURCE);
        if (stream == null) {
            return Collections.emptyList();
        }

        List<String> classNames = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String className = line.trim();
                if (!className.isEmpty()) {
                    classNames.add(className);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed reading test class list resource: " + CLASS_LIST_RESOURCE, e);
        }
        return classNames;
    }

    private String simpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < className.length()) {
            return className.substring(lastDot + 1);
        }
        return className;
    }

    protected void addDisplayedClasses(Set<Class<?>> classes) {
        find("jme3test", true, classes);
    }
}
