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

import com.android.build.gradle.api.TestVariant
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File

open class ScreenshotTask : DefaultTask() {
  @Input protected lateinit var extension: ScreenshotsPluginExtension

  @Input protected lateinit var variantName: String

  @Internal protected lateinit var applicationIdProvider: Provider<String>

  @Input protected fun getApplicationId(): String = applicationIdProvider.get()

  @Internal protected lateinit var buildDirectory: File

  @Internal protected lateinit var projectDirectory: File

  open fun init(variant: TestVariant, extension: ScreenshotsPluginExtension) {
    this.extension = extension
    this.variantName = variant.name
    this.applicationIdProvider = project.provider { variant.testedVariant.applicationId }
    this.buildDirectory = project.layout.buildDirectory.get().asFile
    this.projectDirectory = project.projectDir
  }
}
