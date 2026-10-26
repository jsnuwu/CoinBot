# Android UI/UX 0.5.0

Ziel: In wenigen Sekunden erkennen, ob Empfang bereit ist, was als Nächstes zu tun ist und welcher
Entwurf aus der letzten X-Mitteilung entstand. Keine neuen Datenquellen, Profile-Rundläufe,
Wallet-/Pump-Aktionen oder Änderungen am vorgemerkten Account-Eingabeparser.

Die gezeigte illustrative Konzepttafel umfasste Handy-Hochformat und Tablet-Querformat. Der Nutzer
beauftragte anschließend die Fortsetzung und die TradeBot-Prüfung nach Abschluss. Beispielentwürfe
im Konzept sind keine echten Ergebnisse. Die App verwendet ausschließlich tatsächliche lokale
Entwürfe; keine Demo-Daten werden in die Datenbank eingefügt.

## Festgelegte Gestaltung

Navy #101820, Oberfläche #1B2936, Mint #60E4BD, Text #F3F6F4, sekundärer Text #A7B7C4.
Drei Navigationseinträge: Übersicht, Entwürfe, Einstellungen. Empfangsstatus und eine nächste Aktion
stehen vorn. Konfiguration und Diagnose sind nachgeordnet. Ab 720 dp teilt sich die Übersicht in
zwei Spalten; Details und Einstellungen bleiben scrollbar. Der eingebettete X-Inhalt ist eine
externe Website und erhält keine manipulierte Anmeldung oder injizierte Design-Skripte.

## Wahrheitsbedingungen

- Grün bedeutet tatsächliche Empfangsbereitschaft, nicht bestätigte X-Anmeldung oder Profit.
- Aktive Suche wird nur bei einem realen laufenden Auftrag angezeigt.
- X-Anmeldung heißt ohne lesbare X-Seite noch nicht geprüft; abgelaufene Anmeldung benötigt Eingriff.
- Ein bestätigter Quellenstatus braucht einen echten Post-Link und bekannten Herkunftsstatus.
  `notification_unverified` ist ausdrücklich kein bestätigter Status.
- Fehlende Quellenlinks werden nicht mit Ersatz- oder Suchlinks kaschiert.
- Der Erhalt von `publishable=false` und fehlende Pump-Veröffentlichung bleiben sichtbar.

## Bedienung und QA

Mindestens 48-dp-Aktionen, lesbare Systemschrift, beschriftete Zustände, auswählbare Quelltexte,
TalkBack-Button-Rollen und systemabhängige kurze statt dauerhafter Animationen. Tabwechsel erhalten
unfertige Account-Eingabe. Gespeicherte Accounts werden weiterhin mit dem vorhandenen Core-Parser
validiert. Ein API-/Datenbankfehler wird nicht durch positives Styling als Erfolg gezeigt.

18 reine UX-Zustandsprüfungen testen Berechtigungspriorität, getrennte Pause/Unterbrechung,
Anmeldebedarf, tatsächliche laufende Suche, Quellenkennzeichnung und relative Zeittexte. Bestehende
Matching-/Quellen-/DOM-Tests laufen weiter. Kein physisches Android-Gerät/Emulator verfügbar:
Touch, Tastatur, TalkBack, 200-%-Schrift und konkrete Pixel-/Screenshot-Abnahme sind noch offen.
Das generierte Bild ist ein Entwurf und kein Screenshot der implementierten App.
