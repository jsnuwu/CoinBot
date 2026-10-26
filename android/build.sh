#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk" ]]; then echo 'Set ANDROID_HOME to an Android SDK with platform 36 and build-tools 36.0.0.' >&2; exit 1; fi
tools="$sdk/build-tools/36.0.0"
platform="$sdk/platforms/android-36/android.jar"
rm -rf build/classes build/core-tests build/link-tests build/browser-tests build/ui-tests
mkdir -p build/classes
# No Gradle or third-party runtime dependencies. Compile against the Android SDK.
javac --release 8 -cp "$platform" -d build/classes app/src/dev/coinbot/mobile/*.java
javac --release 8 -d build/core-tests app/src/dev/coinbot/mobile/Core.java tests/dev/coinbot/mobile/CoreTest.java
java -cp build/core-tests dev.coinbot.mobile.CoreTest
javac --release 8 -d build/link-tests tests/stubs/android/{app,content,net,os}/*.java \
  app/src/dev/coinbot/mobile/{Core,NotificationLinks}.java tests/dev/coinbot/mobile/NotificationLinksTest.java
java -cp build/link-tests dev.coinbot.mobile.NotificationLinksTest
javac --release 8 -d build/browser-tests app/src/dev/coinbot/mobile/{Core,BrowserRules,NotificationMatch}.java tests/dev/coinbot/mobile/{BrowserRulesTest,NotificationMatchTest}.java
java -cp build/browser-tests dev.coinbot.mobile.BrowserRulesTest
java -cp build/browser-tests dev.coinbot.mobile.NotificationMatchTest
javac --release 8 -d build/ui-tests app/src/dev/coinbot/mobile/{Core,UiState,UiTime}.java tests/dev/coinbot/mobile/UiStateTest.java
java -cp build/ui-tests dev.coinbot.mobile.UiStateTest
"$tools/aapt2" link -I "$platform" --manifest AndroidManifest.xml -A app/assets -o build/resources.apk
find build/classes -name '*.class' -print > build/class-files.txt
"$tools/d8" --lib "$platform" --min-api 26 --output build @build/class-files.txt
cp build/resources.apk build/unaligned.apk
(cd build && zip -q unaligned.apk classes.dex)
"$tools/zipalign" -f -p 4 build/unaligned.apk build/aligned.apk
if [[ ! -f build/debug.jks ]]; then
  keytool -genkeypair -keystore build/debug.jks -storepass android -keypass android -alias coinbot \
    -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=CoinBot Local Test Build' -noprompt >/dev/null 2>&1
fi
"$tools/apksigner" sign --ks build/debug.jks --ks-pass pass:android --key-pass pass:android --ks-key-alias coinbot \
  --out build/CoinBot-Android.apk build/aligned.apk
"$tools/apksigner" verify --verbose build/CoinBot-Android.apk
echo 'Built android/build/CoinBot-Android.apk (test build; no real Pump.fun account connection yet).'
