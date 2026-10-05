"""A small voxel builder that writes vanilla structure templates (.nbt) and preview images."""
import json
import math
import os

import numpy as np
from PIL import Image

SUMMARY_BLOCKS = "/home/claude/misode/mcmeta-summary/blocks/data.json"
VANILLA_TEX = "/home/claude/misode/mcmeta-full/assets/minecraft/textures/block"
MOD_TEX = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets",
                                       "heavenhell", "textures", "block"))
DATA_VERSION = 5023

_SUMMARY = None


def summary():
    global _SUMMARY
    if _SUMMARY is None:
        _SUMMARY = json.load(open(SUMMARY_BLOCKS))
    return _SUMMARY


STAIR_PROPS = {"facing": ["north", "south", "west", "east"], "half": ["top", "bottom"],
               "shape": ["straight", "inner_left", "inner_right", "outer_left", "outer_right"],
               "waterlogged": ["true", "false"]}
MOD_BLOCKS = {
    **{b: ({}, {}) for b in [
        "cloud", "golden_cloud", "heaven_grass", "heaven_soil", "pearlstone", "pearlstone_bricks", "chiseled_pearlstone",
        "gilded_pearlstone", "golden_bricks", "celestial_planks", "celestial_leaves", "golden_leaves", "angel_lily",
        "halo_lamp", "return_light", "brimstone", "brimstone_bricks", "hellstone_bricks", "chiseled_hellstone",
        "gilded_hellstone", "soul_shard_ore", "ash_block", "ember_lamp", "redemption_light"]},
    **{b: ({"axis": ["x", "y", "z"]}, {"axis": "y"}) for b in ["pearlstone_pillar", "celestial_log", "hellstone_pillar"]},
    **{b: (STAIR_PROPS, {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
       for b in ["pearlstone_brick_stairs", "hellstone_brick_stairs"]},
    **{b: ({"type": ["top", "bottom", "double"], "waterlogged": ["true", "false"]}, {"type": "bottom", "waterlogged": "false"})
       for b in ["pearlstone_brick_slab", "hellstone_brick_slab"]},
}


def parse(state):
    """'ns:id[a=b,c=d]' -> (id, {props})"""
    if "[" in state:
        name, rest = state.split("[", 1)
        props = dict(p.split("=") for p in rest.rstrip("]").split(",") if p)
    else:
        name, props = state, {}
    if ":" not in name:
        name = "minecraft:" + name
    return name, props


def full_state(state):
    """Returns (id, props) with every property filled in (defaults) and validated."""
    name, props = parse(state)
    ns, path = name.split(":")
    if ns == "minecraft":
        if path not in summary():
            raise ValueError(f"unknown block {name}")
        allowed, defaults = summary()[path]
    elif ns == "heavenhell":
        if path not in MOD_BLOCKS:
            raise ValueError(f"unknown mod block {name}")
        allowed, defaults = MOD_BLOCKS[path]
    else:
        raise ValueError(name)
    out = dict(defaults)
    for k, v in props.items():
        if k not in allowed:
            raise ValueError(f"{name} has no property {k}")
        if v not in allowed[k]:
            raise ValueError(f"{name}[{k}={v}] invalid (allowed {allowed[k]})")
        out[k] = v
    return name, out


class Build:
    def __init__(self, origin, size):
        self.origin = origin
        self.size = size
        self.blocks = {}
        self.nbt = {}

    def inside(self, x, y, z):
        ox, oy, oz = self.origin
        sx, sy, sz = self.size
        return ox <= x < ox + sx and oy <= y < oy + sy and oz <= z < oz + sz

    def set(self, x, y, z, state, nbt=None):
        x, y, z = int(x), int(y), int(z)
        if not self.inside(x, y, z):
            return
        if state is None or state == "air":
            self.blocks.pop((x, y, z), None)
            self.nbt.pop((x, y, z), None)
            return
        self.blocks[(x, y, z)] = state
        if nbt is not None:
            self.nbt[(x, y, z)] = nbt
        else:
            self.nbt.pop((x, y, z), None)

    def get(self, x, y, z):
        return self.blocks.get((int(x), int(y), int(z)))

    def fill(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, state)

    def walls(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, state)

    def clear(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, None)

    def disc(self, cx, y, cz, r, state, r_in=-1.0):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if r_in <= d < r:
                    self.set(x, y, z, state)

    def ball(self, cx, cy, cz, r, state, only_air=False, ry=None):
        ry = r if ry is None else ry
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - r - 1), int(cz + r + 2)):
                    if ((x - cx) / r) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / r) ** 2 <= 1.0:
                        if only_air and self.get(x, y, z) is not None:
                            continue
                        self.set(x, y, z, state)

    # ------------------------------------------------------------------ output
    def write_nbt(self, path):
        import nbtlib
        from nbtlib.tag import Compound, Int, List, String
        palette = []
        index = {}
        blocks = []
        ox, oy, oz = self.origin
        for (x, y, z), state in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            name, props = full_state(state)
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = {"id": String(name)}
                if props:
                    entry["properties"] = Compound({k: String(v) for k, v in sorted(props.items())})
                palette.append(Compound(entry))
            b = {"pos": List[Int]([Int(x - ox), Int(y - oy), Int(z - oz)]), "state": Int(index[key])}
            if (x, y, z) in self.nbt:
                b["nbt"] = Compound({k: String(v) for k, v in self.nbt[(x, y, z)].items()})
            blocks.append(Compound(b))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(s) for s in self.size]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound]([]),
        })
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbtlib.File(root, gzipped=True).save(path)
        return len(blocks), len(palette)


# ---------------------------------------------------------------- colours / previews
_COLORS = {}
FALLBACK = {
    "air": (0, 0, 0), "water": (52, 92, 230), "lava": (255, 120, 20), "fire": (255, 150, 40), "soul_fire": (80, 220, 255),
    "end_rod": (250, 250, 240), "iron_chain": (60, 64, 72), "iron_bars": (120, 120, 126), "lantern": (255, 200, 90),
    "soul_lantern": (90, 210, 230), "campfire": (255, 160, 60), "soul_campfire": (90, 220, 230),
    "lectern": (160, 120, 70), "white_bed": (235, 235, 235), "red_bed": (180, 30, 30), "lily_pad": (40, 120, 40),
    "chest": (160, 110, 40), "ladder": (150, 110, 60), "flower_pot": (140, 80, 60), "cobweb": (230, 230, 230),
    "skeleton_skull": (220, 220, 210), "wither_skeleton_skull": (40, 40, 40), "red_carpet": (160, 30, 30),
    "white_carpet": (240, 240, 240), "yellow_carpet": (240, 200, 60), "glass_pane": (200, 220, 230),
    "red_stained_glass_pane": (170, 40, 40), "nether_brick_fence": (60, 30, 35), "birch_door": (215, 205, 160),
    "polished_blackstone_wall": (55, 50, 60), "potted_white_tulip": (140, 80, 60), "brewing_stand": (130, 110, 90),
    "anvil": (70, 70, 72), "enchanting_table": (120, 30, 60), "ender_chest": (30, 50, 50),
    "magma_block": (190, 80, 25), "bell": (230, 190, 60), "grindstone": (130, 130, 130), "decorated_pot": (150, 90, 60),
    "beacon": (120, 230, 230), "respawn_anchor": (60, 20, 90), "lava_cauldron": (230, 110, 30), "cauldron": (60, 60, 64),
}


def color_of(state):
    name, _ = parse(state)
    if name in _COLORS:
        return _COLORS[name]
    ns, path = name.split(":")
    base = path
    c = None
    if path in FALLBACK:
        c = FALLBACK[path]
    else:
        folder = MOD_TEX if ns == "heavenhell" else VANILLA_TEX
        cands = [base, base + "_top", base + "_side", base.replace("_stairs", "").replace("_slab", ""),
                 base.replace("_brick_stairs", "_bricks").replace("_brick_slab", "_bricks"),
                 base.replace("_stairs", "s").replace("_slab", "s"), base.replace("_wall", "")]
        if base.endswith("_pillar"):
            cands.insert(0, base)
        for cnd in cands:
            p = os.path.join(folder, cnd + ".png")
            if os.path.exists(p):
                im = np.array(Image.open(p).convert("RGBA")).astype(float)
                a = im[..., 3] > 0
                if a.any():
                    c = tuple(int(v) for v in im[..., :3][a].mean(axis=0))
                    break
    if c is None:
        c = (200, 0, 200)
    _COLORS[name] = c
    return c


def top_down(build, path, scale=4, y_max=None):
    ox, oy, oz = build.origin
    sx, sy, sz = build.size
    img = np.zeros((sz, sx, 3), dtype=np.uint8)
    img[:] = (24, 24, 30)
    best = {}
    for (x, y, z), s in build.blocks.items():
        if y_max is not None and y > y_max:
            continue
        k = (x, z)
        if k not in best or y > best[k][0]:
            best[k] = (y, s)
    ys = [v[0] for v in best.values()] or [0]
    lo, hi = min(ys), max(ys)
    for (x, z), (y, s) in best.items():
        c = np.array(color_of(s), dtype=float)
        shade = 0.55 + 0.45 * ((y - lo) / max(1, hi - lo))
        img[z - oz, x - ox] = np.clip(c * shade, 0, 255)
    Image.fromarray(img).resize((sx * scale, sz * scale), Image.NEAREST).save(path)


def iso(build, path, view="se", cut_y=None, scale=3):
    """Isometric render. Each voxel = 2:1 diamond top + two side faces."""
    ox, oy, oz = build.origin
    sx, sy, sz = build.size
    t = scale  # half-width of a voxel top in px
    W = (sx + sz) * t * 2 + 40
    H = (sx + sz) * t + sy * t * 2 + 40
    img = np.zeros((H, W, 3), dtype=np.uint8)
    img[:] = (20, 20, 26)
    depth = np.full((H, W), -1e9)

    def proj(x, y, z):
        if view == "se":
            u, v = (x - ox), (z - oz)
        else:  # sw: rotate 90deg
            u, v = (sz - 1 - (z - oz)), (x - ox)
        px = (u - v) * t * 2 + sz * t * 2 + 20
        py = (u + v) * t - (y - oy) * t * 2 + sy * t * 2 + 20
        return px, py, u + v + (y - oy) * 0.01

    items = []
    for (x, y, z), s in build.blocks.items():
        if cut_y is not None and y > cut_y:
            continue
        items.append((x, y, z, s))
    items.sort(key=lambda it: proj(it[0], it[1], it[2])[2] + it[1] * 2.0)
    for x, y, z, s in items:
        c = np.array(color_of(s), dtype=float)
        px, py, d = proj(x, y, z)
        top = np.clip(c * 1.05 + 8, 0, 255).astype(np.uint8)
        left = np.clip(c * 0.78, 0, 255).astype(np.uint8)
        right = np.clip(c * 0.6, 0, 255).astype(np.uint8)
        # top diamond
        for j in range(t * 2):
            w = (t * 2 - abs(j - t + 0.5) * 2)
            x0 = int(px - w)
            x1 = int(px + w)
            yy = py + j
            if 0 <= yy < H:
                img[yy, max(0, x0):max(0, min(W, x1))] = top
        # left & right faces
        for j in range(t * 2):
            yy0 = py + t + j
            for k in range(t * 2):
                yy = yy0 + k - (k // 1) * 0 + 0
                xl = px - t * 2 + k
                yl = py + t + j + k // 2
                if 0 <= yl < H and 0 <= xl < W:
                    img[yl, xl] = left
                xr = px + k
                yr = py + t * 2 + j - k // 2
                if 0 <= yr < H and 0 <= xr < W:
                    img[yr, xr] = right
    Image.fromarray(img).save(path)


def slice_img(build, y, path, scale=6):
    ox, oy, oz = build.origin
    sx, sy, sz = build.size
    img = np.zeros((sz, sx, 3), dtype=np.uint8)
    img[:] = (24, 24, 30)
    for (x, yy, z), s in build.blocks.items():
        if yy == y:
            img[z - oz, x - ox] = color_of(s)
    Image.fromarray(img).resize((sx * scale, sz * scale), Image.NEAREST).save(path)
