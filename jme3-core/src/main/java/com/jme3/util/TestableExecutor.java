package com.jme3.util;

import com.jme3.testable.Testable;
import com.jme3.system.Annotations;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.misc.Unsafe;

/**
 *
 * @author pavl_g.
 */
public final class TestableExecutor {

    private static final Logger logger = Logger.getLogger(TestableExecutor.class.getName());

    private TestableExecutor() {
    }

    public static void execute(String[] packages, Object userData, String[] signatures) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        for (String javaPackage: packages) {
            execute(javaPackage, userData, signatures);
        }
    }

    public static void execute(String javaPackage, Object userData, String[] signatures) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        File[] files = openPackage(getFileRepresentation(javaPackage));
        for (File file : files) {
            if (file.list() != null) {
                execute(getPackageFromFile(file, javaPackage), userData, signatures);
            } else {
                executeTestable(getClassFromPackage(javaPackage, file), userData, signatures);
            }
        }
    }

    private static void executeTestable(Class<?> clazz, Object userData, String[] signatures) throws IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        // sanity check for non-testables
        if (!(Testable.class.isAssignableFrom(clazz))) {
            logger.log(Level.SEVERE, "Skipping non-testable class " + clazz.getName());
            return;
        }
        // sanity check for non-class-signatures
        if (!hasClassSignatures(clazz, signatures)) {
            logger.log(Level.SEVERE, "Skipping non-signature class " + clazz.getName());
            return;
        }

        // execute test
        Testable testable = (Testable) clazz.getDeclaredConstructor().newInstance();
        testable.launch(userData);
        logger.log(Level.INFO, "Testing testable class " + clazz.getName());

        // block until the test case finishes
        while (testable.isActive());
    }

    private static boolean hasClassSignatures(Class<?> clazz, String[] signatures) {
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            // skip non-test annotations
            if (!(annotation instanceof Annotations.Test)) {
                continue;
            }
            // return if one of the user signatures is true
            for (String sig : signatures) {
                // compare all values of the annotation
                for (String value : ((Annotations.Test) annotation).signatures()) {
                    if (value.equals(sig)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Opens a java package and lists down files.
     *
     * @return the files inside the java package.
     */
    private static File[] openPackage(String packageName) {
        packageName = TestableExecutor.class.getResource("/" + packageName).getFile();
        logger.log(Level.INFO, "Opening package " + packageName);
        return new File(packageName).listFiles();
    }

    /**
     * Replaces the package signature with the file signature.
     *
     * @param package_ the package.
     * @return a new string representation of that file.
     */
    private static String getFileRepresentation(String package_) {
        return package_.replaceAll("\\.", "/");
    }

    /**
     * Retrieves a package from a file.
     *
     * @param file
     * @param javaPackage
     * @return
     */
    private static String getPackageFromFile(File file, String javaPackage) {
        return javaPackage + "." + file.getName();
    }

    /**
     * Retrieves a class from a package and a File representing that class.
     *
     * @param javaPackage the package to retrieve from.
     * @param clazz the class file.
     * @return a new Class representation of that file in this package.
     * @throws ClassNotFoundException if the class is not found in the package.
     */
    private static Class<?> getClassFromPackage(String javaPackage, File clazz) throws ClassNotFoundException {
        String extension = ".class";
        String clazzName = clazz.getName();
        clazzName = clazzName.substring(0, clazzName.length() - extension.length());
        return Class.forName(javaPackage + "." + clazzName);
    }
}
