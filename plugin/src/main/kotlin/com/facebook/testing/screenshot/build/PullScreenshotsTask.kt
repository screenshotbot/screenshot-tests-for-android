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
import com.google.gson.JsonParser
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

    val reportOutputDir = File(outputDir, "report").absolutePath
    val verifyTempDir = File(outputDir, "verifyTempDir")
    File(reportOutputDir).mkdirs()
    verifyTempDir.mkdirs()

    val puller = AdditionalTestOutputPuller(project) as RemoteFilePuller
    // Pull metadata from device if we're performing a pull
    val deviceDir =
        if (!isVerifyOnly) {
            pullMetadata(variant.applicationId, File(reportOutputDir), puller)
        } else {
            "" // Empty string when not pulling
        }

    if (!isVerifyOnly) {
        pullImages(File(reportOutputDir), deviceDir, testRunId, puller)
    }

    copyHtmlAssets(File(reportOutputDir))

    generateHtml(File(reportOutputDir))

    val screenshots = getMetadataJson(File(reportOutputDir))
    val recordDir = File(project.projectDir, extension.recordDir)
    val expectedOutputDir = directoryWithDeviceName(recordDir)

    val outputDirForRecording = (if (record) expectedOutputDir else verifyTempDir)
    
    _doClean(outputDirForRecording);
    _record(screenshots, File(reportOutputDir), outputDirForRecording)

    
    val failureBaseDir = File(project.projectDir, extension.failureDir)
    val failureOutputDir = if (extension.failureDir != null) {
      directoryWithDeviceName(failureBaseDir)
      } else {
        null
      }

    if (verify) {
      verifyHelper(screenshots, verifyTempDir, expectedOutputDir, failureOutputDir)
    }

    printLinkToHtml(reportOutputDir);
  }

  private fun directoryWithDeviceName(recordDir: File): File = if (extension.multipleDevices) {
    val executor = GradleAdbExecutor(project)
    val calculator = DeviceNameCalculator(executor)
    File(recordDir, calculator.name())
  } else {
    recordDir
  }

  private fun printLinkToHtml(tempDir: String) {
    val metadataFile = File(tempDir, "metadata.json")
    val metadataJson = metadataFile.bufferedReader().use { reader ->
      JsonParser.parseReader(reader).asJsonArray
    }
    val count = metadataJson.size()

    logger.lifecycle("\n")
    
    logger.lifecycle("Found $count screenshots")
    val indexHtml = File(tempDir, "index.html").absolutePath
    val fullPath = "file://$indexHtml"
    logger.lifecycle("\n")
    logger.lifecycle("Open the following url in a browser to view the results: ")
    logger.lifecycle("  $fullPath")
    logger.lifecycle("\n")
  }

}
