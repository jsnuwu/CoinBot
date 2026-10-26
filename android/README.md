# CoinBot Android 0.6.0 – lokale Bilder und übersichtliche Entwürfe

Version 0.4.1 korrigiert den Umfang von 0.4.0: **Keine regelmäßige Profilbeobachtung.** Eine eigene
X-Handy-Mitteilung ist der einzige automatische Auslöser. Login und Link-Auflösung laufen lokal auf
dem Handy, ohne kostenpflichtige X-API. Die App veröffentlicht weiterhin keine Coins auf Pump.fun.

## Oberfläche und Einrichtung

Die untere Navigation trennt **Übersicht**, **Entwürfe** und **Einstellungen**. Die Übersicht zeigt
Zustand und nächste Aktion: Mitteilungszugriff erlauben, Dienst-Mitteilung erlauben, bei X anmelden,
Empfang starten, verbinden oder pausieren. Neue Nutzer werden so durch die Einrichtung geführt.
Bestehende Web-Sitzungen werden nicht bloß wegen des Updates als ungültig behandelt.

1. Die Update-APK über 0.4.1 installieren, sofern Android den Signierer akzeptiert (siehe Update-Hinweis zu 0.5.1 unten). Nicht vorher deinstallieren. Konto, Einstellungen und Entwürfe bleiben beim Update erhalten.
2. Auf der Übersicht die angebotene nächste Aktion ausführen. Die Anmeldung bleibt direkt in der
   X-Webseite. Nach **Empfang starten** kehrt die App zur Übersicht zurück.
3. In der eigenen X-App bei den eingestellten Accounts die Post-Glocke aktivieren.
4. Unter **Einstellungen → X-Accounts** werden die bekannten Nutzernamen gepflegt. Textänderungen
   bleiben beim Tabwechsel erhalten und werden erst nach **Accounts speichern** übernommen.
   Der vorgemerkte Parser-/Trennzeichenfehler wurde nicht geändert.
5. Ein echter Mitteilungsauftrag aktualisiert die Übersicht und später **Entwürfe**. Die Liste lässt
   sich nach **Alle**, **Mit Post-Link** und **Unbestätigt** filtern. Ein Entwurf öffnet Bild, Ticker,
   Originalpost-Link sowie – wenn vorhanden – tatsächlichen Quelltext. Quelle und lokaler Entwurf
   bleiben sichtbar getrennt von einer Veröffentlichung.
6. Technische Zustände und Zähler stehen unter **Einstellungen → Diagnose ansehen**; dort können sie
   kopiert werden. **Linksuche ansehen** ist ein optionales Kontrollfenster mit klarer Warteansicht
   statt einer scheinbar kaputten leeren Seite. Der Empfang startet weiterhin nur nach Mitteilungen.

**Design und Bedienbarkeit:** dunkle Navy-Oberflächen, Mint-Akzent, klare Typografie, große
Berührungsflächen (mindestens 48 dp), Statusbeschriftung zusätzlich zu Farben, scrollbare Details,
Bildvorschau-Cache und gezielte Aktualisierung statt Neuaufbau der Einstellungen alle zwei Sekunden.
Es gibt keine dauerhaft blinkende Aktivität im Leerlauf. Kurze Navigationsübergänge respektieren
Androids abgeschaltete Animationen. Große Ansichten erhalten zwei Spalten auf der Übersicht.
X-Sitzung löschen erfordert eine Bestätigung und zeigt danach den erneuten Anmeldebedarf.

Die Anmeldung wird nur nach einer echten lesbaren X-Seite als zuletzt beobachtet markiert. Der
Knopf **Empfang starten** allein bestätigt weder die Anmeldung noch einen erfolgreich gelesenen
Post. Ein verlorener Android-Dienst, fehlende Zugriffe, X-Sperren und normale Wartebereitschaft
haben getrennte Zustände. Nach Ablauf einer Wartefrist zeigt die UI keine endlose alte Sperre an.

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
**Empfang pausieren** bzw. **Pausieren** in der Android-Mitteilung beendet den Dienst.

Netzwerkfehler beenden den betreffenden Auftrag. HTTP 403/429 verwirft Warteschlangenaufträge und
blockiert neue Linkaufträge für 15 Minuten. Danach gibt es keinen automatischen Wiederholungsbesuch;
erst eine neue echte Mitteilung löst wieder einen Auftrag aus. Eine ungültige Sitzung hält den Dienst
an und fordert zur Anmeldung auf. Abgelaufene Aufträge werden verworfen, nicht als neue Posts behandelt.

Android kann einen wartenden Dienst oder eine nicht sichtbare Webansicht drosseln/beenden. Der Typ
`dataSync` und sein Timeout werden weiterhin respektiert; insbesondere Android 15+ begrenzt diese
Hintergrunddienst-Art auf sechs Stunden pro 24 Stunden. Es gibt keinen Boot-/Zwangsneustart, keinen
bezahlten Ersatzdienst und keine 24/7-/Sekundengarantie. Für den ersten Gerätetest Linksuche offen lassen.

## Verifikation von 0.5.0

137 Python-Tests, 126 Core-, 15 Metadaten-, 26 Browser-Regel-, 25 Mitteilungs-/Zuordnungs- und 18
DOM-Fixture- sowie 18 UX-Zustandsprüfungen bestehen; außerdem Ruff, Offline-Demo, SDK-Build und APK-/Signierer-Prüfung.
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

## Änderungen in 0.5.1: zuverlässige lokale Entwurfsbilder

Die kostenlose Grundlage bleibt Android Canvas, ohne externe Bild-API oder KI-Abonnement.
19 Themenmotive erhalten individuelle Farbpaletten, drei Hintergrundkompositionen und drei
Dekorationsarten. Der tatsächlich vorhandene Quelltext wählt zusätzlich Flugzeug statt Zug,
Krypto-Münzen statt Balkendiagramm, Schnee/Sonne statt Regenwolke oder eine Wahlurne statt Parlament.
Das ist eine lokale Heuristik, keine semantisch fehlerfreie KI. Name/Ticker sind ergänzende Beschriftung;
das Hauptmotiv wird gezeichnet. Bestehende gültige Bilder werden beim Update nicht ersetzt.

PNG und JSON werden im internen App-Speicher zuerst temporär geschrieben, synchronisiert und atomar
ersetzt. Vor Erfolg prüft CoinBot PNG-Signatur, Abmessungen (768 × 768), Chunk-Prüfsummen, Dateiende
und Android-Decodierung; eine fehlgeschlagene PNG-Komprimierung ist ein Fehler. Metadaten enthalten
Bilddateiname, Bildstatus und bei neu gezeichneten Bildern Generator, Thema, konkretes Motiv und Größe.
Quelltext und Quellenbelege werden vor der Bilderstellung gespeichert. Scheitert diese, bleibt der
Entwurf zur Reparatur erhalten; ein Speicherfehler erzeugt keinen zusätzlichen Quellen-Fallback.

Beim Öffnen der App/Detailansicht und beim Verbinden des Mitteilungsempfängers prüft ein lokaler
Hintergrundauftrag bestehende Entwürfe. Fehlende oder beschädigte Bilder werden aus deren gespeicherten
Feldern und vorhandenem Quelltext ergänzt, ohne X-Abfrage, neue Entwürfe oder Vorschlagslimit-Verbrauch.
Namen, Ticker, Beschreibung, Zeit, Quellenlinks und Verifizierungsstatus bleiben erhalten. Fehlende
JSON-Dateien werden aus vorhandenen Daten rekonstruiert; verlorene Originaltexte sind nicht wiederherstellbar.
Eine beschädigte vorhandene JSON-Datei wird wegen möglicher Quellenbelege **nicht überschrieben**:
Das Bild kann ergänzt werden, aber die Metadaten bleiben sichtbar fehlerhaft. Diagnosezähler und
Detailansicht zeigen Fehler; dort lässt sich die Prüfung erneut starten. Cache-Revisionen aktualisieren
reparierte Vorschaubilder. Die Detailansicht bietet eine zusätzliche Großansicht. Es wird kein
Bilderfolg allein aus einem vorhandenen Datenbankeintrag abgeleitet.

Schema 4 ergänzt ausschließlich Bildstatus und Bildrevision. Einstellungen, lokale X-Anmeldung und
Entwürfe bleiben erhalten. Die acht gewünschten Accounts sind Vorgabe für Neuinstallationen;
bestehende gespeicherte Account-Auswahl wird nicht überschrieben. Der zurückgestellte Komma-Parser
bleibt unverändert. Keine Profil-Rundläufe, X-API, Pump-Veröffentlichung oder Wallet-Aktivierung.

Verifiziert: 137 Python-Tests, Ruff, Offline-Demo, 126 Core-, 15 Mitteilungsmetadaten-, 26 Browser-Regel-,
25 Mitteilungszuordnungs-, 18 UI-Zustands- und 18 DOM-Fixture-Prüfungen sowie 37 neue Datei-/Reparatur-
und Motivprüfungen. `bash android/test-assets.sh` nutzt Host-PNGs, eine In-Memory-Datenbank und eine
SHA-256-fixierte JSON-Testbibliothek; es prüft Dateifehler und Metadatenerhalt, **nicht** Android Canvas,
SQLite-Migration auf dem Handy oder die tatsächliche UI. Die APK kompiliert gegen Android 36.

Der vorhandene lokale `android/build/debug.jks` wird weiterverwendet; das Zertifikat stimmt mit der
vor dem Build vorhandenen lokalen APK überein (SHA-256
`32c5dc55280e82cafdd10d731b142ed73fd033f6b7be56073ba44964e1895497`). Eine 0.4.1-APK liegt
hier nicht separat zum erneuten Vergleich vor. Android akzeptiert ein Update nur bei gleichem
Signierer. Bei Signaturkonflikt **nicht deinstallieren**, da sonst lokale Daten verloren gehen.
CI-Artefakte mit frisch erzeugtem Testschlüssel sind keine Ersatz-Update-Datei.

Auf echtem Android noch prüfen: Update von der installierten 0.4.1, Erhalt von Einstellungen/Sitzung/
Entwürfen, automatische Ergänzung alter fehlender Bilder, neue echte X-Mitteilung → Motiv/PNG/JSON →
Vorschau/Großansicht, Verhalten bei vollem Speicher sowie Neustart während der Speicherung.
Es fand kein echter Handy-, Login- oder Pump.fun-Test statt.


## Änderungen in 0.5.2: Entwurf trotz fehlendem Web-Treffer

Die Handy-Diagnose von 0.5.1 zeigte einen aktiven Empfang und ein lesbares Profil, aber keinen
eindeutigen Mitteilungs-Post. Im Webmodus kehrte der Empfänger früh zurück und der Browser verwarf
solche Aufträge ohne den sonst vorhandenen lokalen Mitteilungsentwurf. Das war ein Ablaufdefekt,
kein gemeldeter PNG-Fehler (17 Bildreparaturen / 0 Bildfehler bedeuten nicht 17 neue Posts).

Nach erfolglosem Abgleich auf einer lesbaren Seite, leerer Post-Liste, Netz-/HTTP-Fehler oder bereits
aktiver Zugriffssperre wird jetzt ein lokaler Entwurf aus dem **vollständigen Original-Mitteilungstext**
geprüft. Er erhält Name, Ticker, Beschreibung und Bild, aber `notification_unverified`,
`source_verified=false` und `publishable=false`. Ohne tatsächlich auslesbare Post-ID wird kein Link
erzeugt. Ein beobachteter Link bleibt unbestätigt. Gekürzter/fehlender Text, unbekannte Absender,
veraltete Aufträge und vertrauliche Inhalte liefern keinen Ersatzentwurf. Die Anmeldehürde bleibt
ein separater Stopp; es gibt keine Umgehung und keinen zusätzlichen Profilbesuch.

Die begrenzte lokale Warteschlange und SQLite-Schlüssel verhindern wiederholte Entwürfe desselben
Mitteilungstexts. Auch der Ersatzweg hält 20 Entwürfe pro Tag / 60 Sekunden Mindestabstand ein.
Blockierte Vorbereitungen werden ausdrücklich als Vorschlagslimit gemeldet. Die Diagnose zählt
fehlende auslesbare Post-Links jetzt auch im Webmodus. Alte nicht gespeicherte Handy-Mitteilungen
können aus dieser Diagnose nicht nachträglich rekonstruiert werden.

Verifiziert: 137 Python-Tests, 126 Core-, 15 Metadaten-, 26 Browser-, **35 Mitteilungs-/Zuordnungs-**,
18 UI-, 18 DOM- und 37 Datei-/Reparatur-/Motivprüfungen, Ruff, Offline-Demo und APK-Build.
Die zehn neuen Regeln prüfen vollständige/gekürzte/veraltete Mitteilungen, fehlenden Text, Erhalt des
Originaltexts, Deduplizierung und unbestätigte Metadaten ohne erfundenen Link. Host-/Fixturetests
beweisen keine echte Android-WebView-/Dienstzustellung. Kein Handytest dieser Version.
Die Update-APK verwendet denselben verfügbaren lokalen Signierer wie die bereitgestellte 0.5.1;
Einstellungen, Sitzung, Datenbankschema und Entwürfe bleiben unverändert.


## Änderungen in 0.6.0: surreale Meme-Szenen und genauerer Link-Abgleich

Die bisherigen Canvas-Kategoriezeichen waren kein ausreichender Meme-Bildstil. Neue Entwürfe
verwenden deshalb 20 vollständige surreale Szenen: unter anderem Raketenfrosch, Koffein-Waschbär,
Pizza-Katzenkaiser, schmelzender Roboter-Gärtner, Börsen-Bulle/Bär, Pilz-Parlament, Universums-Axolotl,
Croissant-Zug und fliegender Koi. Keine Ticker-/Titelbalken und keine Kategorie-Icons im Bild.

Die zwei Bildtafeln wurden einmalig als Illustrationen erzeugt und unverändert mit der APK gebündelt.
`MemeScene` wählt anhand des lokal erkannten Themas und vorhandenen Quelltexts eine Szene aus.
Android Canvas komponiert daraus einen 768×768-Ausschnitt mit kleiner Ausschnitt-, Farb- und
Lichtvariation. **Kein neues KI-Bild pro Post**, keine beliebig offene Text-zu-Bild-Generierung:
Szenen können sich bei ähnlichen Themen wiederholen. Der laufende Betrieb benötigt keine Bild-API,
kein Bild-Abo, keinen Download und keinen Modellserver. Motivwahl bleibt eine begrenzte Heuristik.
Die Bildmetadaten nennen `local_surreal_compositor_v3`, Szene und Quelldatei ehrlich.

Neue Entwürfe und die Reparatur fehlender Bilder verwenden den neuen Stil. Vorhandene gültige Bilder
werden nicht ungefragt ersetzt. **Einstellungen → Vorhandene Bilder im Meme-Stil erneuern** gestaltet
bestehende Entwürfe lokal neu; alternativ gibt es in der Detailansicht eine Aktion für genau ein Bild.
Das erste vorherige PNG und dessen JSON werden als `.before-v3.png` / `.before-v3.json` gesichert
und bei wiederholter Gestaltung nicht überschrieben. Namen, Texte, Links und Bestätigungsstatus
werden nicht geändert. Bei kaputten bestehenden JSON-Quellenbelegen wird die Umgestaltung verweigert.
Die bisherigen 30-Tage-Löschregeln erfassen auch Bildsicherungen.

Warum oft kein Link entsteht: Android liefert häufig keine auslesbare Post-ID, sondern nur ein
nicht öffentlich auslesbares `PendingIntent`. Der Web-Abgleich braucht dann eindeutig passenden
Autor, Text und Zeitraum. Außerdem las der bisherige DOM-Reader Emoji-Bilder nicht mit und verwarf
jeden Quote-Post pauschal. Version 0.6.0 übernimmt Emoji-Alternativtexte, normalisiert reine
Darstellungszeichen/typografische Apostrophe und prüft bei Quote-Posts ausschließlich den eigenen
äußeren Text samt eigenem Link/Zeitstempel. Verschachtelte Quote-Texte/-Links werden nie dem äußeren
Post zugeordnet; reine Quote-Karten ohne eigenen Text liefern keinen bestätigten Treffer.
Antworten/Reposts/Anzeigen/erkennbare Edits bleiben ausgeschlossen. Keine unscharfe Textähnlichkeit,
kein neuester Ersatzpost, kein Profil-Rundlauf und keine Anmeldungsschutz-Umgehung.

Die Diagnose zeigt `Letzter Webabgleich` mit Anzahl gelesener DOM-Posts, ausgeschlossener Kontexte,
gekürzter Texte, gültiger Posts und passender Texte/IDs. Sie enthält dafür keine Rohtexte oder Tokens.
Das verbessert die Fehlersuche, belegt aber nicht, warum die zwei früher gemeldeten Posts konkret
nicht gefunden wurden. Eine höhere echte Linkquote ist ohne neuen Handytest noch nicht nachgewiesen.

Verifiziert: 137 Python-Tests, 126 Core-, 15 Mitteilungsmetadaten-, 26 Browser-Regel-, 40 Zuordnungs-,
18 UI-, 25 DOM-, 44 Datei-/Reparatur- und 80 Bildquellen-/Motivrouting-/Ausschnittprüfungen.
Ruff, Offline-Demo, APK-Build und Signaturprüfung bestehen. Bildquellen wurden visuell angesehen;
Host-ImageIO/Datei-/DOM-Fixtures ersetzen weder Android Canvas noch eine echte X-Mitteilung.
Die APK verwendet denselben lokalen Signierer wie die bereitgestellte 0.5.2.
Noch am Handy prüfen: Linkquote mit neuen Mitteilungen samt Abgleichdiagnose, Quote-/Emoji-Fälle,
Meme-Vorschau/Großansicht, Umgestaltung alter Bilder mit Daten-/Quellenerhalt und Speicherausfälle.
Keine Pump-Veröffentlichung, Wallet-Anbindung oder Änderung des zurückgestellten Komma-Parsers.
