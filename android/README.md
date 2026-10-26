# CoinBot Android 0.3 – Benachrichtigungsempfang und lokale Entwürfe

Diese App ersetzt den öffentlichen X-Timeline-Poller durch Benachrichtigungen der eigenen X-App.
Sie benötigt keinen Server und keinen API-Key. Verarbeitung und Entwürfe bleiben auf dem Handy.
**Diese APK veröffentlicht noch keine Coins. Die Pump.fun-Kontoverbindung fehlt weiterhin.**

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

- Wenn vorhanden: eindeutiger X-Post-Link oder strukturierte Post-ID in den von Android bereitgestellten
  Textfeldern, Tags oder expliziten `tweet_id`/`status_id`-Feldern; kein Auslesen privater
  X-Datenbanken, kein automatisches Öffnen des Telefons und keine Umgehung von Anmeldungen.
- Post höchstens fünf Minuten alt (X-Snowflake-Zeitstempel), Absender aus der eingestellten Liste.
- Einbettung über `publish.x.com/oembed` liefert dieselbe Post-ID und einen passenden Absender.
  Der Einbettungsdienst kann ebenfalls begrenzen oder ausfallen; keine Zusage seiner Verfügbarkeit.
- Vollständiger Text: keine automatische Vorbereitung aus einer erkennbar gekürzten Antwort.
- Allgemeine Themen aus Nachrichten, Politik, Wirtschaft, Technik, Wissenschaft, Sport, Verkehr,
  Wetter und Gesundheit. Zugangsdaten bleiben ausgeschlossen. PNG wird direkt auf dem Gerät gezeichnet.
- SQLite verhindert doppelte Verarbeitung derselben Post-ID über App-Neustarts hinweg.
- Maximal 20 vorbereitete Entwürfe pro Tag mit 60 Sekunden Abstand; Entwürfe älter als 30 Tage
  werden lokal gelöscht. Eine Zugriffssperre von X löst 15 Minuten Wartezeit aus, keine Umgehung.

**Offener Gerätetest:** Es wurde hier kein physisches Android-Gerät und keine echte X-Benachrichtigung
verwendet. Manche X-/Android-Versionen liefern keinen Link in den verfügbaren Feldern. Version 0.2
kann dann aus einem vollständig gelieferten Mitteilungstext und einem zur Liste passenden Titel
einen **unverifizierten lokalen Entwurf** erzeugen. Ein bloßer Anzeigename wie „Elon Musk“ gilt
weiterhin nicht als verifizierter Absender und darf keine automatische Veröffentlichung erlauben. Android-Energiesparen, Netz, X-Zustellung und fehlende Link-Daten können
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

## Änderungen in 0.2

Der erste echte Benutzerbericht bestätigte den Empfang, aber keinen passenden Link. Die bisherige
Link-Pflicht verhinderte dabei jedes Ergebnis. Deshalb ist nun ein separater lokaler Ablauf für
vollständige Mitteilungstexte eingebaut. Titel werden nur zu ausdrücklich ausgewählten Handles
zugeordnet; bekannte Anzeigenamen und explizite Handles werden erkannt, unbekannte/mehrdeutige
Titel sowie gekürzte/fehlende Texte bleiben ausgeschlossen. Es wird kein frei erfundener Post-Link
angelegt. Diese Herkunft bleibt in SQLite als `notification_unverified` erhalten und das Core-Objekt
führt `publishable=false`. Solche Entwürfe sind ausschließlich zur lokalen Ansicht bestimmt.

Absender-Zuordnung anhand eines Titels ist keine Identitätsprüfung. Auch die Vollständigkeit des
Benachrichtigungstexts ist nur anhand vorhandener Kürzungszeichen prüfbar. Nur ein separat bestätigter
Post-Link erhält `post_link_verified`. Ein gespeicherter Entwurf ist weiterhin kein Pump.fun-Coin.

`Diagnose kopieren` enthält Sitzungs-/Zustandsdaten, gewählte Handles, Zähler und Fehlergründe,
aber keine Benachrichtigungstexte, Privatnachrichten, Wallet- oder Sitzungsschlüssel. Sammelmeldungen,
Nachrichtenformate, fehlende Absender und Themenfilter sind nun jeweils sichtbar. Bei erteiltem
Zugriff kann die App den Systemdienst neu verbinden und aktuell sichtbare X-Mitteilungen innerhalb
des Fünf-Minuten-Fensters prüfen. Ältere Posts werden nicht nachgeholt.

Die SQLite-Migration von Version 1 auf 2 fügt nur die Herkunftsspalte hinzu. Bestehende Einstellungen,
Entwürfe und Bilder werden nicht gelöscht. Die hier bereitgestellte lokale Update-APK wird gegen die
vorherige lokale APK auf gleichen Signierer geprüft; bei dieser Datei ist keine Deinstallation nötig.
CI-APKs mit frisch erzeugtem Test-Schlüssel sind davon getrennt (siehe Hinweis zum Signieren oben).

Für Version 0.2 wurden 40 reine Core-Prüfungen ausgeführt. Der vollständige Empfangsablauf und die neue
Titelzuordnung müssen weiterhin auf dem Handy mit einer echten X-Benachrichtigung geprüft werden.

## Änderungen in 0.3: allgemeine Post-Themen

Die Benutzerdiagnose aus 0.2 zeigte empfangene Meldungen und passende Absender, aber zwei Ablehnungen
am Themenfilter. Die feste Liste mit zehn Motiven war zu eng. Version 0.3 behält bekannte Motive,
verwendet aber zusätzlich eine offene Begriffsableitung für sonstige deutsch-/englischsprachige Texte.
Nachrichten, Politik, Sport, Verkehr, Gesundheit, Wirtschaft, Wissenschaft, Technik oder Wetter
brauchen kein vorher bekanntes Meme-Schlüsselwort mehr. Neun redaktionelle Bildmotive und der
abgeleitete Themenbegriff bestimmen das neue Bild. Länder, Orte und einige häufige Organisationen
haben Vorrang; sie sind keine Zugangsliste, andere Begriffe bleiben möglich.

Ein vom Nutzer gelieferter unbestätigter Text über Iran/Israel und Wahlen wurde in beiden
Implementierungen als Politik erkannt (`Iran Brief`, Ticker mit individuellem Suffix). Der Test
bestätigt die Themenverarbeitung, nicht die Tatsachenbehauptungen des Posts. Sachliche Berichte über
Konflikte werden neutral illustriert statt pauschal wegen Wörtern wie „war“ oder „Krieg“ verworfen.
Leere Texte, bloße URLs/Erwähnungen, Zugangsdaten und Texte ohne brauchbaren Begriff bleiben ausgeschlossen.

Das bleibt eine kostenlose lokale Heuristik, kein Sprachmodell und keine Vorhersage von Interesse
oder Kursen. Falsche Schwerpunkte und Kategorisierungen sind möglich. Die Suche nach allen aktuellen
X-Trends ist nicht enthalten. Unbestätigte Mitteilungen bleiben `notification_unverified` und dürfen
weiterhin nicht automatisch veröffentlicht werden. Die Pump-Kontoverbindung ist weiterhin offen.

Version 0.3 hat Datenbankschema 3. Einstellungen, Bilder und fertige Entwürfe bleiben erhalten. Noch
sichtbare frische Mitteilungen, die ausschließlich vom alten Themenfilter abgelehnt wurden, dürfen
mit der neuen Themenlogik einmal erneut geprüft werden. Bereits fertige Entwürfe und Quellenfehler
werden dadurch nicht blind erneut ausgeführt. Die Fünf-Minuten-Grenze bleibt gültig.

**99 Android-Core-Prüfungen**, APK-Build und Signaturprüfung bestehen. Die 14 allgemeinen Themenfälle
werden gemeinsam von Python und Java geprüft (`topic-cases.tsv`). Die Update-Datei wird mit demselben
lokalen Schlüssel wie zuvor signiert. Ein echter Gerätetest der Version 0.3 steht noch aus.
