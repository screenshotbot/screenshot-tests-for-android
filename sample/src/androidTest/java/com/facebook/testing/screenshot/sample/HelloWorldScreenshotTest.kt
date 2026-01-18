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

import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.runtime.R
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.WindowAttachment
import org.junit.Test

import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.savedstate.SavedStateRegistryController

class TestSavedStateRegistryOwner(val testLifecycleOwner: LifecycleOwner) :
  SavedStateRegistryOwner {
  private val controller = SavedStateRegistryController.create(this)

  override val lifecycle = testLifecycleOwner.lifecycle
  override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry

  init {
    controller.performRestore(null)
  }

}

class HelloWorldScreenshotTest {
  @Test
  @UiThreadTest
  fun testHelloWorld() {
    performScreenshot(300, 200) {
      MaterialTheme {
        HelloWorld()
      }
    }
  }

  private fun performScreenshot(widthDp: Int, heightDp: Int, composable: @Composable () -> Unit) {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val composeView = ComposeView(targetContext).apply {
      setContent(composable)
    }

    val lifecycleOwner = TestLifecycleOwner()
    lifecycleOwner.lifecycle.currentState = Lifecycle.State.INITIALIZED;
    composeView.setTag(R.id.view_tree_lifecycle_owner, lifecycleOwner)

    val savedStateRegistryOwner = TestSavedStateRegistryOwner(lifecycleOwner);

    composeView.setTag(
      androidx.savedstate.R.id.view_tree_saved_state_registry_owner,
      savedStateRegistryOwner
    );
    val detacher = WindowAttachment.dispatchAttach(composeView);

    ViewHelpers.setupView(composeView)
      .setExactWidthDp(widthDp)
      .setExactHeightDp(heightDp)
      .layout()

    lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED;

    Screenshot.snap(composeView).record()
    detacher.detach();
  }
}
