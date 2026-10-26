# X → Pump Launch Bot

Eigenständiger Bot, getrennt von TradeBot. Der aktuelle Render-Blueprint startet eine **kostenlose
Preview-Webseite** mit experimenteller Beobachtung öffentlicher X-Einbettungsansichten. Er bereitet
Meme-Namen, Ticker, Beschreibung und PNG vor. Kein API-Key und keine Wallet erforderlich.

**Ein verlässlicher 24/7-Sekunden-Launcher mit echten Onchain-Tokens für 0 € ist damit nicht erreicht.**
Render Free schläft bei Inaktivität ein, X garantiert die öffentliche Ansicht nicht, und echte
Solana-Veröffentlichungen können Gebühren kosten. Der ursprüngliche Onchain-Weg bleibt deaktiviert. Ein zusätzlich vorbereiteter kostenloser
Website-Adapter benötigt noch eine echte Pump-Kontoanmeldung und externe Verifikation.
Die frühere CLI-Launch-Pipeline bleibt als getrennte, standardmäßig deaktivierte Funktion im Code.

Token-Erstellung ist kein Geschäftsmodell mit garantierten Einnahmen. Ein Launch erzeugt weder Käufer
noch Handelsvolumen. Dieses Projekt enthält keine künstlichen Käufe, Eigenkäufe, Volumenmanipulation,
Verkaufsstrategie oder automatische Creator-Fee-Abholung.

## Android-Anbindung für den eigenen X-Benachrichtigungsempfang

Unter [`android/`](android/README.md) liegt die Android-App **0.4.0** (Android 8+). Sie bietet jetzt
zusätzlich eine lokale X-Webanmeldung: Der Nutzer meldet sich direkt auf der X-Seite im WebView an.
Ein sichtbarer, pausierbarer Android-Dienst besucht die ausgewählten Profile und liest neue Posts
mit Text, Autor, Datum und Original-Link. Dafür wird kein kostenpflichtiger X-API-Key verwendet.
Anmeldedaten und Web-Sitzung verbleiben im Android-WebView; keine Cookie-/Token-Extraktion,
keine native JavaScript-Bridge und keine Übertragung an Render. Die App tätigt keine X-Aktionen.

Nach Anmeldung: **„Anmeldung abgeschlossen · Beobachtung starten“** drücken, Android-Mitteilung
für den Dienst erlauben und zum ersten Test die automatische Leseansicht offen lassen. Neue
Entwürfe erhalten einen lesbaren Namen, einen deterministischen Ticker, eine Beschreibung mit
Post-Bezug, ein lokal gezeichnetes Themenbild und den Quellenlink in `twitter` / `source_post`.
Die Begriffs-/Bildableitung bleibt regelbasiert, kein Sprachmodell und keine Viralitätsprognose.
Die App enthält keine manuelle Coin-Erstellung, keinen Wallet-Signer und keine Pump-Veröffentlichung.

**Experimentell, noch kein echter Konto-/Gerätetest der Web-Anbindung:** X kann WebView-Anmeldungen
ablehnen, Bestätigungen verlangen oder die Webseite ändern. Android kann eine nicht sichtbare
Webansicht drosseln bzw. den Dienst beenden. Der Dienst respektiert die `dataSync`-Zeitlimits
(neuere Android-Versionen begrenzen insbesondere Hintergrundlaufzeit); keine 24/7-/Sekundenzusage.
Neue Posts kommen nur ab dem aktuellen Start und innerhalb von fünf Minuten in Betracht.
Profile werden nacheinander besucht (mindestens 60 Sekunden je Profil); erfolgreiche Zyklen haben
15 Sekunden Abstand. Mitteilungen können ein Profil vorziehen. Es gelten weiterhin 20 Entwürfe
je 24 Stunden und 60 Sekunden Mindestabstand. Bei HTTP 403/429 pausiert der Webleser 15 Minuten.

Die ursprüngliche Benachrichtigungsdiagnose bleibt erhalten. Nach Aktivierung des Webmodus erzeugt
sie keine neuen unverifizierten Textentwürfe; Mitteilungen dienen dann nur noch als Leseauslöser.
Vorhandene Entwürfe und Einstellungen bleiben erhalten. Der neue Herkunftsstatus `x_browser_linked`
unterscheidet beobachtete Web-Posts von alten `notification_unverified`-Entwürfen. Die zuletzt
gelesene Profilansicht, Lesungszeit und Anzahl verknüpfter Entwürfe stehen getrennt in der Diagnose.

Verifiziert: **137 Python-Tests, 126 Core-, 15 Metadaten-, 26 Browser-Regel- und 18 DOM-Fixture-Prüfungen**,
Ruff, Offline-Demo, APK-Build und Signaturprüfung. Fixtures sind kein Beweis erfolgreicher Anmeldung
oder tatsächlicher X-Zustellung. Die lokal bereitgestellte Update-APK verwendet denselben Signierer
wie 0.3.2. CI-APKs können einen anderen Test-Schlüssel verwenden. Details: [Android-Anleitung](android/README.md).

## Kostenlos ausprobieren

Python 3.12+, Linux/macOS:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.lock
pip install --no-deps -e .
xpump demo
pytest -q
ruff check .
```

`demo` verarbeitet einen synthetischen Post und schreibt PNG + JSON nach `data/demo/previews/`.
Es nutzt **keine APIs, keine Wallet und kein KI-Abonnement**, selbst wenn die Konfiguration bereits
Live-Einstellungen enthält. Installation lädt Python-Pakete herunter. Demo-, Preview- und Live-Zustand
sind in getrennten Unterordnern abgelegt. Generierte Dateien sind von Git ausgeschlossen.

## Kostenlose Website lokal starten

```bash
XPUMP_FREE_HOSTING=true XPUMP_PUBLIC_X_ACCOUNTS=realDonaldTrump:25073877,elonmusk:44196397 XPUMP_PUBLIC_X_POLL_SECONDS=60 xpump web
```

Danach `http://localhost:8000` öffnen. Optional `PORT` setzen. Ohne `XPUMP_PUBLIC_X_ACCOUNTS` bleibt
die Beobachtung aus. Die Website enthält keine Eingabefelder und keinen Endpunkt zum manuellen Erstellen von Vorschlägen.
Die automatische Beobachtung prüft standardmäßig alle 120 Sekunden. Die Website zeigt Prüfzeit,
gelieferte Post-Zeit und Quellenfehler getrennt an. Die Website zeigt ausschließlich automatisch verarbeitete Posts und daraus entstandene Entwürfe.
PNG und Metadaten bereits vorbereiteter Entwürfe lassen sich herunterladen.
Neue Metadaten enthalten den aus der Post-ID gebildeten Originalpost-Link sowohl als `source_post`
als auch im üblichen Social-Feld `twitter`. Der vorbereitete Pump-Website-Adapter übergibt ihn
beim Metadaten-Upload und beim Erstellungsaufruf; dafür ist keine zusätzliche X-Abfrage nötig.
Die tatsächliche Darstellung auf Pump.fun ist noch nicht mit einem verbundenen Konto geprüft.

### Kostenloser X-Zugriff: experimentell

Der Reader lädt die öffentliche [X-Einbettungsansicht](https://syndication.twitter.com/srv/timeline-profile/screen-name/realDonaldTrump),
prüft Handle **und** numerische Account-ID und wertet eingebettete Post-Daten aus. Dieser Zugriff wurde
am 25.10.2026 ohne Anmeldung mit tatsächlich gelieferten Posts geprüft. Er ist **keine dokumentierte
Echtzeit-Daten-API**: X kann Inhalt, Caching, Vollständigkeit und Zugriff jederzeit ändern. Er kann
auf Render anders reagieren als in der Entwicklungsumgebung. Sichtbare alte Posts sind kein Nachweis,
dass neue Posts zeitnah eintreffen. Bei fehlenden Daten, Login-Hürde oder Rate-Limit meldet der Bot die
Störung und wartet mindestens 15 Minuten; keine Cookies, Login-Umgehung, Proxyrotation oder bezahlte Ersatz-API.
Die geltenden Bedingungen der Quelle bleiben zu beachten.

Nur passende neue Originalposts nach Prozessstart kommen in die automatische Vorschau. Alte Posts,
Replies, Retweets, erkennbare Edits/Quotes und Zugangsdaten werden nicht als neuer Anlass behandelt.
Die öffentliche Ansicht enthält nicht zwingend alle Informationen des offiziellen Streams.
Account-Abfragen laufen parallel. Bei einem Fehler pausiert nur der betroffene Account (15–60 Minuten);
andere werden weiter in ihrem Intervall abgefragt. Die Oberfläche zeigt den Status und nächsten
Prüfzeitpunkt je Account sowie den konkreten Quellenfehler (HTTP-Status, Timeout, Netzwerkfehler
oder nicht lesbare Antwort). Die Startzeit der aktuellen Sitzung ist sichtbar. Sie belegt keinen
lückenlosen Nachtbetrieb: Nach Sleep/Neustart können historische Daten fehlen. Beim Lesetest am 25.10.2026 war Trump erreichbar, der Musk-Zugriff wurde
von X abgewiesen. Die Konfiguration eines Accounts ist somit kein Nachweis seiner Erreichbarkeit.
Innerhalb eines Accounts bekommen die neuesten Posts zuerst Zugriff auf das Vorschlagslimit.
Voreinstellung im Render-Blueprint: `@realDonaldTrump` und `@elonmusk`, jeweils mit festgelegter
numerischer Account-ID, Abfrage alle 60 Sekunden und höchstens fünf Minuten Post-Alter. Maximal
20 automatische Vorschläge je 24 Stunden, 60 Sekunden Mindestabstand und 200 Kandidatenprüfungen
pro Tag; diese höheren Grenzen gelten für die kostenlosen Vorschläge, nicht als neue Geldfreigabe. Bekannte Cartoon-Motive und neun zusätzliche redaktionelle Motive erzeugen themenbezogene Bilder;
der Vorlagenmodus ist keine KI und der Relevanzwert keine Gewinnwahrscheinlichkeit.

## Zusätzlich vorhandene CLI-Launch-Pipeline (nicht Teil des Gratisbetriebs)

- Offizieller X Filtered Stream mit Reconnect/Backoff, begrenzter Queue und Heartbeat.
- Erkennung über unveränderliche Account-IDs; keine nachgeahmten Anzeigenamen.
- Nur neue Originalposts: keine alten Posts beim Start, keine Antworten, Reposts, Quotes oder Edits.
- SQLite verhindert mit persistentem Datenpfad erneute Verarbeitung auch nach Neustarts. Ein Edit ist kein zweiter Launch.
- Lokaler Vorlagengenerator oder **echte lokale KI über Ollama**. Strikt geprüfte Felder für Name,
  Ticker, Motiv und Relevanz. Ein Score von 0,9 bedeutet keine 90-prozentige Gewinnchance.
- Zehn schnelle lokale PNG-Motive: Rakete, Katze, Hund, Kaffee, Taco, Pinguin, goldene Gans, Roboter,
  Pizza und Paket. Erkannte Themen bestimmen Bild und Namen; keine Promi-Porträts oder Markenlogos.
  Sonstige Themen werden über konkrete Begriffe im Post abgeleitet und redaktionell illustriert.
  Das ist eine lokale Heuristik ohne Aussage über Viralität oder Profit. Zugangsdaten bleiben ausgeschlossen.
- Beschreibung und Bild kennzeichnen den Coin als inoffizielles Community-Meme, ohne behauptete
  Empfehlung des Post-Autors. Namen wie „Official Trump“ werden abgewiesen. Filter sind konservative
  Regeln, keine lückenlose semantische Prüfung aller Namen oder Sprachen.
- IPFS-Metadaten über Pinata; unsigned Create-Transaktion über PumpPortal Local API.
- Lokale Signatur, Prüfung der konkreten Create-Instruktion und aller zugehörigen Accounts/PDA-Adressen,
  Simulation, Gebührenprüfung, Walletreserve, SOL-Limits und Kill-Switch.
- Durable Mint-Adresse und Signatur **vor** dem Senden; erst finalisierte Transaktionen zählen als
  abgeschlossen. Bei unbekanntem Ausgang sind weitere Launches gesperrt.
- CLI-Status mit letzten Entscheidungen, Heartbeats, Erkennungs-/Vorbereitungszeiten und Launch-Adressen.

Der separat aktivierte CLI-Live-Lauf ist automatisch: X-Stream → Regelprüfung → Idee → Bild/Metadaten → Budgetreservierung
→ Transaktionsprüfung/Simulation → Senden → Finalisierung. Ein einzelner Worker verarbeitet die
Wallet-Operationen seriell; der Stream liest gleichzeitig weiter. Queue maximal 32 Posts, ältere
Ereignisse werden bei Überlast verworfen. Geschwindigkeit rechtfertigt keine doppelten Launches.

## Was kostet tatsächlich Geld?

Prüfstand 25.10.2026, keine Preisgarantie:

| Baustein | Dieser Bot |
| --- | --- |
| Demo, Bilder, Vorlagen | Lokal ohne API-Gebühren |
| Lokale KI | Ollama ohne Gebühren pro Anfrage; benötigt eigenen Rechner/RAM/Strom |
| Öffentliche X-Ansicht | Ohne API-Key, experimentell und ohne Verfügbarkeits-/Latenzgarantie |
| Offizieller X-Stream | Pay-per-use; im Gratisprofil gesperrt |
| Metadaten | Pinata bietet einen Free-Tarif mit Limits; gültiger JWT erforderlich |
| PumpPortal-Erstellung | Laut Anbieter keine zusätzliche Creation-Gebühr; Handelsgebühren bei initialem Kauf, den dieser Bot nicht ausführt |
| Solana-Veröffentlichung | Netzwerk-/Prioritätsgebühren und benötigte Account-Einlagen; keine Nullkosten-Zusage |
| RPC / Dauerbetrieb | Eigener Zugang/Rechner oder Hosting; keine bezahlten Dienste automatisch angelegt |

Die ursprüngliche CLI verwendet die dokumentierte Onchain-Create-Schnittstelle. Zusätzlich ist ein
experimenteller Adapter für den kostenlosen Website-Ablauf vorbereitet; siehe unten. PumpPortal ist ein Drittanbieter, nicht Pump.fun.
Die Dokumentation zeigt Beispiele mit Eigenkauf. Ob `amount=0` aktuell akzeptiert wird, wurde nicht mit
einem echten Launch geprüft; bei Ablehnung stoppt der Bot, statt stillschweigend SOL für einen Kauf auszugeben.

X dokumentiert für den Filtered Stream ungefähr **4–5 Sekunden P99-Latenz**. KI, zwei IPFS-Uploads,
Transaktionsbau, Simulation und Solana-Verarbeitung kommen dazu. Eine garantierte Veröffentlichung
innerhalb weniger Sekunden lässt sich mit diesem Setup nicht versprechen. Auch Ollama auf schwacher
Hardware kann den Zeitrahmen überschreiten; dann wird verworfen, nicht verspätet veröffentlicht.

Quellen:

- [PumpPortal Token Creation](https://pumpportal.fun/creation/): aktueller Pinata-Upload, Local-API-Beispiel.
  Der frühere `pump.fun/api/ipfs`-Upload wird laut Anbieter nicht mehr unterstützt.
- [PumpPortal Gebühren](https://pumpportal.fun/fees/).
- [X Filtered Stream](https://docs.x.com/x-api/posts/filtered-stream/introduction).
- [X Preise und Ausgabenlimits](https://docs.x.com/x-api/getting-started/pricing).
- [Pump-Programm-IDL](https://github.com/pump-fun/pump-public-docs/blob/639b87fb45c3adeffa6f7c4a1bf0cb85e392bf22/idl/pump.json),
  auf diesen Commit fixierter Create-Ausschnitt in `src/xpump/pump_create.json`.

## Optionaler offizieller X-Stream außerhalb des Gratisprofils

1. Eigenen zulässigen X-API-Zugang einrichten. Für dieses Projekt eine dedizierte X-App nutzen, damit
   keine fremden Stream-Regeln Traffic und Gebühren erzeugen. Ausgabenlimit im Anbieterportal setzen;
   automatische Aufladung nicht ungeprüft aktivieren. Der Bot kann die X-Rechnung nicht durch SOL-Limits begrenzen.
2. In `config.toml` die **numerischen** `author_ids` setzen und `x_enabled = true` aktivieren.
   Kein Account ist voreingestellt. X-Posts sind außerdem nicht identisch mit Truth-Social-Posts.
3. `X_BEARER_TOKEN` als Umgebungsvariable setzen; keine Schlüssel ins Repository schreiben.
4. Mit `xpump configure-x` die Filterregel anlegen, danach `xpump run` starten.
   Dieser Schritt nutzt den externen X-Zugang und ist nicht Teil der kostenlosen Offline-Demo.

Alle Einstellungen aus `config.toml` lassen sich durch Umgebungsvariablen mit Präfix `XPUMP_`
überschreiben, beispielsweise `XPUMP_AUTHOR_IDS=123,456`, `XPUMP_X_ENABLED=true` oder
`XPUMP_MAX_LAUNCH_SOL=0.01`. Boolesche Werte müssen `true` oder `false` sein. Änderungen erfordern
einen Neustart. `XPUMP_ENABLE_MAINNET` bleibt zusätzlich zum Live-Modus erforderlich.
Mit `XPUMP_CONFIGURE_X_ON_START=true` verwaltet der Worker beim Start seine dedizierte X-Regel;
fremde Regeln in derselben X-App führen weiterhin zum Abbruch.

Bei deaktiviertem X-Zugang bleibt `xpump run` im Wartezustand mit Heartbeat. Im Preview-Modus
werden dabei keine externen APIs aufgerufen. Das bedeutet „Prozess läuft“, nicht „X wird überwacht“.

`mode = "preview"` lädt weder Bilder hoch noch erzeugt es eine Blockchain-Transaktion.
Auch Preview berücksichtigt Cooldown, Häufigkeit und Relevanz. Standardmäßig maximal zwei Vorschläge
innerhalb von 24 Stunden, 15 Minuten Abstand und maximal 60 Sekunden Post-Alter.

Optional tatsächliche KI: Ollama auf demselben Rechner installieren, ein geeignetes Modell laden
(beispielsweise `ollama pull qwen3:4b`), `idea_provider = "ollama"` und das passende `ollama_model` setzen.
Das Modell muss beim Start verfügbar sein; keine automatische Modellinstallation oder kostenpflichtige
Cloud-Ausweichroute. Bei Ausfall/ungültiger Ausgabe kein Launch. Standard `templates` ist regelbasiert,
**keine vorgetäuschte KI**. Der Ollama-Endpunkt ist fest auf `127.0.0.1:11434` begrenzt.

## Echtgeld-Veröffentlichungen: außerhalb des Gratisprofils, nicht aktiviert

Vor dem ersten Live-Lauf müssen der gewünschte Account, Datenzugang und SOL-Kostenrahmen feststehen.
Die mitgelieferte Konfiguration enthält bewusst keine freigegebenen Geldbeträge. Das Gratisprofil
und die öffentliche X-Quelle erlauben diesen Betrieb nicht; dafür müsste außerhalb dieses Profils
ein separater Betrieb mit dauerhaftem Speicher und freigegebenen Kosten eingerichtet werden.

Erforderlich:

- Eine **eigene, nur für diesen Bot verwendete** Solana-Wallet mit begrenztem Guthaben.
  `XPUMP_WALLET_FILE` zeigt auf eine lokale JSON-Keypair-Datei im Solana-CLI-Format (64 Bytes),
  Dateirechte `600`. Keine Haupt-Wallet und keine Recovery Phrase an Drittanbieter geben.
- `SOLANA_RPC_URL` (HTTPS), `PINATA_JWT` und X-Zugang. Diese Secrets werden nicht aus TradeBot übernommen.
- `mode = "live"` plus `XPUMP_ENABLE_MAINNET=true` als zweiter Aktivierungsschalter.
- Positive `max_launch_sol`, `max_24h_sol`, `max_total_sol`, in dieser Reihenfolge aufsteigend.
  Werte als Dezimalstrings in SOL, maximal neun Nachkommastellen.
- `wallet_reserve_sol`, maximale Anzahl Launches, Cooldown und Prioritätsgebühr passend einstellen.

`.env.example` ist eine Vorlage; die Anwendung liest `.env` **nicht automatisch**. Secrets über den
Host/Container setzen oder eine private `.env` bewusst in der Shell laden. Keine Secrets in CLI-Argumente.

Die Budgetbuchhaltung reserviert pro Versuch den **vollen Höchstbetrag**, auch bei Fehlschlägen.
Sie gibt ungenutzten Betrag nicht automatisch frei. Das ist absichtlich konservativ. Das Tageslimit
gilt rollierend für 24 Stunden; das Gesamtlifetime-Limit wird bei Neustarts nicht zurückgesetzt.

Grenzen: Die Simulation ist eine Vorabprüfung, kein atomar auf der Blockchain erzwungenes Ausgabenlimit.
Zwischen Simulation und Ausführung kann sich der Chain-Zustand ändern. Wallet-Guthaben eng begrenzen,
Wallet exklusiv verwenden und nicht parallel manuell handeln. Bei einem nachträglich erkannten
Limitverstoß aktiviert der Bot den Kill-Switch. Netzwerkgebühren können auch bei gescheiterten
Onchain-Transaktionen entstehen. SOL-Grenzen begrenzen weder X-/Pinata-Kosten noch Hostingkosten.

## Betrieb und Fehlersuche

### Auf Render per kostenlosem Blueprint

[`render.yaml`](render.yaml) definiert ausschließlich **einen Webservice mit `plan: free`** namens
`coinbot-free`. Kein kostenpflichtiger Worker, keine Disk, keine Datenbank, kein API-Key.
[Deploy to Render](https://render.com/deploy?repo=https://github.com/jsnuwu/CoinBot) öffnen oder in Render
**New → Blueprint → jsnuwu/CoinBot → main** auswählen. Vor dem Bestätigen kontrollieren, dass der Dienst
als **Free / 0 Dollar** ausgewiesen wird. Der vorherige kostenpflichtige Blueprint wurde ersetzt.
Falls daraus bereits ein bezahlter Worker angelegt wurde, wird dieser durch den neuen Code nicht
automatisch gelöscht oder kostenlos; ihn separat im Render-Dashboard entfernen, wenn ungenutzt.

Nach dem Deploy die angezeigte `onrender.com`-Adresse öffnen. Die öffentliche X-Abfrage startet
selbstständig und die Website zeigt echte Prüfzeiten. Kein Bank-/API-Schlüssel wird vom Bot benötigt.
Eine eventuelle Konto-/Zahlungsmittel-Verifizierung durch Render kann der Code nicht umgehen.

[Render-Free-Grenzen](https://render.com/docs/free):

- Nach etwa 15 Minuten ohne eingehenden Verkehr schläft der Dienst; währenddessen keine Beobachtung.
  Aufwecken per Seitenaufruf kann ungefähr eine Minute dauern. Keine künstlichen Keep-alive-Pings.
- 750 kostenlose Instanzstunden pro Workspace und Monat werden mit anderen Free-Webdiensten geteilt.
  Zwei dauerhaft aktive Free-Dienste passen nicht beide rund um die Uhr in dieses Kontingent.
- Serverdateien unter `/tmp/coinbot` können bei Sleep, Neustart oder Deploy verschwinden. Vorschläge
  herunterladen. Der Neustart holt keine alten Posts nach; es gibt hier kein Echtgeld-Budgetledger.
- Bandbreiten-/Build-Kontingente beachten. Render kann bei Überschreitung und hinterlegter Zahlungsmethode
  Zusatznutzung abrechnen; ohne Zahlungsmethode werden Free-Dienste bzw. weitere Builds gemäß Anbieterregeln
  ausgesetzt. Ein kostenfreier Blueprint ist keine Garantie gegen jede mögliche Kontorechnung.

Einstellungen unter **Environment** (für dauerhafte Änderungen auch die Blueprint-YAML anpassen):

| Variable | Bedeutung / Vorgabe |
| --- | --- |
| `XPUMP_FREE_HOSTING` | `true`; verweigert Live-Modus, offiziellen X-Stream und KI-Providerwechsel |
| `XPUMP_PUBLIC_X_ACCOUNTS` | `realDonaldTrump:25073877,elonmusk:44196397`; bis zu fünf `handle:numerische_id`-Paare |
| `XPUMP_PUBLIC_X_POLL_SECONDS` | `60` im Blueprint, mindestens 60 Sekunden |
| `XPUMP_MAX_POST_AGE_SECONDS` | `300` im kostenlosen Blueprint |
| `XPUMP_X_ENABLED` | `false`; damit ist nur der **bezahlte offizielle Stream** gemeint |
| `XPUMP_MODE` / `XPUMP_ENABLE_MAINNET` | `preview` / `false` |

Die Website besitzt keinen manuellen Launch-Endpunkt und keinen Solana-Signer. Der optionale
`FreePumpCreator` nutzt ausschließlich den kostenlosen First-Party-Website-Ablauf, wenn separat aktiviert.
`xpump web` lehnt Live-Konfigurationen auch unabhängig vom Gratis-Schalter ab. Für den Gratisbetrieb
keine Wallet-Secrets hinterlegen. Die kleine Instanz nutzt lokale Vorlagen; Ollama ist dort nicht installiert.

### Status und Stopps

```bash
xpump status
xpump stop
```

`status` liest ausschließlich die Datenbank. `stop` erstellt `data/<mode>/STOP`: keine neuen Launches,
bereits versendete Transaktionen können weiterhin finalisieren. Nach geklärter Ursache die STOP-Datei
bewusst entfernen, um neue Launches wieder zuzulassen. Stream-Empfang/X-Kosten werden durch den
Launch-Kill-Switch nicht beendet; dazu den Prozess stoppen.

| Status/Grund | Bedeutung |
| --- | --- |
| `preview` | Lokales Bild/JSON fertig; kein Token und keine Contract-Adresse |
| `insufficient_meme_relevance` | Kein passendes Motiv oder Score unter Mindestwert |
| `old_post` | Vor Prozessstart oder zu spät angekommen; kein Nachholen |
| `candidate_limit_24h` | Analyse-Limit erreicht |
| `sol_budget`, `cooldown`, `launch_count_24h` | Bewusst kein weiterer Launch |
| `pending`, `unresolved_transaction` | Signatur gespeichert, finaler Ausgang noch unbekannt |
| `finalized` | Blockchain-Transaktion endgültig erfolgreich; Adresse im Launch-Datensatz |
| `failed` | Finalisiert, aber Onchain fehlgeschlagen; mögliche Gebühren bleiben verbucht |
| `aborted` | Vor dauerhaft gespeicherter Signatur abgebrochen; kein Broadcast durch diesen Bot |

Bei RPC-Timeout oder unbekannter Signatur wird **niemals automatisch ein neuer Mint erzeugt**.
Der Worker fragt den Status erneut ab. Bei dauerhaft unbekanntem Ausgang ist eine manuelle Prüfung
über Signatur/Explorer und RPC erforderlich; es gibt bewusst keinen automatischen „Budget zurücksetzen“-Knopf.
Ein Absturz vor persistierter Signatur wird beim nächsten Live-Start als abgebrochen erkannt.

Persistenten Datenträger verwenden und `data/live/` sichern. Nicht mehrere Instanzen mit derselben Wallet
auf getrennten Datenträgern betreiben: Der lokale Dateilock schützt genau einen gemeinsamen Datenpfad,
kein verteiltes Cluster. Nach Löschen/Verlust der Datenbank sind historische Limits und Duplikatschutz weg.
Alte Nicht-Launch-Postdatensätze werden nach 30 Tagen entfernt; Launch-Ledger bleibt erhalten.

Dockerfile vorhanden. Daten nach `/app/data`, Konfiguration nach `/app/config.toml`, Secret-Datei nur
lesbar mounten. Der Container läuft als UID 10001; Dateieigentümer und Volume-Rechte passend setzen.
Ollama auf dem Host ist im normalen Docker-Bridge-Netz nicht unter Container-`127.0.0.1` erreichbar;
dafür denselben Netzwerk-Namespace verwenden oder außerhalb des Containers betreiben.
Kein bezahltes Hosting wird automatisch provisioniert. Ein schlafender Free-Webdienst ist kein
zuverlässiger dauerhaft verbundener Stream-Worker.

## Verifikation / bewusste Grenzen

Tests verwenden ausschließlich synthetische Posts, lokale Schlüssel und Mock-HTTP/RPC. Sie prüfen
Budgetgrenzen, Neustarts/Deduplizierung, Kandidatenfilter, falsche Transaktionsdaten/Accounts, zusätzliche
Transfers/Käufe, Prioritätsgebühren, Simulation, Sendetimeouts und Finalisierung. GitHub Actions führt
Tests, Linter und Offline-Demo aus.

Lokal verifiziert: **137 Tests bestanden**, Ruff ohne Befund, Offline-Demo erfolgreich und Python-Wheel
inklusive des fixierten Pump-IDL-Ausschnitts gebaut. Der Blueprint wurde gegen das offizielle Render-Schema
validiert; ausschließlich lesende Web-Endpunkte, abgeschaltete manuelle Erstellung und experimenteller X-Reader getestet.
Die öffentliche X-Ansicht wurde außerdem separat ohne API-Key gelesen (20 gelieferte Posts).
Desktop-/Mobilansicht, PNG-Vorschau und JSON-Download wurden im Browser ohne JavaScript-Fehler geprüft. Kein Render-Deploy oder Mainnet-Test
ist darin enthalten.

Noch **nicht** mit einem finanzierten Mainnet-Launch, offiziellem X-API-Zugang oder Pinata-Upload validiert. Der
Transaktionsvalidator erlaubt nur die fixierten Create/Create-v2-Formate mit SOL als Quote, deaktiviertem
Mayhem/Cashback/Holder-Reward und ohne weitere Instruktionen außer begrenztem Compute Budget. Lookup Tables,
Tips, Eigenkäufe, neue Protokollformate oder unbekannte Accounts werden abgelehnt. Die tatsächliche
PumpPortal-Kompatibilität muss mit einem kontrollierten ersten Launch bestätigt werden; dabei keine
Validierung abschalten, um eine Antwort künstlich „passend“ zu machen.

Keine Profitabilitäts- oder Sekunden-Garantie. Reale Aktivierung bleibt ein separater, bewusst
konfigurierter Schritt; die automatische Entscheidung pro Post ist bereits implementiert.

## Kostenloser Pump-Website-Ablauf: Adapter vorbereitet, Konto-Verifikation fehlt

Bei erneuter Prüfung am 26.10.2026 zeigte Pump.funs eigener öffentlicher Website-Code einen
kostenlosen Erstellungsweg ohne Erstkauf: `POST /coins/create-v2`, authentifiziert mit einer
Pump-Session, gibt eine Mint-Adresse zurück. Die UI zeichnet diesen Zweig als `isFree: true`,
`tokensBought: 0`, `solPaid: 0` auf. Beim ersten Kauf wird getrennt die Onchain-Erstellung behandelt.
Das korrigiert die frühere zu pauschale Aussage, jede Website-Erstellung erfordere vom Ersteller SOL.

Prüfquelle: [Erstellungsseite](https://pump.fun/create), insbesondere die von ihr eingebundenen
Dateien `1d42gkuet1_-2.js` (Erstellungsablauf), `2b-_n6eypkqhx.js` (Typdefinition der Website-Routen)
und `0xjg-q9n0jeif.js` (Metadaten-Upload). Dies ist eine beobachtete Website-Schnittstelle,
keine zugesicherte öffentliche Developer-API. Verfügbarkeit und Authentifizierung können sich ändern.

`src/xpump/free_pump.py` bildet diesen Ablauf ab: Konto über `/auth/my-profile` prüfen, Bild/Metadaten
über `/ipfs/token-metadata` hochladen und `/coins/create-v2` einmalig aufrufen. Keine RPC-Aufrufe,
keine Zahlungsbeträge, kein PumpPortal, keine automatische Konto-Registrierung, keine Solana-Signatur.
Ein Netzwerkfehler beim Erstellen wird als unbekannter Ausgang gespeichert; keine Wiederholung und
keine weiteren Erstellungen, bis der Ausgang geklärt ist. Eine zurückgegebene Adresse wird als
Website-Listing angezeigt, nicht als nachgewiesener Onchain-Kauf oder Gewinn.

**Noch nicht mit einem echten Konto ausgeführt oder verifiziert.** Die bestehenden Mock-Tests prüfen
den programmierten Ablauf und verhindern Käufe/RPC-Aufrufe; sie beweisen keine externe Verfügbarkeit.
Der Schalter `XPUMP_FREE_PUMP_ENABLED` bleibt im Blueprint `false`.
Für die Verbindung sind die öffentliche Besitzeradresse (`XPUMP_PUMP_CREATOR_ADDRESS`) und eine
legitime gültige Pump-Session (`PUMP_ACCESS_TOKEN`, nur im Secret-Speicher) vorgesehen. Der Adapter
verweigert eine Session, deren Kontoadresse nicht zur Besitzeradresse passt. Die einmalige
Anmeldung/Session-Einrichtung muss noch mit dem Besitzerkonto verbunden und getestet werden;
keine Passwörter, Private Keys, Seed Phrases oder Sitzungscookies in den Chat senden.

Die bisherige Bezeichnung `mode=preview` verhindert weiterhin den Onchain-Publisher. Ein optionales
kostenloses Website-Listing ist davon getrennt und wird über `free_creation` im Status ausgewiesen.
Die experimentelle X-Quelle kann weiterhin HTTP 429 liefern; Render Free schläft weiterhin ein.
Der neue Adapter allein erfüllt deshalb noch keine zuverlässige Sekunden-/24/7-Automatisierung.

## Themenauswahl ab Android 0.3

Die feste Meme-Wortliste ist keine Voraussetzung mehr. Allgemeine Texte erhalten einen abgeleiteten
Themenbegriff und ein passendes redaktionelles Motiv. Der Bericht aus dem Gerätetest (`Kein passendes
Thema: 2`) führte zu dieser Änderung. Beispielprüfung mit dem vom Nutzer gelieferten politischen
Text: `Iran Brief` / Politikmotiv. Das ist keine Bestätigung der behaupteten Ereignisse.
Die Heuristik arbeitet offline ohne KI-API-Rechnung; sie ersetzt keine vollständige Sprach- oder
Trendbewertung. Sichtbare frühere Ablehnungen am alten Themenfilter können innerhalb des bestehenden
Fünf-Minuten-Fensters einmal neu verarbeitet werden. Die neue APK erhält den lokalen Signaturschlüssel
für ein Update mit bestehenden Einstellungen. Die Pump-Anmeldung bleibt weiterhin erforderlich.
