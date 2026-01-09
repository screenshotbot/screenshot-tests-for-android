
ANDROID_SDK = $(or $(ANDROID_HOME),$(ANDROID_SDK_ROOT),$(ANDROID_SDK))
ADB = $(ANDROID_SDK)/platform-tools/adb

EAASE=~/eaase/eaase run --verbose

.PHONY:
	true

settings: .PHONY
	$(ADB) shell settings put global hidden_api_policy_p_apps 1
	$(ADB) shell settings put global hidden_api_policy_pre_p_apps 1
	$(ADB) shell settings put global hidden_api_policy  1

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
	# broken
	$(EAASE) --api-level 33 -- $(MAKE) android-impl

android-34: .PHONY
	# broken!
	$(EAASE) --api-level 34 -- $(MAKE) android-impl


install-eaase: .PHONY
	curl https://eaase.dev/installer.sh | sh

expensive-tests: | install-eaase python-tests android-28 android-29 android-30  android-31 android-32

python-tests-impl: .PHONY
	cd plugin/src/py && python3 -m unittest discover -s . -p "test_*.py"

python-tests: .PHONY
	$(EAASE) --api-level 30 -- $(MAKE) python-tests-impl

ci:
	./gradlew :core:test
	$(EAASE) --api-level 30 -- ./gradlew :plugin:test
	$(MAKE) expensive-tests

integration-test-impl: install-eaase
	git clone /workspace-original ~/workspace
	sudo apt-get install python3 python3-pillow
	cd ~/workspace && true
	cd ~/workspace && ./gradlew :plugin:publishToMavenLocal 
	cd ~/workspace && ./gradlew :core:publishToMavenLocal
	echo Running the sample tests now
	cd ~/workspace && $(EAASE) --api-level 30 -- bash -c "make settings && ./gradlew :sample:recordDebugAndroidTestScreenshotTest"

integration-tests:
		docker run  --rm -e EAASE_API_TOKEN=$$EAASE_API_TOKEN -w /workspace-original  -v .:/workspace-original cimg/android:2026.01 make integration-test-impl 
