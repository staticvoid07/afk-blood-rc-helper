# AFK Blood RC Helper

Contextual highlights, whole-screen idle tint, path lines and a stage panel for Arceuus blood runecrafting.

Assumes 73+ Agility (uses the 69 and 73 shortcuts) and blood runes only.

## The loop

1. **Load 1:** mine a full inventory of dense essence, climb the 69 shortcut, venerate at the Dark Altar, chisel everything into fragments.
2. **Load 2:** climb back down, mine another full inventory, venerate, walk to the Blood Altar, bind, chisel the dark blocks, bind again, then take the 73 shortcut back to the runestones.

## Features

- **Next-action highlight:** flashes the next runestone, rock shortcut or altar. If the rocks or the Blood Altar are clickable on screen they are flashed directly; otherwise the (walkable) tiles to walk to are highlighted.
- **Instant return prompt:** after the final bind, the 73 rocks are highlighted immediately instead of after the idle delay.
- **Step Closer to Blood Altar (optional):** after venerating the second load, step a few tiles south of the Dark Altar and click the Blood Altar from there.
- **Path lines:** walking path to the next destination on the floor and/or minimap.
- **Inventory highlight:** chisel, dark essence block and inactive blood essence.
- **Screen tint:** idle and active tints cover the whole client, including the inventory and side panels.
- **Info panel:** current stage, load (1/2 or 2/2), whether you carry fragments, and the next step.
- **Survives relogs:** which load you're on is saved per account and restored on login.

## Credits

Dark Altar approach tile, Blood Altar reach and the Dark Altar to Blood Altar route are from
[Jam's Arceuus Runecrafting](https://github.com/JamsRepos/zeah-rc-helper) (BSD 2-Clause).
