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

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

class ScreenshotsPluginTest {
  @Rule
  public final TemporaryFolder testProjectDir = new TemporaryFolder()

  private File buildFile
  private File settingsFile
  private File manifestFile
  private List<File> pluginClasspath

  @Before
  void "setup"() {
    final appId = "com.facebook.testing.screenshot.integration"

    // Get the plugin classpath from the metadata file
    def pluginClasspathResource = getClass().classLoader.findResource("plugin-under-test-metadata.properties")
    if (pluginClasspathResource == null) {
      throw new IllegalStateException("Did not find plugin classpath resource")
    }

    def pluginClasspathProperties = new Properties()
    pluginClasspathResource.openStream().withCloseable { inputStream ->
      pluginClasspathProperties.load(inputStream)
    }

    pluginClasspath = pluginClasspathProperties.get("implementation-classpath")
        .split(File.pathSeparator)
        .collect { new File(it) }

    settingsFile = testProjectDir.newFile('settings.gradle')
    buildFile = testProjectDir.newFile('build.gradle')

    File manifestDir = testProjectDir.newFolder('src', 'main')
    manifestFile = new File(manifestDir, 'AndroidManifest.xml')
    manifestFile.write("""<?xml version="1.0" encoding="utf-8"?>
      <manifest package="$appId">
        <application/>
      </manifest>""")
  }

  private void writeBuildFile(String screenshotsConfig = '') {
    buildFile.text = """
      buildscript {
        repositories {
          mavenCentral()
          google()
        }
        dependencies {
          classpath 'com.android.tools.build:gradle:7.4.2'
          classpath files(${pluginClasspath.collect { "'$it'" }.join(', ')})
        }
      }

      apply plugin: 'com.android.application'
      apply plugin: com.facebook.testing.screenshot.build.ScreenshotsPlugin

      repositories {
        mavenCentral()
        google()
      }

      android {
        compileSdkVersion 22
        namespace "${'com.facebook.testing.screenshot.integration'}"

        defaultConfig {
          applicationId "${'com.facebook.testing.screenshot.integration'}"
        }
      }

      ${screenshotsConfig}
    """
  }

  @Test
  void "Ensure core dependency added"() {
    writeBuildFile()

    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('dependencies', '--configuration', 'androidTestImplementation')
        .withGradleVersion('7.5')
        .build()

    assertTrue(result.output.contains('com.facebook.testing.screenshot:core'))
  }

  @Test
  void "Ensure core dependency not added when requested"() {
    writeBuildFile('''
      screenshots {
        addDeps = false
      }
    ''')

    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('dependencies', '--configuration', 'androidTestImplementation')
        .withGradleVersion('7.5')
        .build()

    assertFalse(result.output.contains('com.facebook.testing.screenshot:core'))
  }

  @Test
  void "Ensure tasks added"() {
    writeBuildFile()

    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('tasks', '--all')
        .withGradleVersion('7.5')
        .build()

    assertTrue(result.output.contains('pullDebugAndroidTestScreenshots'))
    assertTrue(result.output.contains('runDebugAndroidTestScreenshotTest'))
    assertTrue(result.output.contains('recordDebugAndroidTestScreenshotTest'))
    assertTrue(result.output.contains('verifyDebugAndroidTestScreenshotTest'))
  }
}
