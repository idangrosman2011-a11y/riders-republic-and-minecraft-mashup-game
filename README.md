# Riders Craft — Riders Republic × Minecraft: Java Edition

Ride Riders Republic's gear through any Minecraft world. Solo, Minecraft 1.21.1, Fabric.

## What a player gets
- **Six sports:** Mountain Bike, BMX, Skis, Snowboard, Wingsuit and Rocket Wingsuit, each with its own handling (`sheets/gear.json`).
- **Gear wheel:** hold **1** to open Riders Republic's gear wheel, point at a sport and let go. Pick **On Foot** to walk again. A quick tap of 1 still selects hotbar slot 1.
- **Tricks:** flips (W/S), spins (A/D), grabs and style tricks (Z, X, C plus a direction), tailwhips, barspins, corks, rodeos, barrel rolls and wingsuit proximity. There are 29 tricks in `sheets/tricks.json`. Land clean to bank points into a combo, land crooked and you bail.
- **Rocket wingsuit:** hold Sprint to boost, and fuel refills while you glide.
- **From your own Riders Republic install:** the gear models, the ride sounds (tyres, snow, wind, rocket) and the trick callout text. None of it is shipped: the mod reads it from the folder Melty passes in `RIDERSCRAFT_RR_DIR`.
- No fall damage while riding: landings are judged by the trick system.

## Required games
- Minecraft: Java Edition 1.21.1 (host) with Fabric Loader and Fabric API.
- Riders Republic (companion, PC): the source of the gear models, sounds and callouts.

## How it's built
The design lives in `sheets/*.json`, one row per piece of content, and those sheets are the source of truth.
- `python3 tools/preflight.py` lists empty cells, broken references between sheets, missing hook code and unverified Riders Republic rows.
- `python3 tools/codegen.py` generates `mod/src/main/java/gg/riderscraft/gen/*.java`, one enum constant per row.
- `cd mod && ./gradlew build` runs codegen and preflight, then builds `build/libs/riderscraft-<version>.jar`.

## Status
See `docs/STATUS.md`. Short version: the gameplay mod is written but **not yet compiled or tested**, and **reading Riders Republic's files is not implemented yet**.
