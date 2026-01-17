
ANDROID_SDK = $(or $(ANDROID_HOME),$(ANDROID_SDK_ROOT),$(ANDROID_SDK))
ADB = $(ANDROID_SDK)/platform-tools/adb

EAASE=~/eaase/eaase run --verbose

.PHONY:
	true

settings: .PHONY
	$(ADB) shell getprop

android-impl: .PHONY
	$(MAKE) settings
	./gradlew :core:connectedDebugAndroidTest

android-28: .PHONY
	$(EAASE) --api-level 28 -- $(MAKE) android-impl

android-29: .PHONY
	$(EAASE)  --api-level 29 -- $(MAKE) android-impl


android-30: .PHONY
	$(EAASE) --api-level 30 -- $(MAKE) android-impl

android-31: .PHONY
	$(EAASE) --api-level 31 -- $(MAKE) android-impl

android-32: .PHONY
	$(EAASE) --api-level 32 -- $(MAKE) android-impl

android-33: .PHONY
	$(EAASE) --api-level 33 -- $(MAKE) android-impl

android-34: .PHONY
	$(EAASE) --api-level 34 -- $(MAKE) android-impl


install-eaase: .PHONY
	curl https://eaase.dev/installer.sh | sh

expensive-tests: | install-eaase android-28 android-29 android-30  android-31 android-32 android-33 android-34


ci:
	./gradlew :core:test
	$(EAASE) --api-level 30 -- ./gradlew :plugin:test
	$(MAKE) expensive-tests

integration-test-impl: install-eaase
	git clone /workspace-original ~/workspace
	cd ~/workspace && true
	cd ~/workspace && ./gradlew :plugin:publishToMavenLocal 
	cd ~/workspace && ./gradlew :core:publishToMavenLocal
	echo Running the sample tests now
	cd ~/workspace && $(EAASE) --api-level 30 -- bash -c "make settings && ./gradlew :sample:recordDebugAndroidTestScreenshotTest && ./gradlew :sample:verifyDebugAndroidTestScreenshotTest"
	cd ~/workspace && curl https://screenshotbot.io/recorder.sh | sh
	cd ~/workspace && ~/screenshotbot/recorder --directory ~/workspace/sample/screenshots/API_30* --channel screenshot-tests-for-android-sample-android-30

integration-tests:
		docker run  --rm -e EAASE_API_TOKEN=$$EAASE_API_TOKEN  \
	       -e SCREENSHOTBOT_API_KEY=$$SCREENSHOTBOT_API_KEY \
	       -e SCREENSHOTBOT_API_SECRET=$$SCREENSHOTBOT_API_SECRET \
           -w /workspace-original \
           -v .:/workspace-original cimg/android:2026.01 \
	       make integration-test-impl 
