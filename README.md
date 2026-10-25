# X → Pump Launch Bot

Eigenständiger Bot, getrennt von TradeBot. Er überwacht ausgewählte X-Accounts und kann nach festen
Regeln automatisch einen Meme-Token vorbereiten und auf Solana veröffentlichen. **Keine Telegram-Freigabe
pro Coin. Standard ist ein lokaler Preview-Modus; aktuell ist kein produktiver Dienst aktiviert.**

Token-Erstellung ist kein Geschäftsmodell mit garantierten Einnahmen. Ein Launch erzeugt weder Käufer
noch Handelsvolumen. Dieses Projekt enthält keine künstlichen Käufe, Eigenkäufe, Volumenmanipulation,
Verkaufsstrategie oder automatische Creator-Fee-Abholung.

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

## Was eingebaut ist

- Offizieller X Filtered Stream mit Reconnect/Backoff, begrenzter Queue und Heartbeat.
- Erkennung über unveränderliche Account-IDs; keine nachgeahmten Anzeigenamen.
- Nur neue Originalposts: keine alten Posts beim Start, keine Antworten, Reposts, Quotes oder Edits.
- SQLite verhindert erneute Verarbeitung auch nach Neustarts. Ein Edit ist kein zweiter Launch.
- Lokaler Vorlagengenerator oder **echte lokale KI über Ollama**. Strikt geprüfte Felder für Name,
  Ticker, Motiv und Relevanz. Ein Score von 0,9 bedeutet keine 90-prozentige Gewinnchance.
- Ein schnelles lokales Cartoon-PNG-Template mit unterschiedlichen Farben/Tickern. Kein generiertes
  Promi-Bild; das freie KI-Motiv ist derzeit Text und wird noch nicht bildlich ausgerendert.
- Beschreibung und Bild kennzeichnen den Coin als inoffizielles Community-Meme, ohne behauptete
  Empfehlung des Post-Autors. Namen wie „Official Trump“ werden abgewiesen. Filter sind konservative
  Regeln, keine lückenlose semantische Prüfung aller Namen oder Sprachen.
- IPFS-Metadaten über Pinata; unsigned Create-Transaktion über PumpPortal Local API.
- Lokale Signatur, Prüfung der konkreten Create-Instruktion und aller zugehörigen Accounts/PDA-Adressen,
  Simulation, Gebührenprüfung, Walletreserve, SOL-Limits und Kill-Switch.
- Durable Mint-Adresse und Signatur **vor** dem Senden; erst finalisierte Transaktionen zählen als
  abgeschlossen. Bei unbekanntem Ausgang sind weitere Launches gesperrt.
- CLI-Status mit letzten Entscheidungen, Heartbeats, Erkennungs-/Vorbereitungszeiten und Launch-Adressen.

Der normale Lauf ist automatisch: X-Stream → Regelprüfung → Idee → Bild/Metadaten → Budgetreservierung
→ Transaktionsprüfung/Simulation → Senden → Finalisierung. Ein einzelner Worker verarbeitet die
Wallet-Operationen seriell; der Stream liest gleichzeitig weiter. Queue maximal 32 Posts, ältere
Ereignisse werden bei Überlast verworfen. Geschwindigkeit rechtfertigt keine doppelten Launches.

## Was kostet tatsächlich Geld?

Prüfstand 25.10.2026, keine Preisgarantie:

| Baustein | Dieser Bot |
| --- | --- |
| Demo, Bilder, Vorlagen | Lokal ohne API-Gebühren |
| Lokale KI | Ollama ohne Gebühren pro Anfrage; benötigt eigenen Rechner/RAM/Strom |
| X-Daten | Offizieller Stream ist Pay-per-use; Zugang und Kostenlimit im X Developer Console nötig |
| Metadaten | Pinata bietet einen Free-Tarif mit Limits; gültiger JWT erforderlich |
| PumpPortal-Erstellung | Laut Anbieter keine zusätzliche Creation-Gebühr; Handelsgebühren bei initialem Kauf, den dieser Bot nicht ausführt |
| Solana-Veröffentlichung | Netzwerk-/Prioritätsgebühren und benötigte Account-Einlagen; keine Nullkosten-Zusage |
| RPC / Dauerbetrieb | Eigener Zugang/Rechner oder Hosting; keine bezahlten Dienste automatisch angelegt |

Der diskutierte kostenlose Offchain-Website-Ablauf wird hier **nicht** automatisiert. Dieses Projekt
verwendet die dokumentierte Onchain-Create-Schnittstelle. PumpPortal ist ein Drittanbieter, nicht Pump.fun.
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

## Eigene Posts im Preview-Modus verfolgen

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

## Echtgeld-Veröffentlichungen: vorbereitet, noch nicht aktiviert

Vor dem ersten Live-Lauf müssen der gewünschte Account, Datenzugang und SOL-Kostenrahmen feststehen.
Die mitgelieferte Konfiguration enthält bewusst keine freigegebenen Geldbeträge.

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

### Auf Render per Blueprint

[`render.yaml`](render.yaml) liegt im Repository-Stamm. In Render **New → Blueprint** öffnen,
`jsnuwu/CoinBot` verbinden, Branch `main` auswählen und vor **Deploy Blueprint** die Kostenübersicht
prüfen. Alternativ: [Deploy to Render](https://render.com/deploy?repo=https://github.com/jsnuwu/CoinBot).

Der Blueprint erstellt einen **kostenpflichtigen Background Worker** mit einer persistenten 1-GB-Disk.
Nach [Render-Preisliste](https://render.com/pricing) am 25.10.2026 ungefähr **7,25 US-Dollar/Monat**
(7 Dollar Compute + 0,25 Dollar Disk), zuzüglich eventueller Steuern, Zusatznutzung und externer APIs.
Auch der reine Wartezustand kostet Hostinggebühren. Kostenlose Render-Webdienste bieten weder die
benötigte dauerhafte Worker-Ausführung noch eine persistente Disk für dieses SQLite-Ledger.

Der erste Deploy benötigt **keine API-Schlüssel und keine Wallet**. Er startet in `preview` mit
deaktiviertem X-Zugang. In den Logs erscheint `X feed disabled: waiting for configuration`.
Dies ist ein erfolgreicher, absichtlich inaktiver Start. Ein Worker hat keine öffentliche Website;
über **Logs** und **Shell → `xpump status`** lässt sich der Zustand prüfen. Auto-Deploys warten
auf erfolgreiche GitHub-CI-Prüfungen. Daten unter `/var/data/coinbot` überstehen Deploys/Neustarts.

Wenn der X-Zugang samt Anbieter-Kostenlimit eingerichtet ist, im Render-Service unter **Environment**:

| Variable | Wert |
| --- | --- |
| `X_BEARER_TOKEN` | Secret aus der dedizierten X-App |
| `XPUMP_AUTHOR_IDS` | Gewünschte numerische X-Account-ID(s), mit Kommas getrennt |
| `XPUMP_X_ENABLED` | `true` |

Danach **Save, rebuild, and deploy**. Der Blueprint setzt bereits
`XPUMP_CONFIGURE_X_ON_START=true`, sodass die X-Regel automatisch geprüft/angelegt wird.
Diese drei Einstellungen aktivieren ausschließlich die Post-Verarbeitung im Preview-Modus,
keine echten Coin-Launches. Bereits im Blueprint festgelegte Werte wie `XPUMP_X_ENABLED` auch
in der YAML-Datei aktuell halten, wenn sie dauerhaft geändert werden: Ein späterer Blueprint-Sync
kann Dashboard-Änderungen wieder durch den YAML-Wert ersetzen.

Der kleine Render-Worker verwendet Vorlagen. Ein lokales Ollama-Modell ist dort nicht installiert
und passt nicht sinnvoll in den vorgesehenen 512-MB-Arbeitsspeicher. Kein kostenpflichtiger KI-Dienst
wird als Ersatz zugeschaltet. Die Voraussetzungen für echte Veröffentlichungen stehen oben;
dieser Blueprint aktiviert sie ausdrücklich nicht.

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

Lokal verifiziert: **66 Tests bestanden**, Ruff ohne Befund, Offline-Demo erfolgreich und Python-Wheel
inklusive des fixierten Pump-IDL-Ausschnitts gebaut. Der Blueprint wurde gegen das offizielle Render-Schema
validiert und der Start ohne Secrets/Netzwerkzugriffe getestet. Kein Render-Deploy oder Mainnet-Test
ist darin enthalten.

Noch **nicht** mit einem finanzierten Mainnet-Launch oder echtem X-/Pinata-Zugang validiert. Der
Transaktionsvalidator erlaubt nur die fixierten Create/Create-v2-Formate mit SOL als Quote, deaktiviertem
Mayhem/Cashback/Holder-Reward und ohne weitere Instruktionen außer begrenztem Compute Budget. Lookup Tables,
Tips, Eigenkäufe, neue Protokollformate oder unbekannte Accounts werden abgelehnt. Die tatsächliche
PumpPortal-Kompatibilität muss mit einem kontrollierten ersten Launch bestätigt werden; dabei keine
Validierung abschalten, um eine Antwort künstlich „passend“ zu machen.

Keine Profitabilitäts- oder Sekunden-Garantie. Reale Aktivierung bleibt ein separater, bewusst
konfigurierter Schritt; die automatische Entscheidung pro Post ist bereits implementiert.
