
.PHONY:
	true

android-28-impl: .PHONY
	adb shell settings put global hidden_api_policy_p_apps 1
	adb shell settings put global hidden_api_policy_pre_p_apps 1
	./gradlew :core:connectedDebugAndroidTests

android-28: .PHONY
	~/eaase/eaase run --verbose --api-level 28 -- $(MAKE) android-28-impl

install-eaase: .PHONY
	curl https://eaase.dev/installer.sh | sh

ci: | install-eaase android-28
