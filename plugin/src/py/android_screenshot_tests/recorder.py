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


def _do_clean(output):
    if os.path.exists(output):
        shutil.rmtree(output)
    os.makedirs(output)

class Recorder:
    def __init__(self, input, output, failure_output):
        self._input = input
        self._output = output
        self._failure_output = failure_output

    def _get_metadata_json(self):
        with open(join(self._input, "metadata.json"), "r") as f:
            return json.load(f)

    def _clean(self):
        _do_clean(self._output)

    def record(self):
        self._clean()


