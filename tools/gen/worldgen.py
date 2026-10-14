"""The Poolrooms dimension: dimension, dimension type, biome and the rooms feature."""
from common import NS, data_path, pr, write_json

AMBIENT = {
    "loop": pr("ambient.poolrooms.hum"),
    "additions": {"sound": pr("ambient.poolrooms.drip"), "tick_chance": 0.0065},
}

NO_SLEEP = {"can_set_spawn": "never", "can_sleep": "never",
            "error_message": {"text": "You can't sleep at the pool. Find the EXIT instead!"}}


def main():
    # a flat world of solid white tiles; the rooms feature carves the rooms out of it chunk by chunk
    write_json(data_path(NS, "dimension", "poolrooms"), {
        "type": pr("poolrooms"),
        "generator": {
            "type": "minecraft:flat",
            "settings": {
                "biome": pr("poolrooms"),
                "features": True,
                "lakes": False,
                "layers": [
                    {"block": "minecraft:bedrock", "height": 1},
                    {"block": pr("white_pool_tiles"), "height": 95},
                ],
                "structure_overrides": [],
            },
        },
    })
    write_json(data_path(NS, "dimension_type", "poolrooms"), {
        "ambient_light": 0.3,
        "attributes": {
            "minecraft:audio/ambient_sounds": AMBIENT,
            "minecraft:audio/background_music": {},
            "minecraft:audio/music_volume": 0.0,
            "minecraft:gameplay/bed_rule": NO_SLEEP,
            "minecraft:gameplay/straw_bed_rule": NO_SLEEP,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/nether_portal_spawns_piglin": False,
            "minecraft:visual/ambient_light_color": "#4a6466",
            "minecraft:visual/fog_color": "#cfeff0",
            "minecraft:visual/fog_start_distance": 48.0,
            "minecraft:visual/fog_end_distance": 160.0,
            "minecraft:visual/sky_color": "#cfeff0",
            "minecraft:visual/sky_light_factor": 0.0,
        },
        "coordinate_scale": 1.0,
        "has_ceiling": True,
        "has_ender_dragon_fight": False,
        "has_fixed_time": True,
        "has_skylight": False,
        "height": 128,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "logical_height": 128,
        "min_y": 0,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": 0,
        "skybox": "none",
        "timelines": "#minecraft:universal",
    })
    write_json(data_path(NS, "worldgen/biome", "poolrooms"), {
        "attributes": {
            "minecraft:audio/ambient_sounds": AMBIENT,
            "minecraft:audio/background_music": {},
            "minecraft:audio/music_volume": 0.0,
            "minecraft:gameplay/natural_mob_spawns": {
                "argument": {"spawn_costs": {}, "spawns_by_category": {
                    c: [] for c in ("ambient", "axolotls", "creature", "misc", "monster",
                                    "underground_water_creature", "water_ambient", "water_creature")}},
                "modifier": "overlay",
            },
            "minecraft:visual/fog_color": "#cfeff0",
            "minecraft:visual/sky_color": "#cfeff0",
            "minecraft:visual/water_fog_color": "#4fc9d6",
            "minecraft:visual/water_fog_end_distance": {"argument": 0.45, "modifier": "multiply"},
        },
        "carvers": [],
        "downfall": 0.0,
        "effects": {"water_color": "#56d6e0"},
        "features": [[pr("rooms")]] + [[] for _ in range(10)],
        "has_precipitation": False,
        "temperature": 0.8,
    })
    write_json(data_path(NS, "worldgen/feature", "rooms"), {"type": pr("rooms")})
    write_json(data_path(NS, "worldgen/placed_feature", "rooms"), {"feature": pr("rooms"), "placement": []})


if __name__ == "__main__":
    main()
    print("worldgen files written")
