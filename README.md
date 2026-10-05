# Heaven & Hell

A Minecraft **Java 26.3** mod (Fabric) by **timeywimey**.

Every so often, time freezes and the world asks you to choose. Save the cow that's about to stumble into lava,
or let it fall? Feed the starving traveler, or rob him? Every choice changes your **karma**. When you die, your
soul is weighed:

* **Heaven**: a sea of clouds and floating islands with endless daylight. You'll find the Pearly Gates, the
  Gatekeeper, angels, the Hall of Wonders (angel wings that let you fly, a halo, a harp of peace and more), and
  cottages where you can live in peace. Whenever you like, the Gate of Return will send you back to the living
  world.
* **Hell**: a burning cavern-world ruled by **Lucifer** from his throne in the Infernal Court. Mine Soul Shards
  and offer them to him to climb the ranks (Imp → Fiend → Demon → Archdemon → Prince of Hell). Each rank brings
  better perks, gear and more lavish quarters. The only way out is to defeat Lucifer himself.

## Install
1. Install the **Fabric Loader** for Minecraft 26.3 (https://fabricmc.net/use/installer/).
2. Put **Fabric API** (for 26.3) and the `heaven-and-hell-*.jar` from this repo's
   [Actions → build → Artifacts](../../actions) (or the `ci/build` branch) in your `.minecraft/mods` folder.
3. Start Minecraft with the Fabric profile. Existing worlds work too: Heaven and Hell appear the first
   time someone goes there.

## Commands
* `/heavenhell karma`: your karma, and where you'd go if you died right now
* `/heavenhell deeds`: open your Book of Deeds

Creator tools (cheats on):
* `/heavenhell moment <lava_animal|zombie_ambush|trapped_wolf|hungry_traveler|lost_satchel|devils_bargain>`: trigger a choice right now
* `/heavenhell send <player> heaven|hell|living`
* `/heavenhell karma set|add <player> <value>`, `/heavenhell rank <player> <0-5>`, `/heavenhell free <player>`
* `/heavenhell kit heaven|hell`: all the special items
* `/heavenhell moments on|off|every <min> <max>`, `/heavenhell judgment on|off`

## Building
GitHub Actions builds the mod on every push (Java 25, Gradle). The workflow also runs game tests and takes
in-game screenshots.
