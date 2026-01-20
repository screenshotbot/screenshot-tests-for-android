/*
 * Copyright (c) Modern Interpreters Inc.
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

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import org.junit.Test

class ElevatedViewTest {
  @Test
  fun testElevatedComponents() {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val themedContext = ContextThemeWrapper(
      targetContext,
      com.google.android.material.R.style.Theme_MaterialComponents_Light
    )
    val inflater = LayoutInflater.from(themedContext)
    val view = inflater.inflate(R.layout.elevated_components, null, false)

    ViewHelpers.setupView(view).setExactWidthDp(320).layout()
    Screenshot.snap(view).record()
  }
}
