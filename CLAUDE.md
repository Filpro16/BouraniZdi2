# CLAUDE.md

## Projekt
Hra Bourání zdí (ve stylu Breakout) pro Android – školní zadání.
Hráč ovládá pálku ve spodní části obrazovky **nakláněním telefonu doleva/doprava**.
Míček se odráží po obrazovce a rozbíjí bloky. Míček se nesmí dotknout spodního okraje – jen pálky.

Autor: Filip Štěpáník · Třída: C4 · Termín: 9. 11. 2026

## Technologie
- Android Studio, Kotlin, min SDK [24]
- Bez herního enginu – vlastní `SurfaceView` s vláknem herní smyčky, kreslení na `Canvas`
- Ovládání: `SensorManager` + `Sensor.TYPE_ACCELEROMETER` (osa x)
- Aktivita uzamčená na výšku (portrait)

## Struktura
- `MainActivity.kt` – nastaví `GameView` jako obsah, předává `onResume` / `onPause`
- `GameView.kt` – SurfaceView, herní smyčka (update + draw), stavy hry
- `Ball.kt`, `Paddle.kt`, `Block.kt` – herní objekty (pozice, velikost, update, draw)
- `TiltController.kt` – listener senzoru, low-pass filtr, mrtvá zóna

## Pravidla hry
- Stavy: READY (klepnutím odpálit) → PLAYING → LIFE_LOST / GAME_OVER / WIN
- [3] životy; když míček spadne dolů, hráč ztratí život
- Skóre: +[10] za každý blok
- Úhel odrazu míčku od pálky závisí na tom, kam na pálku dopadne
- Level je vyhraný, když jsou zničeny všechny bloky

## Konvence
- Veškerý pohyb se násobí delta time (nezávislé na snímkové frekvenci)
- Všechny velikosti a pozice jsou relativní k šířce/výšce obrazovky – žádné pevné pixely
- Listener senzoru se registruje v `onResume` a odregistruje v `onPause`
- Vlákno herní smyčky se musí čistě zastavit, když se aktivita pozastaví
- Kód má být čitelný a okomentovaný
- Komunikuj se mnou česky; názvy funkcí, proměnných a tříd v kódu piš anglicky

## Vždy dodržuj
- **Git:** na začátku inicializuj git repozitář v kořeni projektu (se správným `.gitignore` pro Android), pokud ještě neexistuje. Po každé dokončené změně udělej commit s krátkou, jasnou zprávou, co se udělalo.
- **Minimální změny:** upravuj jen kód, který je nutné změnit pro aktuální úkol. Nerefaktoruj, nepřejmenovávej, nepřeformátovávej ani „neuklízej“ nesouvisející kód.
- **Krátké, izolované funkce:** piš krátké funkce, které dělají vždy jen jednu věc (např. `movePaddle()`, `checkWallCollision()`, `drawBlocks()`), aby šla každá část najít a upravit bez zásahu do zbytku. Vyhýbej se dlouhým funkcím a skrytým závislostem mezi nimi; kde to dává smysl, předávej hodnoty jako parametry. Nastavitelná čísla (rychlosti, velikosti, citlivost, životy) patří do pojmenovaných konstant na jednom místě, ne rozházená po kódu.
- **Ptát se vs. rozhodnout sám:** zeptej se před každým velkým rozhodnutím (změna architektury, nové soubory/třídy mimo plánovanou strukturu, změna pravidel hry, přidání závislostí, mazání kódu) a vždy, když si nejsi jistý, co chci. Malá, zřejmá rozhodnutí můžeš udělat bez ptaní.
- **Otestuj každou funkci:** po přidání každé funkce ji otestuj, než půjdeš dál:
  - sestav projekt (`./gradlew assembleDebug`) a oprav všechny chyby a varování, které změna způsobila
  - napiš unit testy pro herní logiku, která nepotřebuje telefon (kolize, odrazy, skóre, životy, stavy hry), a spusť je (`./gradlew test`)
  - projdi okrajové případy (míček narazí do rohu, pálka u okraje obrazovky, zničení posledního bloku, ztráta posledního života)
  - dej mi krátký seznam, co mám ručně otestovat na telefonu (náklon, ovládání, vzhled)
  - commitni až ve chvíli, kdy build i testy projdou

## Jak má Claude pracovat
- Claude píše veškerý kód tohoto projektu
- Hru stav po etapách a po každé etapě ověř, že se projekt zkompiluje:
  1. Nastavení projektu, uzamčení na výšku, prázdný `GameView` s běžící herní smyčkou
  2. Pálka ovládaná náklonem
  3. Pohyb míčku a odrazy od stěn
  4. Kolize s pálkou, úhel podle místa dopadu
  5. Bloky a kolize s bloky
  6. Životy, skóre, stavy hry, obrazovky výhry / konce hry
  7. Vylepšení (barvy bloků, zvuky, nejvyšší skóre) – až když funguje 1–6
- Po každé etapě shrň, co se změnilo a co mám otestovat na telefonu
- Nepřidávej knihovny ani herní enginy bez zeptání
- Při opravě chyby vysvětli příčinu, ne jen opravu

## Testování
- Testuj na skutečném zařízení (ovládání náklonem); emulátor: Extended Controls → Virtual sensors
- Build: Run ▶ v Android Studiu, nebo `./gradlew assembleDebug`
