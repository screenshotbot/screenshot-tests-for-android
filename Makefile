
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

expensive-tests: | install-eaase android-28 android-29 android-30  android-31 android-32 

ci:
	./gradlew test
	$(MAKE) expensive-tests

integration-test-impl:
	git clone /workspace-original ~/workspace
	cd ~/workspace && true
	cd ~/workspace && ./gradlew :plugin:publishToMavenLocal 
	cd ~/workspace && ./gradlew :core:publishToMavenLocal

integration-test:
	docker run  --rm -w /workspace-original  -v .:/workspace-original cimg/android:2026.01 make integration-test-impl 
