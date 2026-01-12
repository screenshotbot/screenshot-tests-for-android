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
  private lateinit var inputDir: File

  @Before
  fun setUp() {
    outputDir = createTempDir(prefix = "output")
    expectedOutputDir = createTempDir(prefix = "expected")
    failureOutputDir = createTempDir(prefix = "failure")
    inputDir = createTempDir(prefix = "input")
  }

  @After
  fun tearDown() {
    if (outputDir.exists()) {
      outputDir.deleteRecursively()
    }
    expectedOutputDir.deleteRecursively()
    failureOutputDir.deleteRecursively()
    inputDir.deleteRecursively()
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

  @Test
  fun testGetMetadataJson_validFile_returnsJsonArray() {
    val metadataFile = File(outputDir, "metadata.json")
    metadataFile.writeText("""[{"name": "test1"}, {"name": "test2"}]""")

    val result = getMetadataJson(outputDir)

    assertEquals(2, result.size())
    assertEquals("test1", result.get(0).asJsonObject.get("name").asString)
    assertEquals("test2", result.get(1).asJsonObject.get("name").asString)
  }

  @Test
  fun testGetMetadataJson_emptyArray_returnsEmptyJsonArray() {
    val metadataFile = File(outputDir, "metadata.json")
    metadataFile.writeText("[]")

    val result = getMetadataJson(outputDir)

    assertEquals(0, result.size())
  }

  @Test
  fun testGetMetadataJson_complexMetadata_parsesCorrectly() {
    val metadataFile = File(outputDir, "metadata.json")
    metadataFile.writeText("""
      [
        {
          "name": "screenshot1",
          "tileWidth": 1,
          "tileHeight": 1,
          "group": "group1"
        },
        {
          "name": "screenshot2",
          "tileWidth": 2,
          "tileHeight": 2
        }
      ]
    """.trimIndent())

    val result = getMetadataJson(outputDir)

    assertEquals(2, result.size())

    val first = result.get(0).asJsonObject
    assertEquals("screenshot1", first.get("name").asString)
    assertEquals(1, first.get("tileWidth").asInt)
    assertEquals("group1", first.get("group").asString)

    val second = result.get(1).asJsonObject
    assertEquals("screenshot2", second.get("name").asString)
    assertEquals(2, second.get("tileHeight").asInt)
  }

  @Test
  fun testRecord_singleInput_copiesImage() {
    createTempImage("foobar.png", 10, 10, Color.BLUE)
    val metadata = createRecordMetadata(listOf(Triple("foobar", 1, 1)))

    _record(metadata, inputDir, outputDir)

    assertTrue("Output file should exist", File(outputDir, "foobar.png").exists())
    val outputImage = ImageIO.read(File(outputDir, "foobar.png"))
    assertEquals(10, outputImage.width)
    assertEquals(10, outputImage.height)
  }

  @Test
  fun testRecord_twoFiles_copiesBothImages() {
    createTempImage("foo.png", 10, 10, Color.BLUE)
    createTempImage("bar.png", 10, 10, Color.RED)
    val metadata = createRecordMetadata(listOf(Triple("foo", 1, 1), Triple("bar", 1, 1)))

    _record(metadata, inputDir, outputDir)

    assertTrue("foo.png should exist", File(outputDir, "foo.png").exists())
    assertTrue("bar.png should exist", File(outputDir, "bar.png").exists())
  }

  @Test
  fun testRecord_oneColumnTiles_stitchesVertically() {
    createTempImage("foobar.png", 10, 10, Color.BLUE)
    createTempImage("foobar_0_1.png", 10, 10, Color.RED)
    val metadata = createRecordMetadata(listOf(Triple("foobar", 1, 2)))

    _record(metadata, inputDir, outputDir)

    val outputImage = ImageIO.read(File(outputDir, "foobar.png"))
    assertEquals(10, outputImage.width)
    assertEquals(20, outputImage.height)

    assertEquals(Color.BLUE.rgb, outputImage.getRGB(1, 1))
    assertEquals(Color.RED.rgb, outputImage.getRGB(1, 11))
  }

  @Test
  fun testRecord_oneRowTiles_stitchesHorizontally() {
    createTempImage("foobar.png", 10, 10, Color.BLUE)
    createTempImage("foobar_1_0.png", 10, 10, Color.RED)
    val metadata = createRecordMetadata(listOf(Triple("foobar", 2, 1)))

    _record(metadata, inputDir, outputDir)

    val outputImage = ImageIO.read(File(outputDir, "foobar.png"))
    assertEquals(20, outputImage.width)
    assertEquals(10, outputImage.height)

    assertEquals(Color.BLUE.rgb, outputImage.getRGB(1, 1))
    assertEquals(Color.RED.rgb, outputImage.getRGB(11, 1))
  }

  @Test
  fun testRecord_fractionalTiles_stitchesCorrectly() {
    createTempImage("foobar.png", 10, 10, Color.BLUE)
    createTempImage("foobar_1_0.png", 9, 10, Color.RED)
    createTempImage("foobar_0_1.png", 10, 8, Color.RED)
    createTempImage("foobar_1_1.png", 9, 8, Color.BLUE)
    val metadata = createRecordMetadata(listOf(Triple("foobar", 2, 2)))

    _record(metadata, inputDir, outputDir)

    val outputImage = ImageIO.read(File(outputDir, "foobar.png"))
    assertEquals(19, outputImage.width)
    assertEquals(18, outputImage.height)

    assertEquals(Color.BLUE.rgb, outputImage.getRGB(1, 1))
    assertEquals(Color.RED.rgb, outputImage.getRGB(11, 1))
    assertEquals(Color.BLUE.rgb, outputImage.getRGB(11, 11))
    assertEquals(Color.RED.rgb, outputImage.getRGB(1, 11))
  }

  @Test
  fun testRecord_createsOutputDirectory() {
    val newOutputDir = File(outputDir, "subdir")
    assertFalse("Output dir should not exist initially", newOutputDir.exists())

    createTempImage("test.png", 10, 10, Color.BLUE)
    val metadata = createRecordMetadata(listOf(Triple("test", 1, 1)))

    _record(metadata, inputDir, newOutputDir)

    assertTrue("Output directory should be created", newOutputDir.exists())
    assertTrue("Output file should exist", File(newOutputDir, "test.png").exists())
  }

  private fun createTempImage(name: String, width: Int, height: Int, color: Color) {
    val image = createTestImage(width, height, color)
    val file = File(inputDir, name)
    ImageIO.write(image, "PNG", file)
  }

  private fun createRecordMetadata(screenshots: List<Triple<String, Int, Int>>): JsonArray {
    val array = JsonArray()
    for ((name, tileWidth, tileHeight) in screenshots) {
      val obj = JsonObject()
      obj.addProperty("name", name)
      obj.addProperty("tileWidth", tileWidth)
      obj.addProperty("tileHeight", tileHeight)
      array.add(obj)
    }
    return array
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
