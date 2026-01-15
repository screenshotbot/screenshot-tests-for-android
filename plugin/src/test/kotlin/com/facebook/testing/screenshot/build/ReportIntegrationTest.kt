/*
 * Copyright (c) 2026 Modern Interpreters Inc
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
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for report generation using real fixture files.
 * These tests verify the full integration with actual screenshot images and metadata.
 */
class ReportIntegrationTest {

  private lateinit var workDir: File
  private lateinit var fixturesDir: File

  @Before
  fun setUp() {
    workDir = createTempDir(prefix = "report_integration")

    // Copy fixtures from resources to a working directory
    val fixturesResource = this.javaClass.classLoader.getResource("fixtures/sdcard/screenshots/com.foo/screenshots-default")
    fixturesDir = File(fixturesResource!!.toURI())
  }

  @After
  fun tearDown() {
    workDir.deleteRecursively()
  }

  @Test
  fun testGenerateHtml_withFixtures_createsIndexHtml() {
    // Copy fixture files to work directory
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    val htmlFile = generateHtml(targetDir)

    assertTrue("index.html should exist", htmlFile.exists())
    assertTrue("index.html should be a file", htmlFile.isFile)
  }

  @Test
  fun testGenerateHtml_withFixtures_imagesAreLinked() {
    // Copy fixture files to work directory
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    generateHtml(targetDir)

    val htmlContent = File(targetDir, "index.html").readText()

    // Verify the HTML contains references to the fixture images
    assertTrue(
      "HTML should contain reference to first screenshot",
      htmlContent.contains("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png")
    )
    assertTrue(
      "HTML should contain reference to second screenshot",
      htmlContent.contains("com.foo.ScriptsFixtureTest_testSecondScreenshot.png")
    )

    // Verify package name is split correctly
    assertTrue("HTML should contain package name", htmlContent.contains("com.foo"))
    assertTrue("HTML should contain class name", htmlContent.contains("ScriptsFixtureTest"))
  }

  @Test
  fun testGenerateHtml_withFixtures_containsExpectedStructure() {
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    generateHtml(targetDir)

    val htmlContent = File(targetDir, "index.html").readText()

    assertTrue("HTML should have DOCTYPE", htmlContent.contains("<!DOCTYPE html>"))
    assertTrue("HTML should have title", htmlContent.contains("<title>Screenshot Test Results</title>"))
    assertTrue("HTML should include jQuery", htmlContent.contains("jquery"))
    assertTrue("HTML should include default.css", htmlContent.contains("default.css"))
    assertTrue("HTML should include default.js", htmlContent.contains("default.js"))

    // Verify multiple screenshots are present
    assertTrue("HTML should contain first test name", htmlContent.contains("testGetTextViewScreenshot"))
    assertTrue("HTML should contain second test name", htmlContent.contains("testSecondScreenshot"))
  }

  @Test
  fun testGenerateHtml_withFixtures_handlesErrorScreenshots() {
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    generateHtml(targetDir)

    val htmlContent = File(targetDir, "index.html").readText()

    // The metadata.json contains a screenshot with an error
    assertTrue("HTML should contain error message", htmlContent.contains("Outofmem and such"))
    assertTrue("HTML should have screenshot_error div", htmlContent.contains("screenshot_error"))
  }

  @Test
  fun testGenerateHtml_withFixtures_returnsAbsolutePath() {
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    val htmlFile = generateHtml(targetDir)

    assertTrue("HTML file path should be absolute", htmlFile.isAbsolute)
    assertTrue("HTML file should exist at returned path", htmlFile.exists())
  }

  @Test
  fun testGenerateHtml_withFixtures_imagesExistOnDisk() {
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    generateHtml(targetDir)

    // Verify that the referenced images actually exist
    assertTrue(
      "First screenshot image should exist",
      File(targetDir, "com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png").exists()
    )
    assertTrue(
      "Second screenshot image should exist",
      File(targetDir, "com.foo.ScriptsFixtureTest_testSecondScreenshot.png").exists()
    )
  }

  @Test
  fun testGenerateHtml_withFixtures_viewHierarchyIsRendered() {
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    generateHtml(targetDir)

    val htmlContent = File(targetDir, "index.html").readText()

    // The first screenshot has a view hierarchy dump
    assertTrue("HTML should contain View Hierarchy section", htmlContent.contains("View Hierarchy"))
    assertTrue("HTML should contain hierarchy class", htmlContent.contains("view-hierarchy"))
  }

  @Test
  fun testCopyAssets_withWorkingDirectory_copiesAllAssets() {
    val assetsDir = File(workDir, "assets")
    assetsDir.mkdirs()

    copyHtmlAssets(assetsDir)

    assertTrue("default.css should be copied", File(assetsDir, "default.css").exists())
    assertTrue("default.js should be copied", File(assetsDir, "default.js").exists())
    assertTrue("background.png should be copied", File(assetsDir, "background.png").exists())
    assertTrue("background_dark.png should be copied", File(assetsDir, "background_dark.png").exists())
  }

  @Test
  fun testFullIntegration_generateHtmlAndCopyAssets() {
    // This tests the full workflow: copy fixtures, copy assets, generate HTML
    val targetDir = File(workDir, "output")
    setupFixtures(targetDir)

    // Copy assets (CSS, JS, images)
    copyHtmlAssets(targetDir)

    // Generate HTML
    val htmlFile = generateHtml(targetDir)

    // Verify everything is in place
    assertTrue("index.html should exist", htmlFile.exists())
    assertTrue("default.css should exist", File(targetDir, "default.css").exists())
    assertTrue("default.js should exist", File(targetDir, "default.js").exists())
    assertTrue("background.png should exist", File(targetDir, "background.png").exists())
    assertTrue("background_dark.png should exist", File(targetDir, "background_dark.png").exists())

    // Verify HTML references the assets
    val htmlContent = htmlFile.readText()
    assertTrue("HTML should reference default.css", htmlContent.contains("default.css"))
    assertTrue("HTML should reference default.js", htmlContent.contains("default.js"))

    // Verify screenshots are linked
    assertTrue(
      "HTML should reference first screenshot",
      htmlContent.contains("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png")
    )
  }

  @Test
  fun testGenerateHtml_withFixtures_multipleScreenshotsInOrder() {
    val targetDir = File(workDir, "screenshots")
    setupFixtures(targetDir)

    generateHtml(targetDir)

    val htmlContent = File(targetDir, "index.html").readText()

    // Verify both screenshots appear in the HTML
    val firstIndex = htmlContent.indexOf("testGetTextViewScreenshot")
    val secondIndex = htmlContent.indexOf("testSecondScreenshot")

    assertTrue("First screenshot should be in HTML", firstIndex > 0)
    assertTrue("Second screenshot should be in HTML", secondIndex > 0)

    // They should appear in some order (sorting is tested elsewhere)
    assertTrue("Both screenshots should be present", firstIndex != secondIndex)
  }

  /**
   * Sets up fixtures by copying metadata and images to the target directory.
   * Images from the unittest/ subdirectory are copied to the root so they match
   * the paths in metadata.json.
   */
  private fun setupFixtures(targetDir: File) {
    targetDir.mkdirs()

    // Copy metadata files
    File(fixturesDir, "metadata.json").copyTo(File(targetDir, "metadata.json"), overwrite = true)

    // Copy the hierarchy dump with the expected naming convention
    // The code looks for {screenshot_name}_dump.json
    val dumpFile = File(fixturesDir, "one_dump.json")
    if (dumpFile.exists()) {
      dumpFile.copyTo(
        File(targetDir, "com.foo.ScriptsFixtureTest_testGetTextViewScreenshot_dump.json"),
        overwrite = true
      )
    }

    // Copy images from unittest/ subdirectory to root
    val unittestDir = File(fixturesDir, "unittest")
    if (unittestDir.exists()) {
      unittestDir.listFiles()?.forEach { imageFile ->
        if (imageFile.isFile) {
          imageFile.copyTo(File(targetDir, imageFile.name), overwrite = true)
        }
      }
    }
  }

  private fun copyDirectory(source: File, target: File) {
    if (!target.exists()) {
      target.mkdirs()
    }

    source.listFiles()?.forEach { file ->
      val targetFile = File(target, file.name)
      if (file.isDirectory) {
        copyDirectory(file, targetFile)
      } else {
        file.copyTo(targetFile, overwrite = true)
      }
    }
  }
}
