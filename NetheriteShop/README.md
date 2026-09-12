# NetheriteShop

Paper-Plugin mit Vault-Economy-Anbindung und einem GUI-basierten Shop-System
fuer verzauberte Netherite- und Diamant-Ausruestung.

## Wichtiger Hinweis zur Minecraft-Version

Zum Zeitpunkt der Erstellung existiert keine veroeffentlichte Paper-Version
"1.26.2". In `pom.xml` wurde daher `1.21.10-R0.1-SNAPSHOT` als Platzhalter
eingetragen. Bitte passe **vor dem Build** Folgendes an eure tatsaechliche
Server-Version an:

- `pom.xml` → `<version>` der `paper-api`-Dependency
- `plugin.yml` → `api-version` (muss eine von Paper unterstuetzte Versionsgruppe sein)

Der gesamte Code verwendet ausschliesslich aktuelle, nicht als deprecated
markierte Paper-/Bukkit-APIs (Adventure-`Component` statt `ChatColor`,
`Registry.ENCHANTMENT` statt `Enchantment.getByName()`, moderne
Enchantment-Namen wie `PROTECTION`, `SHARPNESS`, `EFFICIENCY` etc.).

Falls euer Ziel-Server Java 25 voraussetzt (z.B. bei sehr neuen Minecraft-
Versionen), erhoeht zusaetzlich `maven.compiler.release` in der `pom.xml`.

## CI-Build (GitHub Actions)

Unter `.github/workflows/maven.yml` liegt ein Workflow, der das Plugin bei
jedem Push/Pull-Request auf `main` (sowie manuell per "Run workflow")
automatisch mit Java 21 und Maven baut und die fertige JAR als
Build-Artefakt anhaengt. Java-Version im Workflow ggf. an eure Server-
Anforderungen anpassen (siehe Hinweis zur Minecraft-Version oben).

## Build

```bash
mvn clean package
```

Die fertige JAR liegt danach unter `target/NetheriteShop.jar`.

## Voraussetzungen auf dem Server

- [Vault](https://www.spigotmc.org/resources/vault.34315/)
- Ein Vault-kompatibles Economy-Plugin (z.B. EssentialsX Economy)

Ohne eine registrierte Vault-Economy deaktiviert sich das Plugin beim Start
automatisch (siehe Server-Log), damit niemals ein Item ohne funktionierende
Abbuchung ausgegeben werden kann.

## Befehl & Permission

| Befehl | Permission | Standard | Beschreibung |
|--------|-----------|----------|--------------|
| `/buy` | `shop.buy` | `true` (alle Spieler) | Oeffnet das 54-Slot Shop-GUI |

Permission in der `plugin.yml` bei Bedarf auf `op` setzen, falls der Shop
nicht standardmaessig fuer alle Spieler verfuegbar sein soll.

## Ablauf eines Kaufs

1. `/buy` → 54-Slot Inventory mit allen Shop-Items, Rest mit Glas-Panels gefuellt
2. Klick auf ein Item → Bestaetigungsdialog (27 Slots: Item-Vorschau, gruener
   "Bestaetigen"- und roter "Abbrechen"-Button)
3. Bestaetigen → Guthabenpruefung via Vault → Abbuchung → Item wird per
   `addItem()` frisch erzeugt ins Inventar gelegt
4. Bei zu wenig Guthaben, vollem Inventar oder fehlender Economy wird der
   Kauf abgebrochen und **nichts** abgebucht

## Anti-Dupe-Konzept

- Shop-Icons tragen einen `PersistentDataContainer`-Eintrag
  (`shop_item_id`, `NamespacedKey`), der sie eindeutig einem konfigurierten
  Item zuordnet.
- Alle Klicks und Drag-Events in Shop- und Bestaetigungs-Inventar werden
  grundsaetzlich abgefangen (`event.setCancelled(true)`) - die Icons koennen
  das GUI also nie verlassen.
- Das tatsaechlich ausgegebene Item ist ein **neu erzeugtes** `ItemStack`
  (ohne Shop-Markierung), niemals das verschobene Icon selbst.
- Ein einfacher Klick-Lock verhindert doppelte Abbuchung bei sehr schnellem
  Doppelklick auf "Bestaetigen".

## Preise anpassen (config.yml)

Alle Preise, Materialien, Anzeige-Namen und Verzauberungen liegen in
`config.yml` und koennen ohne Neukompilierung geaendert werden:

```yaml
diamond_sword:
  slot: 33
  material: DIAMOND_SWORD
  display-name: "Diamant Schwert"
  price: 600000   # <- hier anpassen
  enchantments:
    sharpness: 5
    ...
```

**Die Preise fuer Diamant-Schwert (600K) und Diamant-Spitzhacke (400K) sind
gemaess Anfrage nur Platzhalter** und sollten vor dem produktiven Einsatz
an eure Server-Economy angepasst werden.

## Klassenuebersicht

| Klasse | Aufgabe |
|--------|---------|
| `ShopPlugin` | Bootstrap, Vault-Setup, Registrierung |
| `EconomyManager` | Kapselt alle Vault-Economy-Operationen |
| `PriceFormatter` | `formatPrice(double)` → "999" / "250K" / "5M" |
| `ShopItem` | Datenklasse fuer einen konfigurierten Artikel |
| `ShopManager` | Laedt `config.yml`, baut Icons & Reward-Items |
| `ShopGUI` | Baut das 54-Slot Hauptmenue |
| `ConfirmGUI` | Baut den Kauf-Bestaetigungsdialog |
| `ShopHolder` / `ConfirmHolder` | Eindeutige Inventory-Marker |
| `ShopListener` | Klick-Handling, kompletter Kaufablauf |
| `BuyCommand` | `/buy`-Befehl |
| `Keys` | Zentrale `NamespacedKey`-Verwaltung |
