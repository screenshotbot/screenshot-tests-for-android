package com.facebook.testing.screenshot.build

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
      metadataFile.replace("metadata.json", "")
    }
    puller.remoteFileExists(oldMetadataFile) -> {
      puller.pull(oldMetadataFile, localMetadataFile.absolutePath)
      oldMetadataFile.replace("metadata.json", "")
    }
    else -> {
      createEmptyMetadataFile(localMetadataFile)
      ""
    }
  }
}