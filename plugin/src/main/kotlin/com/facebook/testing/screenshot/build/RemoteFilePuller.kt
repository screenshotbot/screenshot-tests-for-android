package com.facebook.testing.screenshot.build

interface RemoteFilePuller {
  /**
   * Checks if a remote file exists on the device.
   *
   * @param src Absolute path on the device
   * @return true if the file exists, false otherwise
   */
  fun remoteFileExists(src: String): Boolean

  /**
   * Pulls a file from the device to local filesystem.
   *
   * @param src Absolute path on the device
   * @param dest Local destination path
   */
  fun pull(src: String, dest: String)

  /**
   * Pulls a folder from the device to local filesystem.
   *
   * Uses tar compression to optimize transfer of folders with many files,
   * as each file transmission needs to reestablish the connection otherwise.
   *
   * @param src Absolute path to folder on the device
   * @param dest Local destination path
   */
  fun pullFolder(src: String, dest: String)

  fun getExternalDataDir(): String
}