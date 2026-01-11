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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class RecorderTest {

  private lateinit var outputDir: File
  private lateinit var expectedOutputDir: File
  private lateinit var failureOutputDir: File

  @Before
  fun setUp() {
    outputDir = createTempDir(prefix = "output")
    expectedOutputDir = createTempDir(prefix = "expected")
    failureOutputDir = createTempDir(prefix = "failure")
  }

  @After
  fun tearDown() {
    if (outputDir.exists()) {
      outputDir.deleteRecursively()
    }
    expectedOutputDir.deleteRecursively()
    failureOutputDir.deleteRecursively()
  }

  @Test
  fun testVerifyHelper_identicalImages_succeeds() {
    val image = createTestImage(100, 100, Color.BLUE)
    val screenshot1 = File(outputDir, "test1.png")
    val screenshot2 = File(expectedOutputDir, "test1.png")

    ImageIO.write(image, "PNG", screenshot1)
    ImageIO.write(image, "PNG", screenshot2)

    val metadata = createMetadata(listOf("test1"))

    verifyHelper(metadata, outputDir, expectedOutputDir, null)

    assertFalse("Output directory should be deleted on success", outputDir.exists())
  }

  @Test
  fun testVerifyHelper_differentImages_withoutFailureOutput_throwsImmediately() {
    val image1 = createTestImage(100, 100, Color.BLUE)
    val image2 = createTestImage(100, 100, Color.RED)

    val screenshot1 = File(outputDir, "test1.png")
    val screenshot2 = File(expectedOutputDir, "test1.png")

    ImageIO.write(image1, "PNG", screenshot1)
    ImageIO.write(image2, "PNG", screenshot2)

    val metadata = createMetadata(listOf("test1"))

    try {
      verifyHelper(metadata, outputDir, expectedOutputDir, null)
      fail("Should have thrown VerifyError")
    } catch (e: VerifyError) {
      assertTrue(e.message!!.contains("is not same as"))
      assertTrue(e.message!!.contains("test1.png"))
    }
  }

  @Test
  fun testVerifyHelper_differentImages_withFailureOutput_createsFailureFiles() {
    val image1 = createTestImage(100, 100, Color.BLUE)
    val image2 = createTestImage(100, 100, Color.RED)

    val screenshot1 = File(outputDir, "test1.png")
    val screenshot2 = File(expectedOutputDir, "test1.png")

    ImageIO.write(image1, "PNG", screenshot1)
    ImageIO.write(image2, "PNG", screenshot2)

    val metadata = createMetadata(listOf("test1"))

    try {
      verifyHelper(metadata, outputDir, expectedOutputDir, failureOutputDir)
      fail("Should have thrown VerifyError")
    } catch (e: VerifyError) {
      assertTrue(e.message!!.contains("is not same as"))
    }

    assertTrue("Diff file should exist", File(failureOutputDir, "test1_diff.png").exists())
    assertTrue("Expected file should exist", File(failureOutputDir, "test1_expected.png").exists())
    assertTrue("Actual file should exist", File(failureOutputDir, "test1_actual.png").exists())
  }

  @Test
  fun testVerifyHelper_multipleScreenshots_allIdentical_succeeds() {
    val names = listOf("test1", "test2", "test3")

    for (name in names) {
      val image = createTestImage(100, 100, Color.BLUE)
      ImageIO.write(image, "PNG", File(outputDir, "$name.png"))
      ImageIO.write(image, "PNG", File(expectedOutputDir, "$name.png"))
    }

    val metadata = createMetadata(names)

    verifyHelper(metadata, outputDir, expectedOutputDir, null)

    assertFalse("Output directory should be deleted on success", outputDir.exists())
  }

  @Test
  fun testVerifyHelper_multipleScreenshots_someFailures_collectsAllFailures() {
    val image1 = createTestImage(100, 100, Color.BLUE)
    val image2 = createTestImage(100, 100, Color.RED)

    ImageIO.write(image1, "PNG", File(outputDir, "test1.png"))
    ImageIO.write(image1, "PNG", File(expectedOutputDir, "test1.png"))

    ImageIO.write(image2, "PNG", File(outputDir, "test2.png"))
    ImageIO.write(image1, "PNG", File(expectedOutputDir, "test2.png"))

    ImageIO.write(image2, "PNG", File(outputDir, "test3.png"))
    ImageIO.write(image1, "PNG", File(expectedOutputDir, "test3.png"))

    val metadata = createMetadata(listOf("test1", "test2", "test3"))

    try {
      verifyHelper(metadata, outputDir, expectedOutputDir, failureOutputDir)
      fail("Should have thrown VerifyError")
    } catch (e: VerifyError) {
      assertTrue("Error should mention test2", e.message!!.contains("test2.png"))
      assertTrue("Error should mention test3", e.message!!.contains("test3.png"))
      assertFalse("Error should not mention test1", e.message!!.contains("test1.png"))
    }

    assertTrue(File(failureOutputDir, "test2_diff.png").exists())
    assertTrue(File(failureOutputDir, "test2_expected.png").exists())
    assertTrue(File(failureOutputDir, "test2_actual.png").exists())

    assertTrue(File(failureOutputDir, "test3_diff.png").exists())
    assertTrue(File(failureOutputDir, "test3_expected.png").exists())
    assertTrue(File(failureOutputDir, "test3_actual.png").exists())

    assertFalse(File(failureOutputDir, "test1_diff.png").exists())
  }

  @Test
  fun testVerifyHelper_differentSizes_fails() {
    val image1 = createTestImage(100, 100, Color.BLUE)
    val image2 = createTestImage(200, 200, Color.BLUE)

    val screenshot1 = File(outputDir, "test1.png")
    val screenshot2 = File(expectedOutputDir, "test1.png")

    ImageIO.write(image1, "PNG", screenshot1)
    ImageIO.write(image2, "PNG", screenshot2)

    val metadata = createMetadata(listOf("test1"))

    try {
      verifyHelper(metadata, outputDir, expectedOutputDir, failureOutputDir)
      fail("Should have thrown VerifyError")
    } catch (e: VerifyError) {
      assertTrue(e.message!!.contains("is not same as"))
    }

    assertTrue("Diff file should exist", File(failureOutputDir, "test1_diff.png").exists())
  }

  @Test
  fun testVerifyHelper_singlePixelDifference_fails() {
    val image1 = createTestImage(100, 100, Color.BLUE)
    val image2 = createTestImage(100, 100, Color.BLUE)

    image2.setRGB(50, 50, Color.RED.rgb)

    val screenshot1 = File(outputDir, "test1.png")
    val screenshot2 = File(expectedOutputDir, "test1.png")

    ImageIO.write(image1, "PNG", screenshot1)
    ImageIO.write(image2, "PNG", screenshot2)

    val metadata = createMetadata(listOf("test1"))

    try {
      verifyHelper(metadata, outputDir, expectedOutputDir, failureOutputDir)
      fail("Should have thrown VerifyError")
    } catch (e: VerifyError) {
      assertTrue(e.message!!.contains("is not same as"))
    }

    assertTrue(File(failureOutputDir, "test1_diff.png").exists())

    val diffImage = ImageIO.read(File(failureOutputDir, "test1_diff.png"))
    val redPixelExists = (0 until diffImage.width).any { x ->
      (0 until diffImage.height).any { y ->
        val color = Color(diffImage.getRGB(x, y))
        color.red == 255 && color.green == 0 && color.blue == 0
      }
    }
    assertTrue("Diff image should contain red rectangle", redPixelExists)
  }

  @Test
  fun testVerifyHelper_partialDifference_createsCorrectDiffBounds() {
    val image1 = createTestImage(200, 200, Color.WHITE)
    val image2 = createTestImage(200, 200, Color.WHITE)

    for (x in 50 until 100) {
      for (y in 50 until 100) {
        image2.setRGB(x, y, Color.BLACK.rgb)
      }
    }

    val screenshot1 = File(outputDir, "test1.png")
    val screenshot2 = File(expectedOutputDir, "test1.png")

    ImageIO.write(image1, "PNG", screenshot1)
    ImageIO.write(image2, "PNG", screenshot2)

    val metadata = createMetadata(listOf("test1"))

    try {
      verifyHelper(metadata, outputDir, expectedOutputDir, failureOutputDir)
      fail("Should have thrown VerifyError")
    } catch (e: VerifyError) {
      assertTrue(e.message!!.contains("is not same as"))
    }

    assertTrue(File(failureOutputDir, "test1_diff.png").exists())
  }

  private fun createTestImage(width: Int, height: Int, color: Color): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      g.color = color
      g.fillRect(0, 0, width, height)
    } finally {
      g.dispose()
    }
    return image
  }

  private fun createMetadata(names: List<String>): JsonArray {
    val array = JsonArray()
    for (name in names) {
      val obj = JsonObject()
      obj.addProperty("name", name)
      array.add(obj)
    }
    return array
  }
}
