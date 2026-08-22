# Ores and Stuff

[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/ABO47/Ores-and-Stuff)
[![Minecraft](https://img.shields.io/badge/minecraft-1.20.1-green)](https://www.minecraft.net/en-us/article/minecraft--java-edition-1-20-1)
[![Forge](https://img.shields.io/badge/forge-47.4.10-orange)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Fabric](https://img.shields.io/badge/fabric-0.15.11-yellow)](https://modrinth.com/mod/fabric-api/version/0.92.2+1.20.1)
[![License](https://img.shields.io/badge/license-MIT-brightgreen)](LICENSE)

Ores and Stuff is an ore generation mod built around **data-driven ore nodes**. Instead of scattered single ore blocks, ores spawn as clusters called nodes. Find them with a scanner, mine them by hand, or hook up miners that drain them forever.

Everything is plain JSON in your config folder, so you can add a new ore, a new miner tier, or a new extraction tool without writing any code.

## Features

### Ore Nodes
- A node is a small cluster of blocks
- Every node rolls a hidden quality percentage that changes how it looks and how much it produces
- Per-dimension, per-biome spawning with weights and full biome overrides for every placement rule
- Nodes are mined by hand or drained by miners; they never run out (might change that later)

### Scanner
- Shift + Right-click to pick a target then Right-click to fire a scan pulse that reveals matching ore nodes within range
- The scanner HUD shows direction and distance to every hit

### Bio Scanner
- Hold use on a mob to scan it
- Completed scans unlock library entries

### Miners
- Place a miner against a node, feed it FE, and it extracts the ores
- Max miners per node is configurable per ore type (first placed, first served)
- Fully compatible with Forge energy / Fabric energy (Team Reborn)

### Manual Extraction
No machine? No problem.
- Left-click a node with any configured pickaxe to mine ores out of it
- Extract amount, cooldown, and durability cost are configurable per tool

## The Data-Driven System

Everything lives in `config/oresandstuff/`:

```text
config/oresandstuff/
├── orenodes/
├── miners/
├── pickaxes/
├── biolibrary/
└── runtime_pack/     <- auto-generated assets, do not edit
```

Default files are generated on first launch and never overwritten after that. Edit them freely, then restart the game (or world) to reload changes.

### Making Your Own Ore Node

Add a JSON file to `config/oresandstuff/orenodes/`, for example `diamond.json`:

```json
{
  "id": "oresandstuff:diamond",
  "output_item": "minecraft:diamond",
  "drops": {
    "minecraft:diamond": 100,
    "minecraft:stone": 100,
    "minecraft:end_stone": 100
  },
  "enabled": true,
  "hardness": 200.0,
  "base_rate_per_second": 0.3,
  "scanner_color": "#4DE1E1",
  "scanner_radius": 192,
  "quality_min": 20.0,
  "quality_max": 200.0,
  "quality_visuals": [
    {
      "min": 20.0,
      "max": 110.0,
      "node_block": "minecraft:stone",
      "visual_block": "minecraft:diamond_ore",
      "dimensions": ["minecraft:overworld"]
    },
    {
      "min": 110.0,
      "max": 200.0,
      "node_block": "minecraft:deepslate",
      "visual_block": "minecraft:diamond_ore",
      "dimensions": ["minecraft:overworld"]
    },
    {
      "min": 20.0,
      "max": 200.0,
      "node_block": "minecraft:end_stone",
      "visual_block": "minecraft:diamond_ore",
      "dimensions": ["minecraft:the_end"]
    }
  ],
  "dimensions": [
    "minecraft:overworld",
    "minecraft:the_end"
  ],
  "biomes": {
    "plains": 30,
    "forest": 25,
    "taiga": 25,
    "snowy": 20,
    "windswept": 20,
    "desert": 10,
    "swamp": 5,
    "river": 25,
    "ocean": 20,
    "cave": 30,
    "deep_dark": 20,
    "end_highlands": 30,
    "end_midlands": 25,
    "the_end": 20,
    "small_end_islands": 15,
    "end_barrens": 15
  },
  "biome_overrides": {
    "plains": {
      "quality_min": 20.0,
      "quality_max": 80.0
    },
    "end_highlands": {
      "quality_min": 130.0,
      "quality_max": 200.0
    }
  },
  "min_nodes_per_chunk": 1,
  "max_nodes_per_chunk": 2,
  "max_miners_per_node": 1,
  "min_spacing_blocks": 180,
  "placement_attempts": 2,
  "cluster_radius": 2,
  "scatter_count": 6,
  "surface_spawn": true,
  "min_y": 0,
  "max_y": 63
}
```

(Two biome overrides shown; add one block per biome you want to tweak.)

Field reference for every key above:

### Identity & Economy

| Field | Example | Details |
| --- | --- | --- |
| `id` | `"mymod:titanium"` | Unique id in `namespace:path` form. Use your own namespace (`mymod:titanium`) so you never collide with other node files. The path part doubles as a fallback name: `output_item` defaults to `minecraft:<path>_ore`. This is also the name shown in the scanner's type wheel. |
| `output_item` | `"minecraft:raw_iron"` | The primary item this node produces. Its icon represents the node in the scanner HUD and type wheel. Any item registry id works, like `"minecraft:amethyst_shard"`. |
| `base_rate_per_second` | `0.6` | Extraction speed in items per second, before multipliers. Final miner output = `base_rate × quality × tier multiplier`, where quality is the node's rolled quality % divided by 100 (150% quality gives 1.5× output). Hand extraction uses the same base through the pickaxe config. Use high values for common ores and low values for valuable ones; defaults are coal `0.9`, iron `0.6`, copper `0.7`, gold `0.5`, redstone `0.4`. |
| `scanner_color` | `"#D8AF37"` | Hex color (`#RGB` or `#RRGGBB`) representing this ore type. Colors the slice in the scanner's type wheel plus the in-world highlights and beacon pillars revealed by a scan pulse. |
| `scanner_radius` | `192` | Maximum distance in blocks at which a scan pulse can reveal this node type. Lower it to make rare ores harder to find. |
| `hardness` | `80.0` | Block strength of node blocks, same scale as vanilla (stone is 1.5, obsidian 50). Players cannot fully break nodes, but this drives the break progress shown when hitting them. Blast resistance stays at 1.2, so explosions can still destroy node blocks. |
| `enabled` | `true` | Master switch. Set to `false` to hide the node from worldgen and the scanner wheel without deleting the file; existing nodes keep working. |
| `max_miners_per_node` | `2` | How many miners may tap one node at once; extra miners stay idle with a `Max miners` status (first placed, first served). |

### Where Nodes Spawn

| Field | Example | Details |
| --- | --- | --- |
| `dimensions` | `["minecraft:overworld", "minecraft:the_nether"]` | List of dimension ids where this node may generate. Nether and End nodes get netherrack/end stone looks by default if you don't set `quality_visuals`. |
| `biomes` | `{ "snowy": 50, "peak": 40, "desert": 20 }` | Weighted biome filter. Keys are substrings matched against the full biome id: `snowy` matches `minecraft:snowy_plains` and `minecraft:snowy_taiga`. Weights are relative, so heavier weights make a type win the roll more often. A type with no matching key never spawns in a biome. |
| `min_nodes_per_chunk` / `max_nodes_per_chunk` | `1` / `4` | Controls the internal per-chunk spawn chance. Higher values mean fewer skipped chunks, so nodes feel denser. Treat it as density tuning, not an exact count per chunk. |
| `placement_attempts` | `2` | How many random positions per chunk the generator tries before giving up. Raise it in noisy terrain (nether, mountains) if nodes fail to place often. |
| `surface_spawn` | `true` | `true` = nodes are placed on the world surface (with slope and obstruction checks so they don't float or clip into cliffs). `false` = nodes are placed underground at a random `y` between `min_y` and `max_y` inside solid host stone, or on a cave floor if the spot turns out to be air. |
| `min_y` / `max_y` | `0` / `63` | Height window for underground placement. Ignored when `surface_spawn` is `true` (except as overridden per biome). |
| `min_spacing_blocks` | `180` | Minimum distance between two nodes of the same type, keeping ore fields from clumping into one giant vein. |
| `cluster_radius` | `3` | Radius of the main node blob (the blocks holding the actual `OreNodeBlockEntity`). Bigger values make chunkier nodes. |
| `scatter_count` | `6` | Extra decorative ore blocks scattered around the cluster. Visual only, though they do count for scanner highlights. |

### Quality & Looks

| Field | Example | Details |
| --- | --- | --- |
| `quality_min` / `quality_max` | `70.0` / `150.0` | Every node rolls one quality % (uniformly) inside this range when it generates. The roll is permanent for that node and drives both its look (see `quality_visuals`) and its production multiplier (see `base_rate_per_second`). Values are clamped to 1–1000. |
| `quality_visuals` | `[{"min": 70, "max": 110, "node_block": "minecraft:stone", "visual_block": "minecraft:iron_ore"}]` | Array of look tiers. A node whose rolled quality falls inside a tier's `[min, max]` window renders its main blocks as `node_block` and decorates the cluster with `visual_block`. |

- Each tier takes an optional `"dimensions"` list. Dimension-specific tiers beat unrestricted ones, and a dimension with no matching tier falls back to the closest covering one.
- Tier ranges must stay inside `quality_min..quality_max` or the whole file is rejected.
- This is how a poor iron node can look like plain stone while a rich one shows deepslate.

### What Nodes Give

| Field | Example | Details |
| --- | --- | --- |
| `drops` | `{ "minecraft:raw_iron": 100, "minecraft:cobblestone": 60 }` | Map of `item id → percent chance (0–100)`. Every entry is rolled on its own each extraction, so raw iron always drops while cobblestone drops 60% of the time additionally. If every roll misses, `output_item` is produced as a guaranteed fallback. |

### Biome Overrides

| Field | Example | Details |
| --- | --- | --- |
| `biome_overrides` | `{ "end_highlands": { "quality_min": 130.0, "max_y": 90 } }` | Map of biome substring to partial override object. The longest matching key wins (`snowy_slopes` beats `snowy`); anything an override doesn't set inherits from the top level. |

- Overridable fields: `min_nodes_per_chunk`, `max_nodes_per_chunk`, `cluster_radius`, `scatter_count`, `quality_min`, `quality_max`, `min_y`, `max_y`, `surface_spawn`, `placement_attempts`, `min_spacing_blocks`.
- Useful for richer-but-rarer nodes in specific biomes, like the End overrides in the example above.

### Making Your Own Miner Tier

Each tier gets a folder under `config/oresandstuff/miners/`:

```text
config/oresandstuff/miners/
└── mk11/
    ├── mk11.json          <- the tier definition
    ├── models/
    │   └── miner.json     <- the block model
    └── textures/
        └── miner_mk11.png <- textures referenced by the model
```

`mk11.json`:

```json
{
  "id": "mk11",
  "display_name": "Miner Mk11",
  "fe_per_tick": 1600,
  "buffer_fe": 1600000,
  "max_receive_fe": 6400,
  "rate_multiplier": 40.0,
  "model": "miner",
  "texture": ""
}
```

Field reference:

| Field | Example | Details |
| --- | --- | --- |
| `id` | `"mk11"` | Tier id. Must match the folder name and picks the block/item (`miner_mk11`). |
| `display_name` | `"Miner Mk11"` | Human-readable name shown in tooltips and UIs. |
| `fe_per_tick` | `1600` | Energy consumed per tick while actively mining. |
| `buffer_fe` | `1600000` | Internal energy storage size. The miner pulls this from adjacent energy sources (batteries, cables) up to `max_receive_fe` per tick. |
| `max_receive_fe` | `6400` | Maximum FE/tick the miner accepts from neighbors, limiting recharge speed. |
| `rate_multiplier` | `40.0` | Multiplies the connected node's extraction rate (see `base_rate_per_second` for the full formula). |
| `model` | `"miner"` | File name (without `.json`) looked up in the tier's `models/` folder. If that folder has no such file, the model falls back to the built-in Mk1 look. |

Textures live in the tier's `textures/` folder and are referenced as `oresandstuff:block/<file_name>` inside your model; every `.png` in there ships automatically. (`texture` itself is currently unused and reserved for future per-tier overrides.)

The mod builds a runtime resource pack from these folders at startup, generating blocks, items, blockstates, and models so a new tier shows up in game with no extra work.

### Configuring Manual Extraction (Pickaxes)

`config/oresandstuff/pickaxes/vanilla.json`:

```json
{
  "tools": [
    { "item": "minecraft:wooden_pickaxe", "extract_amount": 1, "cooldown_ticks": 60, "durability_cost": 2 },
    { "item": "minecraft:netherite_pickaxe", "extract_amount": 1, "cooldown_ticks": 10, "durability_cost": 1 }
  ]
}
```

Field reference (each entry in `tools`):

| Field | Example | Details |
| --- | --- | --- |
| `item` | `"minecraft:iron_pickaxe"` | Registry id of the tool that becomes a node extractor, modded ids included. Left-clicking a node with it chips items out instead of attacking. |
| `extract_amount` | `2` | How many extraction rolls the tool performs per hit, multiplied by the node's quality. |
| `cooldown_ticks` | `40` | Cooldown between extractions, applied to both the player and the held item (shown as the vanilla cooldown overlay). Also scales with node quality, so better nodes cool down faster. |
| `durability_cost` | `1` | Durability damage dealt to the tool per extraction. `0` makes the tool damage-free (fine for energy/unsided tools). |

### Bio Library Text (WIP)

Files in `config/oresandstuff/biolibrary/` override how entities appear in the bio scanner library:

```json
{
  "id": "minecraft:pig",
  "title": "Pig",
  "category": "Passive",
  "summary": "Mostly bacon with legs.",
  "facts": ["Can be saddled... emotionally."]
}
```

One file per entity, file name is arbitrary.

| Field | Example | Details |
| --- | --- | --- |
| `id` | `"minecraft:pig"` | Entity this entry describes. Required; entries without an id are skipped. |
| `title` | `"Pig"` | Display name in the library grid and detail view. Falls back to the entity's translated name. |
| `category` | `"Passive"` | Free-text grouping used by the library tabs/filters (like `Passive`, `Hostile`, `Boss`). Defaults to `Misc`. |
| `summary` | `"Mostly bacon with legs."` | Short description shown on the scanned entry. Defaults to `"No scan data."` |
| `facts` | `["Likes mud.", "Not a piglet."]` | Optional list of flavor lines listed as scan facts on the detail view. |

## For Mod Developers

An open, loader-agnostic API ships in `com.abo47.oresandstuff.api`:

- `BioScanEvents` + `BioScanEvent`: fired server-side whenever a player completes a bio scan
- `BioScanApi`: query/grant bio scan unlocks, including offline players by UUID (`grantScan(ServerLevel, UUID, ...)`)
- `MinerApi` / `MinerHandle`: query miners near a position
- `MiningEvents` / `MinerExtractEvent`: fired every time a miner produces items
- `NodeApi` / `OreNodeHandle`: query ore nodes

Example: share bio scan unlocks across a team so only one player has to scan each mob:

```java
BioScanEvents.register(event -> {
    if (!event.firstScan()) return;
    ServerLevel level = event.player().serverLevel();
    for (UUID mate : myTeamSystem.memberIds(event.player())) {
        if (!mate.equals(event.player().getUUID())) {
            BioScanApi.grantScan(level, mate, event.entityId());
        }
    }
});
```

## Dependencies

- Forge: LDLib 1.0.50
- Fabric: Fabric API and LDLib 1.0.50

## Notes

- My mods and texture packs are officially published <b>only</b> on Modrinth. Since this mod is licensed under the MIT License, you may also see reuploads elsewhere, so please download only from sources you trust and be careful with random files.

- An AI coding agent was used during development, just putting it out there for transparency. If that bothers you, that is completely fine: use it, avoid it, ignore it, or simply do what you want with it.

## License

<details>
<summary>MIT License</summary>

MIT License

Copyright (c) 2025 ABO47

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

</details>

## Third Party Licenses

<details>
<summary>ISC License (Lucide Icons)</summary>

Copyright (c) 2026 Lucide Icons and Contributors

Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

</details>
