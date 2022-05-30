/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.util;

import com.jme3.system.Annotations;
import com.jme3.testable.Testable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Utility for searching Testable classes with test tags {@link Annotations.TestableTags#value()}
 * in a target package and executing {@link Testable#launch(Object)}.
 * <p>
 * This utility executes those Testables synchronously.
 *
 * @author pavl_g
 */
public final class TestableExecutor {

    private static final Logger logger = Logger.getLogger(TestableExecutor.class.getName());
    private static final TestableExecutor testableExecutor = new TestableExecutor();
    private Testable<?> currentActiveTestable;

    private TestableExecutor() {
    }

    /**
     * Retrieves the singleton instance of this utility.
     *
     * @return the instance of this utility
     */
    public static TestableExecutor getInstance() {
        return testableExecutor;
    }

    /**
     * Searches some packages for {@link Testable}s with some {@link Annotations.TestableTags} and launches them synchronously.
     *
     * @param packages the java packages to search for {@link Testable}s, eg: new String[] {"jme3test.water", "jme3test.app"}
     * @param userData an object to pass down to the {@link Testable#launch(Object)} method
     * @param tags     the testables classes tags {@link Annotations.TestableTags#value()}
     * @throws ClassNotFoundException    if a queried class is not found in a package
     * @throws InstantiationException    if a queried class in a package is an abstract class
     * @throws IllegalAccessException    if a queried class is marked as private
     * @throws NoSuchMethodException     if a queried class doesn't have a constructor, e.g: static class, enums
     * @throws InvocationTargetException if a queried class constructor throws an exception,
     *                                   use {@link InvocationTargetException#getTargetException()} to get the exception at the runtime
     */
    public <T> void launch(String[] packages, T userData, String[] tags) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        for (String javaPackage : packages) {
            launch(javaPackage, userData, tags);
        }
    }

    /**
     * Searches a java package for some {@link Testable}s with some {@link Annotations.TestableTags} and launches them synchronously.
     *
     * @param javaPackage the java package to search for {@link Testable}s, eg: "jme3test.water"
     * @param userData    an object to pass down to the {@link Testable#launch(Object)} method
     * @param tags        the testables classes tags {@link Annotations.TestableTags#value()}
     * @throws ClassNotFoundException    if a queried class is not found in the package
     * @throws InstantiationException    if a queried class in the package is an abstract class
     * @throws IllegalAccessException    if a queried class is marked as private
     * @throws NoSuchMethodException     if a queried class doesn't have a constructor, e.g: static class, enums
     * @throws InvocationTargetException if a queried class constructor throws an exception,
     *                                   use {@link InvocationTargetException#getTargetException()} to get the exception at the runtime
     */
    public <T> void launch(String javaPackage, T userData, String[] tags) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        File[] files = openPackage(getFileRepresentation(javaPackage));
        for (File file : files) {
            // recursively open a sub-package
            if (file.list() != null) {
                launch(getPackageFromFile(javaPackage, file), userData, tags);
                continue;
            }
            // sanity filter out and skip non-class files (in case you have a dirty packaging)
            if (!file.getName().contains(".class")) {
                logger.log(Level.SEVERE, "Skipping non-class file " + file.getName());
                continue;
            }
            launchTestable(getClassFromPackage(javaPackage, file), userData, tags);
        }
    }

    /**
     * Retrieves the current running {@link Testable}.
     *
     * @return the instance of the current running testable
     */
    public <T extends Testable<?>> T getCurrentActiveTestable() {
        return (T) currentActiveTestable;
    }

    /**
     * Executes {@link Testable#launch(Object)} of a Testable class (clazz) with a user param (userData) and tags {@link Annotations.TestableTags#value()}.
     *
     * @param clazz    a testable class
     * @param userData the {@link Testable#launch(Object)} parameter object
     * @param tags     class annotation tags {@link Annotations.TestableTags#value()}
     */
    private <T> void launchTestable(Class<?> clazz, T userData, String[] tags) throws IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        // sanity filter out the non-testable classes
        if (!(Testable.class.isAssignableFrom(clazz))) {
            logger.log(Level.SEVERE, "Skipping non-testable class " + clazz.getName());
            return;
        }
        // sanity filter out the non-tagged classes
        if (!hasOneOfClassTags(clazz, tags)) {
            logger.log(Level.SEVERE, "Skipping non-tagged class " + clazz.getName());
            return;
        }

        // launch a testable with a userData param
        Testable<T> testable = (Testable<T>) clazz.getDeclaredConstructor().newInstance();
        testable.launch(userData);
        this.currentActiveTestable = testable;
        logger.log(Level.INFO, "Testing testable class " + clazz.getName());

        // block until the test case finishes
        while (testable.isActive()) ;
    }

    /**
     * Tests whether a class has one of the annotation tags from a list of tags.
     *
     * @param clazz the proposed class
     * @param tags  some annotation tags to test against them
     * @return true if the class (clazz) has one of the tags as an annotation value, false otherwise
     */
    private boolean hasOneOfClassTags(Class<?> clazz, String[] tags) {
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            // skip non-test annotations
            if (!(annotation instanceof Annotations.TestableTags)) {
                continue;
            }
            // return if one of the user tags is true
            for (String tag : tags) {
                // compare all values of the annotation
                for (String value : ((Annotations.TestableTags) annotation).value()) {
                    if (value.equals(tag)) {
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
     * @return the files inside the java package
     */
    private File[] openPackage(String packageName) {
        packageName = TestableExecutor.class.getResource("/" + packageName).getFile();
        logger.log(Level.INFO, "Opening package " + packageName);
        return new File(packageName).listFiles();
    }

    /**
     * Replaces all the "." delimiters with "/" delimiters, eg: "jme3test.water" -> "jme3test/water"
     *
     * @param javaPackage a java package with "." delimiters, e.g: "jme3test.water"
     * @return a new string representation of that file with "/" delimiters
     */
    private String getFileRepresentation(String javaPackage) {
        return javaPackage.replaceAll("\\.", "/");
    }

    /**
     * Retrieves a child package from a file.
     *
     * @param parentPackage the package used for fetching the child package
     * @param file          a file to retrieve the package from
     * @return a new string representation of the child package
     */
    private String getPackageFromFile(String parentPackage, File file) {
        return parentPackage + "." + file.getName();
    }

    /**
     * Retrieves a class from a package and a file representing that class.
     *
     * @param javaPackage the package to retrieve from
     * @param clazz       the class file
     * @return a new Class representation of that file in this package
     * @throws ClassNotFoundException if the proposed class is not found in the package
     */
    private Class<?> getClassFromPackage(String javaPackage, File clazz) throws ClassNotFoundException {
        String extension = ".class";
        String clazzName = clazz.getName();
        clazzName = clazzName.substring(0, clazzName.length() - extension.length());
        return Class.forName(javaPackage + "." + clazzName);
    }
}
