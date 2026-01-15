package com.facebook.testing.screenshot.build

import org.gradle.api.Project

class AdditionalTestOutputPuller(project: Project) : RemoteFilePuller {
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
    TODO("Not yet implemented")
  }
}

