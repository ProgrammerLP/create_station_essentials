# Migration CTU → Create Station Essentials: Testanleitung

Betroffen sind 5 Blöcke samt Items. Sie wandern von `trainutilities:` nach `tracksta:`, der Pfad bleibt gleich:
`de_platform_block`, `nl_platform_block`, `ch_platform_block`, `iso_wall_block`, `iso_wall_block_green`.

Zwei Mechanismen arbeiten zusammen:

| Situation | Was greift |
|---|---|
| CTU v3.x **und** diese Mod geladen | **Converter**: tauscht Blöcke beim Chunk-Load und Items in Containern und Spielerinventaren aus |
| Ohne CTU (oder später mit CTD v4) | **Registry-Aliase**: Die alten IDs werden beim Laden direkt auf die neuen umgeleitet |

Solange CTU die alten IDs selbst registriert, sind die Aliase wirkungslos. Das ist NeoForge-Verhalten.

## Vorbereitung

- Vor jedem Test **eine Kopie der Welt** anlegen.
- Config: `config/tracksta-common.toml`, Abschnitt `[ctu_migration]`
  - `enabled` (Standard `true`): Converter an/aus
  - `operationsPerTick` (Standard `256`): ein konvertierter Block oder ein geprüfter Block-Entity (z. B. Kiste) = 1 Operation
  - `inventoryIntervalTicks` (Standard `100` = 5 s): wie oft Inventare online befindlicher Spieler geprüft werden, `0` = nur beim Login
- Logs:
  - `logs/latest.log`: Zusammenfassung auf INFO (`CTU migration in the last 60 s: …` und beim Stoppen `… in total this session`)
  - `logs/debug.log`: jede einzelne Konvertierung (`Converted … -> … at x, y, z`)
- Mit **F3 + H** (erweiterte Tooltips) zeigt jedes Item seine ID. Mit **F3** und Blick auf einen Block steht rechts die Block-ID samt Zuständen.

## Test 1: Welt mit CTU v3.x, dann diese Mod dazu (Converter)

**Welt vorbereiten, nur CTU v3.x (ohne diese Mod):**

1. Alle 5 Blöcke in verschiedenen Zuständen setzen:
   - Platform-Blöcke DE/NL/CH in allen 4 Blickrichtungen, mal mittig, mal an den Blockrand gesetzt (`z_align`), mit verschiedenen gewählten Schildern im Menü.
   - Lärmschutzwände (blau und grün) in allen 4 Richtungen. Eine davon genau über eine Chunkgrenze bauen (Chunkgrenzen mit **F3 + G** anzeigen).
   - Einige Blöcke weit weg vom Spawn (> 300 Blöcke), damit der Chunk-Load beim Hinfliegen getestet wird.
2. Items:
   - Kiste mit alten Items, eines davon im Amboss umbenannt
   - Shulkerkiste mit alten Items, die selbst in einer Kiste liegt
   - Alte Items im eigenen Inventar, in der Offhand und in der Endertruhe
   - optional: Create Item Vault oder Depot mit alten Items
3. Welt speichern und schließen.

**Diese Mod dazulegen und Welt laden:**

| Prüfen | Erwartung |
|---|---|
| `latest.log` beim Start | `Create Train Utilities is installed: converting 5 old blocks and 5 old items …` |
| Blöcke am Spawn (F3) | ID `tracksta:…`. Richtung, Ausrichtung und gewähltes Schild unverändert |
| Lärmschutzwände | alle 6 Teile vorhanden (auch über die Chunkgrenze). Ein Teil abbauen → ganze Wand weg, 1 Item droppt |
| Weit entfernte Blöcke | nach dem Hinfliegen sofort `tracksta:…` |
| Kiste, Shulker, Vault | Items sind `tracksta:…`, Anzahl und Name erhalten |
| Inventar, Offhand, Endertruhe | nach dem Login sofort konvertiert |
| Altes Item aus dem CTU-Creative-Tab nehmen | wird nach spätestens 5 s zum neuen Item |
| Alten Block aus dem CTU-Tab setzen | wird im nächsten Tick zum neuen Block (Wand bleibt ganz) |
| Altes Item allein ins Crafting-Grid | ergibt 1 neues Item (Fallback-Rezept) |
| Nach 1 min | Zusammenfassung im `latest.log` |
| `enabled = false` in der Config, neu starten | nichts wird konvertiert, Log: `CTU migration is disabled in the config` |

**Fix nebenbei: Abenteuermodus**

- `/gamemode adventure` → Rechtsklick auf einen Platform-Block öffnet **kein** Menü mehr. Im Survival und Kreativ weiterhin schon.

**Bekannte Grenzen (solange CTU noch geladen ist):**

- Blöcke in zusammengebauten Create-Contraptions oder Zügen bleiben alt, bis sie abgebaut sind und der Chunk neu geladen wird.
- Items in Entities (Rahmen, Rüstungsständer, gedroppte Items, Güterlore mit Truhe) bleiben alt.
- Beides wird spätestens durch die Aliase umgestellt, sobald CTU entfernt ist.

## Test 2: Gleiche Welt ohne CTU, nur diese Mod (Aliase)

Eine **Kopie der Welt aus Test 1, Schritt „Welt vorbereiten“** nehmen, die **noch nie** mit dieser Mod geladen wurde.

1. CTU aus dem `mods`-Ordner entfernen, diese Mod dazulegen.
2. Welt laden. NeoForge meldet, dass `trainutilities` fehlt. Bestätigen.
   - Erwartet: CTUs **andere** Blöcke (Türen, Linienblöcke …) gehen dabei verloren. Sie werden hier nicht migriert, deshalb die Kopie.
3. Prüfen wie in Test 1: Blöcke, Zustände, Wände über die Chunkgrenze, Kisten, Shulker, Inventar, Endertruhe.
   - Zusätzlich hier: Items in Rahmen, Rüstungsständern und gedroppte Items sind ebenfalls neu.
4. `latest.log`: **keine** Zeile `Create Train Utilities is installed …`, denn der Converter bleibt aus. Es gibt auch keine Fehler zu `trainutilities:`-Blöcken oder -Items und keine Rezeptfehler zu `ctu_migration`.
5. Im Crafting gibt es **kein** Fallback-Rezept (JEI zeigt nichts zu `ctu_migration`).

## Test 3: Performance mit spark

Aufbau: Welt aus Test 1 mit **vielen** alten Blöcken, z. B. per WorldEdit einige 10.000 Platform-Blöcke über viele Chunks verteilt. Dann mit CTU und dieser Mod laden.

1. `/spark profiler start --thread *` (alle Threads, weil Chunk-Loads nicht nur im Server-Thread laufen)
2. Losfliegen, ca. 2–3 min, dann `/spark profiler stop` und den Link öffnen.
3. Im Viewer nach `CtuMigrationHandler` suchen:
   - `onChunkLoad` / `containsOldBlocks`: sollte praktisch nicht auftauchen (nur eine Palette-Prüfung je Section).
   - `onServerTick` → `convertBlocks` / `convertContainers`: Anteil am Tick. Bei 256 Operationen pro Tick deutlich unter 1 ms.
4. `/spark tickmonitor` beim Laden der Welt: keine Spitzen > 50 ms durch die Migration.
5. `/spark profiler start --only-ticks-over 50`: Falls es Ausreißer gibt, zeigt das, ob sie von der Migration kommen.
6. Im `latest.log` zeigt die Minuten-Zusammenfassung `(N chunks queued)`. Die Zahl sollte nach dem Laden schnell auf 0 gehen.

Stellschraube: Bei MSPT-Spitzen `operationsPerTick` senken (z. B. 64). Die Queue braucht dann länger, die Spitzen sinken.
Ohne CTU (Test 2) läuft der Converter gar nicht, dort kostet die Migration nichts.
