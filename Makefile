
ANDROID_SDK = $(or $(ANDROID_HOME),$(ANDROID_SDK_ROOT),$(ANDROID_SDK))
ADB = $(ANDROID_SDK)/platform-tools/adb

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
	~/eaase/eaase run --verbose --api-level 28 -- $(MAKE) android-impl

android-29: .PHONY
	~/eaase/eaase run --verbose --api-level 29 -- $(MAKE) android-impl


android-30: .PHONY
	~/eaase/eaase run --verbose --api-level 30 -- $(MAKE) android-impl

android-31: .PHONY
	~/eaase/eaase run --verbose --api-level 31 -- $(MAKE) android-impl

android-32: .PHONY
	~/eaase/eaase run --verbose --api-level 32 -- $(MAKE) android-impl

android-33: .PHONY
	~/eaase/eaase run --verbose --api-level 33 -- $(MAKE) android-impl

android-34: .PHONY
	# broken!
	~/eaase/eaase run --verbose --api-level 34 -- $(MAKE) android-impl


install-eaase: .PHONY
	curl https://eaase.dev/installer.sh | sh

ci: | install-eaase android-28 android-29 android-30  android-31 android-32 android-33 
