# CoinBot Android – erster Empfangstest

Diese App ersetzt den öffentlichen X-Timeline-Poller durch Benachrichtigungen der eigenen X-App.
Sie benötigt keinen Server und keinen API-Key. Verarbeitung und Entwürfe bleiben auf dem Handy.
**Diese erste APK veröffentlicht noch keine Coins. Die Pump.fun-Kontoverbindung fehlt weiterhin.**

## Installation und Einrichtung

1. Die bereitgestellte `CoinBot-Android.apk` installieren. Android fragt dabei nach Erlaubnis zur
   Installation aus dieser Quelle. Die APK ist eine signierte Testversion, keine Play-Store-App.
2. CoinBot öffnen → **Benachrichtigungszugriff öffnen** → Zugriff für CoinBot erlauben.
   Bei seitlich installierten Apps kann Android zunächst **Eingeschränkte Einstellung** anzeigen.
   Dann in **Android-Einstellungen → Apps → CoinBot → Menü → Eingeschränkte Einstellungen zulassen**
   freigeben und die Zugriffsseite erneut öffnen. Bezeichnungen hängen von Android/Hersteller ab.
3. Die gewünschten X-Nutzernamen einstellen (Vorgabe Trump und Musk), Beobachtung einschalten und speichern.
4. In X den gewünschten Accounts folgen und die Post-Glocke auf alle Posts stellen. X muss selbst
   Benachrichtigungen senden dürfen. Auf einen echten neuen Post warten.

Die Statusanzeige zeigt, ob X eine verwertbare Meldung geschickt hat, ob der Link fehlt, ob die Quelle
abgewiesen wurde oder ein lokaler Entwurf vorliegt. Es gibt keine Eingabe für eigene Coin-Ideen.
Die App liest nur Meldungen des Pakets `com.twitter.android`; andere Apps werden sofort ignoriert.
Direkte Nachrichten im Android-Nachrichtenformat und Gruppen-Zusammenfassungen werden verworfen.
Unverwertbare Benachrichtigungstexte werden nicht gespeichert und nicht an einen Server geschickt.

## Was tatsächlich geprüft wird

- Eindeutiger X-Post-Link oder strukturierte Post-ID in den von Android bereitgestellten Textfeldern,
  Tags oder expliziten `tweet_id`/`status_id`-Feldern; kein Auslesen privater
  X-Datenbanken, kein automatisches Öffnen des Telefons und keine Umgehung von Anmeldungen.
- Post höchstens fünf Minuten alt (X-Snowflake-Zeitstempel), Absender aus der eingestellten Liste.
- Einbettung über `publish.x.com/oembed` liefert dieselbe Post-ID und einen passenden Absender.
  Der Einbettungsdienst kann ebenfalls begrenzen oder ausfallen; keine Zusage seiner Verfügbarkeit.
- Vollständiger Text: keine automatische Vorbereitung aus einer erkennbar gekürzten Antwort.
- Passendes lokales Motiv, sensible Inhalte ausgeschlossen. PNG wird direkt auf dem Gerät gezeichnet.
- SQLite verhindert doppelte Verarbeitung derselben Post-ID über App-Neustarts hinweg.
- Maximal 20 vorbereitete Entwürfe pro Tag mit 60 Sekunden Abstand; Entwürfe älter als 30 Tage
  werden lokal gelöscht. Eine Zugriffssperre von X löst 15 Minuten Wartezeit aus, keine Umgehung.

**Offener Gerätetest:** Es wurde hier kein physisches Android-Gerät und keine echte X-Benachrichtigung
verwendet. Manche X-/Android-Versionen liefern keinen Link in den verfügbaren Feldern. Dann meldet
die App genau dies und verarbeitet den Post nicht. Ein bloßer Anzeigename wie „Elon Musk“ gilt nicht
als verifizierter Absender. Android-Energiesparen, Netz, X-Zustellung und fehlende Link-Daten können
den Ablauf verzögern oder ganz verhindern. Ein Sekunden-Autobot ist mit der APK noch nicht nachgewiesen.
Ein automatisches Ranking aller gerade trendenden X-Posts ist ebenfalls nicht enthalten; es werden
die persönlich gewählten Accounts beobachtet.

Die spätere Pump.fun-Anbindung soll den kostenlosen Website-Ablauf nutzen. Dafür muss erst das
Besitzerkonto verbunden und der echte Ablauf geprüft werden. Diese APK enthält keinen Wallet-Signer,
keinen privaten Schlüssel und keine Pump-Zugangsdaten. Sie sendet keine Solana-Transaktionen.

## Bauen und testen

JDK 17+, Android SDK Platform 36 / Build Tools 36.0.0, `zip`, Linux:

```bash
export ANDROID_HOME=/path/to/android-sdk
bash android/build.sh
```

Der Build benötigt keine Gradle-Installation und keine Drittanbieter-Laufzeitbibliotheken. Er kompiliert
die Java-App, führt die Core-Prüfungen aus, erstellt DEX/Manifest, richtet die APK aus und signiert sie.
Ausgabe: `android/build/CoinBot-Android.apk`. GitHub Actions baut dieselbe APK als Download-Artefakt.

Das generierte Test-Keystore liegt ausschließlich unter `android/build/` und wird nicht veröffentlicht.
Ein frischer CI-Lauf erzeugt einen neuen Test-Schlüssel. Ein Update mit anderem Schlüssel kann eine
Deinstallation der alten Test-App erfordern; dabei gehen lokale Entwürfe und Einstellungen verloren.
Für spätere dauerhafte Installationen muss ein eigener stabiler Release-Schlüssel eingerichtet werden.

Benachrichtigungszustellung, Android-Oberfläche und tatsächliche X-Link-Verfügbarkeit müssen auf dem
Gerät getestet werden. Die JVM-Prüfungen prüfen ausschließlich Auswahl-, Zeit-, Quellen- und Themenregeln.
