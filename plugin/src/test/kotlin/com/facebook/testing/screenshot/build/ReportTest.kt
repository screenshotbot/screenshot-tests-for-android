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
}
