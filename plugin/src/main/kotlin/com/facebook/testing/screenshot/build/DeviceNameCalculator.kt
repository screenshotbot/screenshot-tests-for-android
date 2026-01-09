/*
 * Copyright (c) 2026 Modern Interpreters Inc
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

/**
 * Calculates a device name based on device characteristics for organizing screenshots by device
 * type.
 */
class DeviceNameCalculator(private val executor: AdbExecutor) {

  /**
   * Returns a formatted device name string containing:
   * API_version_playServices_density_size_architecture_locale
   *
   * Example: API_30_GP_XXHDPI_1080x2340_arm64-v8a_en_US
   */
  fun name(): String {
    val apiVersionText = apiVersionText()
    val playServicesText = playServicesText()
    val screenDensityText = screenDensityText()
    val screenSizeText = screenSizeText()
    val architectureText = architectureText()
    val locale = locale()

    val deviceParameters =
        listOf(
            apiVersionText,
            playServicesText,
            screenDensityText,
            screenSizeText,
            architectureText,
            locale)

    if (deviceParameters.any { it == null }) {
      throw RuntimeException(
          "ERROR: you shouldn't see this in normal operation, " +
              "file a bug report please.\n\n " +
              "One or more device params are None")
    }

    return "${apiVersionText}_${playServicesText}_${screenDensityText}_${screenSizeText}_${architectureText}_${locale}"
  }

  private fun screenDensityText(): String {
    val density = screenDensity()?.toIntOrNull() ?: return "XXXHDPI"

    return when (density) {
      in 0..120 -> "LDPI"
      in 121..160 -> "MDPI"
      in 161..240 -> "HDPI"
      in 241..320 -> "XHDPI"
      in 321..480 -> "XXHDPI"
      else -> "XXXHDPI"
    }
  }

  private fun screenDensity(): String? {
    val result = executor.execute(listOf("shell", "wm", "density"))
    val regex = Regex("[0-9]+")
    return regex.find(result)?.value
  }

  private fun screenSizeText(): String? {
    val result = executor.execute(listOf("shell", "wm", "size"))
    val regex = Regex("[0-9]+x[0-9]+")
    return regex.find(result)?.value
  }

  private fun hasPlayServices(): Boolean {
    return try {
      val output = executor.execute(listOf("shell", "pm", "path", "com.google.android.gms"))
      output.isNotEmpty()
    } catch (e: Exception) {
      false
    }
  }

  private fun playServicesText(): String {
    return if (hasPlayServices()) "GP" else "NO_GP"
  }

  private fun apiVersion(): String {
    return executor.execute(listOf("shell", "getprop", "ro.build.version.sdk")).trim()
  }

  private fun apiVersionText(): String {
    return "API_${apiVersion().toInt()}"
  }

  private fun architectureText(): String {
    return executor.execute(listOf("shell", "getprop", "ro.product.cpu.abi")).trim()
  }

  private fun locale(): String {
    val persistLocale = executor.execute(listOf("shell", "getprop", "persist.sys.locale")).trim()
    val productLocale = executor.execute(listOf("shell", "getprop", "ro.product.locale")).trim()
    return if (persistLocale.isNotEmpty()) persistLocale else productLocale
  }
}
