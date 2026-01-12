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
import java.io.FileOutputStream

/**
 * Copies static assets required for rendering the HTML report.
 *
 * @param destination The directory where assets should be copied
 */
fun copyAssets(destination: File) {
  _copyAsset("default.css", destination)
  _copyAsset("default.js", destination)
  _copyAsset("background.png", destination)
  _copyAsset("background_dark.png", destination)
}

/**
 * Copies a single asset file to the destination directory.
 *
 * @param filename The name of the asset file
 * @param destination The destination directory
 */
private fun _copyAsset(filename: String, destination: File) {
  val destFile = File(destination, filename)
  _copyFile(filename, destFile)
}

/**
 * Copies a file from the JAR resources to the destination.
 *
 * @param resourcePath The path to the resource in the JAR
 * @param dest The destination file
 */
private fun _copyFile(resourcePath: String, dest: File) {
  // Try to load from JAR resources
  val fullResourcePath = "android_screenshot_tests/$resourcePath"
  val inputStream = object {}.javaClass.classLoader?.getResourceAsStream(fullResourcePath)
    ?: throw RuntimeException("Resource not found: $fullResourcePath")

  inputStream.use { input ->
    FileOutputStream(dest).use { output ->
      input.copyTo(output)
    }
  }
}
