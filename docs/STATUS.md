# Status

| Part | State |
|---|---|
| Design sheets (gear, surfaces, controls, tricks, hooks, Riders Republic content) | Done. Preflight is clean, with 13 Riders Republic content rows unverified |
| Gameplay mod (physics, 29 tricks, gear wheel, HUD, poses, ride sounds, gear layer) | **Built and play-tested** in a real Minecraft 1.21.1 client (Fabric 0.17.2), driven by a scripted rider (`-Driderscraft.devtest=true`): snowboard about 90 km/h, skis about 91, bike about 79, wingsuit about 110, rocket about 194; flips, spins, grabs, tailwhips, switch landings and combos all score, and riders take no fall damage |
| Release package (portable Prism Launcher 10.0.2 + instance + Fabric API) | Built (`packaging/build-release.sh`). Melty's validate_recipe passes and one_click_check says yes |
| Riders Republic content extraction (models, sounds, callout text) | **Not implemented.** Riders Republic (AnvilNext 2.0, `.forge` archives) has no public extractor, so this has to be written against a real install. Until then the gear is an outline box, there are no ride sounds, and trick names come from the sheet |
| Real gameplay screenshot with Riders Republic content | Not possible until the extractor exists |
| Melty | Recipe in `melty.draft.json` (validated). Draft listing only, no release submitted |
