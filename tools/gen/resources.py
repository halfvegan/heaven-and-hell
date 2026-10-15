"""Blockstates, models, item definitions, lang, loot tables, recipes, tags, advancements, sounds and fonts."""
import json

from common import NS, VANILLA_ASSETS, asset, data_path, pr, write_json

TILES = ["white_pool_tiles", "aqua_pool_tiles", "mosaic_pool_tiles", "lane_line_tiles"]
GLOW = ["ceiling_light", "fake_sky_panel", "pool_light"]
STAIRS = {"white_pool_tile_stairs": "white_pool_tiles", "aqua_pool_tile_stairs": "aqua_pool_tiles"}
SLABS = {"white_pool_tile_slab": "white_pool_tiles", "aqua_pool_tile_slab": "aqua_pool_tiles"}
WRISTBANDS = ["blue_wristband", "yellow_wristband", "red_wristband"]
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

BLOCK_ITEMS = TILES + GLOW + list(STAIRS) + list(SLABS) + ["pool_ladder", "locker", "pool_chair", "life_ring",
                                                           "strange_drain", "exit_sign"] + WRISTBANDS
ITEMS = ["snorkel_mask", "flippers", "pool_noodle", "chlorine_compass"]

NAMES = {
    "white_pool_tiles": "White Pool Tiles", "aqua_pool_tiles": "Aqua Pool Tiles", "mosaic_pool_tiles": "Mosaic Pool Tiles",
    "lane_line_tiles": "Lane Line Tiles", "white_pool_tile_stairs": "White Pool Tile Stairs",
    "white_pool_tile_slab": "White Pool Tile Slab", "aqua_pool_tile_stairs": "Aqua Pool Tile Stairs",
    "aqua_pool_tile_slab": "Aqua Pool Tile Slab", "ceiling_light": "Ceiling Light", "fake_sky_panel": "Fake Sky Panel",
    "pool_light": "Pool Light", "pool_ladder": "Pool Ladder", "locker": "Locker", "pool_chair": "Pool Chair",
    "life_ring": "Life Ring", "strange_drain": "Strange Drain", "exit_sign": "EXIT Sign",
    "blue_wristband": "Blue Wristband", "yellow_wristband": "Yellow Wristband", "red_wristband": "Red Wristband",
    "exit_door": "EXIT Door", "exit_light": "Open EXIT Door", "shallow_water": "Shallow Water",
    "lifeguard_barrier": "Lifeguard Barrier",
    "snorkel_mask": "Snorkel Mask", "flippers": "Flippers", "pool_noodle": "Pool Noodle",
    "chlorine_compass": "Chlorine Compass",
}

DESCRIPTIONS = {
    "item.poolrooms.snorkel_mask.desc": "Wear it: breathe and see clearly underwater",
    "item.poolrooms.flippers.desc": "Wear them: swim much faster",
    "item.poolrooms.pool_noodle.desc": "Harmless, but it pushes things (and the Lifeguard) away",
    "item.poolrooms.chlorine_compass.desc1": "Points to the next wristband you need,",
    "item.poolrooms.chlorine_compass.desc2": "then to the nearest EXIT door",
}


def model(name, obj):
    write_json(asset("models/block", name), obj)


def blockstate(name, obj):
    write_json(asset("blockstates", name), obj)


def item_def(name, model_id):
    write_json(asset("items", name), {"model": {"type": "minecraft:model", "model": model_id}})


def cube(name, texture=None, glow=False):
    tex = pr(f"block/{texture or name}")
    if glow:
        faces = {f: {"texture": "#all", "cullface": f} for f in ("down", "up", "north", "south", "west", "east")}
        model(name, {"parent": "minecraft:block/block", "textures": {"all": tex, "particle": tex},
                     "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "shade": False, "light_emission": 15,
                                   "faces": faces}]})
    else:
        model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})


def facing_variants(model_for, extra=None):
    """Blockstate variants for every horizontal facing (model faces north by default)."""
    variants = {}
    for facing, y in FACINGS.items():
        for key, m in (extra or {"": None}).items():
            state = f"facing={facing}" + (f",{key}" if key else "")
            v = {"model": model_for(key)}
            if y:
                v["y"] = y
            variants[state] = v
    return variants


def blocks():
    for b in TILES:
        cube(b)
        blockstate(b, {"variants": {"": {"model": pr(f"block/{b}")}}})
    for b in GLOW:
        cube(b, glow=True)
        blockstate(b, {"variants": {"": {"model": pr(f"block/{b}")}}})
    vanilla_stairs = json.load(open(f"{VANILLA_ASSETS}/blockstates/stone_brick_stairs.json"))
    for b, full in STAIRS.items():
        variants = {}
        for key, v in vanilla_stairs["variants"].items():
            nv = dict(v)
            nv["model"] = v["model"].replace("minecraft:block/stone_brick_stairs", pr(f"block/{b}"))
            variants[key] = nv
        blockstate(b, {"variants": variants})
        tex = {"bottom": pr(f"block/{full}"), "side": pr(f"block/{full}"), "top": pr(f"block/{full}")}
        model(b, {"parent": "minecraft:block/stairs", "textures": tex})
        model(f"{b}_inner", {"parent": "minecraft:block/inner_stairs", "textures": tex})
        model(f"{b}_outer", {"parent": "minecraft:block/outer_stairs", "textures": tex})
    for b, full in SLABS.items():
        blockstate(b, {"variants": {
            "type=bottom": {"model": pr(f"block/{b}")},
            "type=double": {"model": pr(f"block/{full}")},
            "type=top": {"model": pr(f"block/{b}_top")}}})
        tex = {"bottom": pr(f"block/{full}"), "side": pr(f"block/{full}"), "top": pr(f"block/{full}")}
        model(b, {"parent": "minecraft:block/slab", "textures": tex})
        model(f"{b}_top", {"parent": "minecraft:block/slab_top", "textures": tex})

    # pool ladder: like the vanilla ladder
    model("pool_ladder", {"parent": "minecraft:block/ladder", "textures": {"particle": pr("block/pool_ladder"),
                                                                            "texture": pr("block/pool_ladder")}})
    blockstate("pool_ladder", {"variants": facing_variants(lambda k: pr("block/pool_ladder"))})

    # strange drain: a thin grate on the floor (also when waterlogged)
    model("strange_drain", {
        "parent": "minecraft:block/block",
        "textures": {"top": pr("block/strange_drain"), "side": pr("block/strange_drain_side"),
                     "particle": pr("block/strange_drain")},
        "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
            "up": {"texture": "#top"},
            "down": {"texture": "#side", "cullface": "down"},
            "north": {"uv": [0, 14, 16, 16], "texture": "#side", "cullface": "north"},
            "south": {"uv": [0, 14, 16, 16], "texture": "#side", "cullface": "south"},
            "west": {"uv": [0, 14, 16, 16], "texture": "#side", "cullface": "west"},
            "east": {"uv": [0, 14, 16, 16], "texture": "#side", "cullface": "east"}}}]})
    blockstate("strange_drain", {"variants": {"waterlogged=false": {"model": pr("block/strange_drain")},
                                              "waterlogged=true": {"model": pr("block/strange_drain")}}})

    # wristbands lying flat, glowing a little
    for w in WRISTBANDS:
        model(w, {"parent": "minecraft:block/block", "ambientocclusion": False,
                  "textures": {"band": pr(f"block/{w}"), "particle": pr(f"block/{w}")},
                  "elements": [{"from": [1, 0.2, 1], "to": [15, 1.2, 15], "shade": False, "light_emission": 10,
                                "faces": {"up": {"uv": [1, 1, 15, 15], "texture": "#band"},
                                          "down": {"uv": [1, 1, 15, 15], "texture": "#band"}}}]})
        blockstate(w, {"variants": {"waterlogged=false": {"model": pr(f"block/{w}")},
                                    "waterlogged=true": {"model": pr(f"block/{w}")}}})

    # ankle-deep water: just a moving water surface a little above the floor
    model("shallow_water", {"textures": {"water": pr("block/shallow_water"), "particle": pr("block/shallow_water")},
                            "ambientocclusion": False,
                            "elements": [{"from": [0, 0, 0], "to": [16, 3, 16], "shade": False,
                                          "faces": {"up": {"texture": "#water"}}}]})
    blockstate("shallow_water", {"variants": {"": {"model": pr("block/shallow_water")}}})

    # the invisible Lifeguard-only wall
    model("lifeguard_barrier", {"textures": {"particle": pr("block/white_pool_tiles")}})
    blockstate("lifeguard_barrier", {"variants": {"": {"model": pr("block/lifeguard_barrier")}}})

    # locker
    model("locker", {"parent": "minecraft:block/orientable", "textures": {
        "front": pr("block/locker_front"), "side": pr("block/locker_side"), "top": pr("block/locker_top")}})
    blockstate("locker", {"variants": facing_variants(lambda k: pr("block/locker"))})

    # EXIT door (6 parts), open door light, EXIT sign
    for part in range(6):
        model(f"exit_door_{part}", {"parent": "minecraft:block/block", "textures": {
            "front": pr(f"block/exit_door_{part}"), "side": pr("block/exit_door_side"),
            "particle": pr("block/exit_door_side")},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "north": {"texture": "#front", "cullface": "north"},
                "south": {"texture": "#side", "cullface": "south"},
                "east": {"texture": "#side", "cullface": "east"},
                "west": {"texture": "#side", "cullface": "west"},
                "up": {"texture": "#side", "cullface": "up"},
                "down": {"texture": "#side", "cullface": "down"}}}]})
    parts = {f"part={p}": p for p in range(6)}
    blockstate("exit_door", {"variants": facing_variants(lambda k: pr(f"block/exit_door_{parts[k]}"),
                                                         {k: None for k in parts})})
    faces = {f: {"texture": "#light"} for f in ("north", "south", "east", "west", "up", "down")}
    model("exit_light", {"parent": "minecraft:block/block", "ambientocclusion": False,
                         "textures": {"light": pr("block/exit_light"), "particle": pr("block/exit_light")},
                         "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "shade": False, "light_emission": 15,
                                       "faces": faces}]})
    blockstate("exit_light", {"variants": facing_variants(lambda k: pr("block/exit_light"), {k: None for k in parts})})
    for side in ("left", "right"):
        model(f"exit_sign_{side}", {"parent": "minecraft:block/block", "ambientocclusion": False,
                                    "textures": {"front": pr(f"block/exit_sign_{side}"),
                                                 "back": pr("block/exit_sign_back"),
                                                 "particle": pr("block/exit_sign_back")},
                                    "elements": [{"from": [0, 3, 14], "to": [16, 13, 16], "shade": False,
                                                  "light_emission": 15, "faces": {
                                                      "north": {"uv": [0, 3, 16, 13], "texture": "#front"},
                                                      "south": {"uv": [0, 3, 16, 13], "texture": "#back"},
                                                      "east": {"uv": [0, 3, 2, 13], "texture": "#back"},
                                                      "west": {"uv": [0, 3, 2, 13], "texture": "#back"},
                                                      "up": {"uv": [0, 0, 16, 2], "texture": "#back"},
                                                      "down": {"uv": [0, 0, 16, 2], "texture": "#back"}}}]})
    blockstate("exit_sign", {"variants": facing_variants(
        lambda k: pr("block/exit_sign_right" if k == "right=true" else "block/exit_sign_left"),
        {"right=false": None, "right=true": None})})

    # life ring on a wall (model faces north, hangs on the wall to the south)
    model("life_ring", {"parent": "minecraft:block/block", "ambientocclusion": False,
                        "textures": {"ring": pr("block/life_ring"), "particle": pr("block/life_ring")},
                        "elements": [{"from": [0, 0, 15], "to": [16, 16, 16], "faces": {
                            "north": {"uv": [0, 0, 16, 16], "texture": "#ring"},
                            "south": {"uv": [16, 0, 0, 16], "texture": "#ring"}}}]})
    blockstate("life_ring", {"variants": facing_variants(lambda k: pr("block/life_ring"))})

    # pool chair (model looks north: the back rest is at the south end)
    def box(frm, to, tex, rot=None):
        e = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in
                                              ("north", "south", "east", "west", "up", "down")}}
        if rot:
            e["rotation"] = rot
        return e
    chair = [
        box([2, 0, 1], [3, 4, 2], "#frame"), box([13, 0, 1], [14, 4, 2], "#frame"),
        box([2, 0, 13], [3, 4, 14], "#frame"), box([13, 0, 13], [14, 4, 14], "#frame"),
        box([1.5, 4, 0.5], [14.5, 5.5, 10], "#slats"),
        box([1.5, 4, 10], [14.5, 5.5, 16], "#slats", {"angle": -45, "axis": "x", "origin": [8, 4.75, 10]}),
        box([1.5, 5.5, 1], [2.5, 7.5, 9], "#frame"), box([13.5, 5.5, 1], [14.5, 7.5, 9], "#frame"),
    ]
    model("pool_chair", {"parent": "minecraft:block/block",
                         "textures": {"slats": pr("block/pool_chair_slats"), "frame": pr("block/pool_chair_frame"),
                                      "particle": pr("block/pool_chair_slats")},
                         "elements": chair})
    blockstate("pool_chair", {"variants": facing_variants(lambda k: pr("block/pool_chair"))})


def items():
    flat = {"pool_ladder": "block/pool_ladder", "strange_drain": "item/strange_drain", "life_ring": "block/life_ring",
            "exit_sign": "item/exit_sign", "blue_wristband": "item/blue_wristband",
            "yellow_wristband": "item/yellow_wristband", "red_wristband": "item/red_wristband",
            "flippers": "item/flippers"}
    for b in BLOCK_ITEMS:
        if b in flat:
            continue
        item_def(b, pr(f"block/{b}"))
    for name, tex in flat.items():
        write_json(asset("models/item", name), {"parent": "minecraft:item/generated", "textures": {"layer0": pr(tex)}})
        item_def(name, pr(f"item/{name}"))
    write_json(asset("models/item", "pool_noodle"), {"parent": "minecraft:item/handheld",
                                                     "textures": {"layer0": pr("item/pool_noodle")}})
    item_def("pool_noodle", pr("item/pool_noodle"))

    # snorkel mask: a flat picture everywhere except on your head
    write_json(asset("models/item", "snorkel_mask"), {"parent": "minecraft:item/generated",
                                                      "textures": {"layer0": pr("item/snorkel_mask")}})
    t = "#t"

    def el(frm, to, uv, rot=None):
        e = {"from": frm, "to": to, "faces": {f: {"uv": uv, "texture": t} for f in
                                              ("north", "south", "east", "west", "up", "down")}}
        if rot:
            e["rotation"] = rot
        return e
    strap, lens, frame, tube, mouth = [0, 0, 4, 4], [4, 0, 8, 4], [8, 0, 12, 4], [12, 0, 16, 4], [0, 4, 4, 8]
    worn = [
        el([3, 6, 0.6], [13, 11, 1.6], frame),
        el([4, 7, 0.3], [12, 10, 0.6], lens),
        el([7, 5, 0.3], [9, 7, 1.6], frame),
        el([1.0, 8, 1.2], [1.6, 9.5, 14.8], strap),
        el([14.4, 8, 1.2], [15.0, 9.5, 14.8], strap),
        el([1.0, 8, 14.4], [15.0, 9.5, 15.0], strap),
        el([0.1, 5, 3.8], [1.1, 19, 4.8], tube),
        el([-0.1, 18.5, 3.6], [1.3, 20, 5.0], tube),
        el([0.1, 4, 1.0], [1.1, 5.5, 4.8], tube),
        el([1.1, 4, 0.8], [7, 5, 1.8], mouth),
    ]
    write_json(asset("models/item", "snorkel_mask_worn"), {
        "textures": {"t": pr("item/snorkel_mask_worn"), "particle": pr("item/snorkel_mask_worn")},
        "elements": worn,
        "display": {"head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}})
    write_json(asset("items", "snorkel_mask"), {"model": {
        "type": "minecraft:select", "property": "minecraft:display_context",
        "cases": [{"when": ["head"], "model": {"type": "minecraft:model", "model": pr("item/snorkel_mask_worn")}}],
        "fallback": {"type": "minecraft:model", "model": pr("item/snorkel_mask")}}})
    write_json(asset("equipment", "flippers"), {"layers": {"humanoid": [{"texture": pr("flippers")}]}})

    # chlorine compass: 32 needle positions, pointing at the lodestone-style target the mod sets
    for k in range(32):
        write_json(asset("models/item", f"chlorine_compass_{k:02d}"), {
            "parent": "minecraft:item/generated", "textures": {"layer0": pr(f"item/chlorine_compass_{k:02d}")}})
    vanilla = json.load(open(f"{VANILLA_ASSETS}/items/compass.json"))["model"]

    def dispatch(target):
        entries = []
        for e in vanilla["on_true"]["entries"]:
            n = e["model"]["model"].split("_")[-1]
            entries.append({"threshold": e["threshold"],
                            "model": {"type": "minecraft:model", "model": pr(f"item/chlorine_compass_{n}")}})
        return {"type": "minecraft:range_dispatch", "property": "minecraft:compass", "target": target,
                "scale": 32.0, "entries": entries}
    write_json(asset("items", "chlorine_compass"), {"model": {
        "type": "minecraft:condition", "property": "minecraft:has_component",
        "component": "minecraft:lodestone_tracker",
        "on_true": dispatch("lodestone"), "on_false": dispatch("none")}})


def sounds():
    def s(name, files, subtitle=None, distance=None, stream=False):
        entries = []
        for f in files:
            e = {"name": pr(f)}
            if distance:
                e["attenuation_distance"] = distance
            if stream:
                e["stream"] = True
            entries.append(e)
        d = {"sounds": entries}
        if subtitle:
            d["subtitle"] = f"subtitles.poolrooms.{subtitle}"
        return d
    data = {
        "entity.lifeguard.whistle_short": s("entity.lifeguard.whistle_short", ["entity/lifeguard/whistle_short"],
                                            "whistle_short", 48),
        "entity.lifeguard.whistle_long": s("entity.lifeguard.whistle_long", ["entity/lifeguard/whistle_long"],
                                           "whistle_long", 48),
        "ambient.poolrooms.hum": s("ambient.poolrooms.hum", ["ambient/hum"]),
        "ambient.poolrooms.drip": s("ambient.poolrooms.drip", ["ambient/drip1", "ambient/drip2", "ambient/drip3",
                                                               "ambient/lap1", "ambient/lap2"], "drip"),
        "block.shallow_water.step": s("block.shallow_water.step", [f"block/splash_step{i}" for i in range(1, 5)],
                                      "splash"),
        "block.strange_drain.swirl": s("block.strange_drain.swirl", ["block/drain_swirl"], "swirl", 24),
        "item.pool_noodle.boing": s("item.pool_noodle.boing", ["item/noodle_boing"], "boing"),
        "block.exit_door.open": s("block.exit_door.open", ["block/exit_open"], "exit_open", 24),
        "event.fade": s("event.fade", ["event/fade"], "fade"),
    }
    import os
    from common import RES
    path = os.path.join(RES, "assets", NS, "sounds.json")
    write_json(path, data)


def fonts():
    write_json(asset("font", "fade"), {"providers": [{"type": "bitmap", "file": pr("font/fade.png"), "ascent": 125,
                                                      "height": 256, "chars": [""]}]})


def lang():
    tr = {"itemGroup.poolrooms.main": "The Poolrooms", "container.poolrooms.locker": "Locker",
          "entity.poolrooms.lifeguard": "The Lifeguard", "biome.poolrooms.poolrooms": "The Poolrooms",
          "dimension.poolrooms.poolrooms": "The Poolrooms"}
    for k, v in NAMES.items():
        tr[f"block.poolrooms.{k}"] = v
        tr[f"item.poolrooms.{k}"] = v
    tr.update(DESCRIPTIONS)
    tr.update({
        "subtitles.poolrooms.whistle_short": "Two whistle blasts: Lifeguard nearby",
        "subtitles.poolrooms.whistle_long": "Lifeguard whistles: No running!",
        "subtitles.poolrooms.drip": "Water drips",
        "subtitles.poolrooms.splash": "Footsteps splash",
        "subtitles.poolrooms.swirl": "Drain swirls",
        "subtitles.poolrooms.boing": "Pool noodle boings",
        "subtitles.poolrooms.exit_open": "EXIT door opens",
        "subtitles.poolrooms.fade": "Everything fades to white",
    })
    write_json(asset("lang", "en_us"), tr)


# ---------------------------------------------------------------- data
def entry(name, count=None, weight=None):
    e = {"type": "minecraft:item", "name": name}
    if weight:
        e["weight"] = weight
    if count is not None:
        lo, hi = count if isinstance(count, tuple) else (count, count)
        e["modifier"] = [{"type": "minecraft:set_count",
                          "count": lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def loot():
    for b in BLOCK_ITEMS:
        name = pr(b)
        if b in SLABS:
            pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": name, "modifier": [
                {"type": "minecraft:set_count", "count": 2,
                 "condition": {"type": "minecraft:match_block", "blocks": name, "state": {"type": "double"}}},
                {"type": "minecraft:explosion_decay"}]}]}]
        else:
            pools = [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                      "entries": [{"type": "minecraft:item", "name": name}]}]
        write_json(data_path(NS, "loot_table/blocks", b), {"type": "minecraft:block", "pools": pools,
                                                           "random_sequence": f"poolrooms:blocks/{b}"})

    def chest(name, pools):
        write_json(data_path(NS, "loot_table/chests", name), {"type": "minecraft:chest", "pools": pools,
                                                              "random_sequence": f"poolrooms:chests/{name}"})
    chest("lost_and_found", [
        {"rolls": 1, "entries": [entry(pr("chlorine_compass"))]},
        {"rolls": 1, "entries": [entry(pr("snorkel_mask"))]},
        {"rolls": 1, "entries": [entry(pr("pool_noodle"))]},
        {"rolls": 2, "entries": [entry("minecraft:cookie", (3, 6), 3), entry("minecraft:dried_kelp", (4, 8), 2),
                                 entry("minecraft:melon_slice", (2, 5), 2), entry("minecraft:sweet_berries", (3, 6), 2)]},
    ])
    chest("lounge_locker", [
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 3}, "entries": [
            entry(pr("flippers"), None, 6), entry(pr("pool_noodle"), None, 6), entry(pr("snorkel_mask"), None, 3),
            entry("minecraft:cookie", (2, 5), 10), entry("minecraft:dried_kelp", (3, 6), 6),
            entry("minecraft:bread", (1, 3), 5), entry("minecraft:white_carpet", (1, 2), 8),
            entry("minecraft:light_blue_carpet", (1, 2), 6), entry(pr("white_pool_tiles"), (4, 12), 8),
            entry(pr("aqua_pool_tiles"), (4, 12), 6), entry(pr("ceiling_light"), (1, 3), 4),
            entry(pr("mosaic_pool_tiles"), (2, 8), 4)]},
    ])
    chest("office_locker", [
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 3}, "entries": [
            entry(pr("chlorine_compass"), None, 4), entry(pr("pool_noodle"), None, 4), entry(pr("snorkel_mask"), None, 3),
            entry(pr("flippers"), None, 3), entry("minecraft:paper", (1, 4), 6), entry("minecraft:book", 1, 3),
            entry("minecraft:cookie", (2, 4), 6), entry(pr("pool_light"), (1, 2), 3),
            entry(pr("lane_line_tiles"), (4, 8), 4), entry(pr("life_ring"), 1, 3)]},
    ])


def recipes():
    def shaped(name, pattern, key, result, count=1, category="building"):
        write_json(data_path(NS, "recipe", name), {"type": "minecraft:crafting_shaped", "category": category,
                                                   "key": key, "pattern": pattern,
                                                   "result": {"count": count, "id": result}})

    def shapeless(name, ingredients, result, count=1, category="building"):
        write_json(data_path(NS, "recipe", name), {"type": "minecraft:crafting_shapeless", "category": category,
                                                   "ingredients": ingredients,
                                                   "result": {"count": count, "id": result}})

    def cutting(name, ingredient, result, count=1):
        write_json(data_path(NS, "recipe", name), {"type": "minecraft:stonecutting", "ingredient": ingredient,
                                                   "result": {"count": count, "id": result}})
    shaped("strange_drain", [" B ", "QEQ", " B "], {"B": "minecraft:iron_bars", "Q": "minecraft:quartz",
                                                    "E": "minecraft:ender_pearl"}, pr("strange_drain"), 1, "misc")
    shaped("white_pool_tiles", ["CC", "CC"], {"C": "minecraft:white_concrete"}, pr("white_pool_tiles"), 4)
    shaped("aqua_pool_tiles", ["CC", "CC"], {"C": "minecraft:light_blue_concrete"}, pr("aqua_pool_tiles"), 4)
    shaped("lane_line_tiles", ["CC", "CC"], {"C": "minecraft:blue_concrete"}, pr("lane_line_tiles"), 4)
    shaped("mosaic_pool_tiles", ["WA", "AW"], {"W": pr("white_pool_tiles"), "A": pr("aqua_pool_tiles")},
           pr("mosaic_pool_tiles"), 4)
    for b, full in STAIRS.items():
        shaped(b, ["T  ", "TT ", "TTT"], {"T": pr(full)}, pr(b), 4)
        cutting(f"{b}_from_{full}_stonecutting", pr(full), pr(b))
    for b, full in SLABS.items():
        shaped(b, ["TTT"], {"T": pr(full)}, pr(b), 6)
        cutting(f"{b}_from_{full}_stonecutting", pr(full), pr(b), 2)
    shaped("ceiling_light", [" T ", "TGT", " T "], {"T": pr("white_pool_tiles"), "G": "minecraft:glowstone"},
           pr("ceiling_light"), 4)
    shapeless("fake_sky_panel", ["minecraft:glowstone", "minecraft:light_blue_dye", pr("white_pool_tiles"),
                                 pr("white_pool_tiles")], pr("fake_sky_panel"), 2)
    shapeless("pool_light", ["minecraft:sea_lantern", pr("aqua_pool_tiles")], pr("pool_light"), 2)
    shaped("pool_ladder", ["N N", "NNN", "N N"], {"N": "minecraft:iron_nugget"}, pr("pool_ladder"), 4, "misc")
    shaped("locker", ["NNN", "NCN", "NNN"], {"N": "minecraft:iron_nugget", "C": "minecraft:chest"}, pr("locker"), 1,
           "misc")
    shaped("pool_chair", ["W  ", "WWW", "S S"], {"W": "minecraft:white_wool", "S": "minecraft:stick"},
           pr("pool_chair"), 1, "misc")
    shaped("life_ring", [" W ", "WRW", " W "], {"W": "minecraft:white_wool", "R": "minecraft:red_wool"},
           pr("life_ring"), 1, "misc")


def tags():
    def tag(kind, name, values):
        write_json(data_path("minecraft", f"tags/{kind}", name), {"replace": False, "values": values})
    tag("block", "mineable/pickaxe", [pr(b) for b in TILES + list(STAIRS) + list(SLABS) + ["pool_light", "strange_drain",
                                                                                           "locker", "pool_ladder"]])
    tag("block", "climbable", [pr("pool_ladder")])
    tag("block", "stairs", [pr(b) for b in STAIRS])
    tag("block", "slabs", [pr(b) for b in SLABS])
    tag("item", "stairs", [pr(b) for b in STAIRS])
    tag("item", "slabs", [pr(b) for b in SLABS])
    tag("block", "inside_step_sound_blocks", [pr("shallow_water")])
    # In 26.3 "blocks motion" is a block tag. Heightmaps use it, and so does the flat world generator: a flat-world
    # layer whose block is not in this tag is placed as a late "fill the air" feature, which would fill every room
    # back in after the rooms are carved. So every solid Poolrooms block goes in here.
    tag("block", "blocks_motion_no_leaves", [pr(b) for b in TILES + GLOW + ["locker", "pool_chair", "strange_drain",
                                                                            "exit_door"]])


def advancements():
    def adv(name, icon, title, desc, criteria, parent=None, frame=None, toast=True, background=None):
        d = {"display": {"icon": {"id": icon}, "title": {"text": title}, "description": {"text": desc},
                         "show_toast": toast, "announce_to_chat": toast, "hidden": False},
             "criteria": criteria}
        if background:
            d["display"]["background"] = background
        if frame:
            d["display"]["frame"] = frame
        if parent:
            d["parent"] = pr(parent)
        write_json(data_path(NS, "advancement", name), d)

    def has(item):
        return {"got": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}}
    impossible = {"granted": {"trigger": "minecraft:impossible"}}
    adv("root", pr("strange_drain"), "The Poolrooms", "Swim down a Strange Drain into the Poolrooms",
        {"entered": {"trigger": "minecraft:changed_dimension", "conditions": {"to": pr("poolrooms")}}},
        background="poolrooms:gui/advancements/backgrounds/pool_tiles")
    adv("blue_wristband", pr("blue_wristband"), "The Deep End", "Find the blue wristband at the bottom of the Deep End",
        has(pr("blue_wristband")), "root")
    adv("yellow_wristband", pr("yellow_wristband"), "Slide Into It",
        "Find the yellow wristband at the top of the Slide Tower", has(pr("yellow_wristband")), "root")
    adv("red_wristband", pr("red_wristband"), "Staff Only", "Take the red wristband from the Lifeguard's desk",
        has(pr("red_wristband")), "root")
    adv("caught", pr("pool_noodle"), "No Running!", "Get walked back to the Entrance Pool by the Lifeguard",
        impossible, "root")
    adv("escaped", pr("exit_sign"), "Swim at Your Own Risk", "Escape the Poolrooms through an EXIT door", impossible,
        "red_wristband", frame="challenge")


def main():
    blocks()
    items()
    sounds()
    fonts()
    lang()
    loot()
    recipes()
    tags()
    advancements()


if __name__ == "__main__":
    main()
    print("resources written")
