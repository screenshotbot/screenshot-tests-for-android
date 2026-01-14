/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class DeviceNameCalculatorTest {

  @Test
  fun testAPI_19_GP_XXHDPI_1080x1920_arm64_v8a_esES() {
    val executor = createMockExecutor { command ->
      when {
        command.contains("ro.build.version.sdk") -> "19"
        command.contains("com.google.android.gms") ->
            "package:/data/app/com.google.android.gms-pHwJaHhvXiRvuTo2Qxdbww==/base.apk"
        command.contains("density") -> "Physical density: 420"
        command.contains("size") -> "Physical size: 1080x1920"
        command.contains("ro.product.cpu.abi") -> "arm64-v8a"
        command.contains("persist.sys.locale") -> "es-ES"
        else -> ""
      }
    }

    val calculator = DeviceNameCalculator(executor)
    val result = calculator.name()

    assertEquals("API_19_GP_XXHDPI_1080x1920_arm64-v8a_es-ES", result)
  }

  @Test
  fun testAPI_23_NO_GP_XXHDPI_1080x1920_arm64_v8a_esES() {
    val executor = createMockExecutor { command ->
      when {
        command.contains("ro.build.version.sdk") -> "23"
        command.contains("com.google.android.gms") -> ""
        command.contains("density") -> "Physical density: 420"
        command.contains("size") -> "Physical size: 1080x1920"
        command.contains("ro.product.cpu.abi") -> "arm64-v8a"
        command.contains("persist.sys.locale") -> "es-ES"
        else -> ""
      }
    }

    val calculator = DeviceNameCalculator(executor)
    val result = calculator.name()

    assertEquals("API_23_NO_GP_XXHDPI_1080x1920_arm64-v8a_es-ES", result)
  }

  @Test
  fun testAPI_25_NO_GP_XXHDPI_1080x1920_x86_esES() {
    val executor = createMockExecutor { command ->
      when {
        command.contains("ro.build.version.sdk") -> "25"
        command.contains("com.google.android.gms") -> ""
        command.contains("density") -> "Physical density: 420"
        command.contains("size") -> "Physical size: 1080x1920"
        command.contains("ro.product.cpu.abi") -> "x86"
        command.contains("persist.sys.locale") -> ""
        command.contains("ro.product.locale") -> "es-ES"
        else -> ""
      }
    }

    val calculator = DeviceNameCalculator(executor)
    val result = calculator.name()

    assertEquals("API_25_NO_GP_XXHDPI_1080x1920_x86_es-ES", result)
  }

  @Test
  fun testDensity_10_to_LDPI() {
    val executor = createMockExecutor { "Physical density: 10" }
    val calculator = DeviceNameCalculator(executor)
    val result = calculator.screenDensityTextForTesting()
    assertEquals("LDPI", result)
  }

  @Test
  fun testDensity_140_to_MDPI() {
    val executor = createMockExecutor { "Physical density: 140" }
    val calculator = DeviceNameCalculator(executor)
    val result = calculator.screenDensityTextForTesting()
    assertEquals("MDPI", result)
  }

  @Test
  fun testDensity_200_to_HDPI() {
    val executor = createMockExecutor { "Physical density: 200" }
    val calculator = DeviceNameCalculator(executor)
    val result = calculator.screenDensityTextForTesting()
    assertEquals("HDPI", result)
  }

  @Test
  fun testDensity_250_to_XHDPI() {
    val executor = createMockExecutor { "Physical density: 250" }
    val calculator = DeviceNameCalculator(executor)
    val result = calculator.screenDensityTextForTesting()
    assertEquals("XHDPI", result)
  }

  @Test
  fun testDensity_340_to_XXHDPI() {
    val executor = createMockExecutor { "Physical density: 340" }
    val calculator = DeviceNameCalculator(executor)
    val result = calculator.screenDensityTextForTesting()
    assertEquals("XXHDPI", result)
  }

  @Test
  fun testDensity_500_to_XXXHDPI() {
    val executor = createMockExecutor { "Physical density: 500" }
    val calculator = DeviceNameCalculator(executor)
    val result = calculator.screenDensityTextForTesting()
    assertEquals("XXXHDPI", result)
  }

  @Test
  fun testAbsentGmsGracefullyHandled() {
    val executor = createMockExecutor { "" }
    val calculator = DeviceNameCalculator(executor)
    val result = calculator.hasPlayServicesForTesting()
    assertFalse(result)
  }

  /**
   * Creates a mock AdbExecutor that returns responses based on the command.
   *
   * @param responseProvider Function that takes the command list and returns the simulated ADB
   * output
   */
  private fun createMockExecutor(responseProvider: (List<String>) -> String): AdbExecutor {
    val executor = mock(AdbExecutor::class.java)
    `when`(executor.execute(org.mockito.ArgumentMatchers.anyList()))
        .thenAnswer { invocation ->
          @Suppress("UNCHECKED_CAST")
          val command = invocation.arguments[0] as List<String>
          responseProvider(command)
        }
    return executor
  }
}

/** Extension functions to expose private methods for testing */
internal fun DeviceNameCalculator.screenDensityTextForTesting(): String {
  val method = this::class.java.getDeclaredMethod("screenDensityText")
  method.isAccessible = true
  return method.invoke(this) as String
}

internal fun DeviceNameCalculator.hasPlayServicesForTesting(): Boolean {
  val method = this::class.java.getDeclaredMethod("hasPlayServices")
  method.isAccessible = true
  return method.invoke(this) as Boolean
}
