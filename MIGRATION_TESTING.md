# Migration CTU → Create Station Essentials: Testanleitung (1.20.1, Forge und Fabric)

Betroffen sind 5 Blöcke samt Items. Sie wandern von `trainutilities:` nach `tracksta:`, der Pfad bleibt gleich:
`de_platform_block`, `nl_platform_block`, `ch_platform_block`, `iso_wall_block`, `iso_wall_block_green`.

Zwei Mechanismen arbeiten zusammen:

| Situation | Was greift |
|---|---|
| CTU v3.x **und** diese Mod geladen | **Converter**: tauscht Blöcke beim Chunk-Load und Items in Containern und Spielerinventaren aus |
| Ohne CTU (oder später mit CTD v4) | **Aliase**: Die alten IDs werden beim Laden auf die neuen umgeleitet |

Wie die Aliase umgesetzt sind:

- **Forge:** über `MissingMappingsEvent`. Forge speichert den Remap in der Welt, er gilt also auch bei jedem späteren Laden.
- **Fabric:** über einen kleinen Mixin in der Registry. Er greift nur, wenn die alte ID nicht registriert ist.

Solange CTU geladen ist, greifen die Aliase auf beiden Loadern nicht.

Beide Tests 1 und 2 **einmal mit Forge und einmal mit Fabric** durchführen.

## Vorbereitung

- Vor jedem Test **eine Kopie der Welt** anlegen.
- Config (beide Loader): `config/tracksta-common.toml`, Abschnitt `[ctu_migration]`
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
| Kiste, Shulker in der Kiste | Items sind `tracksta:…`, Anzahl und Name erhalten. Shulker abstellen → Inhalt ist neu |
| Vault, Depot | Items sind neu. Auf Fabric kann ein Item dabei in einen anderen Slot rutschen |
| Inventar, Offhand, Endertruhe | nach dem Login sofort konvertiert |
| Altes Item aus dem CTU-Creative-Tab nehmen | wird nach spätestens 5 s zum neuen Item |
| Alten Block aus dem CTU-Tab setzen | wird im nächsten Tick zum neuen Block (Wand bleibt ganz) |
| Altes Item allein ins Crafting-Grid | ergibt 1 neues Item (Fallback-Rezept) |
| Nach 1 min | Zusammenfassung im `latest.log` |
| `enabled = false` in der Config, neu starten | nichts wird konvertiert, Log: `CTU migration is disabled in the config` |

**Fix nebenbei: Abenteuer- und Zuschauermodus**

- `/gamemode adventure` → Rechtsklick auf einen Platform-Block öffnet **kein** Menü. Im Survival und Kreativ weiterhin schon.
- Neu ist: Der Server lehnt Schild-Änderungen von Spielern ab, die nicht bauen dürfen oder zu weit weg sind.

**Bekannte Grenzen (solange CTU noch geladen ist):**

- Blöcke in zusammengebauten Create-Contraptions oder Zügen bleiben alt, bis sie abgebaut sind und der Chunk neu geladen wird.
- Items in Entities (Rahmen, Rüstungsständer, gedroppte Items, Güterlore mit Truhe) bleiben alt.
- Beides wird spätestens durch die Aliase umgestellt, sobald CTU entfernt ist.

## Test 2: Gleiche Welt ohne CTU, nur diese Mod (Aliase)

Eine **Kopie der Welt aus Test 1, Schritt „Welt vorbereiten“** nehmen, die **noch nie** mit dieser Mod geladen wurde.

1. CTU aus dem `mods`-Ordner entfernen, diese Mod dazulegen.
2. Welt laden.
   - **Forge:** Der Client meldet fehlende Registry-Einträge für CTUs **andere** Blöcke (Türen, Linienblöcke …). Bestätigen. Im `latest.log` stehen 10 Zeilen `Remapped trainutilities:… -> tracksta:…` (5 Blöcke, 5 Items).
   - **Fabric:** keine Meldung zu den 5 verschobenen Blöcken.
   - Erwartet: CTUs andere Blöcke gehen verloren. Sie werden hier nicht migriert, deshalb die Kopie.
3. Prüfen wie in Test 1: Blöcke, Zustände, Wände über die Chunkgrenze, Kisten, Inventar, Endertruhe.
   - Zusätzlich hier: Items in Rahmen, Rüstungsständern und gedroppte Items sind ebenfalls neu.
   - **Shulkerkiste als Item:** Der Inhalt wird erst beim Benutzen eingelesen. Shulker abstellen und öffnen → Inhalt ist neu. Mit `/data get` steht im rohen NBT noch `trainutilities:…`, das ist in 1.20.1 normal.
4. **Forge zusätzlich:** Welt schließen und noch einmal laden. Es kommen keine `Remapped`-Zeilen mehr, trotzdem ist alles weiterhin neu (Shulker abstellen und prüfen). Forge hat den Alias also in der Welt gespeichert.
5. `latest.log`: **keine** Zeile `Create Train Utilities is installed …`, denn der Converter bleibt aus. Es gibt auch keine Rezeptfehler zu `ctu_migration`.
6. Im Crafting gibt es **kein** Fallback-Rezept (JEI/REI zeigt nichts zu `ctu_migration`).

## Test 3: Performance mit spark

Aufbau: Welt aus Test 1 mit **vielen** alten Blöcken, z. B. per WorldEdit einige 10.000 Platform-Blöcke über viele Chunks verteilt. Dann mit CTU und dieser Mod laden.

1. `/spark profiler start --thread *` (alle Threads, weil Chunk-Loads nicht nur im Server-Thread laufen)
2. Losfliegen, ca. 2–3 min, dann `/spark profiler stop` und den Link öffnen.
3. Im Viewer nach `CtuMigrationHandler` suchen:
   - `detect` / `containsOldBlocks`: sollte praktisch nicht auftauchen (nur eine Palette-Prüfung je Section).
   - `onServerTick` → `convertBlocks` / `convertContainers`: Anteil am Tick. Bei 256 Operationen pro Tick deutlich unter 1 ms.
4. **Fabric zusätzlich:** nach `MappedRegistryMixin` suchen. Er läuft bei jeder Registry-Abfrage, sollte aber im Profil nicht sichtbar sein (nur ein Namespace-Vergleich).
5. `/spark tickmonitor` beim Laden der Welt: keine Spitzen > 50 ms durch die Migration.
6. `/spark profiler start --only-ticks-over 50`: Falls es Ausreißer gibt, zeigt das, ob sie von der Migration kommen.
7. Im `latest.log` zeigt die Minuten-Zusammenfassung `(N chunks queued)`. Die Zahl sollte nach dem Laden schnell auf 0 gehen.

Stellschraube: Bei MSPT-Spitzen `operationsPerTick` senken (z. B. 64). Die Queue braucht dann länger, die Spitzen sinken.
Ohne CTU (Test 2) läuft der Converter gar nicht.
