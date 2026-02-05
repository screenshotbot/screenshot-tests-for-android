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

package com.facebook.testing.screenshot.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to strip metadata chunks from PNG files.
 *
 * PNG files can contain metadata chunks like tIME (timestamps) that cause
 * byte-level differences between otherwise identical images. This class
 * removes non-essential chunks to ensure deterministic output.
 */
public class PngMetadataStripper {

  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };

  // Essential chunks that must be preserved
  private static final Set<String> ESSENTIAL_CHUNKS = new HashSet<>(Arrays.asList(
    "IHDR",  // Image header
    "PLTE",  // Palette
    "IDAT",  // Image data
    "IEND",  // Image end
    "tRNS",  // Transparency
    "cHRM",  // Chromaticity
    "gAMA",  // Gamma
    "iCCP",  // ICC profile
    "sBIT",  // Significant bits
    "sRGB"   // Standard RGB
  ));

  /**
   * Strips non-essential metadata chunks from a PNG byte array.
   *
   * @param pngBytes The original PNG file bytes
   * @return PNG bytes with metadata chunks removed
   */
  public static byte[] strip(byte[] pngBytes) {
    if (pngBytes == null || pngBytes.length < 8) {
      return pngBytes;
    }

    // Verify PNG signature
    for (int i = 0; i < PNG_SIGNATURE.length; i++) {
      if (pngBytes[i] != PNG_SIGNATURE[i]) {
        return pngBytes; // Not a valid PNG, return as-is
      }
    }

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      out.write(PNG_SIGNATURE);

      int offset = 8; // Skip signature

      while (offset < pngBytes.length) {
        if (offset + 8 > pngBytes.length) {
          break; // Not enough bytes for chunk header
        }

        // Read chunk length (4 bytes, big-endian)
        int length = ((pngBytes[offset] & 0xFF) << 24)
                   | ((pngBytes[offset + 1] & 0xFF) << 16)
                   | ((pngBytes[offset + 2] & 0xFF) << 8)
                   | (pngBytes[offset + 3] & 0xFF);

        // Read chunk type (4 bytes)
        String chunkType = new String(pngBytes, offset + 4, 4);

        // Total chunk size: 4 (length) + 4 (type) + length (data) + 4 (CRC)
        int totalChunkSize = 12 + length;

        if (offset + totalChunkSize > pngBytes.length) {
          break; // Chunk extends beyond file
        }

        // Keep essential chunks, discard metadata
        if (ESSENTIAL_CHUNKS.contains(chunkType)) {
          out.write(pngBytes, offset, totalChunkSize);
        }

        offset += totalChunkSize;

        // Stop after IEND
        if ("IEND".equals(chunkType)) {
          break;
        }
      }

      return out.toByteArray();
    } catch (IOException e) {
      return pngBytes; // On error, return original
    }
  }
}
