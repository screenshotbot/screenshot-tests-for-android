package com.facebook.testing.screenshot.build

import java.io.File

class AdditionalTestOutputPuller(val buildDirectory: File, val variantName: String) : RemoteFilePuller {
  override fun remoteFileExists(src: String): Boolean {
    return File(src).exists()
  }

  override fun pull(src: String, dest: String) {
    copyFile(File(src), File(dest))
  }

  override fun pullFolder(src: String, dest: String) {
    File(src).copyRecursively(File(dest), overwrite = true)
  }

  override fun getExternalDataDir(): String {
    // The files look like this: P162
    // I wonder if there's a better way to retrieve this than hardcoding these subdirectories

    val outputDir = File(buildDirectory, "outputs/connected_android_test_additional_output")

    val flavorDir = File(outputDir, variantName /* e.g., debugAndroidTest */)

    return getOnlySubdir(getOnlySubdir(flavorDir)).absolutePath
  }

  fun getOnlySubdir(input: File) : File {
    val files = input.list()
    var result: String? = null;

    for (name in files) {
      if (name.equals(".") || name.equals("..")) {
        continue
      }
      if (result != null) {
        throw RuntimeException("Multiple subdirectories in ${input}. We don't know how to handle this")
      }
      result = name
    }

    return File(input, result)
  }

  private fun copyFile(src: File, dest: File) {
    src.copyTo(dest, overwrite = true)
  }
}

