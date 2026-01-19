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

package com.facebook.testing.screenshot.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.annotation.UiThreadTest
import org.junit.Test
import com.facebook.testing.screenshot.compose.screenshot

class HelloWorldScreenshotTest {
  @Test
  @UiThreadTest
  fun testHelloWorld() {
    screenshot(300, 200) {
      MaterialTheme {
        HelloWorld()
      }
    }
  }

  @Test
  @UiThreadTest
  fun testButtonVariants() {
    screenshot(400, 400) {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Button(onClick = {}) {
              Text("Filled Button")
            }
            ElevatedButton(onClick = {}) {
              Text("Elevated Button")
            }
            FilledTonalButton(onClick = {}) {
              Text("Tonal Button")
            }
            OutlinedButton(onClick = {}) {
              Text("Outlined Button")
            }
            TextButton(onClick = {}) {
              Text("Text Button")
            }
            Button(onClick = {}, enabled = false) {
              Text("Disabled Button")
            }
          }
        }
      }
    }
  }

  @Test
  @UiThreadTest
  fun testTextFieldVariants() {
    screenshot(400, 350) {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            TextField(
              value = "Hello",
              onValueChange = {},
              label = { Text("Standard TextField") }
            )
            OutlinedTextField(
              value = "World",
              onValueChange = {},
              label = { Text("Outlined TextField") }
            )
            TextField(
              value = "",
              onValueChange = {},
              label = { Text("With placeholder") },
              placeholder = { Text("Enter text here") }
            )
            OutlinedTextField(
              value = "With error",
              onValueChange = {},
              label = { Text("Error State") },
              isError = true,
              supportingText = { Text("This field has an error") }
            )
          }
        }
      }
    }
  }

  @Test
  @UiThreadTest
  fun testCardWithContent() {
    screenshot(400, 450) {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Card(
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text("Basic Card", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("This is the card content with some descriptive text.")
              }
            }
            Card(
              modifier = Modifier.fillMaxWidth(),
              elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text("Elevated Card", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Card with higher elevation for more prominence.")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                  TextButton(onClick = {}) { Text("Cancel") }
                  Button(onClick = {}) { Text("Confirm") }
                }
              }
            }
          }
        }
      }
    }
  }

  @Test
  @UiThreadTest
  fun testCheckboxAndSwitch() {
    screenshot(350, 350) {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = true, onCheckedChange = {})
              Text("Checked checkbox")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = false, onCheckedChange = {})
              Text("Unchecked checkbox")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = true, enabled = false, onCheckedChange = {})
              Text("Disabled checked")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Switch(checked = true, onCheckedChange = {})
              Spacer(modifier = Modifier.width(8.dp))
              Text("Switch On")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Switch(checked = false, onCheckedChange = {})
              Spacer(modifier = Modifier.width(8.dp))
              Text("Switch Off")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Switch(checked = true, enabled = false, onCheckedChange = {})
              Spacer(modifier = Modifier.width(8.dp))
              Text("Disabled Switch")
            }
          }
        }
      }
    }
  }

  @Test
  @UiThreadTest
  fun testProgressAndSlider() {
    screenshot(400, 350) {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
          ) {
            Column {
              Text("Determinate Progress (30%)", style = MaterialTheme.typography.labelMedium)
              Spacer(modifier = Modifier.height(8.dp))
              LinearProgressIndicator(
                progress = { 0.3f },
                modifier = Modifier.fillMaxWidth()
              )
            }
            Column {
              Text("Determinate Progress (70%)", style = MaterialTheme.typography.labelMedium)
              Spacer(modifier = Modifier.height(8.dp))
              LinearProgressIndicator(
                progress = { 0.7f },
                modifier = Modifier.fillMaxWidth()
              )
            }
            Column {
              Text("Slider at 25%", style = MaterialTheme.typography.labelMedium)
              Slider(value = 0.25f, onValueChange = {})
            }
            Column {
              Text("Slider at 75%", style = MaterialTheme.typography.labelMedium)
              Slider(value = 0.75f, onValueChange = {})
            }
          }
        }
      }
    }
  }

}
