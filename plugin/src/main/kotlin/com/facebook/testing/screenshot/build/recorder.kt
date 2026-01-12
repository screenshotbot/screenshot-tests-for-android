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
import com.google.gson.JsonParser
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class VerifyError(message: String) : Exception(message)

/**
 * Compares two images and determines if they are the same.
 *
 * @param file1 The first image file path
 * @param file2 The second image file path
 * @param failureFile The file path to save the diff image with highlighted differences (optional)
 * @return true if images are the same, false otherwise
 */
private fun isImageSame(file1: File, file2: File, failureFile: File?): Boolean {
  val im1 = ImageIO.read(file1)
  val im2 = ImageIO.read(file2)

  try {
    if (im1.width != im2.width || im1.height != im2.height) {
      if (failureFile != null) {
        saveDiffImage(im2, null, failureFile)
      }
      return false
    }

    val diff = calculateDifference(im1, im2)

    if (diff == null) {
      return true
    } else {
      if (failureFile != null) {
        saveDiffImage(im2, diff, failureFile)
      }
      return false
    }
  } finally {
    im1.flush()
    im2.flush()
  }
}

/**
 * Calculates the bounding box of differences between two images.
 *
 * @param im1 The first image
 * @param im2 The second image
 * @return A Rectangle representing the bounding box of differences, or null if images are identical
 */
private fun calculateDifference(im1: BufferedImage, im2: BufferedImage): DiffBounds? {
  var minX = Int.MAX_VALUE
  var minY = Int.MAX_VALUE
  var maxX = Int.MIN_VALUE
  var maxY = Int.MIN_VALUE
  var hasDiff = false

  for (y in 0 until im1.height) {
    for (x in 0 until im1.width) {
      val rgb1 = im1.getRGB(x, y)
      val rgb2 = im2.getRGB(x, y)

      if (rgb1 != rgb2) {
        hasDiff = true
        minX = minOf(minX, x)
        minY = minOf(minY, y)
        maxX = maxOf(maxX, x)
        maxY = maxOf(maxY, y)
      }
    }
  }

  return if (hasDiff) {
    DiffBounds(minX, minY, maxX, maxY)
  } else {
    null
  }
}

private data class DiffBounds(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

/**
 * Saves the diff image with a red rectangle highlighting the differences.
 *
 * @param image The base image
 * @param diff The bounding box of differences, or null if images have different sizes
 * @param failureFile The file to save the diff image to
 */
private fun saveDiffImage(image: BufferedImage, diff: DiffBounds?, failureFile: File) {
  val copy = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
  val g = copy.createGraphics()

  try {
    g.drawImage(image, 0, 0, null)

    if (diff != null) {
      g.color = Color.RED
      g.drawRect(diff.minX, diff.minY, diff.maxX - diff.minX, diff.maxY - diff.minY)
    }
  } finally {
    g.dispose()
  }

  failureFile.parentFile.mkdirs()
  ImageIO.write(copy, "PNG", failureFile)
}

/**
 * Verifies screenshots against expected output.
 *
 * @param screenshots The parsed JSON array from metadata.json
 * @param output The directory with the actual screenshots
 * @param expectedOutput The directory with the expected screenshots
 * @param failureOutput The directory where we copy the failure screenshots to (optional)
 * @throws VerifyError if any screenshots don't match
 */
fun verifyHelper(
    screenshots: JsonArray,
    output: File,
    expectedOutput: File,
    failureOutput: File?
) {
  val failures = mutableListOf<Pair<File, File>>()

  for (element in screenshots) {
    val screenshot = element.asJsonObject
    val name = screenshot.get("name").asString + ".png"
    val actual = File(output, name)
    val expected = File(expectedOutput, name)

    if (failureOutput != null) {
      val diffName = screenshot.get("name").asString + "_diff.png"
      val diff = File(failureOutput, diffName)

      if (!isImageSame(expected, actual, diff)) {
        val expectedName = screenshot.get("name").asString + "_expected.png"
        val actualName = screenshot.get("name").asString + "_actual.png"

        actual.copyTo(File(failureOutput, actualName), overwrite = true)
        expected.copyTo(File(failureOutput, expectedName), overwrite = true)

        failures.add(Pair(expected, actual))
      }
    } else {
      if (!isImageSame(expected, actual, null)) {
        throw VerifyError("Image $expected is not same as $actual")
      }
    }
  }

  if (failures.isNotEmpty()) {
    var reason = ""
    for ((expected, actual) in failures) {
      reason += "\nImage $expected is not same as $actual"
    }
    throw VerifyError(reason)
  }

  output.deleteRecursively()
}

/**
 * Reads and parses the metadata.json file from the input directory.
 *
 * @param input The directory containing metadata.json
 * @return The parsed JSON array from metadata.json
 */
fun getMetadataJson(input: File): JsonArray {
  val metadataFile = File(input, "metadata.json")
  return metadataFile.bufferedReader().use { reader ->
    JsonParser.parseReader(reader).asJsonArray
  }
}

/**
 * Gets the dimensions of an image file.
 *
 * @param fileName The path to the image file
 * @return A Pair containing the width and height of the image
 */
private fun getImageSize(fileName: File): Pair<Int, Int> {
  val image = ImageIO.read(fileName)
  try {
    return Pair(image.width, image.height)
  } finally {
    image.flush()
  }
}

/**
 * Copies and stitches tiled screenshots into a single image.
 *
 * @param name The base name of the screenshot
 * @param w The number of tiles in the x-direction (width)
 * @param h The number of tiles in the y-direction (height)
 * @param input The directory containing the tiled images
 * @param output The directory to save the stitched image
 */
private fun _copy(name: String, w: Int, h: Int, input: File, output: File) {
  val (tileWidth, tileHeight) = getImageSize(
    File(input, getImageFileName(name, 0, 0))
  )

  var canvasWidth = 0
  for (i in 0 until w) {
    val inputFile = getImageFileName(name, i, 0)
    val (width, _) = getImageSize(File(input, inputFile))
    canvasWidth += width
  }

  var canvasHeight = 0
  for (j in 0 until h) {
    val inputFile = getImageFileName(name, 0, j)
    val (_, height) = getImageSize(File(input, inputFile))
    canvasHeight += height
  }

  val canvas = BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB)
  val g = canvas.createGraphics()

  try {
    for (i in 0 until w) {
      for (j in 0 until h) {
        val inputFile = getImageFileName(name, i, j)
        val inputImage = ImageIO.read(File(input, inputFile))
        try {
          g.drawImage(inputImage, i * tileWidth, j * tileHeight, null)
        } finally {
          inputImage.flush()
        }
      }
    }
  } finally {
    g.dispose()
  }

  output.mkdirs()
  ImageIO.write(canvas, "PNG", File(output, "$name.png"))
}

/**
 * Records screenshots by stitching together tiled images.
 *
 * @param metadata The parsed JSON array from metadata.json
 * @param input The directory containing the tiled images
 * @param output The directory to save the stitched images
 */
fun _record(metadata: JsonArray, input: File, output: File) {
  for (element in metadata) {
    val screenshot = element.asJsonObject
    _copy(
      screenshot.get("name").asString,
      screenshot.get("tileWidth").asInt,
      screenshot.get("tileHeight").asInt,
      input,
      output
    )
  }
}
