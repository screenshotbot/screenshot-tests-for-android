/*
 * Copyright (c) 2026 Modern Interpreters Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.testing.screenshot.build

import java.io.ByteArrayOutputStream
import java.io.File
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SimplePullerTest {

  private lateinit var puller: SimplePuller
  private lateinit var tmpDir: File
  private lateinit var serial: String

  @Before
  fun setUp() {
    val project = ProjectBuilder.builder().build()
    puller = SimplePuller(project, getAdb())

    // Get device serial
    serial = getDeviceSerial(project)

    // Create test file on device
    execAdb(listOf("shell", "echo foobar > /sdcard/blah"))

    tmpDir = createTempDir(prefix = "screenshots")
  }

  @After
  fun tearDown() {
    tmpDir.deleteRecursively()
    execAdb(listOf("shell", "rm", "-f", "/sdcard/blah"))
  }

  @Test
  fun testPullIntegration() {
    val file = File(tmpDir, "foo")
    puller.pull("/sdcard/blah", file.absolutePath)

    val content = file.readText()
    assertEquals("foobar\n", content)
  }

  @Test
  fun testFileExists() {
    assertTrue(puller.remoteFileExists("/sdcard/blah"))
    assertFalse(puller.remoteFileExists("/sdcard/sdfdsfdf"))
  }

  @Test
  fun testPullWithFilter() {
    val project = ProjectBuilder.builder().build()
    val filteredPuller = SimplePuller(project, getAdb(), listOf("-s", serial))

    val file = File(tmpDir, "foo")
    filteredPuller.pull("/sdcard/blah", file.absolutePath)

    val content = file.readText()
    assertEquals("foobar\n", content)
  }

  @Test
  fun testGetExternalDataDir() {
    val acceptedDirs = setOf(
        "/mnt/sdcard",
        "/sdcard",
        "/storage/sdcard",
        "/storage/emulated/legacy"
    )

    val externalDir = puller.getExternalDataDir()
    assertTrue("External dir '$externalDir' not in accepted list",
        externalDir in acceptedDirs)
  }

  @Test
  fun testPullFolder() {
    val targetRemoteFolder = "/sdcard/folder"
    val targetRemoteSubFolders = listOf(".", "a", "b")

    try {
      // Create folder structure on device
      execAdb(listOf("shell", "mkdir -p $targetRemoteFolder"))

      for (subFolder in targetRemoteSubFolders) {
        execAdb(listOf("shell", "mkdir -p $targetRemoteFolder/$subFolder"))

        for (i in 0 until 10) {
          execAdb(listOf(
              "shell",
              "echo foobar$i > $targetRemoteFolder/$subFolder/pic$i.png"
          ))
        }
      }

      // Pull the folder
      puller.pullFolder(targetRemoteFolder, tmpDir.absolutePath)

      // Verify all files were pulled correctly
      for (subFolder in targetRemoteSubFolders) {
        for (i in 0 until 10) {
          val file = File(tmpDir, "$subFolder/pic$i.png")
          assertTrue("File should exist: ${file.absolutePath}", file.exists())

          val content = file.readText()
          assertEquals("foobar$i\n", content)
        }
      }
    } finally {
      // Cleanup
      execAdb(listOf("shell", "rm", "-rf", targetRemoteFolder))
    }
  }

  private fun getAdb(): String {
    val androidSdk = System.getenv("ANDROID_SDK") ?: System.getenv("ANDROID_HOME")
        ?: throw RuntimeException("ANDROID_SDK or ANDROID_HOME needs to be set")

    return File(androidSdk, "platform-tools/adb").absolutePath
  }

  private fun getDeviceSerial(project: org.gradle.api.Project): String {
    val output = ByteArrayOutputStream()
    project.exec { execSpec ->
      execSpec.executable = getAdb()
      execSpec.args = listOf("get-serialno")
      execSpec.standardOutput = output
    }
    return output.toString().trim()
  }

  private fun execAdb(args: List<String>) {
    val project = ProjectBuilder.builder().build()
    project.exec { execSpec ->
      execSpec.executable = getAdb()
      execSpec.args = args
    }
  }
}
