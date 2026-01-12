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

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedList

const val KEY_VIEW_HIERARCHY = "viewHierarchy"
const val KEY_AX_HIERARCHY = "axHierarchy"
const val KEY_CLASS = "class"
const val KEY_LEFT = "left"
const val KEY_TOP = "top"
const val KEY_WIDTH = "width"
const val KEY_HEIGHT = "height"
const val KEY_CHILDREN = "children"
const val DEFAULT_VIEW_CLASS = "android.view.View"

/**
 * Copies static assets required for rendering the HTML report.
 *
 * @param destination The directory where assets should be copied
 */
fun copyAssets(destination: File) {
  _copyAsset("default.css", destination)
  _copyAsset("default.js", destination)
  _copyAsset("background.png", destination)
  _copyAsset("background_dark.png", destination)
}

/**
 * Copies a single asset file to the destination directory.
 *
 * @param filename The name of the asset file
 * @param destination The destination directory
 */
private fun _copyAsset(filename: String, destination: File) {
  val destFile = File(destination, filename)
  _copyFile(filename, destFile)
}

/**
 * Copies a file from the JAR resources to the destination.
 *
 * @param resourcePath The path to the resource in the JAR
 * @param dest The destination file
 */
private fun _copyFile(resourcePath: String, dest: File) {
  // Try to load from JAR resources
  val fullResourcePath = "com/facebook/testing/screenshot/$resourcePath"
  val inputStream = object {}.javaClass.classLoader?.getResourceAsStream(fullResourcePath)
    ?: throw RuntimeException("Resource not found: $fullResourcePath")

  inputStream.use { input ->
    FileOutputStream(dest).use { output ->
      input.copyTo(output)
    }
  }
}

/**
 * Sorts screenshots by group and name.
 *
 * @param screenshots The JSON array of screenshots
 * @return Sorted list of screenshots
 */
private fun sortScreenshots(screenshots: JsonArray): List<JsonElement> {
  return screenshots.sortedWith(compareBy(
    { element ->
      val obj = element.asJsonObject
      obj.get("group")?.asString ?: ""
    },
    { element ->
      element.asJsonObject.get("name").asString
    }
  ))
}

/**
 * Generates an HTML report for screenshot test results.
 *
 * @param outputDir The directory containing metadata.json and screenshot images
 * @return The path to the generated index.html file
 */
fun generateHtml(outputDir: File): File {
  val screenshots = getMetadataJson(outputDir)
  var alternate = false
  val indexHtml = File(outputDir, "index.html").absoluteFile

  val html = buildString {
    append("<!DOCTYPE html>")
    append("<html>")
    append("<head>")
    append("<title>Screenshot Test Results</title>")
    append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js\"></script>")
    append("<script src=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.3/jquery-ui.min.js\"></script>")
    append("<script src=\"default.js\"></script>")
    append("<link rel=\"stylesheet\" href=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.3/themes/smoothness/jquery-ui.css\" />")
    append("<link rel=\"stylesheet\" href=\"default.css\"></head>")
    append("<body>")

    var screenshotNum = 0
    for (element in sortScreenshots(screenshots)) {
      screenshotNum += 1
      alternate = !alternate
      val screenshot = element.asJsonObject
      val canonicalName = screenshot.get("name").asString
      var packageName = ""
      var name = canonicalName

      if ("." in canonicalName) {
        val lastSeparator = canonicalName.lastIndexOf('.') + 1
        packageName = canonicalName.substring(0, lastSeparator)
        name = canonicalName.substring(lastSeparator)
      }

      append("<div class=\"screenshot ${if (alternate) "alternate" else ""}\">")
      append("<div class=\"screenshot_name\">")
      append("<span class=\"demphasize\">$packageName</span>$name")
      append("</div>")

      val group = screenshot.get("group")?.asString
      if (group != null) {
        append("<div class=\"screenshot_group\">$group</div>")
      }

      val extras = screenshot.get("extras")?.asJsonObject
      if (extras != null) {
        var str = ""
        for (key in extras.keySet()) {
          val value = extras.get(key)

          if (key != null) {
            str = str + "*****" + key + "*****\n\n" + value + "\n\n\n"
          }
        }
        if (str != "") {
          val extraHtml = "<button class=\"extra\" data=\"$str\">Extra info</button>"
          append(extraHtml)
        }
      }

      val description = screenshot.get("description")?.asString
      if (description != null) {
        append("<div class=\"screenshot_description\">$description</div>")
      }

      val error = screenshot.get("error")?.asString
      if (error != null) {
        append("<div class=\"screenshot_error\">$error</div>")
      } else {
        val hierarchyData = getViewHierarchy(outputDir, screenshot)
        val hierarchy: JsonObject?
        val axHierarchy: JsonObject?

        if (hierarchyData != null && hierarchyData.has(KEY_VIEW_HIERARCHY)) {
          hierarchy = hierarchyData.get(KEY_VIEW_HIERARCHY)?.asJsonObject
          axHierarchy = hierarchyData.get(KEY_AX_HIERARCHY)?.asJsonObject
        } else {
          hierarchy = hierarchyData
          axHierarchy = null
        }

        append("<div class=\"flex-wrapper\">")
        writeImage(hierarchy, outputDir, this, screenshot, screenshotNum)
        append("<div class=\"command-wrapper\">")
        writeCommands(this)
        writeViewHierarchy(hierarchy, this, screenshotNum)
        writeAxHierarchy(axHierarchy, this, screenshotNum)
        append("</div>")
        append("</div>")
      }

      append("</div>")
      append("<div class=\"clearfix\"></div>")
      append("<hr/>")
    }

    append("</body></html>")
  }

  indexHtml.writeText(html)
  return indexHtml
}

/**
 * Writes the command buttons to the HTML.
 *
 * @param html The StringBuilder to append to
 */
private fun writeCommands(html: StringBuilder) {
  html.append("<button class=\"toggle_dark\">Toggle Dark Background</button>")
  html.append("<button class=\"toggle_hierarchy\">Toggle View Hierarchy Overlay</button>")
}

/**
 * Writes the view hierarchy section to the HTML.
 *
 * @param hierarchy The view hierarchy JSON object
 * @param html The StringBuilder to append to
 * @param parentId The parent screenshot number for ID generation
 */
private fun writeViewHierarchy(hierarchy: JsonObject?, html: StringBuilder, parentId: Int) {
  if (hierarchy == null) {
    return
  }

  html.append("<h3>View Hierarchy</h3>")
  html.append("<div class=\"view-hierarchy\">")
  writeViewHierarchyTreeNode(hierarchy, html, parentId, true)
  html.append("</div>")
}

/**
 * Writes the accessibility hierarchy section to the HTML.
 *
 * @param hierarchy The accessibility hierarchy JSON object
 * @param html The StringBuilder to append to
 * @param parentId The parent screenshot number for ID generation
 */
private fun writeAxHierarchy(hierarchy: JsonObject?, html: StringBuilder, parentId: Int) {
  if (hierarchy == null) {
    return
  }

  html.append("<h3>Accessibility Hierarchy</h3>")
  html.append("<div class=\"view-hierarchy\">")
  writeViewHierarchyTreeNode(hierarchy, html, parentId, false)
  html.append("</div>")
}

/**
 * Writes a single node of the view hierarchy tree to the HTML.
 *
 * @param node The node JSON object
 * @param html The StringBuilder to append to
 * @param parentId The parent screenshot number for ID generation
 * @param withOverlayTarget Whether to include overlay target attributes
 */
private fun writeViewHierarchyTreeNode(
  node: JsonObject,
  html: StringBuilder,
  parentId: Int,
  withOverlayTarget: Boolean
) {
  if (withOverlayTarget) {
    html.append("<details target=\"#$parentId-${getViewHierarchyOverlayNodeId(node)}\">")
  } else {
    html.append("<details>")
  }

  val className = node.get(KEY_CLASS)?.asString ?: DEFAULT_VIEW_CLASS
  html.append("<summary>$className</summary>")
  html.append("<ul>")

  for ((key, value) in node.entrySet().sortedBy { it.key }) {
    if (key == KEY_CHILDREN || key == KEY_CLASS) {
      continue
    }
    html.append("<li><strong>$key:</strong> $value</li>")
  }

  html.append("</ul>")

  val children = node.get(KEY_CHILDREN)
  if (children != null && !children.isJsonNull && children.asJsonArray.size() > 0) {
    for (child in children.asJsonArray) {
      if (child != null) {
        writeViewHierarchyTreeNode(child.asJsonObject, html, parentId, withOverlayTarget)
      }
    }
  }

  html.append("</details>")
}


/**
 * Writes overlay nodes for the view hierarchy to the HTML.
 *
 * @param hierarchy The view hierarchy JSON object
 * @param html The StringBuilder to append to
 * @param parentId The parent screenshot number for ID generation
 */
private fun writeViewHierarchyOverlayNodes(
  hierarchy: JsonObject?,
  html: StringBuilder,
  parentId: Int
) {
  if (hierarchy == null) {
    return
  }

  val toOutput = LinkedList<JsonObject>()
  toOutput.add(hierarchy)

  while (toOutput.isNotEmpty()) {
    val node = toOutput.removeFirst()
    val left = node.get(KEY_LEFT).asInt
    val top = node.get(KEY_TOP).asInt
    val width = node.get(KEY_WIDTH).asInt - 4
    val height = node.get(KEY_HEIGHT).asInt - 4
    val id = getViewHierarchyOverlayNodeId(node)

    val nodeHtml = """
        <div
          class="hierarchy-node"
          style="left:${left}px;top:${top}px;width:${width}px;height:${height}px;"
          id="$parentId-$id"></div>
        """
    html.append(nodeHtml)

    val children = node.get(KEY_CHILDREN)?.asJsonArray
    if (children != null) {
      for (child in children) {
        toOutput.add(child.asJsonObject)
      }
    }
  }
}

/**
 * Generates a unique ID for a view hierarchy node for overlay purposes.
 *
 * @param node The node JSON object
 * @return A unique ID string
 */
private fun getViewHierarchyOverlayNodeId(node: JsonObject): String {
  val cls = node.get(KEY_CLASS)?.asString ?: DEFAULT_VIEW_CLASS
  val x = node.get(KEY_LEFT).asInt
  val y = node.get(KEY_TOP).asInt
  val width = node.get(KEY_WIDTH).asInt
  val height = node.get(KEY_HEIGHT).asInt
  return "node-${cls.replace(".", "-")}-$x-$y-$width-$height"
}

/**
 * Gets the view hierarchy data for a screenshot.
 *
 * @param dir The directory containing the dump files
 * @param screenshot The screenshot JSON object
 * @return The view hierarchy JSON object or null if not found
 */
private fun getViewHierarchy(dir: File, screenshot: JsonObject): JsonObject? {
  val jsonPath = File(dir, screenshot.get("name").asString + "_dump.json")
  if (!jsonPath.exists()) {
    return null
  }

  return jsonPath.bufferedReader().use { reader ->
    JsonParser.parseReader(reader).asJsonObject
  }
}

/**
 * Writes the image section to the HTML.
 *
 * @param hierarchy The view hierarchy JSON object
 * @param dir The directory containing the images
 * @param html The StringBuilder to append to
 * @param screenshot The screenshot JSON object
 * @param parentId The parent screenshot number for ID generation
 */
private fun writeImage(
  hierarchy: JsonObject?,
  dir: File,
  html: StringBuilder,
  screenshot: JsonObject,
  parentId: Int
) {
  html.append("<div class=\"img-block\">")
  html.append("<div class=\"img-wrapper\">")
  html.append("<table>")

  val tileHeight = screenshot.get("tileHeight").asInt
  val tileWidth = screenshot.get("tileWidth").asInt
  val name = screenshot.get("name").asString

  for (y in 0 until tileHeight) {
    html.append("<tr>")
    for (x in 0 until tileWidth) {
      html.append("<td>")
      val imageFile = "./" + getImageFileName(name, x, y)

      if (File(dir, imageFile).exists()) {
        html.append("<img src=\"$imageFile\" />")
      }

      html.append("</td>")
    }
    html.append("</tr>")
  }

  html.append("</table>")
  html.append("<div class=\"hierarchy-overlay\">")
  writeViewHierarchyOverlayNodes(hierarchy, html, parentId)
  html.append("</div></div></div>")
}
