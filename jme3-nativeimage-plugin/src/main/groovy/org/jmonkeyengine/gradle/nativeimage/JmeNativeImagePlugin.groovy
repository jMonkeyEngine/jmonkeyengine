package org.jmonkeyengine.gradle.nativeimage

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.security.MessageDigest
import java.nio.file.Paths

/**
 * Applies jMonkeyEngine's GraalVM native image conventions.
 */
class JmeNativeImagePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        URL metadataScript = getClass().getResource('/org/jmonkeyengine/gradle/nativeimage/native-image-metadata.gradle')
        if (metadataScript == null) {
            throw new IllegalStateException('Unable to locate bundled native image Gradle script.')
        }
        URL codeSourceLocation = getClass().protectionDomain?.codeSource?.location
        if (codeSourceLocation != null) {
            project.extensions.extraProperties.set(
                    'jmeNativeImagePluginJarPath',
                    Paths.get(codeSourceLocation.toURI()).toFile().absolutePath
            )
        }
        project.extensions.extraProperties.set('jmeNativeImageGeneratorScriptHash', sha256(metadataScript))
        project.apply(from: metadataScript)
    }

    private static String sha256(URL resource) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        resource.openStream().withCloseable { InputStream input ->
            byte[] buffer = new byte[8192]
            int bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().collect { String.format('%02x', it & 0xff) }.join()
    }
}
