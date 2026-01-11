#!/usr/bin/env python3
# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import json
import os
import shutil
import sys
import tempfile
import xml.etree.ElementTree as ET
from os.path import join

from PIL import Image, ImageChops, ImageDraw

from . import common


class VerifyError(Exception):
    pass

def _is_image_same(file1, file2, failure_file):
    with Image.open(file1) as im1, Image.open(file2) as im2:
        diff_image = ImageChops.difference(im1.convert("RGB"), im2.convert("RGB"))
        try:
            diff = diff_image.getbbox()
            if diff is None and im1.size == im2.size:
                return True
            else:
                if failure_file:
                    diff_list = list(diff) if diff else []
                    draw = ImageDraw.Draw(im2)
                    draw.rectangle(diff_list, outline=(255, 0, 0))
                    im2.save(failure_file)
                return False
        finally:
            diff_image.close()

def _get_image_size(file_name):
    with Image.open(file_name) as im:
        return im.size


def _copy(name, w, h, input, output):
    """
    Copy the screenshot `name` from input dir to output dir.

    w and h are the width and height of the number of tiles
    """
    tilewidth, tileheight = _get_image_size(
        join(input, common.get_image_file_name(name, 0, 0))
    )

    canvaswidth = 0

    for i in range(w):
        input_file = common.get_image_file_name(name, i, 0)
        canvaswidth += _get_image_size(join(input, input_file))[0]

    canvasheight = 0

    for j in range(h):
        input_file = common.get_image_file_name(name, 0, j)
        canvasheight += _get_image_size(join(input, input_file))[1]

    im = Image.new("RGBA", (canvaswidth, canvasheight))

    for i in range(w):
        for j in range(h):
            input_file = common.get_image_file_name(name, i, j)
            with Image.open(join(input, input_file)) as input_image:
                im.paste(input_image, (i * tilewidth, j * tileheight))

    im.save(join(output, name + ".png"))
    im.close()


def _record(metadata, input, output):
    for screenshot in metadata:
        _copy(
            screenshot["name"],
            int(screenshot["tileWidth"]),
            int(screenshot["tileHeight"]),
            input,
            output
        )

class Recorder:
    def __init__(self, input, output, failure_output):
        self._input = input
        self._output = output
        self._failure_output = failure_output

    def _get_metadata_json(self):
        with open(join(self._input, "metadata.json"), "r") as f:
            return json.load(f)

    def _clean(self):
        if os.path.exists(self._output):
            shutil.rmtree(self._output)
        os.makedirs(self._output)

    def record(self):
        self._clean()
        _record(self._get_metadata_json(), self._input, self._output)

    def verify(self, output):
        _record(self._get_metadata_json(), self._input, output)
        verify_helper(
            screenshots=self._get_metadata_json(),
            output=output,
            expected_output=self._output,
            failure_output=self._failure_output)


def verify_helper(screenshots, output, expected_output, failure_output):
    """
    screenshots is is the parsed json from metadata.json
    output, expected_output are the directories with the screenshots
    failure_output is where we copy the failure screenshots to.
    """
    failures = []
    for screenshot in screenshots:
        name = screenshot["name"] + ".png"
        actual = join(output, name)
        expected = join(expected_output, name)
        if failure_output:
            diff_name = screenshot["name"] + "_diff.png"
            diff = join(failure_output, diff_name)

            if not _is_image_same(expected, actual, diff):
                expected_name = screenshot["name"] + "_expected.png"
                actual_name = screenshot["name"] + "_actual.png"

                shutil.copy(actual, join(failure_output, actual_name))
                shutil.copy(expected, join(failure_output, expected_name))

                failures.append((expected, actual))
        else:
            if not _is_image_same(expected, actual, None):
                raise VerifyError("Image %s is not same as %s" % (expected, actual))

    if failures:
        reason = ""
        for expected, actual in failures:
            reason = reason + "\nImage %s is not same as %s" % (expected, actual)
        raise VerifyError(reason)

    shutil.rmtree(output)
