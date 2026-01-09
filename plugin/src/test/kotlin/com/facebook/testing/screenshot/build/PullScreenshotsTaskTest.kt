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

import org.junit.Assert.assertEquals
import org.junit.Test

class PullScreenshotsTaskTest {

  @Test
  fun testAndroidPathJoinWithNoArgs() {
    val result = PullScreenshotsTask.androidPathJoin("/base")
    assertEquals("/base", result)
  }

  @Test
  fun testAndroidPathJoinWithSingleArg() {
    val result = PullScreenshotsTask.androidPathJoin("/base", "path")
    assertEquals("/base/path", result)
  }

  @Test
  fun testAndroidPathJoinWithMultipleArgs() {
    val result = PullScreenshotsTask.androidPathJoin("/base", "path", "to", "file")
    assertEquals("/base/path/to/file", result)
  }

  @Test
  fun testAndroidPathJoinWithEmptyBase() {
    val result = PullScreenshotsTask.androidPathJoin("", "path", "to", "file")
    assertEquals("/path/to/file", result)
  }

  @Test
  fun testAndroidPathJoinPreservesTrailingSlash() {
    val result = PullScreenshotsTask.androidPathJoin("/base/", "path")
    assertEquals("/base//path", result)
  }

  @Test
  fun testAndroidPathJoinWithComplexPath() {
    val result = PullScreenshotsTask.androidPathJoin(
        "/data/data",
        "com.example.app",
        "app_screenshots-default",
        "metadata.json"
    )
    assertEquals("/data/data/com.example.app/app_screenshots-default/metadata.json", result)
  }
}
