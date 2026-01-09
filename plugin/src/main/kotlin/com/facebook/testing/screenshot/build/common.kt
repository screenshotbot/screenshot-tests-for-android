package com.facebook.testing.screenshot.build

/**
 * Joins Android device paths using forward slashes.
 *
 * @param a The base path
 * @param args Additional path components
 * @return The joined path
 */
fun androidPathJoin(a: String, vararg args: String): String {
  if (args.isEmpty()) return a
  return args.fold(a) { acc, path -> "$acc/$path" }
}