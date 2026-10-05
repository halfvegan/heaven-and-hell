"""Block/item models, lang, loot tables, recipes, tags and advancements for Heaven & Hell."""
import os

from common import NS, RES, data_path, hh, write_json

# ---------------------------------------------------------------- ids
SIMPLE = ["cloud", "golden_cloud", "pearlstone", "pearlstone_bricks", "chiseled_pearlstone", "gilded_pearlstone",
          "golden_bricks", "celestial_planks", "celestial_leaves", "golden_leaves", "halo_lamp", "return_light",
          "brimstone", "brimstone_bricks", "hellstone_bricks", "chiseled_hellstone", "gilded_hellstone",
          "soul_shard_ore", "ash_block", "ember_lamp", "redemption_light", "heaven_soil"]
PILLARS = ["pearlstone_pillar", "celestial_log", "hellstone_pillar"]
STAIRS = {"pearlstone_brick_stairs": "pearlstone_bricks", "hellstone_brick_stairs": "hellstone_bricks"}
SLABS = {"pearlstone_brick_slab": "pearlstone_bricks", "hellstone_brick_slab": "hellstone_bricks"}
ALL_BLOCKS = SIMPLE + PILLARS + list(STAIRS) + list(SLABS) + ["heaven_grass", "angel_lily"]

HANDHELD = ["seraph_blade", "hellfire_sword", "morningstar", "infernal_pickaxe"]
GENERATED = ["angel_wings", "harp", "manna", "cloud_in_a_bottle", "feather_of_return", "golden_feather", "soul_shard",
             "demon_wings", "palace_key", "book_of_deeds"]
WORN = ["halo", "infernal_crown", "fallen_halo"]
VISIONS = ["vision_lava_cow", "vision_zombie_ambush", "vision_trapped_wolf", "vision_hungry_traveler",
           "vision_lost_satchel", "vision_devils_bargain", "vision_heaven", "vision_hell", "vision_lucifer",
           "vision_gatekeeper", "vision_scales"]

NAMES = {
    "cloud": "Cloud", "golden_cloud": "Golden Cloud", "heaven_grass": "Heaven Grass", "heaven_soil": "Heaven Soil",
    "pearlstone": "Pearlstone", "pearlstone_bricks": "Pearlstone Bricks",
    "pearlstone_brick_stairs": "Pearlstone Brick Stairs", "pearlstone_brick_slab": "Pearlstone Brick Slab",
    "chiseled_pearlstone": "Chiseled Pearlstone", "pearlstone_pillar": "Pearlstone Pillar",
    "gilded_pearlstone": "Gilded Pearlstone", "golden_bricks": "Golden Bricks", "celestial_log": "Celestial Log",
    "celestial_planks": "Celestial Planks", "celestial_leaves": "Celestial Leaves", "golden_leaves": "Golden Leaves",
    "angel_lily": "Angel Lily", "halo_lamp": "Halo Lamp", "return_light": "Light of Return",
    "brimstone": "Brimstone", "brimstone_bricks": "Brimstone Bricks", "hellstone_bricks": "Hellstone Bricks",
    "hellstone_brick_stairs": "Hellstone Brick Stairs", "hellstone_brick_slab": "Hellstone Brick Slab",
    "chiseled_hellstone": "Chiseled Hellstone", "hellstone_pillar": "Hellstone Pillar",
    "gilded_hellstone": "Gilded Hellstone", "soul_shard_ore": "Soul Shard Ore", "ash_block": "Ash",
    "ember_lamp": "Ember Lamp", "redemption_light": "Light of Redemption",
    "angel_wings": "Angel Wings", "halo": "Halo", "harp": "Harp of Peace", "seraph_blade": "Seraph Blade",
    "manna": "Manna", "cloud_in_a_bottle": "Cloud in a Bottle", "feather_of_return": "Feather of Return",
    "golden_feather": "Golden Feather", "soul_shard": "Soul Shard", "infernal_pickaxe": "Infernal Pickaxe",
    "hellfire_sword": "Hellfire Sword", "demon_wings": "Demon Wings", "infernal_crown": "Infernal Crown",
    "morningstar": "The Morningstar", "fallen_halo": "Fallen Halo", "palace_key": "Palace Key",
    "book_of_deeds": "Book of Deeds",
}


def asset(kind, name):
    return os.path.join(RES, "assets", NS, kind, f"{name}.json")


# ---------------------------------------------------------------- models
def block_models():
    for b in SIMPLE:
        write_json(asset("blockstates", b), {"variants": {"": {"model": hh(f"block/{b}")}}})
        write_json(asset("models/block", b), {"parent": "minecraft:block/cube_all",
                                               "textures": {"all": hh(f"block/{b}")}})
    for b in PILLARS:
        write_json(asset("blockstates", b), {"variants": {
            "axis=x": {"model": hh(f"block/{b}_horizontal"), "x": 90, "y": 90},
            "axis=y": {"model": hh(f"block/{b}")},
            "axis=z": {"model": hh(f"block/{b}_horizontal"), "x": 90}}})
        tex = {"end": hh(f"block/{b}_top"), "side": hh(f"block/{b}")}
        write_json(asset("models/block", b), {"parent": "minecraft:block/cube_column", "textures": tex})
        write_json(asset("models/block", f"{b}_horizontal"), {"parent": "minecraft:block/cube_column_horizontal",
                                                              "textures": tex})
    import json
    vanilla_stairs = json.load(open("/home/claude/misode/mcmeta-assets/assets/minecraft/blockstates/stone_brick_stairs.json"))
    for b, full in STAIRS.items():
        variants = {}
        for key, v in vanilla_stairs["variants"].items():
            nv = dict(v)
            nv["model"] = v["model"].replace("minecraft:block/stone_brick_stairs", hh(f"block/{b}"))
            variants[key] = nv
        write_json(asset("blockstates", b), {"variants": variants})
        tex = {"bottom": hh(f"block/{full}"), "side": hh(f"block/{full}"), "top": hh(f"block/{full}")}
        write_json(asset("models/block", b), {"parent": "minecraft:block/stairs", "textures": tex})
        write_json(asset("models/block", f"{b}_inner"), {"parent": "minecraft:block/inner_stairs", "textures": tex})
        write_json(asset("models/block", f"{b}_outer"), {"parent": "minecraft:block/outer_stairs", "textures": tex})
    for b, full in SLABS.items():
        write_json(asset("blockstates", b), {"variants": {
            "type=bottom": {"model": hh(f"block/{b}")},
            "type=double": {"model": hh(f"block/{full}")},
            "type=top": {"model": hh(f"block/{b}_top")}}})
        tex = {"bottom": hh(f"block/{full}"), "side": hh(f"block/{full}"), "top": hh(f"block/{full}")}
        write_json(asset("models/block", b), {"parent": "minecraft:block/slab", "textures": tex})
        write_json(asset("models/block", f"{b}_top"), {"parent": "minecraft:block/slab_top", "textures": tex})
    write_json(asset("blockstates", "heaven_grass"), {"variants": {"": {"model": hh("block/heaven_grass")}}})
    write_json(asset("models/block", "heaven_grass"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "bottom": hh("block/heaven_soil"), "side": hh("block/heaven_grass_side"), "top": hh("block/heaven_grass_top")}})
    write_json(asset("blockstates", "angel_lily"), {"variants": {"": {"model": hh("block/angel_lily")}}})
    write_json(asset("models/block", "angel_lily"), {"parent": "minecraft:block/cross",
                                                     "textures": {"cross": hh("block/angel_lily")}})
    # block items
    for b in ALL_BLOCKS:
        if b == "angel_lily":
            write_json(asset("models/item", b), {"parent": "minecraft:item/generated",
                                                 "textures": {"layer0": hh("block/angel_lily")}})
            write_json(asset("items", b), {"model": {"type": "minecraft:model", "model": hh(f"item/{b}")}})
        else:
            write_json(asset("items", b), {"model": {"type": "minecraft:model", "model": hh(f"block/{b}")}})


def box(frm, to, tex="#ring", rot=None, glow=False):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        faces[f] = {"texture": tex, "uv": [0, 0, 16, 16] if f in ("up", "down") else [0, 6, 16, 8]}
    e = {"from": frm, "to": to, "faces": faces}
    if rot:
        e["rotation"] = rot
    if glow:
        e["light_emission"] = 15
    return e


def ring(y0, y1, half, width, center=8.0, gap=False, glow=True):
    """An octagonal ring lying flat (8 thin bars)."""
    c = center
    els = [
        box([c - half * 0.42, y0, c - half], [c + half * 0.42, y1, c - half + width], glow=glow),      # north
        box([c - half * 0.42, y0, c + half - width], [c + half * 0.42, y1, c + half], glow=glow),      # south
        box([c - half, y0, c - half * 0.42], [c - half + width, y1, c + half * 0.42], glow=glow),      # west
        box([c + half - width, y0, c - half * 0.42], [c + half, y1, c + half * 0.42], glow=glow),      # east
    ]
    d = half * 0.71
    for sx, sz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        if gap and sx == 1 and sz == 1:
            continue
        cx, cz = c + sx * d * 0.82, c + sz * d * 0.82
        els.append(box([cx - half * 0.3, y0, cz - width / 2], [cx + half * 0.3, y1, cz + width / 2],
                       rot={"angle": 45.0 * (1 if sx == sz else -1), "axis": "y", "origin": [cx, y0, cz]}, glow=glow))
    return els


HAND_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, -4, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, -1, 1.13], "scale": [0.68, 0.68, 0.68]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def worn_models():
    halo = ring(16.4, 17.2, 6.4, 1.2)
    write_json(asset("models/item", "halo_worn"), {
        "textures": {"ring": hh("item/halo_worn"), "particle": hh("item/halo_worn")},
        "elements": halo, "display": HAND_DISPLAY})
    fallen = ring(16.2, 17.0, 6.4, 1.2, gap=True, glow=False)
    for e in fallen:
        e.setdefault("rotation", {"angle": 0.0, "axis": "y", "origin": [8, 16.6, 8]})
    # tilt the whole broken ring by rotating the straight bars on x
    for e in fallen[:4]:
        e["rotation"] = {"angle": -22.5, "axis": "x", "origin": [8, 16.6, 8]}
    write_json(asset("models/item", "fallen_halo_worn"), {
        "textures": {"ring": hh("item/fallen_halo_worn"), "particle": hh("item/fallen_halo_worn")},
        "elements": fallen, "display": HAND_DISPLAY})
    # crown: a band around the top of the head with spikes
    band = [
        box([1.4, 14.2, 1.4], [14.6, 16.6, 2.4], "#crown"),
        box([1.4, 14.2, 13.6], [14.6, 16.6, 14.6], "#crown"),
        box([1.4, 14.2, 2.4], [2.4, 16.6, 13.6], "#crown"),
        box([13.6, 14.2, 2.4], [14.6, 16.6, 13.6], "#crown"),
    ]
    spikes = []
    for x, z in ((2.0, 2.0), (8.0, 1.6), (14.0, 2.0), (1.6, 8.0), (14.4, 8.0), (2.0, 14.0), (8.0, 14.4), (14.0, 14.0)):
        h = 19.6 if (x == 8.0 or z == 8.0) else 18.4
        spikes.append(box([x - 0.6, 16.6, z - 0.6], [x + 0.6, h, z + 0.6], "#crown"))
    gems = [box([7.3, 15.0, 1.0], [8.7, 16.0, 1.4], "#crown", glow=True)]
    write_json(asset("models/item", "infernal_crown_worn"), {
        "textures": {"crown": hh("item/infernal_crown_worn"), "particle": hh("item/infernal_crown_worn")},
        "elements": band + spikes + gems, "display": HAND_DISPLAY})


def item_models():
    for i in HANDHELD:
        write_json(asset("models/item", i), {"parent": "minecraft:item/handheld", "textures": {"layer0": hh(f"item/{i}")}})
        write_json(asset("items", i), {"model": {"type": "minecraft:model", "model": hh(f"item/{i}")}})
    for i in GENERATED + VISIONS:
        write_json(asset("models/item", i), {"parent": "minecraft:item/generated", "textures": {"layer0": hh(f"item/{i}")}})
        write_json(asset("items", i), {"model": {"type": "minecraft:model", "model": hh(f"item/{i}")}})
    for i in WORN:
        write_json(asset("models/item", i), {"parent": "minecraft:item/generated", "textures": {"layer0": hh(f"item/{i}")}})
        write_json(asset("items", i), {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"],
                       "model": {"type": "minecraft:model", "model": hh(f"item/{i}")}}],
            "fallback": {"type": "minecraft:model", "model": hh(f"item/{i}_worn")}}})
    worn_models()
    for w in ("angel_wings", "demon_wings"):
        write_json(asset("equipment", w), {"layers": {"wings": [{"texture": hh(w)}]}})


def lang():
    tr = {"itemGroup.heavenhell.main": "Heaven & Hell"}
    for b in ALL_BLOCKS:
        tr[f"block.heavenhell.{b}"] = NAMES[b]
        tr[f"item.heavenhell.{b}"] = NAMES[b]
    for i in HANDHELD + GENERATED + WORN:
        tr[f"item.heavenhell.{i}"] = NAMES[i]
    for v in VISIONS:
        tr[f"item.heavenhell.{v}"] = "Vision"
    tr.update({
        "entity.heavenhell.angel": "Angel", "entity.heavenhell.gatekeeper": "The Gatekeeper",
        "entity.heavenhell.imp": "Imp", "entity.heavenhell.lucifer": "Lucifer",
        "biome.heavenhell.elysian_fields": "Elysian Fields", "biome.heavenhell.golden_groves": "Golden Groves",
        "biome.heavenhell.cloud_peaks": "Cloud Peaks", "biome.heavenhell.brimstone_wastes": "Brimstone Wastes",
        "biome.heavenhell.soul_pits": "Soul Pits", "biome.heavenhell.infernal_spires": "Infernal Spires",
    })
    write_json(asset("lang", "en_us"), tr)


# ---------------------------------------------------------------- loot
def item_entry(name, count=None, extra=None):
    e = {"type": "minecraft:item", "name": name}
    mods = []
    if count is not None:
        lo, hi = count if isinstance(count, tuple) else (count, count)
        mods.append({"type": "minecraft:set_count", "count": lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if extra:
        mods.extend(extra)
    if mods:
        e["modifier"] = mods
    return e


def block_loot():
    SILK = "minecraft:tool/can_silk_touch"
    SHEAR_OR_SILK = {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", SILK]}
    for b in ALL_BLOCKS:
        if b in ("return_light", "redemption_light"):
            continue
        name = hh(b)
        seq = f"heavenhell:blocks/{b}"
        if b in SLABS:
            pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": name, "modifier": [
                {"type": "minecraft:set_count", "count": 2,
                 "condition": {"type": "minecraft:match_block", "blocks": name, "state": {"type": "double"}}},
                {"type": "minecraft:explosion_decay"}]}]}]
        elif b == "heaven_grass":
            pools = [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": SILK, "name": name},
                {"type": "minecraft:item", "condition": {"type": "minecraft:survives_explosion"}, "name": hh("heaven_soil")}]}]}]
        elif b in ("celestial_leaves", "golden_leaves"):
            chances = [0.04, 0.05, 0.065, 0.08, 0.12] if b == "celestial_leaves" else [0.08, 0.1, 0.12, 0.15, 0.2]
            pools = [
                {"rolls": 1, "entries": [{"type": "minecraft:item", "condition": SHEAR_OR_SILK, "name": name}]},
                {"rolls": 1, "condition": {"type": "minecraft:inverted", "term": SHEAR_OR_SILK},
                 "entries": [{"type": "minecraft:item", "name": hh("golden_feather"),
                              "condition": {"type": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                                            "chances": chances},
                              "modifier": [{"type": "minecraft:explosion_decay"}]}]}]
        elif b == "soul_shard_ore":
            pools = [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": SILK, "name": name},
                {"type": "minecraft:item", "name": hh("soul_shard"), "modifier": [
                    {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                    {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"type": "minecraft:explosion_decay"}]}]}]}]
        else:
            pools = [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                      "entries": [{"type": "minecraft:item", "name": name}]}]
        write_json(data_path(NS, "loot_table/blocks", b), {"type": "minecraft:block", "pools": pools,
                                                           "random_sequence": seq})


def table(kind, path, pools):
    write_json(data_path(NS, f"loot_table/{path.split('/')[0]}", path.split("/", 1)[1]),
               {"type": f"minecraft:{kind}", "pools": pools, "random_sequence": f"heavenhell:{path}"})


def one(name, count=None, extra=None, condition=None):
    p = {"rolls": 1, "entries": [item_entry(name, count, extra)]}
    if condition:
        p["condition"] = condition
    return p


def looting(lo=0.0, hi=1.0):
    return [{"type": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
             "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]


def other_loot():
    table("entity", "entities/imp", [one(hh("soul_shard"), (0, 2), looting())])
    table("entity", "entities/angel", [one(hh("golden_feather"), (1, 2), looting()), one(hh("manna"), (0, 1))])
    write_json(data_path(NS, "loot_table/entities", "gatekeeper"),
               {"type": "minecraft:entity", "random_sequence": "heavenhell:entities/gatekeeper"})
    table("entity", "entities/lucifer", [one(hh("morningstar")), one(hh("fallen_halo")),
                                         one(hh("soul_shard"), (16, 32)), one("minecraft:netherite_ingot", (1, 2))])
    chests = {
        "hall_wings": [(hh("angel_wings"), None), (hh("golden_feather"), (2, 4))],
        "hall_halo": [(hh("halo"), None), (hh("manna"), (4, 8))],
        "hall_harp": [(hh("harp"), None), (hh("golden_feather"), (2, 3))],
        "hall_blade": [(hh("seraph_blade"), None)],
        "hall_manna": [(hh("manna"), (8, 16)), ("minecraft:golden_apple", (1, 2))],
        "hall_clouds": [(hh("cloud_in_a_bottle"), (4, 8)), (hh("cloud"), (16, 32)), (hh("golden_cloud"), (8, 16))],
        "hall_return": [(hh("feather_of_return"), (2, 3)), (hh("book_of_deeds"), None)],
        "quarters_rank_1": [("minecraft:bread", (4, 8)), ("minecraft:iron_ingot", (2, 5)), (hh("soul_shard"), (1, 3))],
        "quarters_rank_2": [("minecraft:cooked_beef", (6, 12)), ("minecraft:gold_ingot", (3, 6)), ("minecraft:iron_sword", None)],
        "quarters_rank_3": [("minecraft:golden_apple", (2, 3)), ("minecraft:diamond", (2, 4)),
                            ("minecraft:experience_bottle", (4, 8))],
        "quarters_rank_4": [("minecraft:diamond", (4, 8)), ("minecraft:netherite_scrap", (1, 2)),
                            ("minecraft:golden_apple", 4), ("minecraft:gold_block", (2, 4))],
        "quarters_rank_5": [("minecraft:netherite_ingot", None), ("minecraft:enchanted_golden_apple", None),
                            ("minecraft:diamond", (8, 12)), ("minecraft:emerald_block", (2, 4))],
    }
    for name, items in chests.items():
        table("chest", f"chests/{name}", [one(i, c) for i, c in items])


# ---------------------------------------------------------------- recipes
def shaped(name, pattern, key, result, count=1, category="building"):
    write_json(data_path(NS, "recipe", name), {"type": "minecraft:crafting_shaped", "category": category,
                                               "key": key, "pattern": pattern,
                                               "result": {"count": count, "id": result}})


def shapeless(name, ingredients, result, count=1, category="misc"):
    write_json(data_path(NS, "recipe", name), {"type": "minecraft:crafting_shapeless", "category": category,
                                               "ingredients": ingredients, "result": {"count": count, "id": result}})


def cutting(name, ingredient, result, count=1):
    r = {"type": "minecraft:stonecutting", "ingredient": ingredient, "result": {"id": result}}
    if count > 1:
        r["result"]["count"] = count
    write_json(data_path(NS, "recipe", name), r)


def recipes():
    for stone, bricks, stairs, slab, chiseled, pillar in (
            ("pearlstone", "pearlstone_bricks", "pearlstone_brick_stairs", "pearlstone_brick_slab",
             "chiseled_pearlstone", "pearlstone_pillar"),
            ("brimstone_bricks", "hellstone_bricks", "hellstone_brick_stairs", "hellstone_brick_slab",
             "chiseled_hellstone", "hellstone_pillar")):
        if stone == "pearlstone":
            shaped(bricks, ["##", "##"], {"#": hh(stone)}, hh(bricks), 4)
        shaped(stairs, ["#  ", "## ", "###"], {"#": hh(bricks)}, hh(stairs), 4)
        shaped(slab, ["###"], {"#": hh(bricks)}, hh(slab), 6)
        shaped(chiseled, ["#", "#"], {"#": hh(slab)}, hh(chiseled), 1)
        shaped(pillar, ["#", "#"], {"#": hh(bricks)}, hh(pillar), 2)
        for src in ([stone, bricks] if stone == "pearlstone" else [bricks]):
            if src != bricks:
                cutting(f"{bricks}_from_{src}_stonecutting", hh(src), hh(bricks))
            cutting(f"{stairs}_from_{src}_stonecutting", hh(src), hh(stairs))
            cutting(f"{slab}_from_{src}_stonecutting", hh(src), hh(slab), 2)
            cutting(f"{chiseled}_from_{src}_stonecutting", hh(src), hh(chiseled))
            cutting(f"{pillar}_from_{src}_stonecutting", hh(src), hh(pillar))
    shaped("brimstone_bricks", ["##", "##"], {"#": hh("brimstone")}, hh("brimstone_bricks"), 4)
    cutting("brimstone_bricks_from_brimstone_stonecutting", hh("brimstone"), hh("brimstone_bricks"))
    shaped("hellstone_bricks", ["BK", "KB"], {"B": hh("brimstone_bricks"), "K": "minecraft:blackstone"},
           hh("hellstone_bricks"), 4)
    shapeless("gilded_pearlstone", [hh("pearlstone_bricks"), "minecraft:gold_nugget"], hh("gilded_pearlstone"), 1, "building")
    shapeless("gilded_hellstone", [hh("hellstone_bricks"), "minecraft:gold_nugget"], hh("gilded_hellstone"), 1, "building")
    shaped("golden_bricks", ["###", "#G#", "###"], {"#": hh("pearlstone_bricks"), "G": "minecraft:gold_ingot"},
           hh("golden_bricks"), 8)
    shapeless("celestial_planks", [hh("celestial_log")], hh("celestial_planks"), 4, "building")
    shaped("halo_lamp", [" G ", "GLG", " G "], {"G": "minecraft:gold_ingot", "L": "minecraft:glowstone"}, hh("halo_lamp"))
    shaped("ember_lamp", [" I ", "IMI", " I "], {"I": "minecraft:iron_ingot", "M": "minecraft:magma_block"}, hh("ember_lamp"))
    shapeless("cloud_in_a_bottle", ["minecraft:glass_bottle", hh("cloud"), hh("cloud")], hh("cloud_in_a_bottle"))
    shaped("angel_wings", ["FCF", "F F", "F F"], {"F": hh("golden_feather"), "C": hh("cloud")}, hh("angel_wings"),
           category="equipment")
    shaped("harp", ["GSG", "G G", " P "], {"G": "minecraft:gold_ingot", "S": "minecraft:string",
                                           "P": hh("celestial_planks")}, hh("harp"), category="misc")
    shapeless("manna", ["minecraft:wheat", "minecraft:wheat", "minecraft:honey_bottle"], hh("manna"), 2)
    shapeless("feather_of_return", [hh("golden_feather"), "minecraft:feather", "minecraft:ender_pearl"],
              hh("feather_of_return"))
    for kind, t in (("smelting", 200), ("blasting", 100)):
        write_json(data_path(NS, "recipe", f"soul_shard_from_{kind}"), {
            "type": f"minecraft:{kind}", "category": "misc", "cookingtime": t, "experience": 1.0,
            "group": "soul_shard", "ingredient": hh("soul_shard_ore"), "result": {"id": hh("soul_shard")}})


# ---------------------------------------------------------------- tags
def tags():
    t = lambda kind, name, values: write_json(data_path("minecraft", f"tags/{kind}", name),
                                              {"replace": False, "values": values})
    pick = ["pearlstone", "pearlstone_bricks", "pearlstone_brick_stairs", "pearlstone_brick_slab", "chiseled_pearlstone",
            "pearlstone_pillar", "gilded_pearlstone", "golden_bricks", "brimstone", "brimstone_bricks",
            "hellstone_bricks", "hellstone_brick_stairs", "hellstone_brick_slab", "chiseled_hellstone",
            "hellstone_pillar", "gilded_hellstone", "soul_shard_ore", "halo_lamp", "ember_lamp"]
    t("block", "mineable/pickaxe", [hh(b) for b in pick])
    t("block", "mineable/axe", [hh("celestial_log"), hh("celestial_planks")])
    t("block", "mineable/shovel", [hh(b) for b in ("heaven_grass", "heaven_soil", "ash_block", "cloud", "golden_cloud")])
    t("block", "mineable/hoe", [hh("celestial_leaves"), hh("golden_leaves")])
    t("block", "needs_stone_tool", [hh("soul_shard_ore")])
    t("block", "planks", [hh("celestial_planks")])
    t("item", "planks", [hh("celestial_planks")])
    t("block", "logs_that_burn", [hh("celestial_log")])
    t("item", "logs_that_burn", [hh("celestial_log")])
    t("block", "leaves", [hh("celestial_leaves"), hh("golden_leaves")])
    t("item", "leaves", [hh("celestial_leaves"), hh("golden_leaves")])
    t("block", "stairs", [hh("pearlstone_brick_stairs"), hh("hellstone_brick_stairs")])
    t("block", "slabs", [hh("pearlstone_brick_slab"), hh("hellstone_brick_slab")])
    t("item", "swords", [hh("seraph_blade"), hh("hellfire_sword"), hh("morningstar")])
    t("item", "pickaxes", [hh("infernal_pickaxe")])


# ---------------------------------------------------------------- advancements
def adv(name, parent, icon, title, desc, criteria=None, frame=None, hidden=False, toast=True, chat=True):
    obj = {
        "display": {"icon": {"id": icon}, "title": {"text": title}, "description": {"text": desc},
                    "show_toast": toast, "announce_to_chat": chat, "hidden": hidden},
        "criteria": criteria or {"granted": {"trigger": "minecraft:impossible"}},
    }
    if frame:
        obj["display"]["frame"] = frame
    if parent:
        obj["parent"] = hh(parent)
    write_json(data_path(NS, "advancement", name), obj)


def advancements():
    write_json(data_path(NS, "advancement", "root"), {
        "display": {"icon": {"id": hh("book_of_deeds")}, "title": {"text": "Heaven & Hell"},
                    "description": {"text": "Every choice is remembered."},
                    "background": "minecraft:gui/advancements/backgrounds/stone",
                    "show_toast": False, "announce_to_chat": False},
        "criteria": {"tick": {"trigger": "minecraft:tick"}}})
    adv("moment", "root", hh("vision_lava_cow"), "A Moment of Choice", "Face your first moral choice")
    adv("saint", "moment", hh("halo"), "Saintly", "Reach 50 karma", frame="goal")
    adv("damned", "moment", hh("soul_shard"), "Damned", "Fall to -50 karma", frame="goal")
    adv("heaven", "root", hh("cloud"), "Pearly Gates", "Wake up in Heaven")
    adv("second_chance", "heaven", hh("feather_of_return"), "Second Chance", "Return to the living world from Heaven")
    adv("wings", "heaven", hh("angel_wings"), "Wings of Light", "Obtain a pair of Angel Wings", criteria={
        "wings": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": hh("angel_wings")}]}}})
    adv("hell", "root", hh("brimstone"), "Abandon All Hope", "Be cast into Hell")
    ranks = [("Imp", hh("infernal_pickaxe")), ("Fiend", hh("hellfire_sword")), ("Demon", hh("demon_wings")),
             ("Archdemon", hh("infernal_crown")), ("Prince of Hell", hh("palace_key"))]
    parent = "hell"
    for i, (name, icon) in enumerate(ranks, start=1):
        adv(f"rank_{i}", parent, icon, name, f"Rise to {name} in Lucifer's court",
            frame="challenge" if i == 5 else ("goal" if i == 3 else None))
        parent = f"rank_{i}"
    adv("morningstar_fallen", "rank_3", hh("morningstar"), "The Morningstar Falls", "Defeat Lucifer on his own throne",
        frame="challenge", criteria={"kill": {"trigger": "minecraft:player_killed_entity",
                                              "conditions": {"entity": {"type": "minecraft:entity_properties", "entity": "this",
                                                                        "predicate": {"minecraft:entity_type": hh("lucifer")}}}}})
    adv("redemption", "morningstar_fallen", hh("feather_of_return"), "Redemption",
        "Escape Hell through the Redemption Gate", frame="challenge")


def build():
    block_models()
    item_models()
    lang()
    block_loot()
    other_loot()
    recipes()
    tags()
    advancements()


if __name__ == "__main__":
    build()
    print("resources written")
