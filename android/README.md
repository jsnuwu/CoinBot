# CoinBot Android 0.4.1 – Mitteilung → passender Tweet-Link → Entwurf

Version 0.4.1 korrigiert den Umfang von 0.4.0: **Keine regelmäßige Profilbeobachtung.** Eine eigene
X-Handy-Mitteilung ist der einzige automatische Auslöser. Login und Link-Auflösung laufen lokal auf
dem Handy, ohne kostenpflichtige X-API. Die App veröffentlicht weiterhin keine Coins auf Pump.fun.

## Einrichtung

1. Update über die bestehende App installieren. Lokale Signatur, Konto, Entwürfe und Einstellungen
   bleiben erhalten. Die Namenseingabe und ihr vorgemerktes Trennzeichenproblem wurden nicht geändert.
2. **Benachrichtigungszugriff öffnen** und CoinBot Zugriff auf die eigenen X-Mitteilungen geben.
   Bei seitlich installierten Apps kann zusätzlich **Apps → CoinBot → Eingeschränkte Einstellungen
   zulassen** nötig sein. In X bei den gewünschten Accounts die Post-Glocke aktivieren.
3. Falls nötig **X öffnen / anmelden** wählen und direkt bei X anmelden. Ein direkter X-Nutzername-
   Login ist vorgesehen; externe Google-SSO-Seiten werden in CoinBot nicht geöffnet.
4. **Anmeldung abgeschlossen · Auf X-Mitteilungen warten** bzw. auf der Startseite
   **Auf X-Mitteilungen warten** drücken. Den sichtbaren Android-Dienst erlauben.
5. Die **Linksuche ansehen** kann zum ersten Test offen bleiben. Ohne Auftrag bleibt die Ansicht leer.
   Bei einer echten frischen X-Mitteilung wird nur deren Post gesucht. Nach dem Auftrag wartet der
   Bot wieder. Beim Verbinden werden auch noch sichtbare echte Mitteilungen bis fünf Minuten Alter geprüft.
6. Ergebnisse über **Entwürfe und Status öffnen** ansehen. Ein zugeordneter Entwurf enthält Name,
   Ticker, Themenbild, Beschreibung und den Originalpost-Link zum Öffnen/Kopieren.

Dienstbereitschaft bedeutet zunächst nur: Empfänger ist eingeschaltet. Der Bot prüft die X-Anmeldung
nicht mehr durch einen unaufgeforderten Profilbesuch. Erst ein tatsächlicher Linkauftrag bestätigt,
ob die Sitzung lesbar ist. X kann WebView-Logins verweigern; echter Konto-/Gerätetest steht noch aus.

## Link-Auflösung nach einer Mitteilung

Der Empfänger akzeptiert nur `com.twitter.android`, konfigurierte Absender und höchstens fünf Minuten
alte Mitteilungen. Gruppen-Zusammenfassungen und Android-Nachrichtenformate werden vorher ignoriert.
Die Warteschlange enthält Autor, Mitteilungstext, Mitteilungszeit und eine eventuell auslesbare Post-ID.
Sie bleibt im Arbeitsspeicher, ist begrenzt und dedupliziert wiederholte Mitteilungen für fünf Minuten.

- **Post-ID vorhanden:** direkt diesen Post im lokalen angemeldeten WebView öffnen.
- **Keine Post-ID:** einmal das Profil des Absenders laden und den Post anhand der Mitteilung zuordnen.
- Vollständiger Mitteilungstext muss nach Unicode-/Leerzeichen-/URL-Bereinigung übereinstimmen.
  Ein erkennbar gekürzter Text braucht mindestens 40 Zeichen eindeutigen Präfix; bei bekannter ID
  reichen 15 Zeichen. Fehlt Text vollständig, ist eine echte passende Post-ID nötig.
- Autor, Datum, Snowflake-Frische und Zeitraum müssen passen. Bei null oder mehreren Treffern wird
  kein beliebiger neuester Tweet übernommen. Der Post einer Mitteilung kann inzwischen durch einen
  neueren Post überholt sein; der neuere wird nicht als Ersatz verwendet.
- Es entsteht maximal ein Entwurf je Mitteilung und keine weiteren Entwürfe aus anderen Profilposts.
  Einzelne DOM-Proben warten kurz auf das Laden derselben Seite; sie sind keine neuen Profilaufrufe.
- Nach dem Auftrag wird ausschließlich die vorhandene Auftragswarteschlange abgearbeitet. Ohne
  Auftrag kein Seitenaufruf, kein Account-Rundlauf und kein zeitgesteuerter Wiederholungsbesuch.
  Die geladene X-Seite wird danach durch `about:blank` ersetzt, damit ihre Skripte nicht im Leerlauf weiterlaufen.

Übersetzte, zu kurze oder anderweitig abweichende Mitteilungstexte können ohne echte Post-ID nicht
zugeordnet werden. Der geschützte Android-`PendingIntent` wird weiterhin weder ausgelesen noch
automatisch ausgeführt. Ein Profil ist nur ein bedarfsweiser Suchort für die fehlende URL, keine
zweite Quelle für selbst entdeckte Coin-Anlässe. Der Leser klickt keine X-Elemente und liest keine DMs.

Erkennbare Reposts, Zitate, Antworten, Anzeigen und gekürzte Web-Posts bleiben ausgeschlossen.
Ein fremder Anzeigename aus einer Mitteilung allein gilt nicht als verifizierter Post. Die echten
Post-Felder stammen aus der zur Mitteilung passenden X-Webansicht. Der Webleser setzt weiterhin
`source=x_browser_linked`, `twitter`, `source_post`, `source_text`, `source_author`, `source_created_at`
und `publishable=false`. Bilder sind lokale Dateinamen, noch keine veröffentlichten Bild-URIs.

## Entwürfe und Grenzen

Die lesbaren Namen, deterministischen Ticker, Beschreibungen mit Post-Ausschnitt und lokalen
Themenzeichnungen aus 0.4.0 bleiben erhalten. Die Erzeugung ist regelbasiert, keine generative KI
und keine Vorhersage von Aufmerksamkeit/Gewinn. Allgemeine Themen sind möglich; Zugangsdaten werden
verworfen. Alte unverifizierte Entwürfe bleiben als solche erhalten. SQLite/Post-ID verhindern
mehrfache fertige Entwürfe. 20 Entwürfe pro 24 Stunden und 60 Sekunden Abstand bleiben bestehen.

Native App-Logik liest keine Cookies, Passwörter, Tokens oder Formulareingaben. Login bleibt im
normalen lokalen WebView-Sitzungsspeicher; kein eigener Server, keine JavaScript-Bridge, kein Zugriff
auf Dateien/Content-URIs, keine unsicheren TLS-Ausnahmen. **X-Anmeldung lokal löschen** beendet den
Dienst und entfernt CoinBots Web-Cookies/Storage; die eigenständige X-App bleibt getrennt.
**Mitteilungsverarbeitung pausieren** bzw. **Pausieren** in der Android-Mitteilung beendet den Dienst.

Netzwerkfehler beenden den betreffenden Auftrag. HTTP 403/429 verwirft Warteschlangenaufträge und
blockiert neue Linkaufträge für 15 Minuten. Danach gibt es keinen automatischen Wiederholungsbesuch;
erst eine neue echte Mitteilung löst wieder einen Auftrag aus. Eine ungültige Sitzung hält den Dienst
an und fordert zur Anmeldung auf. Abgelaufene Aufträge werden verworfen, nicht als neue Posts behandelt.

Android kann einen wartenden Dienst oder eine nicht sichtbare Webansicht drosseln/beenden. Der Typ
`dataSync` und sein Timeout werden weiterhin respektiert; insbesondere Android 15+ begrenzt diese
Hintergrunddienst-Art auf sechs Stunden pro 24 Stunden. Es gibt keinen Boot-/Zwangsneustart, keinen
bezahlten Ersatzdienst und keine 24/7-/Sekundengarantie. Für den ersten Gerätetest Linksuche offen lassen.

## Verifikation von 0.4.1

137 Python-Tests, 126 Core-, 15 Metadaten-, 26 Browser-Regel-, 25 Mitteilungs-/Zuordnungs- und 18
DOM-Fixture-Prüfungen bestehen; außerdem Ruff, Offline-Demo, SDK-Build und APK-/Signierer-Prüfung.
Die neuen Tests prüfen insbesondere Leerlauf ohne Auftrag, begrenzte Queue, veraltete/doppelte
Mitteilungen, eindeutigen Text-/ID-Abgleich und den Fall, dass ein anderer Tweet inzwischen neuer ist.
Sie ersetzen keinen echten X-Login oder Android-Hintergrund-/Benachrichtigungstest. Die Nutzerdaten
bleiben beim mit gleichem lokalen Schlüssel signierten Update erhalten.

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
