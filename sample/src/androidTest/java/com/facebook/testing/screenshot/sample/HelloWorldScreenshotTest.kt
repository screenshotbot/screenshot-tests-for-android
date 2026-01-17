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
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.WindowAttachment
import org.junit.Test

class FakeLifecycle(override val currentState: State) : Lifecycle() {
  override fun addObserver(observer: LifecycleObserver) {
  }

  override fun removeObserver(observer: LifecycleObserver) {
  }
}
class FakeLifecycleOwner(override val lifecycle: Lifecycle) : LifecycleOwner {

}

class HelloWorldScreenshotTest {
    @Test
    @UiThreadTest
    fun testHelloWorld() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val composeView = ComposeView(targetContext).apply {
            setContent {
                MaterialTheme {
                    HelloWorld()
                }
            }
        }

      val lifecycleOwner = FakeLifecycleOwner(FakeLifecycle(Lifecycle.State.CREATED))
      composeView.setTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner, lifecycleOwner)
      val detacher = WindowAttachment.dispatchAttach(composeView);

        ViewHelpers.setupView(composeView)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        Screenshot.snap(composeView).record()
      detacher.detach();
    }
}
