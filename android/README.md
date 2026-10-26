# CoinBot Android 0.4.0 – X-Webanmeldung und verknüpfte Entwürfe

Die App liest ausgewählte öffentliche X-Profile über eine lokal angemeldete X-Webansicht. Sie
bereitet automatische Coin-Entwürfe mit Original-Link vor. Kein Server, kostenpflichtiger X-API-Key,
Sprachmodell-Abonnement oder Wallet-Zugriff wird benötigt. **Keine Veröffentlichung auf Pump.fun.**

## Installation und Anmeldung

1. Die bereitgestellte Update-APK über die bisherige App installieren. Der lokale Download ist
   mit demselben Schlüssel wie 0.3.2 signiert; Einstellungen und Entwürfe bleiben erhalten.
2. Beobachtete Accounts kontrollieren bzw. speichern. Die vorhandene Namenseingabe wurde nicht geändert.
3. **X öffnen / anmelden** wählen. Der laufende Leser pausiert während der Anmeldung. Zugangsdaten
   ausschließlich direkt in der angezeigten X-Webseite eingeben. Ein direkter X-Nutzername-/Passwort-
   Ablauf ist vorgesehen; externe Login-Seiten/Google-SSO werden in diesem Fenster nicht geöffnet.
4. Sobald du bei X angemeldet bist, **Anmeldung abgeschlossen · Beobachtung starten** drücken und
   die Android-Mitteilung für den Dienst erlauben. Der Knopf allein gilt noch nicht als bestätigte
   Anmeldung: Erst der Leser prüft die angemeldete Profilansicht.
5. Für den ersten Test die **automatische X-Leseansicht offen lassen**. Sie wechselt Profile selbst;
   sie ist keine manuell bedienbare X-App. Auf einen neuen Originalpost nach Beobachtungsstart warten.
6. **Entwürfe und Status öffnen** zeigt Ergebnisse. Ein neuer Web-Entwurf enthält Name, Ticker,
   Themenbild, Beschreibung mit Post-Ausschnitt und den **Originalpost-Link öffnen/kopieren**-Button.
   In der Diagnose müssen `Letzte Web-Lesung` und später `Entwuerfe mit Originalpost aus X-Web` steigen.

**Noch nicht mit einem echten Konto oder physischen Android-Gerät geprüft.** Build und lokale
Fixtures beweisen nicht, dass X die Anmeldung im Android-WebView auf deinem Handy erlaubt.
Falls X die Webansicht ablehnt, einen Sicherheitscheck fordert oder keine Posts liefert, wird
keine Anmeldung umgangen. Der Status erklärt fehlende Anmeldung, Lesefehler bzw. fehlende neue Posts.
Der im Webfenster eingegebene Account wird nicht allein durch einen Knopfdruck als verbunden gemeldet.

## Was automatisch läuft

Der sichtbare Android-Vordergrunddienst besucht nur die konfigurierten Profile und liest dort
Post-Text, Status-Link und Datum aus dem öffentlichen Post-DOM. Es gibt keine privaten API-Abfragen,
kein Lesen von DMs, keine Likes, Antworten oder Posts. Auch der Leser klickt keine X-Elemente.
Erkennt die eigene X-App eine Benachrichtigung, kann der vorhandene Empfänger das passende Profil
vorziehen. Dafür ist der alte Benachrichtigungszugriff optional; regelmäßige Web-Prüfungen brauchen ihn nicht.

Profile werden nacheinander besucht, frühestens nach 60 Sekunden erneut. Nach einem Zyklus gibt es
15 Sekunden Abstand; Laden/Anmeldeprüfung kann zusätzliche Zeit benötigen. Bei acht Accounts kann
ein Durchlauf entsprechend mehrere Minuten dauern. Nur neue Posts ab dem aktuellen Dienststart,
höchstens fünf Minuten alt, kommen infrage. Link-Autor muss genau zum ausgewählten Profil passen;
HTML-Datum und Snowflake-Datum müssen plausibel zusammenpassen. Sichtbar gekürzte Texte sowie
erkennbare Reposts, Quotes und Anzeigen bleiben ausgeschlossen. Die DOM-Erkennung ersetzt nicht
sämtliche Informationen einer offiziellen API; unbekannte Layouts können keine Ergebnisse liefern.

Die Entwürfe nutzen vorhandene Themenvorlagen plus lesbare Namen ohne den bisherigen fünfstelligen
Hex-Namensanhang, z. B. `Starship Sprint`. Ticker behalten einen kurzen deterministischen Suffix;
das ist keine Garantie global eindeutiger Ticker. Die Beschreibung enthält einen kurzen tatsächlichen
Post-Ausschnitt mit Quellenhandle und klarer Kennzeichnung als unabhängiges Community-Konzept.
Das PNG zeigt Themenmotiv, Name und Ticker. Es gibt weder generative KI-Bilder noch eine Vorhersage
von Viralität oder Gewinn. Allgemeine Themen bleiben zulässig; Zugangsdaten werden verworfen.

SQLite und Post-ID verhindern doppelte fertige Entwürfe. 20 Entwürfe pro 24 Stunden und 60 Sekunden
Abstand gelten für beide Leser zusammen. Neue Dateien: `files/drafts/POST_ID.png` und `.json` mit
`twitter`, `source_post`, `source_text`, `source_author`, `source_created_at`, `source=x_browser_linked`
und weiterhin `publishable=false`. Das Bildfeld ist ein lokaler Dateiname, noch keine IPFS-Adresse.
Alte `notification_unverified`-Entwürfe werden weder rückwirkend verifiziert noch gelöscht.
Nach Aktivierung des Webmodus erzeugt der Benachrichtigungsleser keine weiteren Entwürfe ohne Link.

## Sitzung, Pause und Android-Grenzen

Die Sitzung verwendet ausschließlich den normalen lokalen WebView-Cookie-Speicher. Native App-Logik
liest weder Cookies noch Passwörter/Tokens, sendet sie nicht an einen eigenen Server und injiziert
keinen JavaScript-Code ins Anmeldefenster. Der Post-Leser exportiert ausschließlich öffentliche
Post-Felder, niemals Eingabewerte, Storage oder Cookies. Es gibt keine native JavaScript-Bridge.
Datei-/Content-Zugriff, gemischte HTTP-Inhalte und fremde Top-Level-Login-Seiten sind gesperrt;
TLS-Fehler werden nicht übergangen. X-Ressourcen und X selbst sehen die normale Web-Sitzung.

**Web-Beobachtung pausieren** bzw. **Pausieren** in der Android-Mitteilung beendet den Dienst.
**Mit gespeicherter Anmeldung starten** startet eine neue Beobachtungsphase. **X-Anmeldung lokal
löschen** entfernt CoinBots Web-Cookies und Web-Storage; die eigenständige X-App bleibt getrennt.
Es gibt keinen heimlichen Neustart bei Boot oder nach Beendigung durch Android.

Der Dienst verwendet den passenden Typ `dataSync` und respektiert dessen Timeout. Android 15+
begrenzt diese Art von Hintergrunddienst insbesondere auf insgesamt sechs Stunden innerhalb von
24 Stunden; App-Nutzung, Geräteversion und Systemregeln beeinflussen die Laufzeit. Außerdem können
Bildschirm-Aus, Energiesparen, Prozessbeendigung und WebView-Drosselung den Leser verzögern oder
stoppen. Eine sichtbare Leseansicht ist für den ersten Test vorgesehen. **24/7- oder Sekundenbetrieb
ist damit nicht nachgewiesen.** Abgelaufene Sitzung/Sicherheitscheck braucht eine manuelle Anmeldung;
HTTP 403/429 löst 15 Minuten Pause aus. Kein bezahlter API-Fallback und keine Zugriffsumgehung.

## Verifikation von 0.4.0

137 Python-Tests, 126 Core-Prüfungen, 15 Benachrichtigungs-Metadatenprüfungen, 26 Browser-Regelprüfungen
und 18 DOM-Fixture-Prüfungen bestehen; außerdem Ruff, Offline-Demo, SDK-Kompilierung, APK-Signatur und
Signierer-Vergleich zur bisherigen lokalen APK. Browser-Regeln prüfen Autor-/Link-/Datumszuordnung,
Frische, Startgrenze, Trunkierung und rein lokale Metadaten. Der DOM-Test prüft tatsächlich das
mitgelieferte Extraktionsskript an synthetischen HTML-Seiten inklusive Login-Gate, Quotes und Reposts.

Diese Tests ersetzen **keinen** echten X-Login, Android-Dienst-/WebView-Test, nachgewiesenen
Hintergrundbetrieb oder eine Pump.fun-Veröffentlichung. Ein echter Anmelde-/Lesetest ist der nächste
Schritt auf dem Handy. Diagnose enthält nur Zustände/Zähler/Handles, keine Zugangsdaten oder
rohe Post-Texte; der gewollte Post-Bezug liegt in den lokalen Entwurfsdateien.

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

## Änderungen in 0.3.1: Link zum Originalpost

Der Post muss seinen eigenen Link nicht im Text enthalten. Liefert X eine strukturierte Post-ID
in der Benachrichtigung, wird daraus lokal `https://x.com/i/web/status/POST_ID` gebildet. Dafür
braucht es weder einen X-API-Key noch eine Netzwerkanfrage. Eine bereits gelieferte eindeutige
Post-URL wird ebenfalls übernommen und von Tracking-Parametern bereinigt.

Neue Entwürfe erhalten neben dem PNG eine lokale JSON-Datei mit den Standard-Metadaten und,
sofern vorhanden, `twitter` und `source_post`. Im Entwurfsfenster kann der Quellenlink geöffnet
werden. Ohne Post-ID/URL werden keine Links erfunden und keine Profil- oder Suchseiten als
Originalpost ausgegeben. Die Zeichenfolge `notification_…` ist ein lokaler Duplikatschlüssel,
keine X-Post-ID. Aus dem bloßen Mitteilungstext lässt sich die Originaladresse nicht zuverlässig
bestimmen. Dieses Update verbessert nicht die von X bereitgestellten Benachrichtigungsdaten.

Bei Ausfall der öffentlichen Einbettung bleibt eine bereits beobachtete passende Post-URL am
unverifizierten Mitteilungsentwurf erhalten. Sie bestätigt weder Inhalt noch Identität und
ändert `notification_unverified` / `publishable=false` nicht. Anders benannten Absendern wird
kein Link zugeordnet. Alle Android-Entwürfe bleiben in dieser Version unveröffentlicht; die
lokalen Bilddateinamen sind noch keine hochgeladenen Bild-URIs.

**137 Python-Tests und 117 Android-Core-Prüfungen**, Ruff, Offline-Demo, APK-Build und Signaturprüfung
bestehen. Die lokale APK verwendet denselben Signierer wie 0.3.0; Datenbankschema und Namenseingabe
bleiben unverändert. Kein echter Handy-/Pump.fun-End-to-End-Test dieser Änderung.

## Änderungen in 0.3.2: fehlende Tweet-Links untersuchen

Der Benutzerbericht zu 0.3.1 enthielt 33 empfangene Meldungen, sechs unverifizierte Entwürfe und
keinen per Link bestätigten Entwurf. Das zeigt keine erfolgreiche Quellenlink-Erfassung. Die
Metadaten-Übernahme in 0.3.1 konnte fehlende Post-IDs nicht beschaffen.

Der Reader berücksichtigt jetzt auch verschachtelte Bundles, Arrays/Listen, freigegebene
Intent-Daten-URIs und die Extras von Benachrichtigungsaktionen. Explizite `post_id`-Felder und
`twitter://status?status_id=…` werden erkannt. Fremde Account-/Benachrichtigungs-IDs werden nicht
als Post-IDs behandelt. Traversierung, Tiefe und Datenmenge sind begrenzt; ein unvollständiger
Scan bestätigt keinen Link. Sammelmeldungen und Nachrichtenformate werden weiterhin vorher
ignoriert. Die Namenseingabe bleibt unverändert.

`Letzte Linkpruefung` zeigt nur Feldtypzähler, die Scanbegrenzung und ob ein X-Öffnungsziel
vorliegt. Sie enthält keine Rohtexte, Links, IDs oder Schlüssel. Das Öffnungsziel ist häufig
ein Android-`PendingIntent`: X kann damit den Post öffnen, CoinBot kann dessen Ziel aber nicht
über eine öffentliche Android-Schnittstelle auslesen. Es wird weder reflektiert noch automatisch
ausgeführt. Ohne auslesbare Post-ID/URL bleibt der Link weiterhin unbekannt.

Das Entwurfsfenster ist scrollbar. Vorhandene Post-URLs lassen sich öffnen oder kopieren;
unbestätigte URLs sind als solche beschriftet. Bei fehlender URL erklärt der Entwurf den Grund.
Bestehende Entwürfe ohne Quellenlink werden durch das Update nicht nachträglich verifiziert.

Ein ergänzender Lesetest der öffentlichen X-Timeline am 26.10.2026 lieferte bei MarioNawfal HTTP 429
und bei Musk nur Posts bis 21.10.2026. Diese Quelle ist daher kein verlässlicher kostenloser Ersatz
für die fehlenden Links und wird nicht als automatischer Android-Fallback hinzugefügt. Keine
bezahlte X-API, kein Server, keine Anmeldung oder Zugriffsumgehung wurde ergänzt.

Verifiziert: **137 Python-Tests, 126 Core-Prüfungen und 15 Prüfungen mit synthetischen Android-
Metadaten**, Ruff, Offline-Demo, APK-Build und Signaturprüfung. Die Testobjekte sind ausschließlich
JVM-Fixtures, nicht Bestandteil der APK und kein Nachweis über tatsächlich von X gelieferte Daten.
Die lokale Update-APK verwendet denselben Signierer wie 0.3.1; Einstellungen und Daten bleiben
erhalten. Auf dem echten Handy muss eine neue X-Mitteilung geprüft und die neue Linkdiagnose
angesehen werden. Die Pump-Anmeldung ist weiterhin nicht implementiert.
