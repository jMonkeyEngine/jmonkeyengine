package org.jmonkeyengine.gradle.nativeimage

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * DSL extension for jMonkeyEngine Native Image metadata generation.
 */
class JmeNativeImageExtension {

    final ListProperty<String> additionalTargetTypes
    final ListProperty<String> additionalTargetAnnotations
    final ListProperty<List<String>> additionalProxyInterfaceSets
    final ListProperty<String> additionalUnsafeAllocatedTypes
    final ListProperty<String> additionalUnsafeAllocationContainerTypes
    final ListProperty<String> additionalResourceGlobs
    final Property<String> includeResourcesPattern
    final Property<String> excludeResourcesPattern
    final Property<Boolean> defaultResourceSettings

    @Inject
    JmeNativeImageExtension(ObjectFactory objects) {
        additionalTargetTypes = objects.listProperty(String).convention([])
        additionalTargetAnnotations = objects.listProperty(String).convention([])
        additionalProxyInterfaceSets = objects.listProperty(List).convention([])
        additionalUnsafeAllocatedTypes = objects.listProperty(String).convention([])
        additionalUnsafeAllocationContainerTypes = objects.listProperty(String).convention([])
        additionalResourceGlobs = objects.listProperty(String).convention([])
        includeResourcesPattern = objects.property(String)
        excludeResourcesPattern = objects.property(String)
        defaultResourceSettings = objects.property(Boolean).convention(false)
    }

    void targetType(String className) {
        additionalTargetTypes.add(className)
    }

    void targetAnnotation(String annotationClassName) {
        additionalTargetAnnotations.add(annotationClassName)
    }

    void proxyInterfaceSet(Iterable<String> interfaceClassNames) {
        additionalProxyInterfaceSets.add(interfaceClassNames.toList())
    }

    void unsafeAllocatedType(String className) {
        additionalUnsafeAllocatedTypes.add(className)
    }

    void unsafeAllocationContainerType(String className) {
        additionalUnsafeAllocationContainerTypes.add(className)
    }

    void resourceGlob(String glob) {
        additionalResourceGlobs.add(glob)
    }

    void includeResources(String pattern) {
        includeResourcesPattern.set(pattern)
    }

    void excludeResources(String pattern) {
        excludeResourcesPattern.set(pattern)
    }

    void useDefaultResourceSettings() {
        defaultResourceSettings.set(true)
    }
}
