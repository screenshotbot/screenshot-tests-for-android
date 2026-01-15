package com.facebook.testing.screenshot.build

import com.android.build.gradle.api.TestVariant
import org.gradle.api.Project
import org.gradle.api.file.Directory
import java.io.File
import java.nio.file.Paths

class AdditionalTestOutputPuller(val project: Project, val variant: TestVariant) : RemoteFilePuller {
  override fun remoteFileExists(src: String): Boolean {
    return File(src).exists()
  }

  override fun pull(src: String, dest: String) {
    copyFile(File(src), File(dest))
  }

  override fun pullFolder(src: String, dest: String) {
    TODO("Not yet implemented")
  }

  override fun getExternalDataDir(): String {
    /*
The files look like this: P162
I wonder if there's a better way to retrieve this than hardcoding these subdirectories
 */
    val outputDir = project.layout.buildDirectory.get()
      .dir("outputs")
      .dir("connected_android_test_additional_output")

    val flavorDir = outputDir.dir(variant.name /* e.g., debugAndroidTest */);

    return getOnlySubdir(getOnlySubdir(flavorDir.asFile)).absolutePath
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

