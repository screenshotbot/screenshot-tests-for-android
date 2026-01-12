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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReportTest {

  private lateinit var outputDir: File

  @Before
  fun setUp() {
    outputDir = createTempDir(prefix = "report")
  }

  @After
  fun tearDown() {
    outputDir.deleteRecursively()
  }

  @Test
  fun testCopyAssets_copiesAllRequiredFiles() {
    copyAssets(outputDir)

    assertTrue("default.css should exist", File(outputDir, "default.css").exists())
    assertTrue("default.js should exist", File(outputDir, "default.js").exists())
    assertTrue("background.png should exist", File(outputDir, "background.png").exists())
    assertTrue(
      "background_dark.png should exist",
      File(outputDir, "background_dark.png").exists()
    )
  }

  @Test
  fun testCopyAssets_filesHaveContent() {
    copyAssets(outputDir)

    val cssFile = File(outputDir, "default.css")
    val jsFile = File(outputDir, "default.js")
    val backgroundFile = File(outputDir, "background.png")
    val backgroundDarkFile = File(outputDir, "background_dark.png")

    assertTrue("default.css should have content", cssFile.length() > 0)
    assertTrue("default.js should have content", jsFile.length() > 0)
    assertTrue("background.png should have content", backgroundFile.length() > 0)
    assertTrue("background_dark.png should have content", backgroundDarkFile.length() > 0)
  }

  @Test
  fun testCopyAssets_createsDestinationIfNeeded() {
    val newDir = File(outputDir, "subdir")
    newDir.mkdirs()

    copyAssets(newDir)

    assertTrue("default.css should exist in subdir", File(newDir, "default.css").exists())
  }

  @Test
  fun testCopyAssets_overwritesExistingFiles() {
    val cssFile = File(outputDir, "default.css")
    cssFile.writeText("old content")
    assertTrue("File should exist before copy", cssFile.exists())
    val originalLength = cssFile.length()

    copyAssets(outputDir)

    assertTrue("File should still exist after copy", cssFile.exists())
    assertTrue(
      "File should have different content",
      cssFile.length() != originalLength
    )
  }

  @Test
  fun testGenerateHtml_createsIndexHtml() {
    createTestMetadata(
      listOf(
        Triple("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot", 1, 1)
      )
    )
    createTestImage("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png", 10, 10, Color.BLUE)

    val htmlFile = generateHtml(outputDir)

    assertTrue("index.html should exist", htmlFile.exists())
    assertEquals("index.html", htmlFile.name)
  }

  @Test
  fun testGenerateHtml_returnsValidFile() {
    createTestMetadata(
      listOf(
        Triple("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot", 1, 1),
        Triple("com.foo.ScriptsFixtureTest_testSecondScreenshot", 1, 1)
      )
    )
    createTestImage("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png", 10, 10, Color.BLUE)
    createTestImage("com.foo.ScriptsFixtureTest_testSecondScreenshot.png", 10, 10, Color.RED)

    val html = generateHtml(outputDir)

    assertTrue("HTML file should exist", html.exists())
    assertTrue("HTML file should be absolute", html.isAbsolute)
  }

  @Test
  fun testGenerateHtml_imageIsLinked() {
    createTestMetadata(
      listOf(
        Triple("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot", 1, 1)
      )
    )
    createTestImage("com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png", 10, 10, Color.BLUE)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should contain package name", htmlContent.contains("com.foo"))
    assertTrue(
      "HTML should contain image tag",
      htmlContent.contains("<img src=\"./com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png\"")
    )
  }

  @Test
  fun testGenerateHtml_containsBasicStructure() {
    createTestMetadata(listOf(Triple("test_screenshot", 1, 1)))
    createTestImage("test_screenshot.png", 10, 10, Color.BLUE)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should have DOCTYPE", htmlContent.contains("<!DOCTYPE html>"))
    assertTrue("HTML should have title", htmlContent.contains("<title>Screenshot Test Results</title>"))
    assertTrue("HTML should include jQuery", htmlContent.contains("jquery"))
    assertTrue("HTML should include default.css", htmlContent.contains("default.css"))
    assertTrue("HTML should include default.js", htmlContent.contains("default.js"))
  }

  @Test
  fun testGenerateHtml_withGroup() {
    val metadata = JsonArray()
    val screenshot = JsonObject()
    screenshot.addProperty("name", "test_screenshot")
    screenshot.addProperty("group", "my_group")
    screenshot.addProperty("tileWidth", 1)
    screenshot.addProperty("tileHeight", 1)
    metadata.add(screenshot)

    val metadataFile = File(outputDir, "metadata.json")
    metadataFile.writeText(metadata.toString())
    createTestImage("test_screenshot.png", 10, 10, Color.BLUE)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should contain group", htmlContent.contains("my_group"))
    assertTrue("HTML should have screenshot_group div", htmlContent.contains("screenshot_group"))
  }

  @Test
  fun testGenerateHtml_withDescription() {
    val metadata = JsonArray()
    val screenshot = JsonObject()
    screenshot.addProperty("name", "test_screenshot")
    screenshot.addProperty("description", "This is a test description")
    screenshot.addProperty("tileWidth", 1)
    screenshot.addProperty("tileHeight", 1)
    metadata.add(screenshot)

    val metadataFile = File(outputDir, "metadata.json")
    metadataFile.writeText(metadata.toString())
    createTestImage("test_screenshot.png", 10, 10, Color.BLUE)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should contain description", htmlContent.contains("This is a test description"))
    assertTrue(
      "HTML should have screenshot_description div",
      htmlContent.contains("screenshot_description")
    )
  }

  @Test
  fun testGenerateHtml_withError() {
    val metadata = JsonArray()
    val screenshot = JsonObject()
    screenshot.addProperty("name", "test_screenshot")
    screenshot.addProperty("error", "Test error message")
    screenshot.addProperty("tileWidth", 1)
    screenshot.addProperty("tileHeight", 1)
    metadata.add(screenshot)

    val metadataFile = File(outputDir, "metadata.json")
    metadataFile.writeText(metadata.toString())

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should contain error message", htmlContent.contains("Test error message"))
    assertTrue("HTML should have screenshot_error div", htmlContent.contains("screenshot_error"))
  }

  @Test
  fun testGenerateHtml_withTiledScreenshot() {
    createTestMetadata(listOf(Triple("tiled_screenshot", 2, 2)))
    createTestImage("tiled_screenshot.png", 10, 10, Color.BLUE)
    createTestImage("tiled_screenshot_1_0.png", 10, 10, Color.RED)
    createTestImage("tiled_screenshot_0_1.png", 10, 10, Color.GREEN)
    createTestImage("tiled_screenshot_1_1.png", 10, 10, Color.YELLOW)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should contain first tile", htmlContent.contains("tiled_screenshot.png"))
    assertTrue("HTML should contain second tile", htmlContent.contains("tiled_screenshot_1_0.png"))
    assertTrue("HTML should contain third tile", htmlContent.contains("tiled_screenshot_0_1.png"))
    assertTrue("HTML should contain fourth tile", htmlContent.contains("tiled_screenshot_1_1.png"))
  }

  @Test
  fun testGenerateHtml_alternateRowsStyled() {
    createTestMetadata(
      listOf(
        Triple("screenshot1", 1, 1),
        Triple("screenshot2", 1, 1),
        Triple("screenshot3", 1, 1)
      )
    )
    createTestImage("screenshot1.png", 10, 10, Color.BLUE)
    createTestImage("screenshot2.png", 10, 10, Color.BLUE)
    createTestImage("screenshot3.png", 10, 10, Color.BLUE)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should have alternate class", htmlContent.contains("class=\"screenshot alternate\""))
    assertTrue("HTML should have non-alternate class", htmlContent.contains("class=\"screenshot \""))
  }

  @Test
  fun testSortScreenshots_withSameGroup_orderedTogether() {
    val screenshots = JsonArray()

    val one = JsonObject()
    one.addProperty("name", "one")
    one.addProperty("group", "foo")
    one.addProperty("tileWidth", 1)
    one.addProperty("tileHeight", 1)
    screenshots.add(one)

    val two = JsonObject()
    two.addProperty("name", "two")
    two.addProperty("tileWidth", 1)
    two.addProperty("tileHeight", 1)
    screenshots.add(two)

    val three = JsonObject()
    three.addProperty("name", "three")
    three.addProperty("group", "foo")
    three.addProperty("tileWidth", 1)
    three.addProperty("tileHeight", 1)
    screenshots.add(three)

    createTestMetadata(screenshots)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    val twoIndex = htmlContent.indexOf("two")
    val oneIndex = htmlContent.indexOf("one")
    val threeIndex = htmlContent.indexOf("three")

    assertTrue("two should appear before one", twoIndex < oneIndex)
    assertTrue("one should appear before three", oneIndex < threeIndex)
  }

  @Test
  fun testGenerateHtml_splitPackageAndName() {
    createTestMetadata(listOf(Triple("com.example.package.MyTest_testMethod", 1, 1)))
    createTestImage("com.example.package.MyTest_testMethod.png", 10, 10, Color.BLUE)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue("HTML should contain package with demphasize", htmlContent.contains("demphasize\">com.example.package."))
    assertTrue("HTML should contain test name", htmlContent.contains("MyTest_testMethod"))
  }

  @Test
  fun testGenerateHtml_includesCommandButtons() {
    createTestMetadata(listOf(Triple("test", 1, 1)))
    createTestImage("test.png", 10, 10, Color.BLUE)

    generateHtml(outputDir)

    val htmlContent = File(outputDir, "index.html").readText()
    assertTrue(
      "HTML should have toggle dark button",
      htmlContent.contains("Toggle Dark Background")
    )
    assertTrue(
      "HTML should have toggle hierarchy button",
      htmlContent.contains("Toggle View Hierarchy Overlay")
    )
  }

  private fun createTestMetadata(screenshots: List<Triple<String, Int, Int>>) {
    val metadata = JsonArray()
    for ((name, tileWidth, tileHeight) in screenshots) {
      val screenshot = JsonObject()
      screenshot.addProperty("name", name)
      screenshot.addProperty("tileWidth", tileWidth)
      screenshot.addProperty("tileHeight", tileHeight)
      metadata.add(screenshot)
    }
    createTestMetadata(metadata)
  }

  private fun createTestMetadata(metadata: JsonArray) {
    val metadataFile = File(outputDir, "metadata.json")
    metadataFile.writeText(metadata.toString())
  }

  private fun createTestImage(name: String, width: Int, height: Int, color: Color) {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      g.color = color
      g.fillRect(0, 0, width, height)
    } finally {
      g.dispose()
    }

    val file = File(outputDir, name)
    ImageIO.write(image, "PNG", file)
  }
}
