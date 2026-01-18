package com.facebook.testing.screenshot.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.runtime.R
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.WindowAttachment

fun performScreenshot(widthDp: Int, heightDp: Int, composable: @Composable () -> Unit) {
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