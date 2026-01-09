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
import org.junit.After
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
}
