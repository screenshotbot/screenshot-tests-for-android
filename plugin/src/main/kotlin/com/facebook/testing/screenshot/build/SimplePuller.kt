/*
 * Copyright (c) 2026 Modern Interpreters Inc.
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
import java.io.File
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.gradle.api.Project

/**
 * Pulls files from Android devices via ADB.
 *
 * Mirrors the Python SimplePuller interface for consistency.
 */
class SimplePuller(
    private val project: Project,
    private val adbArgs: List<String> = emptyList()
) {

  /**
   * Checks if a remote file exists on the device.
   *
   * @param src Absolute path on the device
   * @return true if the file exists, false otherwise
   */
  fun remoteFileExists(src: String): Boolean {
    val output = executeAdbShell("ls $src && echo EXISTS || echo DOES_NOT_EXIST")
    return "EXISTS" in output
  }

  /**
   * Pulls a file from the device to local filesystem.
   *
   * @param src Absolute path on the device
   * @param dest Local destination path
   */
  fun pull(src: String, dest: String) {
    executeAdb(listOf("pull", src, dest))
  }

  /**
   * Pulls a folder from the device to local filesystem.
   *
   * Uses tar compression to optimize transfer of folders with many files,
   * as each file transmission needs to reestablish the connection otherwise.
   *
   * @param src Absolute path to folder on the device
   * @param dest Local destination path
   */
  fun pullFolder(src: String, dest: String) {
    val tarName = getTarName(src)

    // Create tar archive on device
    tar(src, tarName)

    // Pull the tar file to a temporary location
    val tempFile = File.createTempFile("screenshot_pull", ".tar.gz")
    try {
      pull(tarName, tempFile.absolutePath)

      // Extract the tar file
      extractTar(tempFile, File(dest))
    } finally {
      tempFile.delete()
      // Clean up tar file on device
      removeTempTar(tarName)
    }
  }

  /**
   * Gets the external data directory path on the device.
   *
   * @return The path to external storage (e.g., "/sdcard")
   */
  fun getExternalDataDir(): String {
    val output = executeAdbShell("echo \$EXTERNAL_STORAGE")
    return output.trim().split(Regex("\\s+")).last()
  }

  private fun tar(src: String, tarName: String) {
    executeAdbShell("tar -zcvf $tarName -C $src .")
  }

  private fun removeTempTar(tarName: String) {
    executeAdbShell("rm $tarName")
  }

  private fun extractTar(tarFile: File, destDir: File) {
    if (!destDir.exists()) {
      destDir.mkdirs()
    }

    tarFile.inputStream().use { fileInput ->
      GzipCompressorInputStream(fileInput).use { gzipInput ->
        TarArchiveInputStream(gzipInput).use { tarInput ->
          var entry = tarInput.nextTarEntry
          while (entry != null) {
            val outputFile = File(destDir, entry.name)

            if (entry.isDirectory) {
              outputFile.mkdirs()
            } else {
              outputFile.parentFile?.mkdirs()
              outputFile.outputStream().use { output ->
                tarInput.copyTo(output)
              }
            }

            entry = tarInput.nextTarEntry
          }
        }
      }
    }
  }

  private fun executeAdbShell(command: String): String {
    return executeAdb(listOf("shell", command))
  }

  private fun executeAdb(command: List<String>): String {
    val output = ByteArrayOutputStream()
    val error = ByteArrayOutputStream()

    project.exec { execSpec ->
      execSpec.executable = "adb"
      execSpec.args = mutableListOf<String>().apply {
        addAll(adbArgs)
        addAll(command)
      }
      execSpec.standardOutput = output
      execSpec.errorOutput = error
      execSpec.isIgnoreExitValue = false
    }

    return output.toString().trim()
  }

  companion object {
    private fun getTarName(src: String): String {
      return "$src.tar.gz"
    }
  }
}
