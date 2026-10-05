"""16x16 block textures."""
import numpy as np

from px import (grid, hexc, mix, new, normalize, put, putw, quantize, rect, rng, save, value_noise)

S = 16


# ------------------------------------------------------------------ helpers
def bricks(base_fn, mortar, hi, lo, seed, rows=4, width=8, offset=4, mortar_rows=None):
    """Brick layout: rows of height 16/rows (last px of each row = mortar), bricks `width` wide."""
    img = base_fn(seed)
    rh = S // rows
    r = rng(seed + 7)
    for row in range(rows):
        y0 = row * rh
        off = 0 if row % 2 == 0 else offset
        # mortar line under the row
        for x in range(S):
            putw(img, x, y0 + rh - 1, mortar)
        # vertical joints
        for k in range(S // width + 1):
            jx = (k * width + off) % S
            for y in range(y0, y0 + rh - 1):
                putw(img, jx, y, mortar)
            # highlight on brick top-left, shadow bottom-right
            bx = jx + 1
            for x in range(bx, bx + width - 1):
                putw(img, x, y0, hi)
            for y in range(y0, y0 + rh - 1):
                putw(img, bx, y, hi)
            for x in range(bx, bx + width - 1):
                putw(img, x, y0 + rh - 2, lo)
            if r.random() < 0.5:
                putw(img, bx + int(r.integers(1, width - 2)), y0 + int(r.integers(1, rh - 2)), lo)
    return img


def speckle(img, colors, count, seed, mask=None):
    r = rng(seed)
    for _ in range(count):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        if mask is not None and not mask[y, x]:
            continue
        img[y, x] = hexc(colors[int(r.integers(0, len(colors)))])
    return img


# ------------------------------------------------------------------ heaven
CLOUD = ["#bcc0e6", "#d2d6f2", "#e6e9fb", "#f4f6ff", "#ffffff"]
GOLD_CLOUD = ["#d9a84a", "#efc766", "#fbdf8c", "#fff0bf", "#fffbe8"]


def cloud_field(seed):
    f = value_noise(S, 2, seed, octaves=3, persistence=0.55)
    # light from top-left: compare with shifted copy
    lit = f - np.roll(np.roll(f, 1, 0), 1, 1)
    return normalize(f * 0.7 + normalize(lit) * 0.5)


def cloud(seed=11, palette=CLOUD):
    return quantize(cloud_field(seed), palette, cut=[0.18, 0.36, 0.56, 0.78])


def pearl_base(seed):
    f = value_noise(S, 2, seed, octaves=2)
    img = quantize(f, ["#d2ccc2", "#e0dbd3", "#ece8e2", "#f8f6f1"], cut=[0.2, 0.45, 0.75])
    # faint iridescent streaks along a diagonal
    r = rng(seed + 3)
    tints = ["#f4e3ec", "#e4ecf8", "#e2f4ec", "#f6efdc"]
    for k in range(5):
        x0, y0 = int(r.integers(0, S)), int(r.integers(0, S))
        t = tints[k % len(tints)]
        for i in range(int(r.integers(3, 7))):
            putw(img, x0 + i, y0 - i // 2, t)
    return img


def pearlstone(seed=21):
    return pearl_base(seed)


def pearlstone_bricks(seed=22):
    return bricks(pearl_base, "#c9c2b8", "#fcfbf8", "#d6d0c6", seed)


def chiseled_pearlstone(seed=23):
    img = pearl_base(seed)
    rect(img, 0, 0, 15, 0, "#fcfbf8")
    rect(img, 0, 0, 0, 15, "#fcfbf8")
    rect(img, 0, 15, 15, 15, "#c9c2b8")
    rect(img, 15, 0, 15, 15, "#c9c2b8")
    rect(img, 1, 1, 14, 1, "#d6d0c6")
    rect(img, 1, 1, 1, 14, "#d6d0c6")
    motif = [
        "................",
        "................",
        ".......y........",
        "...y...Y...y....",
        "....y.....y.....",
        "......ooo.......",
        ".....o...o......",
        "..yY.o.W.o.Yy...",
        ".....o...o......",
        "......ooo.......",
        "....y.....y.....",
        "...y...Y...y....",
        ".......y........",
        "................",
        "................",
        "................",
    ]
    grid(motif, {"y": "#e8c25a", "Y": "#f7dc86", "o": "#d9a43a", "W": "#fff6d0"}, img, ox=0, oy=0)
    return img


def pearlstone_pillar(seed=24):
    img = pearl_base(seed)
    cols = ["#fbfaf6", "#efebe5", "#e2ddd5", "#efebe5"]
    for x in range(S):
        c = cols[x % 4]
        for y in range(2, 14):
            if (x % 4) in (2,):
                img[y, x] = hexc("#d5cfc6")
            elif (x % 4) == 0:
                img[y, x] = hexc("#fbfaf6")
    rect(img, 0, 0, 15, 1, "#e8c25a")
    rect(img, 0, 14, 15, 15, "#e8c25a")
    rect(img, 0, 1, 15, 1, "#c9952e")
    rect(img, 0, 14, 15, 14, "#f7dc86")
    return img


def pearlstone_pillar_top(seed=25):
    img = new(S)
    for y in range(S):
        for x in range(S):
            d = max(abs(x - 7.5), abs(y - 7.5))
            ring = int(d) % 3
            img[y, x] = hexc(["#f6f3ee", "#e9e5df", "#ddd8cf"][ring])
    rect(img, 0, 0, 15, 0, "#e8c25a")
    rect(img, 0, 15, 15, 15, "#c9952e")
    rect(img, 0, 0, 0, 15, "#e8c25a")
    rect(img, 15, 0, 15, 15, "#c9952e")
    rect(img, 6, 6, 9, 9, "#f7dc86")
    rect(img, 7, 7, 8, 8, "#fff6d0")
    return img


def gilded_pearlstone(seed=26):
    img = pearlstone_bricks(seed)
    r = rng(seed)
    gold = ["#c9952e", "#e8c25a", "#f7dc86"]
    for vein in range(3):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        for i in range(14):
            putw(img, x, y, gold[1])
            putw(img, x + 1, y, gold[0])
            if i % 3 == 0:
                putw(img, x, y - 1, gold[2])
            step = int(r.integers(0, 3))
            x += 1
            y += (-1, 0, 1)[step]
    return img


def golden_bricks(seed=27):
    def base(s):
        f = value_noise(S, 2, s, octaves=2)
        return quantize(f, ["#e2a93a", "#f0c24f", "#fbd96d", "#ffe998"], cut=[0.25, 0.5, 0.75])
    img = bricks(base, "#9a6a1c", "#fff6c8", "#c98c26", seed)
    speckle(img, ["#ffffff", "#fff6c8"], 6, seed + 1)
    return img


def heaven_soil(seed=31):
    f = value_noise(S, 3, seed, octaves=2)
    img = quantize(f, ["#cdb489", "#dcc79e", "#ead9b6", "#f5e8cc"], cut=[0.25, 0.5, 0.75])
    speckle(img, ["#bfa476", "#fff6e2"], 10, seed)
    return img


def heaven_grass_top(seed=32):
    f = value_noise(S, 3, seed, octaves=2)
    img = quantize(f, ["#a8d273", "#bde68b", "#d1f3a5", "#e6ffc8"], cut=[0.22, 0.48, 0.74])
    speckle(img, ["#ffe98a", "#fff4b8"], 7, seed + 1)
    r = rng(seed + 2)
    for _ in range(3):
        x, y = int(r.integers(1, 15)), int(r.integers(1, 15))
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            putw(img, x + dx, y + dy, "#ffffff")
        putw(img, x, y, "#ffd75a")
    return img


def heaven_grass_side(seed=33):
    img = heaven_soil(seed)
    top = heaven_grass_top(seed + 1)
    r = rng(seed)
    for x in range(S):
        depth = 3 + int(r.integers(0, 3))
        for y in range(depth):
            img[y, x] = top[y, x]
        img[depth - 1, x] = hexc("#a8d273")
    return img


def celestial_log(seed=41):
    r = rng(seed)
    img = new(S)
    cols = ["#d2cab8", "#e3ddd0", "#f0ece4", "#fbfaf6"]
    base = [int(r.integers(1, 4)) for _ in range(S)]
    for x in range(S):
        for y in range(S):
            v = base[x]
            if r.random() < 0.18:
                v = max(0, v - 1)
            img[y, x] = hexc(cols[v])
    # bark furrows
    for x in range(0, S, 4):
        fx = (x + int(r.integers(0, 2))) % S
        y0 = int(r.integers(0, S))
        for i in range(int(r.integers(5, 11))):
            putw(img, fx, y0 + i, cols[0])
    # soft golden streaks
    for _ in range(3):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        for i in range(int(r.integers(2, 5))):
            putw(img, x, y + i, "#efd27e")
    return img


def celestial_log_top(seed=42):
    img = new(S)
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            ring = int(d * 1.15) % 3
            img[y, x] = hexc(["#fff3cf", "#f3dca0", "#e8c985"][ring])
    bark = celestial_log(seed)
    for i in range(S):
        img[0, i] = bark[0, i]
        img[15, i] = bark[15, i]
        img[i, 0] = bark[i, 0]
        img[i, 15] = bark[i, 15]
    return img


def celestial_planks(seed=43):
    f = value_noise(S, 4, seed, octaves=2)
    img = quantize(f, ["#e0d2b0", "#ece2c8", "#f6efdd", "#fffaf0"], cut=[0.2, 0.45, 0.72])
    for row in range(4):
        y = row * 4 + 3
        for x in range(S):
            img[y, x] = hexc("#cbb98f")
        img[row * 4, :] = np.maximum(img[row * 4, :], np.array(hexc("#fffaf0"), dtype=np.uint8) * 0)
        joint = (row * 7 + 3) % S
        for yy in range(row * 4, row * 4 + 3):
            img[yy, joint] = hexc("#d8c7a0")
    r = rng(seed)
    for _ in range(5):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        if y % 4 != 3:
            putw(img, x, y, "#efd58a")
    return img


def leaves(seed, palette, accent):
    """Clumps of small leaves with see-through gaps (vanilla-like)."""
    r = rng(seed)
    f = value_noise(S, 4, seed, octaves=2)
    img = quantize(f, palette, cut=[0.22, 0.48, 0.74])
    # leaf clumps: light top-left pixel, darker bottom-right pixel
    for _ in range(26):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        putw(img, x, y, palette[3])
        putw(img, x + 1, y + 1, palette[1])
        putw(img, x + 1, y, palette[2])
    for _ in range(5):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        putw(img, x, y, accent)
    # gaps: scattered single pixels and short dashes (about 16%)
    holes = np.zeros((S, S), dtype=bool)
    for _ in range(26):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        holes[y, x] = True
        if r.random() < 0.35:
            holes[y, (x + 1) % S] = True
    img[holes] = 0
    return img


def celestial_leaves(seed=44):
    return leaves(seed, ["#a9cfc0", "#c9e6db", "#e4f5ee", "#ffffff"], "#f3dd8c")


def golden_leaves(seed=45):
    return leaves(seed + 1, ["#d39a2f", "#efbf4a", "#fbdb72", "#fff2b0"], "#ffffff")


def angel_lily():
    rows = [
        "................",
        "................",
        "......w..w......",
        ".....wWwwWw.....",
        "....wWWyyWWw....",
        "....wWyYYyWw....",
        ".....wWyyWw.....",
        "......wWWw......",
        ".......gg.......",
        ".......g........",
        "..ll...g...l....",
        "...lL..g..Ll....",
        "....lL.g.Ll.....",
        ".....lLgLl......",
        ".......g........",
        ".......g........",
    ]
    return grid(rows, {"w": "#e3e6f2", "W": "#ffffff", "y": "#f2c64b", "Y": "#fff1a6",
                       "g": "#5f9e4a", "l": "#4c8a3c", "L": "#7cc062"})


def halo_lamp():
    img = new(S)
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            img[y, x] = hexc(mix("#fffbe6", "#ffd978", min(1.0, d / 9.0)))
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 3.8 <= d <= 5.2:
                img[y, x] = hexc("#ffcf3a" if d < 4.5 else "#f2b42a")
    for i in range(S):
        for c, (x, y) in ((("#6b4a14"), (i, 0)), ("#6b4a14", (0, i)), ("#4d340d", (i, 15)), ("#4d340d", (15, i))):
            put(img, x, y, c)
    for i in range(1, 15):
        put(img, i, 1, "#a87a24")
        put(img, 1, i, "#a87a24")
        put(img, i, 14, "#8a6420")
        put(img, 14, i, "#8a6420")
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, "#ffe08a")
    return img


def light_block(seed, colors, alpha_lo, alpha_hi):
    r = rng(seed)
    img = new(S)
    streak = value_noise(S, 4, seed, octaves=1)
    streak = np.repeat(streak[:1, :], S, axis=0) * 0.7 + value_noise(S, 2, seed + 1) * 0.3
    for y in range(S):
        for x in range(S):
            t = streak[y, x]
            c = hexc(colors[min(len(colors) - 1, int(t * len(colors)))])
            a = int(alpha_lo + (alpha_hi - alpha_lo) * t)
            img[y, x] = (c[0], c[1], c[2], a)
    for _ in range(8):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        img[y, x] = (255, 255, 255, alpha_hi + 20)
    return img


def return_light():
    return light_block(51, ["#fff1c8", "#fff7e0", "#ffffff"], 110, 170)


# ------------------------------------------------------------------ hell
def brim_base(seed):
    f = value_noise(S, 3, seed, octaves=3, persistence=0.6)
    img = quantize(f, ["#2c0c08", "#3d120c", "#511a12", "#6b2617", "#83321c"], cut=[0.2, 0.38, 0.58, 0.78])
    return img


def brimstone(seed=61):
    img = brim_base(seed)
    speckle(img, ["#e8c64a", "#c9a030", "#d0622a"], 9, seed)
    return img


def brimstone_bricks(seed=62):
    return bricks(lambda s: speckle(brim_base(s), ["#c9a030"], 3, s), "#1a0705", "#8a3a22", "#2c0c08", seed)


def hell_base(seed):
    f = value_noise(S, 3, seed, octaves=2)
    return quantize(f, ["#141012", "#1b1618", "#231d1f", "#2d2628"], cut=[0.25, 0.5, 0.75])


def cracks(img, seed, n=3, colors=("#ff5a1f", "#c8321a", "#ff9a3a")):
    r = rng(seed)
    for _ in range(n):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        for i in range(int(r.integers(4, 9))):
            putw(img, x, y, colors[0] if i % 3 else colors[2])
            if r.random() < 0.3:
                putw(img, x + 1, y, colors[1])
            d = int(r.integers(0, 4))
            x += (1, 0, -1, 1)[d]
            y += (0, 1, 1, 1)[d]
    return img


def hellstone_bricks(seed=63):
    img = bricks(hell_base, "#0a0708", "#3a3134", "#0f0b0c", seed)
    # glowing cracks run along a few mortar lines instead of across the bricks
    r = rng(seed + 5)
    for row in range(4):
        y = row * 4 + 3
        start = int(r.integers(0, S))
        for i in range(int(r.integers(3, 7))):
            putw(img, start + i, y, "#ff5a1f" if i % 2 else "#c8321a")
    for _ in range(3):
        x, y = int(r.integers(0, S)), int(r.integers(0, S))
        if y % 4 != 3:
            putw(img, x, y, "#7a2010")
    return img


def chiseled_hellstone(seed=64):
    img = hell_base(seed)
    rect(img, 0, 0, 15, 0, "#3a3134")
    rect(img, 0, 0, 0, 15, "#3a3134")
    rect(img, 0, 15, 15, 15, "#0a0708")
    rect(img, 15, 0, 15, 15, "#0a0708")
    sigil = [
        "................",
        "................",
        "..r..........r..",
        "..rr........rr..",
        "...rr.RRRR.rr...",
        "....rRrrrrRr....",
        "....RrOrrOrR....",
        "....RrOrrOrR....",
        "....RrrrrrrR....",
        ".....RrrNrrR....",
        "......RrrrR.....",
        "......r.r.r.....",
        "......RRRRR.....",
        "................",
        "................",
        "................",
    ]
    return grid(sigil, {"r": "#c8321a", "R": "#ff5a1f", "O": "#ffd36a", "N": "#5a0f08"}, img)


def hellstone_pillar(seed=65):
    img = hell_base(seed)
    for x in (3, 8, 12):
        for y in range(S):
            img[y, x] = hexc("#ff5a1f" if (y + x) % 5 else "#ff9a3a")
            img[y, x - 1] = hexc("#5a1a10")
    rect(img, 0, 0, 15, 1, "#3a3134")
    rect(img, 0, 14, 15, 15, "#0a0708")
    return img


def hellstone_pillar_top(seed=66):
    img = new(S)
    for y in range(S):
        for x in range(S):
            d = max(abs(x - 7.5), abs(y - 7.5))
            img[y, x] = hexc(["#1b1618", "#231d1f", "#2d2628"][int(d) % 3])
    rect(img, 6, 6, 9, 9, "#c8321a")
    rect(img, 7, 7, 8, 8, "#ff9a3a")
    return img


def gilded_hellstone(seed=67):
    img = hellstone_bricks(seed)
    for x in range(S):
        img[7, x] = hexc("#f2c94c")
        img[8, x] = hexc("#b8860b")
    for c in ((0, 0), (15, 0), (0, 15), (15, 15)):
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            x = c[0] + (dx if c[0] == 0 else -dx)
            y = c[1] + (dy if c[1] == 0 else -dy)
            img[y, x] = hexc("#f2c94c")
    return img


def soul_shard_ore(seed=68):
    img = brimstone(seed)
    shards = [
        "................",
        "..c.............",
        "..Cc.......v....",
        "..cCk.....vVk...",
        "...ck......vk...",
        "................",
        ".......c........",
        "......cCc.......",
        "......cWCk......",
        ".......Ck.......",
        "........k.......",
        "...v.........c..",
        "..vVk.......cCk.",
        "...vk.......cWk.",
        ".............k..",
        "................",
    ]
    return grid(shards, {"c": "#4cc3ff", "C": "#7af0ff", "W": "#e0fbff", "v": "#8a5cff", "V": "#c7a6ff",
                         "k": "#1d0f2e"}, img)


def ash_block(seed=69):
    f = value_noise(S, 3, seed, octaves=3)
    img = quantize(f, ["#7d7a78", "#949190", "#a8a6a4", "#bcbcbc", "#d2d0ce"], cut=[0.2, 0.4, 0.6, 0.8])
    speckle(img, ["#ff7a2a", "#ffb04a"], 4, seed + 2)
    return img


def ember_lamp():
    img = new(S)
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            img[y, x] = hexc(mix("#ffe08a", "#d0401a", min(1.0, d / 8.0)))
    for i in range(S):
        for x, y in ((i, 0), (0, i), (i, 15), (15, i), (i, 7), (7, i)):
            put(img, x, y, "#2b2b2e")
        put(img, i, 8, "#2b2b2e")
        put(img, 8, i, "#2b2b2e")
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15), (7, 7), (8, 8), (7, 8), (8, 7)):
        put(img, x, y, "#4a4a52")
    for i in range(1, 15):
        put(img, i, 1, "#3d3d42")
        put(img, 1, i, "#3d3d42")
    return img


def redemption_light():
    return light_block(71, ["#ffe7a0", "#fff4cf", "#ffffff"], 130, 190)


ALL = {
    "cloud": lambda: cloud(),
    "golden_cloud": lambda: cloud(12, GOLD_CLOUD),
    "heaven_grass_top": heaven_grass_top,
    "heaven_grass_side": heaven_grass_side,
    "heaven_soil": heaven_soil,
    "pearlstone": pearlstone,
    "pearlstone_bricks": pearlstone_bricks,
    "chiseled_pearlstone": chiseled_pearlstone,
    "pearlstone_pillar": pearlstone_pillar,
    "pearlstone_pillar_top": pearlstone_pillar_top,
    "gilded_pearlstone": gilded_pearlstone,
    "golden_bricks": golden_bricks,
    "celestial_log": celestial_log,
    "celestial_log_top": celestial_log_top,
    "celestial_planks": celestial_planks,
    "celestial_leaves": celestial_leaves,
    "golden_leaves": golden_leaves,
    "angel_lily": angel_lily,
    "halo_lamp": halo_lamp,
    "return_light": return_light,
    "brimstone": brimstone,
    "brimstone_bricks": brimstone_bricks,
    "hellstone_bricks": hellstone_bricks,
    "chiseled_hellstone": chiseled_hellstone,
    "hellstone_pillar": hellstone_pillar,
    "hellstone_pillar_top": hellstone_pillar_top,
    "gilded_hellstone": gilded_hellstone,
    "soul_shard_ore": soul_shard_ore,
    "ash_block": ash_block,
    "ember_lamp": ember_lamp,
    "redemption_light": redemption_light,
}

TILEABLE = {"cloud", "golden_cloud", "heaven_grass_top", "heaven_soil", "pearlstone", "pearlstone_bricks",
            "gilded_pearlstone", "golden_bricks", "celestial_log", "celestial_planks", "celestial_leaves",
            "golden_leaves", "brimstone", "brimstone_bricks", "hellstone_bricks", "gilded_hellstone", "ash_block",
            "soul_shard_ore"}


def build():
    out = {}
    for name, fn in ALL.items():
        img = fn()
        save(img, f"block/{name}.png")
        out[name] = img
    return out
