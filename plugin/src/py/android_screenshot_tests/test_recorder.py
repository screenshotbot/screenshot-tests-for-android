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

import os
import shutil
import tempfile
import unittest
from os.path import exists, join

from PIL import Image

from .recorder import Recorder


class TestRecorder(unittest.TestCase):
    def setUp(self):
        self.outputdir = tempfile.mkdtemp()
        self.inputdir = tempfile.mkdtemp()
        self.failureDir = tempfile.mkdtemp()
        self.tmpimages = []
        self.recorder = Recorder(self.inputdir, self.outputdir, self.failureDir)

    def create_temp_image(self, name, dimens, color):
        im = Image.new("RGBA", dimens, color)
        filename = os.path.join(self.inputdir, name)
        im.save(filename, "PNG")
        im.close()
        return filename

    def make_metadata(self, str):
        with open(os.path.join(self.inputdir, "metadata.json"), "w") as f:
            f.write(str)

    def tearDown(self):
        for f in self.tmpimages:
            f.close()

        shutil.rmtree(self.outputdir)
        shutil.rmtree(self.inputdir)

    def test_create_temp_image(self):
        im = self.create_temp_image("foobar", (100, 10), "blue")
        self.assertTrue(os.path.exists(im))

    def test_recorder_creates_dir(self):
        shutil.rmtree(self.outputdir)
        self.make_metadata("""[]""")
        self.recorder.record()

        self.assertTrue(os.path.exists(self.outputdir))







if __name__ == "__main__":
    unittest.main()
