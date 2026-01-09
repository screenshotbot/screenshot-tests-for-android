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

import java.io.ByteArrayOutputStream
import org.gradle.api.Project

/**
 * Calculates a device name based on device characteristics for organizing screenshots by device
 * type.
 */
class DeviceNameCalculator(
    private val project: Project,
    private val deviceSerial: String? = null
) {

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
    val result = executeAdb(listOf("shell", "wm", "density"))
    val regex = Regex("[0-9]+")
    return regex.find(result)?.value
  }

  private fun screenSizeText(): String? {
    val result = executeAdb(listOf("shell", "wm", "size"))
    val regex = Regex("[0-9]+x[0-9]+")
    return regex.find(result)?.value
  }

  private fun hasPlayServices(): Boolean {
    return try {
      val output = executeAdb(listOf("shell", "pm", "path", "com.google.android.gms"))
      output.isNotEmpty()
    } catch (e: Exception) {
      false
    }
  }

  private fun playServicesText(): String {
    return if (hasPlayServices()) "GP" else "NO_GP"
  }

  private fun apiVersion(): String {
    return executeAdb(listOf("shell", "getprop", "ro.build.version.sdk")).trim()
  }

  private fun apiVersionText(): String {
    return "API_${apiVersion().toInt()}"
  }

  private fun architectureText(): String {
    return executeAdb(listOf("shell", "getprop", "ro.product.cpu.abi")).trim()
  }

  private fun locale(): String {
    val persistLocale = executeAdb(listOf("shell", "getprop", "persist.sys.locale")).trim()
    val productLocale = executeAdb(listOf("shell", "getprop", "ro.product.locale")).trim()
    return if (persistLocale.isNotEmpty()) persistLocale else productLocale
  }

  /**
   * Executes an ADB command and returns the output as a string.
   *
   * @param args The ADB command arguments (e.g., ["shell", "getprop", "ro.build.version.sdk"])
   * @return The command output as a trimmed string
   */
  private fun executeAdb(args: List<String>): String {
    val output = ByteArrayOutputStream()
    val error = ByteArrayOutputStream()

    project.exec { execSpec ->
      execSpec.executable = "adb"
      execSpec.args =
          mutableListOf<String>().apply {
            if (deviceSerial != null) {
              add("-s")
              add(deviceSerial)
            }
            addAll(args)
          }
      execSpec.standardOutput = output
      execSpec.errorOutput = error
      execSpec.isIgnoreExitValue = true
    }

    return output.toString().trim()
  }
}
