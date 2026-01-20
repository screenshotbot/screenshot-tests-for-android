package com.facebook.testing.screenshot.compose

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.runtime.R
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.facebook.testing.screenshot.WindowAttachment

fun screenshot(composable: @Composable () -> Unit) {
  val displayMetrics = DisplayMetrics()
  val windowManager = InstrumentationRegistry.getInstrumentation()
    .targetContext
    .getSystemService(Context.WINDOW_SERVICE) as WindowManager
  val width = windowManager.currentWindowMetrics.bounds.width()
  val height = windowManager.currentWindowMetrics.bounds.height()

  if (width <= 0) {
    throw RuntimeException("Could not figure out screen width")
  }
  screenshotImpl({ vh: ViewHelpers ->
    vh.setExactWidthPx(width)
      .setExactHeightPx(height)
      .layout()
  }, composable);
}

fun screenshot(widthDp: Int, heightDp: Int, composable: @Composable () -> Unit) {
  screenshotImpl({vh: ViewHelpers ->
    vh.setExactWidthDp(widthDp)
      .setExactHeightDp(heightDp)
      .layout()
  }, composable)
}

private fun screenshotImpl(setup: (vh: ViewHelpers) -> Unit, composable: @Composable () -> Unit) {
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

  setup.invoke(ViewHelpers.setupView(composeView))



  lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED;

  Screenshot.snap(composeView).record()
  detacher.detach();
}

class TestSavedStateRegistryOwner(val testLifecycleOwner: LifecycleOwner) :
  SavedStateRegistryOwner {
  private val controller = SavedStateRegistryController.Companion.create(this)

  override val lifecycle = testLifecycleOwner.lifecycle
  override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry

  init {
    controller.performRestore(null)
  }

}