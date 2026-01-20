# Screenshot Tests for Android

> This is the officially maintained version of screenshot-tests-for-android, initially forked from [facebook/screenshot-tests-for-android](https://github.com/facebook/screenshot-tests-for-android). See [this blog post](https://screenshotbot.io/blog/screenshot-tests-for-android-is-now-maintained-by-screenshotbot) for details.

<img src="/.github/logo.png" width="150" align="right"/>

screenshot-tests-for-android is a library that can generate fast
deterministic screenshots while running instrumentation tests on
Android.

We mimic Android's measure(), layout() and draw() to generate screenshots
on the test thread. By not having to do the rendering on a separate
thread we have control over animations and handler callbacks which
makes the screenshots extremely deterministic and reliable for catching
regressions in continuous integration.

We also provide utilities for using screenshot tests during the development
process. With these scripts you can iterate on a view or layout and quickly
see how the view renders in a real Android environment, without having to
build the whole app. You can also render the view in multiple configurations
at one go.

This library was initially published by Arnold Noronha at Facebook,
maintained and build upon by Hilal Alsibai at Facebook, and is
currently maintained by Arnold Noronha at [Screenshotbot](https://screenshotbot.io).

## Documentation

Take a look at the documentation at http://facebook.github.io/screenshot-tests-for-android/#getting-started

## Requirements

This version of screenshot-tests-for-android does *not* depend on
Python! (If you've worked with previous versions you might remember
that was a requirement)

## Adding to Your Project

In your project's `build.gradle`:
```groovy
buildscript {
    dependencies {
        classpath 'io.screenshotbot.screenshot-tests-for-android:plugin:1.1.0'
    }
}
```

In your app module's `build.gradle`:
```groovy
apply plugin: 'io.screenshotbot.screenshot-tests-for-android'

dependencies {
    androidTestImplementation 'io.screenshotbot.screenshot-tests-for-android:core:1.1.0'
    // For Compose support
    androidTestImplementation 'io.screenshotbot.screenshot-tests-for-android:compose:1.1.0'
}
```

Record screenshots:
```bash
$ gradle :<project>:record<flavor>ScreenshotTest
```

Verify screenshots:
```bash
$ gradle :<project>:verify<flavor>ScreenshotTest
```

## Building screenshot-tests-for-android

You don't have to build screenshot-tests-for-android from scratch if
you don't plan to contribute. All artifacts are available from Maven
Central.

If you plan to contribute, this is the code is broken up into a few modules:

* The `core` module is packaged as part of your instrumentation tests
  and generates screenshots on the device.

* The `plugin` module adds Gradle tasks to make it easier to work
  with screenshot tests.

We have tests for the plugin and the core library. Run these
commands to run all the tests:

```bash
  $ gradle :plugin:tests
  $ gradle :core:connectedAndroidTest
```

Both need a running emulator. 

You can install all the artifacts to your local maven repository using

```bash
  $ gradle installArchives
```

## Running With a Remote Service

For usage with a remote testing service (e.g. Google Cloud Test Lab) where ADB is not available directly the plugin supports a "disconnected" 
workflow.  Collect all screenshots into a single directory and run the plugin using the following options

### Example
The location of the screenshot artifacts can be configured in the project's build.gradle:
```groovy
  screenshots {
      // Points to the directory containing all the files pulled from a device
      referenceDir = path/to/screenshots
  }
```

Then, screenshots may be verified by executing the following:
```bash
  $ gradle :<project>:verify<flavor>ScreenshotTest
```

To record, simply change `verify` to `record`.

## Contributing

Please see the [contributing](.github/CONTRIBUTING.md) file.

## License

screenshot-tests-for-android is Apache-2-licensed.
