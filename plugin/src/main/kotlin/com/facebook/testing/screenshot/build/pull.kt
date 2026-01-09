package com.facebook.testing.screenshot.build

import com.google.gson.JsonParser
import java.io.File
import java.io.FileWriter

/**
 * Creates an empty metadata.json file.
 *
 * @param file The file to create
 */
private fun createEmptyMetadataFile(file: File) {
  file.parentFile?.mkdirs()
  FileWriter(file).use { writer -> writer.write("{}") }
}

/**
 * Pulls metadata file from the device and returns the device directory path.
 *
 * This mirrors the Python pull_metadata function but is implemented in Kotlin
 * to move more logic out of Python and into the Gradle plugin.
 *
 * @param packageName The package name of the test app
 * @param outputDir The local directory to pull metadata to
 * @param puller The SimplePuller instance to use for pulling files
 * @return The device directory path where screenshots are located
 */
fun pullMetadata(packageName: String, outputDir: File, puller: SimplePuller): String {
  val oldRootScreenshotDir = "/data/data/"
  val externalDataDir = puller.getExternalDataDir()

  val rootScreenshotDir = androidPathJoin(externalDataDir, "screenshots")
  val metadataFile =
    androidPathJoin(rootScreenshotDir, packageName, "screenshots-default/metadata.json")
  val oldMetadataFile =
    androidPathJoin(oldRootScreenshotDir, packageName, "app_screenshots-default/metadata.json")

  val localMetadataFile = File(outputDir, "metadata.json")
  

  return when {
    puller.remoteFileExists(metadataFile) -> {
      puller.pull(metadataFile, localMetadataFile.absolutePath)
      validateMetadata(localMetadataFile)
      metadataFile.replace("metadata.json", "")
    }
    puller.remoteFileExists(oldMetadataFile) -> {
      puller.pull(oldMetadataFile, localMetadataFile.absolutePath)
      validateMetadata(localMetadataFile)
      oldMetadataFile.replace("metadata.json", "")
    }
    else -> {
      createEmptyMetadataFile(localMetadataFile)
      ""
    }
  }
}

/**
 * Validates that the metadata file can be parsed as valid JSON.
 *
 * @param localMetadataFile The metadata file to validate
 * @throws RuntimeException If the metadata file cannot be parsed
 */
fun validateMetadata(localMetadataFile: File) {
  try {
    localMetadataFile.bufferedReader().use { reader ->
      JsonParser.parseReader(reader)
    }
  } catch (e: Exception) {
    throw RuntimeException(
      "Unable to parse metadata file, this commonly happens if you did not call ScreenshotRunner.onDestroy() from your instrumentation",
      e
    )
  }
}
