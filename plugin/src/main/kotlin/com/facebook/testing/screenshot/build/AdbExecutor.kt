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

import java.io.ByteArrayOutputStream
import org.gradle.api.Project
import org.gradle.process.ExecOperations

/**
 * Executor for ADB commands.
 *
 * Mirrors the Python AdbExecutor interface for consistency.
 */
interface AdbExecutor {
  /**
   * Executes an ADB command and returns the output.
   *
   * @param command The ADB command arguments (e.g., ["shell", "getprop", "ro.build.version.sdk"])
   * @return The command output as a string
   */
  fun execute(command: List<String>): String
}

/**
 * Configuration-cache compatible implementation of AdbExecutor using ExecOperations.
 *
 * @param execOperations The Gradle ExecOperations for executing commands
 * @param deviceSerial Optional device serial to target specific device
 */
class ExecOperationsAdbExecutor(
    private val execOperations: ExecOperations,
    private val deviceSerial: String? = null
) : AdbExecutor {

  override fun execute(command: List<String>): String {
    val output = ByteArrayOutputStream()
    val error = ByteArrayOutputStream()

    execOperations.exec { execSpec ->
      execSpec.executable = "adb"
      execSpec.args =
          mutableListOf<String>().apply {
            if (deviceSerial != null) {
              add("-s")
              add(deviceSerial)
            }
            addAll(command)
          }
      execSpec.standardOutput = output
      execSpec.errorOutput = error
      execSpec.isIgnoreExitValue = true
    }

    return output.toString().trim()
  }
}

/**
 * Default implementation of AdbExecutor that executes commands via Gradle's exec.
 *
 * @param project The Gradle project for executing commands
 * @param deviceSerial Optional device serial to target specific device
 * @deprecated Use ExecOperationsAdbExecutor for configuration cache compatibility
 */
@Deprecated("Use ExecOperationsAdbExecutor for configuration cache compatibility")
class GradleAdbExecutor(
    private val project: Project,
    private val deviceSerial: String? = null
) : AdbExecutor {

  override fun execute(command: List<String>): String {
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
            addAll(command)
          }
      execSpec.standardOutput = output
      execSpec.errorOutput = error
      execSpec.isIgnoreExitValue = true
    }

    return output.toString().trim()
  }
}
