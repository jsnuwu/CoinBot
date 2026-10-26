# CoinBot Android test builds

[CoinBot 0.5.2 herunterladen](https://github.com/jsnuwu/CoinBot/raw/refs/heads/main/releases/CoinBot-Android-0.5.2.apk)

Version 0.5.2 / Code 10 behebt den fehlenden lokalen Mitteilungsentwurf bei erfolglosem Web-Abgleich.
Unbestätigte Quellen bleiben unbestätigt, gekürzte Texte werden nicht vervollständigt.
APK SHA-256: `a9c5b2e9bd2dddee2eb02be47ec0d6c1bf92d278b146e261e85745d19815d7a8`.
Signierer identisch mit der bereitgestellten 0.5.1. Kein echter Handytest von 0.5.2.

[CoinBot 0.5.1 herunterladen](https://github.com/jsnuwu/CoinBot/raw/refs/heads/main/releases/CoinBot-Android-0.5.1.apk)

APK aus Quellstand `2da63583d56271ad67bfbd3604ca6d44422f5e35`, Version 0.5.1 / Code 9.
SHA-256: `3bcdf6daab19394521b6c785dba3cdb96f5c2910244ba2172fb8bfc00434e62b`.
Signiert mit dem vorhandenen lokalen Testschlüssel (kein Schlüssel im Repository).

Diese Datei ist für das lokale Update vorgesehen; CI-Artefakte können einen anderen Testschlüssel
verwenden. Bei Android-Signaturkonflikt nicht deinstallieren, da dies lokale Daten löschen würde.
Keine echten Handytests dieser Version. Details, Testumfang und verbleibende Geräteprüfungen stehen
in [android/README.md](../android/README.md#änderungen-in-051-zuverlässige-lokale-entwurfsbilder).

Die APK liegt im Repository, weil die verfügbare GitHub-Verbindung den Release-Upload-Endpunkt
nicht authentifizieren konnte. Es werden keine privaten Signierschlüssel veröffentlicht.
