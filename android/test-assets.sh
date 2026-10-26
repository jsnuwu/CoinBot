#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/asset-tests
# Test-only JSON implementation. No additional dependency is shipped in the APK.
jar=build/json-20240303.jar
if [[ ! -f "$jar" ]]; then curl -fsSL https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar -o "$jar"; fi
echo "3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed  $jar" | sha256sum -c -
javac --release 8 -cp "$jar" -d build/asset-tests tests/assets/android/content/Context.java tests/assets/android/content/res/AssetManager.java tests/assets/dev/coinbot/mobile/*.java \
  app/src/dev/coinbot/mobile/{Core,BrowserRules,UiState,DraftFiles,PngChecks,ArtStyle,MemeScene,ActorCutout,ImageFingerprint,ImageGenerationException,ImageLedger,StoryPlan,GeneratedArt,DraftAssets,DraftWriter}.java
java -Djava.awt.headless=true -cp "build/asset-tests:$jar" dev.coinbot.mobile.AssetTest
java -Djava.awt.headless=true -cp "build/asset-tests:$jar" dev.coinbot.mobile.MemeSceneTest
java -Djava.awt.headless=true -cp "build/asset-tests:$jar" dev.coinbot.mobile.ImageUniquenessTest
mkdir -p build/render-tests
javac --release 8 -d build/render-tests tests/assets/android/content/Context.java tests/assets/android/content/res/AssetManager.java \
  tests/assets/dev/coinbot/mobile/Diagnostics.java tests/render/android/graphics/*.java tests/render/RenderTest.java \
  app/src/dev/coinbot/mobile/{Core,ArtStyle,MemeScene,ActorCutout,ImageFingerprint,ImageGenerationException,ImageLedger,StoryPlan,GeneratedArt,DraftFiles,PngChecks,ScenePainter,Art}.java
java -Djava.awt.headless=true -cp build/render-tests dev.coinbot.mobile.RenderTest
