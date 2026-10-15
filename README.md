# The Poolrooms

A Minecraft **Java 26.3** mod (Fabric) by **timeywimey**.

Swim down a Strange Drain into the Poolrooms: an endless, brightly lit maze of tiled pools. Find three wristbands,
find the EXIT, and whatever you do... don't run.

It's horror-flavoured but gentle. There are no jump scares, no screaming, no gore and no dark rooms. Nothing in the
Poolrooms can hurt you except ordinary drowning, and the Snorkel Mask stops that.

## Getting in
1. Craft a **Strange Drain**: iron bars above and below, nether quartz on each side, an ender pearl in the middle.
2. Put it at the bottom of water at least 2 blocks deep.
3. Swim down and right-click it. Bubbles swirl for 3 seconds, the screen fades to white, and you arrive in the
   **Entrance Pool**.

## The Entrance Pool
A safe room with a welcome sign, the pool rules and the **Lost & Found** locker. The Lost & Found always has the
**Chlorine Compass**, a **Snorkel Mask** and a **Pool Noodle** (plus snacks). The Lifeguard can never come in here.

## Getting out
Find the **blue**, **yellow** and **red wristbands**, then find an **EXIT door** (under a glowing green EXIT sign).
Right-click it with all three wristbands (the door keeps them) and walk through the light. You come out in the water
above the drain you came in through.

If you die in the Poolrooms you respawn at your bed as normal; your items stay where you fell.

## The rooms
Endless rooms joined by open doorways:

* **Pool Hall**: a big tiled room with a rectangular pool and pillars
* **Flooded Corridors**: narrow hallways with ankle-deep water and arches
* **Pillar Forest**: thin columns rising out of shallow water
* **Skylight Atrium**: a tall room with a glowing fake-sky ceiling
* **The Deep End**: a 20-block-deep pool with an underwater tunnel; the **blue wristband** is at the bottom (there's
  a pocket of air in the wristband chamber)
* **Slide Tower**: tile stairs up a tower and a water slide back down; the **yellow wristband** is at the top
* **Lounge**: pool chairs, towels and lockers full of loot
* **Lifeguard Office**: a high chair and a desk; the **red wristband** is on the desk
* **Exit Room**: rare, with the EXIT door

All three wristband rooms and one Exit Room are always within a short walk of the Entrance Pool. Every room is evenly
lit; only the bottom of the Deep End is dim (the Snorkel Mask fixes that). You hear a quiet light hum, lapping water,
drips and the Lifeguard's whistle. There is no music.

## The Lifeguard
The only creature here: very tall, blank-faced, in a faded red lifeguard swimsuit, with a whistle.

* It appears far away (40 to 64 blocks) where you can't see it, about 45 seconds after you arrive.
* You always hear **two short whistle blasts** when it comes within about 30 blocks.
* It walks slowly and never runs. When it isn't after anyone, it keeps its distance and wanders away from you.
* **No running!** It only comes after you if it sees you sprinting (one long whistle). Walking and sneaking are fine.
* Stop running and stay out of its sight for 10 seconds and it loses interest. Being in the water counts as out of
  sight (it can't follow you in), and it can never follow you into the Entrance Pool.
* If it reaches you, it does no damage: the screen fades to white and you're back at the Entrance Pool with all your
  things, wristbands included. It comes back a minute later.
* It can't be hurt, but the Pool Noodle knocks it back. Creative and spectator players are ignored.
* Turn it off any time with `/poolrooms lifeguard off` (it's on by default).

## Items
* **Snorkel Mask**: wear it to breathe and see clearly underwater
* **Flippers**: wear them to swim much faster
* **Pool Noodle**: a harmless toy with huge knockback
* **Chlorine Compass**: points to the next wristband you still need, then to the nearest EXIT
* **Blue, Yellow and Red Wristbands**, the **Strange Drain**
* Blocks to build with: white, aqua, mosaic and lane-line pool tiles, tile stairs and slabs, ceiling lights, fake-sky
  panels, pool lights, pool ladders, pool chairs, lockers, life rings and EXIT signs

## Commands
* `/poolrooms` or `/poolrooms help`: the rules
* `/poolrooms lifeguard on|off`: turn the Lifeguard on or off (anyone can use this)

Creator tools (cheats on):
* `/poolrooms enter`: go to the Entrance Pool
* `/poolrooms exit`: go back home
* `/poolrooms kit`: every Poolrooms item

## Install
1. Install **Fabric Loader 0.19.5 or newer** for Minecraft 26.3 (https://fabricmc.net/use/installer/).
2. Put **Fabric API 0.161.0+26.3** (Modrinth or CurseForge) in your `.minecraft/mods` folder.
3. Put `poolrooms-1.0.0.jar` in the same `mods` folder.
4. Start Minecraft with the Fabric profile. Minecraft 26.3 needs Java 25; the official launcher provides it.

Works in existing worlds. It also works next to Heaven & Hell, but if Heaven & Hell's judgment is on, dying anywhere
(including drowning in the Poolrooms) sends you to Heaven or Hell instead of your bed. Use `/heavenhell judgment off`
and `/heavenhell moments off` for a Poolrooms-only playthrough.

## Building
GitHub Actions builds the mod on every push (Java 25, Gradle). It also runs game tests, plays through the mod in a
real client (taking screenshots of every room) and checks every Lifeguard rule with real key presses. Results go to
the `ci/pool-build` and `ci/pool-client` branches.

Textures, models and data come from the scripts in `tools/art` and `tools/gen`, the sounds from `tools/audio`.
