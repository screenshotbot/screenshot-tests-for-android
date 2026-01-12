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

/**
 * Generates the filename for a tiled screenshot image.
 *
 * @param name The base name of the screenshot
 * @param x The x-coordinate of the tile
 * @param y The y-coordinate of the tile
 * @return The filename for the tile (e.g., "name.png" or "name_1_2.png")
 */
fun getImageFileName(name: String, x: Int, y: Int): String {
  var imageFile = name
  if (x != 0 || y != 0) {
    imageFile += "_${x}_$y"
  }
  imageFile += ".png"
  return imageFile
}