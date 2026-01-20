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

package com.facebook.testing.screenshot.sample

import androidx.compose.material3.MaterialTheme
import androidx.test.annotation.UiThreadTest
import com.facebook.testing.screenshot.compose.screenshot
import org.junit.Test

class LoginScreenScreenshotTests {
  @Test
  @UiThreadTest
  fun testLoginScreen() {
    screenshot {
      MaterialTheme {
        LoginScreen()
      }
    }
  }

  @Test
  @UiThreadTest
  fun testLoginScreenError() {
    screenshot {
      MaterialTheme {
        LoginScreen(isError = true)
      }
    }
  }
}
