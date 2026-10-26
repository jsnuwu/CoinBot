#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/asset-tests
# Test-only JSON implementation. No additional dependency is shipped in the APK.
jar=build/json-20240303.jar
if [[ ! -f "$jar" ]]; then curl -fsSL https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar -o "$jar"; fi
echo "3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed  $jar" | sha256sum -c -
javac --release 8 -cp "$jar" -d build/asset-tests tests/assets/android/content/Context.java tests/assets/dev/coinbot/mobile/*.java \
  app/src/dev/coinbot/mobile/{Core,BrowserRules,UiState,DraftFiles,PngChecks,ArtStyle,DraftAssets,DraftWriter}.java
java -Djava.awt.headless=true -cp "build/asset-tests:$jar" dev.coinbot.mobile.AssetTest
