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

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.TestVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class PullScreenshotsTask : ScreenshotTask() {
  companion object {
    fun taskName(variant: TestVariant) = "pull${variant.name.capitalize()}Screenshots"

    fun getReportDir(project: Project, variant: TestVariant): File =
        File(project.buildDir, "screenshots" + variant.name.capitalize())

  }

  @Input protected var verify = false

  @Input protected var record = false

  @Input protected var bundleResults = false

  @Input protected lateinit var testRunId: String

  init {
    description = "Pull screenshots from your device"
    group = ScreenshotsPlugin.GROUP
  }

  override fun init(variant: TestVariant, extension: ScreenshotsPluginExtension) {
    super.init(variant, extension)
    val output =
        variant.outputs.find { it is ApkVariantOutput } as? ApkVariantOutput
            ?: throw IllegalArgumentException("Can't find APK output")
    val packageTask =
        variant.packageApplicationProvider.orNull
            ?: throw IllegalArgumentException("Can't find package application provider")

    bundleResults = extension.bundleResults
    testRunId = extension.testRunId
  }

  @TaskAction
  fun pullScreenshots() {
    val codeSource = ScreenshotsPlugin::class.java.protectionDomain.codeSource
    val jarFile = File(codeSource.location.toURI().path)
    val isVerifyOnly = verify && extension.referenceDir != null

    val outputDir =
        if (isVerifyOnly) {
          File(extension.referenceDir)
        } else {
          getReportDir(project, variant)
        }

    assert(if (isVerifyOnly) outputDir.exists() else !outputDir.exists())


    project.exec { execSpec ->
      execSpec.executable = extension.pythonExecutable
      execSpec.environment("PYTHONPATH", jarFile)

      val tempDir = outputDir.absolutePath

      File(tempDir).mkdirs()

      // Pull metadata from device if we're performing a pull
      val deviceDir =
          if (!isVerifyOnly) {
              val puller = SimplePuller.create(project)
              pullMetadata(variant.applicationId, File(tempDir), puller)
          } else {
              "" // Empty string when not pulling
          }

      execSpec.args =
          mutableListOf(
                  "-m",
                  "android_screenshot_tests.pull_screenshots",
                  "--test-run-id",
                  testRunId,
                  "--temp-dir",
                  tempDir,
              )
              .apply {
                // Add device-dir parameter
                add("--device-dir")
                add(deviceDir)

                if (verify) {
                  add("--verify")
                } else if (record) {
                  add("--record")
                }

                if (verify || record) {
                  add(extension.recordDir)
                }

                if (verify && extension.failureDir != null) {
                  add("--failure-dir")
                  add("${extension.failureDir}")
                }

                if (extension.multipleDevices) {
                  add("--calculated-device-name")
                  val executor = GradleAdbExecutor(project)
                  val calculator = DeviceNameCalculator(executor)
                  add(calculator.name())
                }

                if (isVerifyOnly) {
                  add("--no-pull")
                }

                if (bundleResults) {
                  add("--bundle-results")
                }
              }

      println(execSpec.args)
    }
  }

}
