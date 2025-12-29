
ANDROID_SDK ?= $(ANDROID_HOME)
ANDROID_SDK ?= $(ANDROID_SDK_HOME)
ANDROID_SDK ?= $(ANDROID_SDK_ROOT)


ADB = $(ANDROID_SDK)/platform-tools/adb

.PHONY:
	true

android-28-impl: .PHONY
	$(ADB) shell settings put global hidden_api_policy_p_apps 1
	$(ADB) shell settings put global hidden_api_policy_pre_p_apps 1
	./gradlew :core:connectedDebugAndroidTest

android-28: .PHONY
	~/eaase/eaase run --verbose --api-level 28 -- $(MAKE) android-28-impl

install-eaase: .PHONY
	curl https://eaase.dev/installer.sh | sh

ci: | install-eaase android-28
