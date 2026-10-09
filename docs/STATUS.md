# Status (honest)

| Part | State |
|---|---|
| Design sheets (gear, surfaces, controls, tricks, hooks, Riders Republic content) | Done. Preflight is clean, with 13 Riders Republic content rows unverified |
| Gameplay code: physics, tricks, gear wheel, HUD, poses, sounds, gear layer | Written. **Not compiled**: the build machine could not reach maven.fabricmc.net or Mojang's servers |
| Riders Republic install locator (`RIDERSCRAFT_RR_DIR`) | Written, untested |
| Riders Republic content extraction (Anvil `.forge` models and text, Wwise sound banks to .ogg) | **Not implemented.** It needs a real Riders Republic install to reverse-engineer. Until then the mod shows an outline box where the gear goes, plays no ride sounds and uses the sheet's trick names |
| In-game test, screenshot | Not done |
| Melty recipe | `melty.draft.json`, not validated |

## What unblocks the rest
1. Network access for the build machine: `melty.gg`, `maven.fabricmc.net`, `meta.fabricmc.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`, and `github.com` for the toolkit and Prism Launcher.
2. A machine with Riders Republic installed (the player's Windows PC) to write the extractors against and to test and screenshot in game.
