package com.facebook.testing.screenshot.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.annotation.UiThreadTest
import org.junit.Test

class HelloWorldScreenshotTest {
  @Test
  @UiThreadTest
  fun testHelloWorld() {
    screenshot(300, 200) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.White),
        contentAlignment = Alignment.Center
      ) {
        BasicText("Hello, World!")
      }
    }
  }

  @Test
  @UiThreadTest
  fun testHelloWorldWithDefaultDimensions() {
    screenshot {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.White),
        contentAlignment = Alignment.Center
      ) {
        BasicText("Hello, World!")
      }
    }
  }
}
