package com.facebook.testing.screenshot.build

import com.android.build.gradle.api.TestVariant
import org.gradle.api.Project
import java.io.File

class AdditionalTestOutputPuller(val project: Project, val variant: TestVariant) : RemoteFilePuller {
  override fun remoteFileExists(src: String): Boolean {
    TODO("Not yet implemented")
  }

  override fun pull(src: String, dest: String) {
    TODO("Not yet implemented")
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

    TODO("not implement " + variant.flavorName + " " + flavorDir.toString())

  }
}

