# CoinBot Android test builds

[CoinBot 0.7.0 herunterladen](https://github.com/jsnuwu/CoinBot/raw/refs/heads/main/releases/CoinBot-Android-0.7.0.apk)

Version 0.7.0 / Code 12: lokaler Szenengenerator mit Figuren, Posen, Bühnen und Requisiten.
Dauerhafte Bildhistorie prüft gleiche Pixel, gleiche Komposition und ähnliche Bildstruktur;
nach 24 ungeeigneten Kandidaten wird kein neues Bild gespeichert. Keine Bild-API oder Abo.
Alte Bilder können mit Sicherung umgestaltet werden. Keine unbegrenzte oder weltweite Ähnlichkeitsgarantie.
APK rund 14 MB; SHA-256 `ca379e4974ea2a1118b512ac4f459ca23e188c84a2a88a730137ecfe381bbe87`.
Signierer identisch mit der bereitgestellten 0.6.0. 137 Python-Tests, Auswahl-/DOM-/Datei-/Historien-
und Generator-Referenzprüfungen, Ruff, Offline-Demo und APK-Build bestanden. 20 Bilder desselben Themas
im Referenztest nacheinander erzeugt und geprüft. Referenzrenderer ist Java2D, keine echte Android-
Grafik-/Handyprüfung. Vorhandene App nicht deinstallieren; ihre Daten enthalten auch die Bildhistorie.

[CoinBot 0.6.0 herunterladen](https://github.com/jsnuwu/CoinBot/raw/refs/heads/main/releases/CoinBot-Android-0.6.0.apk)

Version 0.6.0 / Code 11 enthält 20 gebündelte surreale Meme-Szenen und korrigiert
Emoji-/Quote-Text-Verluste im gezielten X-Webabgleich. Keine neue KI-Generierung pro Post;
Szenen können sich wiederholen. Alte Bilder lassen sich mit Sicherung umgestalten.
APK: rund 7,9 MB; SHA-256 `d7fba608c983e44f958a30e1167651759ae2077103ad4adb049352633581d7ae`.
Signierer identisch mit 0.5.2. 137 Python-Tests und 374 Android-/Datei-/Fixture-Prüfungen bestehen;
keine tatsächlichen Handytests oder nachgewiesen höhere echte Linkquote.

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
