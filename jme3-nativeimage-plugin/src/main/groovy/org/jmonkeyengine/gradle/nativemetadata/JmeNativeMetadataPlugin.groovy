package org.jmonkeyengine.gradle.nativemetadata

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.security.MessageDigest
import java.nio.file.Paths

/**
 * Applies jMonkeyEngine's GraalVM native metadata conventions.
 */
class JmeNativeMetadataPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        URL metadataScript = getClass().getResource('/org/jmonkeyengine/gradle/nativemetadata/native-image-metadata.gradle')
        if (metadataScript == null) {
            throw new IllegalStateException('Unable to locate bundled native metadata Gradle script.')
        }
        URL codeSourceLocation = getClass().protectionDomain?.codeSource?.location
        if (codeSourceLocation != null) {
            project.extensions.extraProperties.set(
                    'jmeNativeMetadataPluginJarPath',
                    Paths.get(codeSourceLocation.toURI()).toFile().absolutePath
            )
        }
        project.extensions.extraProperties.set('jmeNativeMetadataGeneratorScriptHash', sha256(metadataScript))
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
