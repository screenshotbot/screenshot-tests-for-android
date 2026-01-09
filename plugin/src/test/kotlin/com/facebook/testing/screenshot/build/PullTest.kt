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

import java.io.File
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class PullTest {

  private lateinit var tmpDir: File

  @Before
  fun setUp() {
    tmpDir = createTempDir(prefix = "pull_test")
  }

  @After
  fun tearDown() {
    tmpDir.deleteRecursively()
  }

  @Test
  fun testValidateMetadataWithValidJson() {
    val metadataFile = File(tmpDir, "metadata.json")
    metadataFile.writeText("""{"screenshots": []}""")

    validateMetadata(metadataFile)
  }

  @Test
  fun testValidateMetadataWithIncompleteJson() {
    val metadataFile = File(tmpDir, "metadata.json")
    metadataFile.writeText("""{"screenshots": """)

    try {
      validateMetadata(metadataFile)
      fail("Expected RuntimeException to be thrown")
    } catch (e: RuntimeException) {
      assertTrue(
        "Exception message should mention ScreenshotRunner.onDestroy()",
        e.message?.contains("ScreenshotRunner.onDestroy()") == true
      )
    }
  }

  @Test
  fun testPullImagesIntegration() {
    val project = ProjectBuilder.builder().build()
    val puller = SimplePuller(project, getAdb())

    // Set up test directory structure on device
    val deviceDir = "/sdcard/test_screenshots"
    val testRunId = "test_run_123"
    val remoteTestDir = "$deviceDir/$testRunId"

    try {
      // Create directory structure on device
      execAdb(listOf("shell", "mkdir", "-p", remoteTestDir))

      // Create three small image files on the device
      // We'll create simple dummy PNG files with different content
      val imageFiles = listOf(
        "img_0_0.png" to "image_data_0_0",
        "img_0_1.png" to "image_data_0_1",
        "img_1_0.png" to "image_data_1_0"
      )

      for ((filename, content) in imageFiles) {
        execAdb(listOf("shell", "echo $content > $remoteTestDir/$filename"))
      }

      // Create a local directory to pull images to
      val localDir = File(tmpDir, "downloaded_screenshots")
      localDir.mkdirs()

      // Call pullImages
      pullImages(localDir, deviceDir, testRunId, puller)

      // Verify all three images were downloaded correctly
      for ((filename, expectedContent) in imageFiles) {
        val localFile = File(localDir, filename)
        assertTrue("File should exist: ${localFile.absolutePath}", localFile.exists())

        val actualContent = localFile.readText().trim()
        assertEquals("Content should match for $filename", expectedContent, actualContent)
      }

      // Verify the directory structure was preserved
      val downloadedFiles = localDir.listFiles()?.map { it.name }?.sorted()
      val expectedFiles = imageFiles.map { it.first }.sorted()
      assertEquals("All image files should be downloaded", expectedFiles, downloadedFiles)

    } finally {
      // Cleanup device
      execAdb(listOf("shell", "rm", "-rf", deviceDir))
    }
  }

  @Test
  fun testPullImagesOverwritesExistingFiles() {
    val project = ProjectBuilder.builder().build()
    val puller = SimplePuller(project, getAdb())

    // Set up test directory structure on device
    val deviceDir = "/sdcard/test_screenshots_overwrite"
    val testRunId = "test_run_456"
    val remoteTestDir = "$deviceDir/$testRunId"

    try {
      // Create a local directory with existing files containing old content
      val localDir = File(tmpDir, "screenshots_with_existing")
      localDir.mkdirs()

      // Define test files with their old and new content
      data class ImageFile(val name: String, val oldContent: String, val newContent: String)
      val imageFiles = listOf(
        ImageFile("img_0_0.png", "old_content_0", "new_content_0"),
        ImageFile("img_0_1.png", "old_content_1", "new_content_1"),
        ImageFile("img_1_0.png", "old_content_2", "new_content_2")
      )

      // Write old content to local files
      for (imageFile in imageFiles) {
        val localFile = File(localDir, imageFile.name)
        localFile.writeText(imageFile.oldContent)
      }

      // Verify old content exists
      for (imageFile in imageFiles) {
        val localFile = File(localDir, imageFile.name)
        assertEquals("Old content should exist", imageFile.oldContent, localFile.readText())
      }

      // Create directory structure on device with new content
      execAdb(listOf("shell", "mkdir", "-p", remoteTestDir))

      for (imageFile in imageFiles) {
        execAdb(listOf("shell", "echo ${imageFile.newContent} > $remoteTestDir/${imageFile.name}"))
      }

      // Call pullImages - this should overwrite the existing files
      pullImages(localDir, deviceDir, testRunId, puller)

      // Verify all files were overwritten with new content
      for (imageFile in imageFiles) {
        val localFile = File(localDir, imageFile.name)
        assertTrue("File should still exist: ${localFile.absolutePath}", localFile.exists())

        val actualContent = localFile.readText().trim()
        assertEquals(
          "Content should be overwritten with new content for ${imageFile.name}",
          imageFile.newContent,
          actualContent
        )
      }

    } finally {
      // Cleanup device
      execAdb(listOf("shell", "rm", "-rf", deviceDir))
    }
  }

  private fun getAdb(): String {
    val androidSdk = System.getenv("ANDROID_SDK") ?: System.getenv("ANDROID_HOME")
      ?: throw RuntimeException("ANDROID_SDK or ANDROID_HOME needs to be set")

    return File(androidSdk, "platform-tools/adb").absolutePath
  }

  private fun execAdb(args: List<String>) {
    val project = ProjectBuilder.builder().build()
    project.exec { execSpec ->
      execSpec.executable = getAdb()
      execSpec.args = args
    }
  }
}
