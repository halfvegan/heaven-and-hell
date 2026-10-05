"""Heaven and Hell: dimension types, terrain, surface (material) rules, biomes and features."""
from common import (NS, data_path, deep, hh, mc, replace_strings, vanilla, write_json)

FEATURES = {}   # id -> configured feature json   (worldgen/feature)
PLACED = {}     # id -> placed feature json        (worldgen/placed_feature)
STEPS = 11      # number of biome feature steps


# ---------------------------------------------------------------- helpers
def anchor(v):
    return v if isinstance(v, dict) else {"absolute": v}


def h_uniform(lo, hi):
    return {"type": "minecraft:height_range",
            "height": {"type": "minecraft:uniform", "min_inclusive": anchor(lo), "max_inclusive": anchor(hi)}}


def h_trap(lo, hi):
    return {"type": "minecraft:height_range",
            "height": {"type": "minecraft:trapezoid", "min_inclusive": anchor(lo), "max_inclusive": anchor(hi)}}


def count(n):
    return {"type": "minecraft:count", "count": n}


def uniform_count(lo, hi):
    return {"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}


def weighted_count(pairs):
    return {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list",
            "distribution": [{"data": d, "weight": w} for d, w in pairs]}}


SQUARE = {"type": "minecraft:in_square"}
BIOME = {"type": "minecraft:biome"}


def rarity(n):
    return {"type": "minecraft:rarity_filter", "chance": n}


def heightmap(h):
    return {"type": "minecraft:heightmap", "heightmap": h}


def scatter(xz=7, y=3):
    t = lambda m: {"type": "minecraft:trapezoid", "max": m, "min": -m, "plateau": 0}
    return {"type": "minecraft:offset", "x": t(xz), "y": t(y), "z": t(xz)}


AIR_ONLY = {"type": "minecraft:block_predicate_filter",
            "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}}


def on_blocks(blocks):
    return {"type": "minecraft:block_predicate_filter",
            "predicate": {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": blocks}}


def feature(fid, obj):
    FEATURES[fid] = obj
    return fid


def placed(pid, feat, placement):
    PLACED[pid] = {"feature": feat, "placement": placement}
    return pid


def match(block):
    return {"predicate_type": "minecraft:block_match", "block": block}


def ore(fid, targets, size, discard=0.0):
    return feature(fid, {"type": "minecraft:ore", "discard_chance_on_air_exposure": discard,
                         "size": size, "targets": targets})


def ore_placed(dim, name, block, target, size, n, lo, hi, discard=0.0, trap=False):
    f = ore(hh(f"{dim}/{name}"), [{"state": block, "target": target}], size, discard)
    return placed(hh(f"{dim}/{name}"), f, [count(n) if isinstance(n, int) else n, SQUARE,
                                          (h_trap if trap else h_uniform)(lo, hi), BIOME])


def tree_from(fid, vanilla_name, trunk, foliage, soil):
    t = deep(vanilla(f"worldgen/feature/{vanilla_name}.json"))
    t["trunk_provider"] = {"id": trunk, "properties": {"axis": "y"}}
    t["foliage_provider"] = foliage
    t["below_trunk_provider"] = {"type": "minecraft:rule_based", "rules": [{
        "if_true": {"type": "minecraft:not", "predicate": {
            "type": "minecraft:matching_block_tag", "tag": "minecraft:cannot_replace_below_tree_trunk"}},
        "then": {"id": soil}}]}
    t["decorators"] = []
    return feature(fid, t)


def simple_patch(dim, name, entries, tries, per, ground, xz=7, y=3):
    f = feature(hh(f"{dim}/{name}"), {"type": "minecraft:simple_block", "to_place": {
        "type": "minecraft:weighted",
        "entries": [{"data": b, "weight": w} for b, w in entries]}})
    return placed(hh(f"{dim}/{name}"), f, [tries, SQUARE, heightmap("MOTION_BLOCKING"), BIOME,
                                          count(per), scatter(xz, y), AIR_ONLY, on_blocks(ground)])


def spawn(t, w, lo, hi=None):
    c = lo if hi is None else {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
    return {"type": mc(t), "count": c, "weight": w}


def mob_spawns(creature=(), monster=(), ambient=(), costs=None):
    cats = {}
    if creature:
        cats["creature"] = list(creature)
    if monster:
        cats["monster"] = list(monster)
    if ambient:
        cats["ambient"] = list(ambient)
    return {"argument": {"spawn_costs": costs or {}, "spawns_by_category": cats}, "modifier": "overlay"}


def music(sound, min_d=6000, max_d=18000):
    return {"default": {"sound": sound, "min_delay": min_d, "max_delay": max_d}}


def particles(*pairs):
    return {"argument": [{"particle": {"type": p}, "probability": prob} for p, prob in pairs],
            "modifier": "append"}


# material rules
def block(b):
    return {"type": "minecraft:block", "result_state": b}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def in_biomes(*biomes):
    return {"type": "minecraft:biome", "biome_is": list(biomes)}


def noise_above(noise, threshold):
    return {"type": "minecraft:noise_threshold", "noise": noise, "min_threshold": threshold,
            "max_threshold": 1.7976931348623157e+308}


def write_biome(b):
    attrs = {
        "minecraft:gameplay/natural_mob_spawns": b["spawns"],
        "minecraft:visual/sky_color": b["sky"],
        "minecraft:visual/fog_color": b["fog"],
        "minecraft:visual/water_fog_color": b.get("water_fog", "#050533"),
        "minecraft:audio/background_music": b["music"],
    }
    if b.get("particles"):
        attrs["minecraft:visual/ambient_particles"] = b["particles"]
    if b.get("ambient_sounds"):
        attrs["minecraft:audio/ambient_sounds"] = b["ambient_sounds"]
    feats = [[] for _ in range(STEPS)]
    for step, ids in b["features"].items():
        feats[step] = list(ids)
    obj = {
        "attributes": attrs,
        "carvers": b.get("carvers", []),
        "downfall": b.get("downfall", 0.0),
        "effects": b["effects"],
        "features": feats,
        "has_precipitation": b.get("precip", False),
        "temperature": b.get("temperature", 0.8),
    }
    write_json(data_path(NS, "worldgen/biome", b["id"]), obj)


def check_feature_order(dim, biomes):
    """Minecraft refuses to load if two biomes in one dimension list shared features in a different order."""
    for step in range(STEPS):
        lists = [b["features"].get(step, []) for b in biomes]
        pos = {}
        for lst in lists:
            for i, a in enumerate(lst):
                for c in lst[i + 1:]:
                    if pos.get((c, a)):
                        raise SystemExit(f"Feature order cycle in {dim} step {step}: {a} vs {c}")
                    pos[(a, c)] = True
        for b in biomes:
            for f in b["features"].get(step, []):
                for s2 in range(STEPS):
                    if s2 != step and any(f in bb["features"].get(s2, []) for bb in biomes):
                        raise SystemExit(f"Feature {f} used in two steps in {dim}")


def ordered(master, chosen):
    return [f for f in master if f in chosen]


# ======================================================================
#                                HEAVEN
# ======================================================================
PEARL = match(hh("pearlstone"))
HEAVEN_GROUND = [hh("heaven_grass")]


def build_heaven():
    d = "heaven"
    ores = [
        ore_placed(d, "gilded_veins", hh("gilded_pearlstone"), PEARL, 28, 6, 0, 230),
        ore_placed(d, "golden_cloud_pocket", hh("golden_cloud"), PEARL, 22, 3, 0, 230),
        ore_placed(d, "ore_gold", "minecraft:gold_ore", PEARL, 9, 9, 0, 230),
        ore_placed(d, "ore_lapis", "minecraft:lapis_ore", PEARL, 7, 3, 0, 230),
        ore_placed(d, "ore_diamond", "minecraft:diamond_ore", PEARL, 6, 2, 0, 230, discard=0.5),
    ]
    # floating golden clouds in the open sky
    sky = ore(hh(f"{d}/sky_cloud"), [{"state": hh("golden_cloud"), "target": match("minecraft:air")},
                                     ], 40)
    sky_golden = placed(hh(f"{d}/sky_cloud"), sky, [rarity(4), SQUARE, h_uniform(170, 236), BIOME])
    sky_white = placed(hh(f"{d}/sky_cloud_white"),
                       ore(hh(f"{d}/sky_cloud_white"), [{"state": hh("cloud"), "target": match("minecraft:air")}], 56),
                       [rarity(2), SQUARE, h_uniform(150, 240), BIOME])

    celestial_leaves = {"id": hh("celestial_leaves")}
    golden_leaves = {"id": hh("golden_leaves")}
    mixed = {"type": "minecraft:weighted", "entries": [{"data": celestial_leaves, "weight": 3},
                                                       {"data": golden_leaves, "weight": 1}]}
    t_cel = tree_from(hh(f"{d}/celestial_tree"), "cherry", hh("celestial_log"), celestial_leaves, hh("heaven_soil"))
    t_gold = tree_from(hh(f"{d}/golden_tree"), "cherry", hh("celestial_log"), golden_leaves, hh("heaven_soil"))
    t_grand = tree_from(hh(f"{d}/grand_tree"), "fancy_oak", hh("celestial_log"), mixed, hh("heaven_soil"))
    p_cel = placed(hh(f"{d}/celestial_tree"), t_cel, [])
    p_gold = placed(hh(f"{d}/golden_tree"), t_gold, [])
    p_grand = placed(hh(f"{d}/grand_tree"), t_grand, [])
    sel_fields = feature(hh(f"{d}/trees_fields"), {"type": "minecraft:random_selector", "default": p_cel,
                                                    "features": [{"chance": 0.2, "feature": p_grand},
                                                                 {"chance": 0.15, "feature": p_gold}]})
    sel_groves = feature(hh(f"{d}/trees_groves"), {"type": "minecraft:random_selector", "default": p_gold,
                                                    "features": [{"chance": 0.3, "feature": p_grand},
                                                                 {"chance": 0.25, "feature": p_cel}]})

    def tree_placement(counts):
        return [weighted_count(counts), SQUARE, heightmap("OCEAN_FLOOR"), on_blocks(HEAVEN_GROUND), BIOME]

    trees_fields = placed(hh(f"{d}/trees_fields"), sel_fields, tree_placement([(0, 6), (1, 3), (2, 1)]))
    trees_groves = placed(hh(f"{d}/trees_groves"), sel_groves, tree_placement([(2, 3), (3, 3), (5, 1)]))
    trees_peaks = placed(hh(f"{d}/trees_peaks"), sel_fields, tree_placement([(0, 12), (1, 1)]))

    flowers = simple_patch(d, "flowers", [(hh("angel_lily"), 6), ("minecraft:lily_of_the_valley", 3),
                                          ("minecraft:oxeye_daisy", 3), ("minecraft:white_tulip", 2),
                                          ("minecraft:azure_bluet", 2), ("minecraft:cornflower", 1)],
                           uniform_count(1, 3), 32, HEAVEN_GROUND)
    golden_flowers = simple_patch(d, "golden_flowers", [(hh("angel_lily"), 3), ("minecraft:dandelion", 3),
                                                        ("minecraft:sunflower", 1), ("minecraft:oxeye_daisy", 2)],
                                  uniform_count(1, 2), 28, HEAVEN_GROUND)
    grass = simple_patch(d, "grass", [("minecraft:short_grass", 8), ("minecraft:tall_grass", 1)],
                         count(5), 32, HEAVEN_GROUND)

    s6 = ores
    s9 = [trees_fields, trees_groves, trees_peaks, flowers, golden_flowers, grass, sky_golden, sky_white]
    effects = {"water_color": "#a8e6ff", "grass_color": "#c9f29b", "foliage_color": "#f4f0d0",
               "dry_foliage_color": "#e8dcb0"}
    creatures = [spawn("sheep", 8, 2, 4), spawn("rabbit", 6, 2, 3), spawn("chicken", 5, 2, 4),
                 spawn("horse", 2, 1, 2), spawn(hh("angel"), 5, 1, 2)]
    base = dict(effects=effects, sky="#8fd0ff", fog="#fff1d6", water_fog="#8fd8ff",
                temperature=0.7, downfall=0.4, precip=False, carvers=[])
    fields = dict(base, id="elysian_fields",
                  spawns=mob_spawns(creatures),
                  particles=particles(("minecraft:white_ash", 0.0015), ("minecraft:end_rod", 0.0004)),
                  music=music("minecraft:music.overworld.meadow"),
                  features={6: s6, 9: ordered(s9, [trees_fields, flowers, grass, sky_golden, sky_white])})
    groves = dict(base, id="golden_groves", sky="#9cd8ff", fog="#ffe8b8",
                  effects=dict(effects, grass_color="#e0f08a", foliage_color="#ffd86b"),
                  spawns=mob_spawns(creatures + [spawn("bee", 2, 2, 3)]),
                  particles=particles(("minecraft:wax_on", 0.0012), ("minecraft:end_rod", 0.0004)),
                  music=music("minecraft:music.overworld.cherry_grove"),
                  features={6: s6, 9: ordered(s9, [trees_groves, golden_flowers, grass, sky_golden, sky_white])})
    peaks = dict(base, id="cloud_peaks", sky="#b4e2ff", fog="#ffffff",
                 effects=dict(effects, grass_color="#e6fff0"),
                 spawns=mob_spawns([spawn(hh("angel"), 6, 1, 3), spawn("rabbit", 3, 1, 2)]),
                 particles=particles(("minecraft:white_ash", 0.004)),
                 music=music("minecraft:music.overworld.snowy_slopes"),
                 features={6: s6, 9: ordered(s9, [trees_peaks, flowers, sky_golden, sky_white])})
    biomes = [fields, groves, peaks]
    check_feature_order(d, biomes)
    for b in biomes:
        write_biome(b)

    # terrain: vanilla floating islands above a solid sea of cloud (sea level 56, "fluid" = cloud)
    ns = deep(vanilla("worldgen/noise_settings/floating_islands.json"))
    ns["default_block"] = hh("pearlstone")
    ns["default_fluid"] = hh("cloud")
    ns["sea_level"] = 56
    ns["material_rule"] = hh("heaven")
    ns["noise_router"]["temperature"] = "minecraft:overworld/temperature"
    ns["noise_router"]["vegetation"] = "minecraft:overworld/vegetation"
    write_json(data_path(NS, "worldgen/noise_settings", "heaven"), ns)

    rule = seq(
        cond("minecraft:on_floor", seq(
            cond(in_biomes(hh("cloud_peaks")), seq(
                cond(noise_above("minecraft:calcite", 0.02), block(hh("golden_cloud"))),
                block(hh("cloud")))),
            block(hh("heaven_grass")))),
        cond("minecraft:under_floor", seq(
            cond(in_biomes(hh("cloud_peaks")), block(hh("cloud"))),
            block(hh("heaven_soil")))),
        cond("minecraft:on_ceiling", seq(
            cond(noise_above("minecraft:calcite", -0.05), block(hh("gilded_pearlstone"))),
            block(hh("pearlstone")))),
    )
    write_json(data_path(NS, "worldgen/material_rule", "heaven"), rule)

    write_json(data_path(NS, "dimension_type", "heaven"), {
        "ambient_light": 0.15,
        "attributes": {
            "minecraft:audio/background_music": music("minecraft:music.overworld.meadow"),
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "always", "can_sleep": "never",
                                            "error_message": {"text": "There is no night in Heaven to sleep through."}},
            "minecraft:gameplay/straw_bed_rule": {"can_set_spawn": "never", "can_sleep": "never",
                                                  "error_message": {"text": "There is no night in Heaven to sleep through."}},
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:gameplay/nether_portal_spawns_piglin": False,
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/can_pillager_patrol_spawn": False,
            "minecraft:gameplay/monsters_burn": True,
            "minecraft:visual/sun_angle": 22.0,
            "minecraft:visual/moon_angle": 202.0,
            "minecraft:visual/star_angle": 22.0,
            "minecraft:visual/star_brightness": 0.0,
            "minecraft:visual/sky_color": "#8fd0ff",
            "minecraft:visual/fog_color": "#fff1d6",
            "minecraft:visual/cloud_color": "#ffffffff",
            "minecraft:visual/cloud_height": 66.33,
            "minecraft:visual/sky_light_color": "#fff4dc",
            "minecraft:visual/ambient_light_color": "#2a2418",
        },
        "coordinate_scale": 1.0,
        "has_ceiling": False,
        "has_ender_dragon_fight": False,
        "has_fixed_time": True,
        "has_skylight": True,
        "height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "logical_height": 256,
        "min_y": 0,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": 0,
        "timelines": "#minecraft:universal",
    })

    def climate(t, h=0.0, offset=0.0):
        return {"temperature": t, "humidity": h, "continentalness": 0.0, "erosion": 0.0,
                "weirdness": 0.0, "depth": 0.0, "offset": offset}

    write_json(data_path(NS, "dimension", "heaven"), {
        "type": hh("heaven"),
        "generator": {
            "type": "minecraft:noise",
            "settings": hh("heaven"),
            "biome_source": {"type": "minecraft:multi_noise", "biomes": [
                {"biome": hh("elysian_fields"), "parameters": climate(0.0, 0.0)},
                {"biome": hh("golden_groves"), "parameters": climate(0.55, 0.3)},
                {"biome": hh("cloud_peaks"), "parameters": climate(-0.55, -0.2)},
            ]},
        },
    })


# ======================================================================
#                                  HELL
# ======================================================================
BRIM = match(hh("brimstone"))
TO_BRIMSTONE = {"minecraft:netherrack": hh("brimstone")}


def from_vanilla(dim, name, vanilla_feature, vanilla_placed=None, mapping=None, placement=None):
    """Copies a vanilla configured feature (+ its placement) re-targeted at brimstone."""
    mapping = mapping or TO_BRIMSTONE
    f = feature(hh(f"{dim}/{name}"), replace_strings(deep(vanilla(f"worldgen/feature/{vanilla_feature}.json")), mapping))
    if placement is None:
        p = replace_strings(deep(vanilla(f"worldgen/placed_feature/{vanilla_placed or vanilla_feature}.json")), mapping)
        placement = p["placement"]
    return placed(hh(f"{dim}/{name}"), f, placement)


def retarget_rule(obj, mapping):
    """Swaps blocks (result_state) and biomes (biome_is) in a material rule, leaving noise ids alone."""
    if isinstance(obj, list):
        return [retarget_rule(v, mapping) for v in obj]
    if not isinstance(obj, dict):
        return obj
    out = {}
    for k, v in obj.items():
        if k == "result_state" and isinstance(v, str):
            out[k] = mapping.get(v, v)
        elif k == "result_state" and isinstance(v, dict) and "id" in v:
            out[k] = dict(v, id=mapping.get(v["id"], v["id"]))
        elif k == "biome_is":
            items = v if isinstance(v, list) else [v]
            mapped = []
            for b in items:
                m = mapping.get(b, b)
                if m not in mapped:
                    mapped.append(m)
            out[k] = mapped
        else:
            out[k] = retarget_rule(v, mapping)
    return out


def build_hell():
    d = "hell"
    spring_open = from_vanilla(d, "spring_open", "spring_nether_open", "spring_open",
                               mapping={"minecraft:netherrack": hh("brimstone")})
    FEATURES[hh(f"{d}/spring_open")].update(valid_blocks=[hh("brimstone"), "minecraft:blackstone", "minecraft:basalt"],
                                            rock_count=4, hole_count=1)
    spring_closed = from_vanilla(d, "spring_closed", "spring_nether_closed", "spring_closed")
    FEATURES[hh(f"{d}/spring_closed")]["valid_blocks"] = [hh("brimstone"), "minecraft:blackstone", "minecraft:basalt"]
    fire = from_vanilla(d, "patch_fire", "patch_fire")
    soul_fire = from_vanilla(d, "patch_soul_fire", "patch_soul_fire")
    magma = from_vanilla(d, "ore_magma", "ore_magma")
    basalt_blobs = from_vanilla(d, "basalt_blobs", "basalt_blobs")
    blackstone_blobs = from_vanilla(d, "blackstone_blobs", "blackstone_blobs")
    delta = from_vanilla(d, "delta", "delta")
    small_cols = from_vanilla(d, "small_basalt_columns", "small_basalt_columns")
    large_cols = from_vanilla(d, "large_basalt_columns", "large_basalt_columns")
    pillar = from_vanilla(d, "basalt_pillar", "basalt_pillar")
    debris_small = from_vanilla(d, "ore_debris_small", "ore_ancient_debris_small", "ore_debris_small")
    soul_sand = from_vanilla(d, "ore_soul_sand", "ore_soul_sand")

    shards = ore_placed(d, "ore_soul_shard", hh("soul_shard_ore"), BRIM, 5, 18, 8, 120)
    shards_rich = ore_placed(d, "ore_soul_shard_rich", hh("soul_shard_ore"), BRIM, 9, 4, 10, 60, discard=0.3)
    glow = ore_placed(d, "ceiling_glow", "minecraft:glowstone", BRIM, 14, 10, 96, 124)
    gilded = ore_placed(d, "ore_gilded_blackstone", "minecraft:gilded_blackstone", BRIM, 6, 4, 10, 110)
    ash = ore_placed(d, "ash_drifts", hh("ash_block"), BRIM, 24, 10, 30, 110)

    s2 = [pillar]
    s4 = [delta, small_cols, large_cols]
    s7 = [basalt_blobs, blackstone_blobs, spring_open, fire, soul_fire, magma, spring_closed, soul_sand,
          shards, shards_rich, glow, gilded, ash, debris_small]

    ambient = lambda name: {
        "additions": {"sound": f"minecraft:ambient.{name}.additions", "tick_chance": 0.0111},
        "loop": f"minecraft:ambient.{name}.loop",
        "mood": {"block_search_extent": 8, "offset": 2.0, "sound": f"minecraft:ambient.{name}.mood", "tick_delay": 6000}}
    effects = {"water_color": "#3f76e4"}
    base = dict(effects=effects, sky="#000000", water_fog="#050533", temperature=2.0, downfall=0.0,
                precip=False, carvers=["minecraft:nether_cave"])
    wastes = dict(base, id="brimstone_wastes", fog="#4a0d06",
                  spawns=mob_spawns(monster=[spawn(hh("imp"), 100, 2, 4), spawn("magma_cube", 30, 2, 4),
                                             spawn("ghast", 25, 1), spawn("blaze", 10, 1, 2)]),
                  particles=particles(("minecraft:ash", 0.03), ("minecraft:lava", 0.0006)),
                  music=music("minecraft:music.nether.nether_wastes"),
                  ambient_sounds=ambient("nether_wastes"),
                  features={7: ordered(s7, [spring_open, fire, magma, spring_closed, shards, shards_rich, glow,
                                            gilded, ash, debris_small])})
    pits = dict(base, id="soul_pits", fog="#122a33",
                spawns=mob_spawns(monster=[spawn("wither_skeleton", 30, 1, 2), spawn("skeleton", 40, 2, 4),
                                           spawn("ghast", 40, 1), spawn(hh("imp"), 40, 1, 3)],
                                  costs={"minecraft:ghast": {"charge": 0.7, "energy_budget": 0.15},
                                         "minecraft:skeleton": {"charge": 0.7, "energy_budget": 0.15}}),
                particles=particles(("minecraft:ash", 0.006), ("minecraft:soul", 0.0008)),
                music=music("minecraft:music.nether.soul_sand_valley"),
                ambient_sounds=ambient("soul_sand_valley"),
                features={2: s2, 7: ordered(s7, [spring_open, fire, soul_fire, magma, spring_closed, soul_sand,
                                                 shards, shards_rich, glow, debris_small])})
    spires = dict(base, id="infernal_spires", fog="#5a2a22",
                  spawns=mob_spawns(monster=[spawn("magma_cube", 100, 2, 5), spawn(hh("imp"), 60, 2, 3),
                                             spawn("ghast", 20, 1)]),
                  particles=particles(("minecraft:white_ash", 0.08)),
                  music=music("minecraft:music.nether.basalt_deltas"),
                  ambient_sounds=ambient("basalt_deltas"),
                  features={4: s4, 7: ordered(s7, [basalt_blobs, blackstone_blobs, fire, soul_fire, magma,
                                                   spring_closed, shards, shards_rich, glow, gilded,
                                                   debris_small])})
    biomes = [wastes, pits, spires]
    check_feature_order(d, biomes)
    for b in biomes:
        write_biome(b)

    ns = deep(vanilla("worldgen/noise_settings/nether.json"))
    ns["default_block"] = hh("brimstone")
    ns["material_rule"] = hh("hell")
    write_json(data_path(NS, "worldgen/noise_settings", "hell"), ns)

    rule = deep(vanilla("worldgen/material_rule/nether.json"))
    rule = retarget_rule(rule, {
        "minecraft:netherrack": hh("brimstone"),
        "minecraft:basalt_deltas": hh("infernal_spires"),
        "minecraft:soul_sand_valley": hh("soul_pits"),
        "minecraft:crimson_forest": hh("brimstone_wastes"),
        "minecraft:warped_forest": hh("brimstone_wastes"),
        "minecraft:nether_wastes": hh("brimstone_wastes"),
        "minecraft:crimson_nylium": hh("ash_block"),
        "minecraft:warped_nylium": hh("ash_block"),
        "minecraft:nether_wart_block": "minecraft:magma_block",
        "minecraft:warped_wart_block": "minecraft:magma_block",
    })
    write_json(data_path(NS, "worldgen/material_rule", "hell"), rule)

    dt = deep(vanilla("dimension_type/the_nether.json"))
    attrs = dt["attributes"]
    attrs["minecraft:audio/background_music"] = music("minecraft:music.nether.nether_wastes")
    attrs["minecraft:visual/ambient_light_color"] = "#4a2216"
    attrs["minecraft:visual/fog_start_distance"] = 6.0
    attrs["minecraft:visual/fog_end_distance"] = 110.0
    attrs["minecraft:gameplay/piglins_zombify"] = False
    dt["ambient_light"] = 0.12
    dt["coordinate_scale"] = 1.0
    dt["monster_spawn_block_light_limit"] = 0
    write_json(data_path(NS, "dimension_type", "hell"), dt)

    def point(t, h, offset=0.0):
        return {"temperature": t, "humidity": h, "continentalness": 0.0, "erosion": 0.0,
                "weirdness": 0.0, "depth": 0.0, "offset": offset}

    write_json(data_path(NS, "dimension", "hell"), {
        "type": hh("hell"),
        "generator": {
            "type": "minecraft:noise",
            "settings": hh("hell"),
            "biome_source": {"type": "minecraft:multi_noise", "biomes": [
                {"biome": hh("brimstone_wastes"), "parameters": point(0.0, 0.0)},
                {"biome": hh("soul_pits"), "parameters": point(0.0, -0.5)},
                {"biome": hh("infernal_spires"), "parameters": point(-0.5, 0.0, 0.175)},
            ]},
        },
    })


def write_tags():
    """Vanilla tags our worldgen relies on."""
    t = lambda kind, name, values: write_json(data_path("minecraft", f"tags/{kind}", name),
                                              {"replace": False, "values": values})
    # heaven grass behaves like grass (saplings, flowers, animals)
    t("block", "dirt", [hh("heaven_grass"), hh("heaven_soil")])
    t("block", "animals_spawnable_on", [hh("heaven_grass")])
    t("block", "rabbits_spawnable_on", [hh("heaven_grass")])
    t("block", "valid_spawn", [hh("heaven_grass")])
    # brimstone counts as nether stone; fortresses also appear in Hell
    t("block", "base_stone_nether", [hh("brimstone")])
    t("block", "infiniburn_nether", [hh("brimstone")])
    t("worldgen/biome", "has_structure/nether_fortress", [hh("brimstone_wastes"), hh("soul_pits")])


def build():
    build_heaven()
    build_hell()
    write_tags()
    for fid, obj in FEATURES.items():
        ns, path = fid.split(":")
        write_json(data_path(ns, "worldgen/feature", path), obj)
    for pid, obj in PLACED.items():
        ns, path = pid.split(":")
        write_json(data_path(ns, "worldgen/placed_feature", path), obj)


if __name__ == "__main__":
    build()
    print(f"worldgen: wrote {len(FEATURES)} features, {len(PLACED)} placed features")
