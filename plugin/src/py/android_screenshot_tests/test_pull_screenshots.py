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

from __future__ import absolute_import, division, print_function, unicode_literals

import errno
import json
import os
import shutil
import sys
import tempfile
import unittest
from os.path import join

from . import pull_screenshots

if sys.version_info >= (3,):
    from unittest.mock import MagicMock
else:
    from mock import MagicMock

from .common import assertRegex

TESTING_PACKAGE = "com.foo"
CURRENT_DIR = os.path.dirname(__file__)
FIXTURE_DIR = "%s/fixtures/sdcard/screenshots/%s/screenshots-default" % (
    CURRENT_DIR,
    TESTING_PACKAGE,
)

def _android_path_join_two(a, b):
    if b.startswith("/"):
        return b

    if not a.endswith("/"):
        a += "/"

    return a + b


def android_path_join(a, *args):
    """Similar to os.path.join(), but might differ in behavior on Windows"""

    if args == []:
        return a

    if len(args) == 1:
        return _android_path_join_two(a, args[0])

    return android_path_join(android_path_join(a, args[0]), *args[1:])




class LocalFileHelper:
    def setup(self, dir, test_run_id):
        shutil.copyfile(
            FIXTURE_DIR + "/metadata_no_errors.json", dir + "/metadata.json"
        )
        shutil.copyfile(
            FIXTURE_DIR
            + "/"
            + test_run_id
            + "/com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png",
            dir + "/com.foo.ScriptsFixtureTest_testGetTextViewScreenshot.png",
        )
        shutil.copyfile(
            FIXTURE_DIR
            + "/"
            + test_run_id
            + "/com.foo.ScriptsFixtureTest_testSecondScreenshot.png",
            dir + "/com.foo.ScriptsFixtureTest_testSecondScreenshot.png",
        )


def assert_nice_filename(filename):
    if "//" in filename:
        raise RuntimeError(
            "%s is not a canonical filename and can cause problems that are hard to debug"
            % filename
        )


class AdbPuller:
    def __init__(self, fixture_dir=join(CURRENT_DIR, "fixtures")):
        self.fixture_dir = fixture_dir

    def pull(self, src, dest):
        self._valid_src(src)
        assert_nice_filename(src)
        src = self.fixture_dir + src
        self._copy(src, dest)

    def pull_folder(self, src, dest):
        self.pull(src, dest)

    def remote_file_exists(self, src):
        self._valid_src(src)
        assert_nice_filename(src)
        src = self.fixture_dir + src
        return os.path.exists(src)

    def _valid_src(self, src):
        if not src.startswith("/"):
            raise RuntimeError("src must be absolute, not: " + src)

    def get_external_data_dir(self):
        return "/sdcard"

    def _copy(self, src, dst):
        try:
            shutil.copytree(src, dst, dirs_exist_ok=True)
        except OSError as exc:
            if exc.errno == errno.ENOTDIR:
                shutil.copy(src, dst)
            else:
                raise

def pull_images(dir, device_dir, test_run_id, adb_puller):
    if adb_puller.remote_file_exists(android_path_join(device_dir, test_run_id)):
        # Optimization to pull down all the screenshots in a single pull.
        # If this file exists, we assume all of the screenshots are inside it.
        adb_puller.pull_folder(
            android_path_join(device_dir, test_run_id), dir
        )

OLD_ROOT_SCREENSHOT_DIR = "/data/data/"

def create_empty_metadata_file(dir):
    with open(join(dir, "metadata.json"), "w") as out:
        out.write("{}")

def pull_metadata(package, dir, adb_puller):
    """Returns the directory where the metadata file is located,
    essentially the root of the screenshot directory.

    This code used to be in Python, but moved to java, so now we have
    this temporarily in the test to keep everything passing. If you
    make any changes here, please keep in sync with pullMetadata in
    Kotlin.

    """

    root_screenshot_dir = android_path_join(
        adb_puller.get_external_data_dir(), "screenshots"
    )
    metadata_file = android_path_join(
        root_screenshot_dir, package, "screenshots-default/metadata.json"
    )

    old_metadata_file = android_path_join(
        OLD_ROOT_SCREENSHOT_DIR, package, "app_screenshots-default/metadata.json"
    )

    if adb_puller.remote_file_exists(metadata_file):
        adb_puller.pull(metadata_file, join(dir, "metadata.json"))
    elif adb_puller.remote_file_exists(old_metadata_file):
        adb_puller.pull(old_metadata_file, join(dir, "metadata.json"))
        metadata_file = old_metadata_file
    else:
        create_empty_metadata_file(dir)

    return metadata_file.replace("metadata.json", "")


            

def pull_all(package, dir, test_run_id, adb_puller):
    device_dir = pull_metadata(package, dir, adb_puller=adb_puller)
    pull_images(dir, device_dir, test_run_id, adb_puller=adb_puller)



class TestAdbHelpers(unittest.TestCase):
    def setUp(self):
        self.tmpdir = tempfile.mkdtemp(prefix="screenshots")

    def tearDown(self):
        shutil.rmtree(self.tmpdir)

    def test_pull_metadata_without_metadata(self):
        adb_instance = MagicMock()

        adb_instance.remote_file_exists = MagicMock()
        adb_instance.remote_file_exists.return_value = False

        adb_instance.pull = MagicMock()
        adb_instance.pull.side_effect = Exception("should not be called")

        pull_all(
            "com.facebook.testing.tests",
            self.tmpdir,
            test_run_id="unittest",
            adb_puller=AdbPuller(),
        )

        self.assertTrue(os.path.exists(self.tmpdir + "/metadata.json"))


class TestPullScreenshots(unittest.TestCase):
    def setUp(self):
        fd, self.output_file = tempfile.mkstemp(
            prefix="final_screenshot", suffix=".png"
        )
        os.close(fd)
        os.unlink(self.output_file)
        self.tmpdir = None
        self.oldstdout = sys.stdout
        self.oldenviron = dict(os.environ)

    def tearDown(self):
        os.environ.clear()
        os.environ.update(self.oldenviron)
        if self.oldstdout:
            sys.stdout = self.oldstdout

        if os.path.exists(self.output_file):
            os.unlink(self.output_file)
        if self.tmpdir:
            shutil.rmtree(self.tmpdir)

    def test_index_html_created(self):
        self.tmpdir = tempfile.mkdtemp(prefix="screenshots")
        adb_puller = AdbPuller()
        device_dir = pull_metadata(
            TESTING_PACKAGE, self.tmpdir, adb_puller
        )
        pull_screenshots.pull_screenshots(
            temp_dir=self.tmpdir,
        )
        self.assertTrue(os.path.exists(self.tmpdir + "/index.html"))

    def test_image_is_linked(self):
        self.tmpdir = tempfile.mkdtemp(prefix="screenshots")
        adb_puller = AdbPuller()
        device_dir = pull_metadata(
            TESTING_PACKAGE, self.tmpdir, adb_puller
        )
        pull_images(
            self.tmpdir,
            device_dir=device_dir,
            test_run_id="unittest",
            adb_puller=adb_puller,
        )
        pull_screenshots.pull_screenshots(
            temp_dir=self.tmpdir,
        )
        with open(self.tmpdir + "/index.html", "r") as f:
            contents = f.read()
            print(contents)
            assertRegex(self, contents, ".*com.foo.*")
            self.assertTrue(contents.find('<img src="./com.foo.') >= 0)

    def test_generate_html_returns_a_valid_file(self):
        self.tmpdir = tempfile.mkdtemp(prefix="screenshots")
        pull_all(
            TESTING_PACKAGE, self.tmpdir, "unittest", adb_puller=AdbPuller()
        )
        html = pull_screenshots.generate_html(self.tmpdir)
        self.assertTrue(os.path.exists(html))

    def test_adb_puller_sanity(self):
        self.assertTrue(AdbPuller().remote_file_exists("/sdcard"))

    def test_setup_paths(self):
        os.environ["ANDROID_SDK"] = "foobar"
        pull_screenshots.setup_paths()
        assertRegex(self, os.environ["PATH"], ".*:foobar/platform-tools.*")

    def test_screenshots_with_same_group_ordered_together(self):
        loaded_json = json.loads(
            # language=json
            """[
                    {
                        "name": "one",
                        "group": "foo"
                    },
                    {
                        "name": "two"
                    },
                    {
                        "name": "three",
                        "group": "foo"
                    }
               ]
"""
        )

        screenshots = pull_screenshots.sort_screenshots(loaded_json)

        self.assertEqual(["two", "one", "three"], [x.get("name") for x in screenshots])

class TestAndroidJoin(unittest.TestCase):
    def test_simple(self):
        self.assertEqual("/foo/bar", android_path_join("/foo", "bar"))
        self.assertEqual("/foo/bar", android_path_join("/foo/", "bar"))

    def test_multiple(self):
        self.assertEqual(
            "/foo/bar/car", android_path_join("/foo", "bar/", "car")
        )

    def test_root(self):
        self.assertEqual("/bar", android_path_join("/foo", "/bar"))


if __name__ == "__main__":
    unittest.main()
