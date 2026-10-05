"""64x64 'vision' illustrations shown big in the choice / judgment dialogs."""
import math

import numpy as np
from PIL import Image, ImageDraw

from px import grid, hexc, mix, new, over, put, rng, save, value_noise

N = 64


def vgrad(top, bottom, h=N, w=N, y0=0, y1=None):
    img = new(w, h)
    y1 = h if y1 is None else y1
    for y in range(h):
        t = min(1.0, max(0.0, (y - y0) / max(1, (y1 - y0))))
        img[y, :] = hexc(mix(top, bottom, t))
    return img


def banded(img, steps=6):
    """Posterize colours a little for a pixel-art look."""
    out = img.copy()
    q = 256 // (steps * 4)
    out[..., :3] = (out[..., :3] // q) * q
    return out


def vignette(img, strength=0.55):
    h, w = img.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    dx = np.abs(xx - (w - 1) / 2) / (w / 2)
    dy = np.abs(yy - (h - 1) / 2) / (h / 2)
    d = np.sqrt(dx ** 2 + dy ** 2) * 0.55 + np.maximum(dx, dy) * 0.45
    f = np.clip((d - 0.68) / 0.4, 0, 1) * strength
    out = img.astype(float)
    out[..., :3] *= (1 - f[..., None])
    return out.astype(np.uint8)


def draw(img, fn):
    """Runs PIL drawing (no anti-aliasing) on a copy and returns the result."""
    im = Image.fromarray(img, "RGBA")
    d = ImageDraw.Draw(im)
    fn(d)
    return np.array(im)


def sparkle(img, x, y, c="#ffffff", big=False):
    put(img, x, y, c)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        put(img, x + dx, y + dy, mix(c, "#88aaff", 0.4))
    if big:
        for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)):
            put(img, x + dx, y + dy, mix(c, "#88aaff", 0.7))


def glow(img, cx, cy, radius, color, strength=0.6):
    h, w = img.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    f = np.clip(1 - d / radius, 0, 1) ** 2 * strength
    c = np.array(hexc(color)[:3], dtype=float)
    out = img.astype(float)
    out[..., :3] = out[..., :3] * (1 - f[..., None]) + c * f[..., None]
    return out.astype(np.uint8)


# ------------------------------------------------------------------ sprites
COW = [
    "........................",
    "...................kk.k.",
    "..kkkkkkkkkkkkkk..kHHkHk",
    ".kWWWWBBBWWWWWWWkkHHHHHk",
    ".kWBBWBBBWWWBBWWWkHeHHHk",
    ".kWBBWWWWWWWBBBWWkHHHPPk",
    "kkWWWWWWWBBWWWWWWWkHPPPk",
    "BkWWWWWWBBBWWWWWWWWkkkk.",
    ".kkWWWWWWWWWWWWWWWk.....",
    "..kWWkkWWk...kWWkkWWk...",
    "..kWWk.kWk...kWk.kWWk...",
    "..kGGk.kGk...kGk.kGGk...",
    "..kkkk.kkk...kkk.kkkk...",
]
COW_PAL = {"W": "#f4f4f4", "B": "#2a2a2a", "H": "#efe6dc", "e": "#111111", "P": "#f0a0a8", "G": "#5a4a3a",
           "k": "#151515"}

VILLAGER = [
    "...kkkkk...",
    "..kSSSSSk..",
    "..kSeSeSk..",
    "..kSSNSSk..",
    "..kSSNSSk..",
    "...kNNNk...",
    "..kRRRRRk..",
    ".kRRrrrRRk.",
    ".kRSrrrSRk.",
    ".kRRrrrRRk.",
    ".kRRRRRRRk.",
    ".kRRRRRRRk.",
    ".kRRRRRRRk.",
    ".kRRRRRRRk.",
    "..kRRkRRk..",
    "..kGGkGGk..",
    "..kkk.kkk..",
]
VILLAGER_PAL = {"S": "#c69c7a", "e": "#2a6a2a", "N": "#b5856a", "R": "#7a5233", "r": "#5d3d24", "G": "#3a2a20",
                "k": "#1a120c"}

ZOMBIE = [
    "...kkkkk..........",
    "..kZZZZZk.........",
    "..kZeZZZk.........",
    "..kZZZZZk.........",
    "..kZzzZZk.........",
    "...kZZZk..........",
    "..kCCCCCkkkkkkkk..",
    ".kCCCCCCZZZZZZZZk.",
    ".kCCCCCCkZZZZZZZk.",
    ".kCCCCCCkkkkkkkk..",
    ".kCCCCCCk.........",
    ".kCCCCCCk.........",
    "..kLLkLLk.........",
    "..kLLkLLk.........",
    "..kLLkLLk.........",
    "..kLLkLLk.........",
    "..kGGkGGk.........",
    "..kkk.kkk.........",
]
ZOMBIE_PAL = {"Z": "#5d9a4a", "z": "#3d6a32", "e": "#1a1a1a", "C": "#2d8a9a", "L": "#3a3a8a", "G": "#555555",
              "k": "#0f1a0f"}

WOLF = [
    "..........kk..kk...",
    ".........kGGkkGGk..",
    "........kGGGGGGGGk.",
    "........kGeGGGeGGk.",
    "........kGGGGGGGGk.",
    "........kGGGNNNGk..",
    "..kkkkkkkGGGGGGk...",
    ".kGGGGGGGGGGGGk....",
    "kGGgGGGGGGgGGGk....",
    "kGGGGGGGGGGGGk.....",
    ".kGGgGGkkGGGGk.....",
    ".kGGkGGk.kGGk......",
    ".kkk.kkk..kkk......",
]
WOLF_PAL = {"G": "#c8c8c8", "g": "#9a9a9a", "e": "#2a3a6a", "N": "#2a2a2a", "k": "#3a3a3a"}

HOODED = [
    "....kkkkk....",
    "...kHHHHHk...",
    "..kHHkkkHHk..",
    "..kHkSSSkHk..",
    "..kHkeSekHk..",
    "..kHkSSSkHk..",
    "..kHHkkkHHk..",
    ".kHHHHHHHHHk.",
    ".kHHHHHHHHHk.",
    "kHHHHhHHHHHHk",
    "kHHHHhHHHHHHk",
    "kHHHHhHHHHHHk",
    "kHHHHhHHHHHHk",
    "kHHHHhHHHHHHk",
    ".kHHHhHHHHHk.",
    ".kkkkkkkkkkk.",
]


def cow_scene():
    img = vgrad("#7fb6ff", "#d8ecff", y1=34)
    img = draw(img, lambda d: (
        d.rectangle([0, 34, 63, 63], fill=hexc("#5fae4a")),
        d.polygon([(30, 38), (63, 36), (63, 60), (26, 62), (24, 50)], fill=hexc("#7a2a10")),
        d.polygon([(31, 40), (62, 38), (62, 58), (28, 60), (27, 50)], fill=hexc("#ff7a1a")),
    ))
    lava = value_noise(N, 6, 5, octaves=2)
    for y in range(40, 60):
        for x in range(27, 63):
            px_ = img[y, x]
            if tuple(px_[:3]) == hexc("#ff7a1a")[:3]:
                t = lava[y, x]
                img[y, x] = hexc("#ffd84a" if t > 0.62 else "#ffa52a" if t > 0.42 else "#e8541a" if t > 0.25 else "#b8340e")
    img = glow(img, 45, 50, 22, "#ffb04a", 0.35)
    # grass tufts
    r = rng(4)
    for _ in range(40):
        x, y = int(r.integers(0, 26)), int(r.integers(35, 63))
        put(img, x, y, "#7cc85c" if r.random() < 0.5 else "#4a9a3a")
    cow = grid(COW, COW_PAL)
    over(img, cow, 8, 27)
    # time-frozen aura
    img = glow(img, 20, 33, 18, "#bcd8ff", 0.35)
    for (x, y) in ((5, 22), (33, 20), (29, 30), (12, 18), (36, 27), (3, 33), (22, 15)):
        sparkle(img, x, y, "#ffffff", big=(x % 2 == 0))
    # clouds in the sky
    img = draw(img, lambda d: (d.ellipse([40, 6, 58, 14], fill=hexc("#ffffff")), d.ellipse([46, 3, 56, 11], fill=hexc("#ffffff")),
                               d.ellipse([4, 8, 18, 14], fill=hexc("#f2f6ff"))))
    return vignette(img)


def zombie_scene():
    img = vgrad("#2a1a4a", "#c86a6a", y1=44)
    img = draw(img, lambda d: (
        d.ellipse([44, 6, 54, 16], fill=hexc("#f2e6c8")),
        d.rectangle([0, 46, 63, 63], fill=hexc("#2f5a2a")),
        d.rectangle([2, 4, 10, 46], fill=hexc("#4a3020")),
        d.ellipse([-8, -6, 22, 18], fill=hexc("#1f3a1a")),
    ))
    over(img, grid(VILLAGER, VILLAGER_PAL), 10, 30)
    over(img, grid(ZOMBIE, ZOMBIE_PAL)[:, ::-1], 26, 29)
    img = glow(img, 18, 36, 10, "#ffcc88", 0.15)
    return vignette(img)


def wolf_scene():
    img = vgrad("#2a4a2a", "#4a6a3a")
    img = draw(img, lambda d: (
        d.rectangle([0, 44, 63, 63], fill=hexc("#3a5a2a")),
        d.ellipse([34, 22, 63, 52], fill=hexc("#2d5a24")),
        d.ellipse([-4, 26, 22, 54], fill=hexc("#2d5a24")),
    ))
    r = rng(9)
    for _ in range(30):
        x, y = int(r.integers(34, 63)), int(r.integers(24, 50))
        put(img, x, y, "#d0302a")
    for _ in range(14):
        x, y = int(r.integers(0, 20)), int(r.integers(28, 52))
        put(img, x, y, "#d0302a")
    def web(d, full=True):
        cx, cy = 30, 36
        col = hexc("#d6d9e4")
        for a in range(0, 360, 45 if full else 90):
            x2 = cx + int(22 * math.cos(math.radians(a + 10)))
            y2 = cy + int(17 * math.sin(math.radians(a + 10)))
            d.line([(cx, cy), (x2, y2)], fill=col)
        if full:
            for rr in (7, 13, 19):
                d.ellipse([cx - rr, cy - int(rr * 0.75), cx + rr, cy + int(rr * 0.75)], outline=col)
    img = draw(img, web)
    over(img, grid(WOLF, WOLF_PAL), 18, 34)
    img = draw(img, lambda d: web(d, full=False))
    # a sad tear
    put(img, 33, 39, "#9ad0ff")
    return vignette(img)


def traveler_scene():
    img = vgrad("#ff9a5a", "#ffd9a0", y1=40)
    img = draw(img, lambda d: (
        d.ellipse([22, 26, 42, 46], fill=hexc("#ffe7a8")),
        d.rectangle([0, 40, 63, 63], fill=hexc("#6a8a3a")),
        d.polygon([(24, 63), (40, 63), (34, 40), (30, 40)], fill=hexc("#a07a4a")),
    ))
    cloak = grid(HOODED, {"H": "#2d4a8a", "h": "#1f3466", "S": "#d6a888", "e": "#2a1a10", "k": "#0f1a33"})
    big = np.repeat(np.repeat(cloak, 2, axis=0), 2, axis=1)
    over(img, big, 8, 30)
    # bowl held out in a hand
    img = draw(img, lambda d: (d.rectangle([33, 44, 38, 47], fill=hexc("#d6a888")),
                               d.pieslice([34, 38, 50, 50], 0, 180, fill=hexc("#8a5a2a")),
                               d.line([(34, 44), (50, 44)], fill=hexc("#5a3a1a"))))
    return vignette(img)


def satchel_scene():
    img = vgrad("#5fae4a", "#3f8a32")
    r = rng(12)
    for _ in range(120):
        put(img, int(r.integers(0, 64)), int(r.integers(0, 64)), "#7cc85c" if r.random() < 0.5 else "#356f2a")
    img = draw(img, lambda d: (
        d.rounded_rectangle([14, 22, 44, 46], 4, fill=hexc("#8a5a2a"), outline=hexc("#3a2410")),
        d.rounded_rectangle([14, 22, 44, 32], 4, fill=hexc("#a87038"), outline=hexc("#3a2410")),
        d.rectangle([27, 28, 31, 34], fill=hexc("#f2c94c")),
        d.line([(16, 24), (10, 10), (30, 6), (42, 24)], fill=hexc("#5a3a1a"), width=2),
    ))
    coins = [(40, 48), (46, 44), (50, 50), (44, 54), (36, 52), (54, 46)]
    for x, y in coins:
        img = draw(img, lambda d, x=x, y=y: d.ellipse([x - 3, y - 2, x + 3, y + 2], fill=hexc("#f2c94c"), outline=hexc("#a8701c")))
    for x, y in ((48, 38), (30, 52), (56, 54)):
        img = draw(img, lambda d, x=x, y=y: d.polygon([(x, y - 3), (x + 3, y), (x, y + 3), (x - 3, y)], fill=hexc("#3ad06a"),
                                                       outline=hexc("#1a6a32")))
    for x, y in ((41, 47), (47, 43), (49, 37)):
        put(img, x, y, "#ffffff")
    # name tag
    img = draw(img, lambda d: (d.rectangle([8, 40, 16, 44], fill=hexc("#f6efd8"), outline=hexc("#6a5a3a")),
                               d.line([(16, 42), (18, 38)], fill=hexc("#6a5a3a"))))
    return vignette(img, 0.45)


def bargain_scene():
    img = vgrad("#1a0f24", "#3a1a4a")
    smoke = value_noise(N, 4, 21, octaves=3)
    for y in range(N):
        for x in range(N):
            if smoke[y, x] > 0.6:
                img[y, x] = hexc(mix(tuple(img[y, x]), "#5a3a6a", 0.4))
    img = draw(img, lambda d: (
        d.polygon([(18, 63), (24, 20), (32, 10), (40, 20), (46, 63)], fill=hexc("#0a060c")),
        d.ellipse([25, 12, 39, 28], fill=hexc("#0a060c")),
        d.polygon([(40, 40), (58, 36), (58, 44), (42, 46)], fill=hexc("#0a060c")),
    ))
    for x in (29, 34):
        put(img, x, 20, "#ff2a2a")
        put(img, x + 1, 20, "#ff6a6a")
    img = glow(img, 31, 20, 8, "#ff2a2a", 0.4)
    # hand + diamonds
    img = draw(img, lambda d: d.ellipse([50, 34, 60, 42], fill=hexc("#3a2a30")))
    for k, (x, y) in enumerate(((52, 30), (57, 28), (55, 33))):
        img = draw(img, lambda d, x=x, y=y: d.polygon([(x, y - 3), (x + 3, y), (x, y + 3), (x - 3, y)], fill=hexc("#5ff0ff"),
                                                       outline=hexc("#1a6a8a")))
        put(img, x - 1, y - 1, "#e0ffff")
    img = glow(img, 55, 31, 12, "#5ff0ff", 0.3)
    return vignette(img)


def heaven_scene():
    img = vgrad("#5aa8ff", "#fff1c8", y1=56)
    # rays
    def rays(d):
        for k in range(-3, 4):
            d.polygon([(32, -4), (32 + k * 10 - 3, 64), (32 + k * 10 + 3, 64)], fill=hexc("#fff7d8", 70))
    img = draw(img, rays)
    img = draw(img, lambda d: (
        d.ellipse([-10, 46, 30, 70], fill=hexc("#ffffff")),
        d.ellipse([20, 48, 60, 72], fill=hexc("#f4f6ff")),
        d.ellipse([40, 44, 74, 70], fill=hexc("#ffffff")),
        d.rectangle([14, 18, 20, 52], fill=hexc("#f6f2ea")),
        d.rectangle([44, 18, 50, 52], fill=hexc("#f6f2ea")),
        d.arc([14, 6, 50, 34], 180, 360, fill=hexc("#f2c94c"), width=3),
        d.rectangle([13, 16, 21, 19], fill=hexc("#f2c94c")),
        d.rectangle([43, 16, 51, 19], fill=hexc("#f2c94c")),
        d.ellipse([15, 12, 19, 16], fill=hexc("#fff6e8")),
        d.ellipse([45, 12, 49, 16], fill=hexc("#fff6e8")),
    ))
    # open gate doors (bars) angled inward
    for k in range(4):
        x = 21 + k * 2
        img = draw(img, lambda d, x=x: d.line([(x, 22 + k), (x, 50)], fill=hexc("#ffffff")))
        x2 = 43 - k * 2
        img = draw(img, lambda d, x2=x2: d.line([(x2, 22 + k), (x2, 50)], fill=hexc("#ffffff")))
    img = glow(img, 32, 30, 20, "#fff2b0", 0.35)
    for x, y in ((8, 10), (56, 8), (32, 4), (26, 40), (40, 38)):
        sparkle(img, x, y, "#ffffff")
    return vignette(img, 0.4)


def hell_scene():
    img = vgrad("#1a0404", "#c8321a", y1=60)
    img = draw(img, lambda d: (
        d.rectangle([0, 52, 63, 63], fill=hexc("#ff7a1a")),
        d.rectangle([12, 14, 20, 54], fill=hexc("#140e10")),
        d.rectangle([44, 14, 52, 54], fill=hexc("#140e10")),
        d.polygon([(12, 14), (16, 4), (20, 14)], fill=hexc("#140e10")),
        d.polygon([(44, 14), (48, 4), (52, 14)], fill=hexc("#140e10")),
        d.arc([12, 8, 52, 40], 180, 360, fill=hexc("#140e10"), width=5),
    ))
    lava = value_noise(N, 8, 31, octaves=2)
    for y in range(52, 64):
        for x in range(N):
            t = lava[y, x]
            img[y, x] = hexc("#ffd84a" if t > 0.65 else "#ff9a2a" if t > 0.4 else "#e0501a")
    r = rng(33)
    for _ in range(70):
        x = int(r.integers(0, 64))
        y = int(r.integers(30, 52))
        h = int(r.integers(2, 8))
        for i in range(h):
            put(img, x, y + i, "#ffb04a" if i < 2 else "#ff5a1f")
    img = glow(img, 32, 40, 18, "#ff3a1a", 0.35)
    for x in (16, 48):
        img = glow(img, x, 14, 6, "#ffcc4a", 0.6)
    return vignette(img)


def portrait(bg_top, bg_bottom, face, hair, eyes, collar, trim, horns=False, halo=False, beard=False, crown=False):
    img = vgrad(bg_top, bg_bottom)
    def body(d):
        # shoulders
        d.polygon([(6, 64), (12, 46), (24, 40), (40, 40), (52, 46), (58, 64)], fill=hexc(collar))
        d.line([(24, 40), (32, 58), (40, 40)], fill=hexc(trim), width=2)
        # neck
        d.rectangle([28, 34, 36, 42], fill=hexc(mix(face, "#000000", 0.15)))
        # head
        d.rectangle([20, 12, 44, 38], fill=hexc(face))
        # hair
        d.rectangle([19, 9, 45, 16], fill=hexc(hair))
        d.rectangle([19, 9, 22, 26], fill=hexc(hair))
        d.rectangle([42, 9, 45, 26], fill=hexc(hair))
    img = draw(img, body)
    # eyes
    for x in (26, 35):
        img = draw(img, lambda d, x=x: d.rectangle([x, 22, x + 3, 24], fill=hexc("#ffffff" if not horns else "#2a1a1a")))
        img = draw(img, lambda d, x=x: d.rectangle([x + 1, 22, x + 2, 24], fill=hexc(eyes)))
    img = draw(img, lambda d: (d.rectangle([30, 26, 33, 30], fill=hexc(mix(face, "#000000", 0.12))),
                               d.line([(27, 33), (37, 33)], fill=hexc(mix(face, "#000000", 0.35)))))
    if beard:
        img = draw(img, lambda d: (d.polygon([(20, 28), (44, 28), (40, 50), (32, 56), (24, 50)], fill=hexc("#f4f4f8")),
                                   d.line([(27, 32), (37, 32)], fill=hexc("#c8c8d6"))))
    if horns:
        img = draw(img, lambda d: (d.polygon([(21, 10), (16, 2), (24, 9)], fill=hexc("#1a1416")),
                                   d.polygon([(43, 10), (48, 2), (40, 9)], fill=hexc("#1a1416")),
                                   d.line([(17, 3), (19, 6)], fill=hexc("#f2c94c")),
                                   d.line([(47, 3), (45, 6)], fill=hexc("#f2c94c"))))
        # glowing crack lines on the face
        for (x, y) in ((22, 30), (23, 31), (42, 17), (41, 18), (40, 19)):
            put(img, x, y, "#ff3a2a")
        img = glow(img, 28, 23, 4, eyes, 0.5)
        img = glow(img, 37, 23, 4, eyes, 0.5)
    if halo:
        img = draw(img, lambda d: d.ellipse([18, 2, 46, 9], outline=hexc("#ffd84a"), width=2))
        img = glow(img, 32, 6, 16, "#fff2b0", 0.3)
    if crown:
        img = draw(img, lambda d: d.polygon([(21, 10), (24, 3), (27, 9), (32, 2), (37, 9), (40, 3), (43, 10)],
                                            fill=hexc("#f2c94c"), outline=hexc("#7a5a10")))
    # collar flaps
    img = draw(img, lambda d: (d.polygon([(14, 46), (22, 36), (26, 46)], fill=hexc(mix(collar, "#000000", 0.3))),
                               d.polygon([(50, 46), (42, 36), (38, 46)], fill=hexc(mix(collar, "#000000", 0.3)))))
    return img


def lucifer_scene():
    img = portrait("#1a0606", "#4a0a08", face="#8a8a92", hair="#141012", eyes="#ffb02a", collar="#141012",
                   trim="#f2c94c", horns=True)
    # throne behind (gold edges peeking out)
    img = draw(img, lambda d: (d.rectangle([2, 6, 6, 64], fill=hexc("#5a3a10")), d.rectangle([58, 6, 62, 64], fill=hexc("#5a3a10")),
                               d.rectangle([3, 6, 5, 64], fill=hexc("#c9952e")), d.rectangle([59, 6, 61, 64], fill=hexc("#c9952e"))))
    # crimson lining inside the coat
    img = draw(img, lambda d: (d.line([(23, 42), (30, 60)], fill=hexc("#8a1020"), width=2),
                               d.line([(41, 42), (34, 60)], fill=hexc("#8a1020"), width=2)))
    img = glow(img, 32, 30, 30, "#ff2a1a", 0.15)
    return vignette(img)


def gatekeeper_scene():
    img = portrait("#ffe9a8", "#fff8e0", face="#e8c6a8", hair="#f4f4f8", eyes="#3a6ab0", collar="#fbfaf6",
                   trim="#f2c94c", halo=True, beard=True)
    img = draw(img, lambda d: (d.line([(18, 50), (26, 64)], fill=hexc("#2a4a9a"), width=3),
                               d.line([(46, 50), (38, 64)], fill=hexc("#2a4a9a"), width=3)))
    return vignette(img, 0.35)


def scales_scene():
    img = vgrad("#0a1430", "#1a2a5a")
    gold, dark = hexc("#f2c94c"), hexc("#a8701c")
    def scale(d):
        d.rectangle([31, 10, 33, 54], fill=gold)
        d.rectangle([22, 54, 42, 57], fill=gold)
        d.line([(10, 18), (54, 18)], fill=gold, width=2)
        d.ellipse([29, 7, 35, 13], fill=gold)
        for cx in (12, 52):
            d.line([(cx, 19), (cx - 7, 34)], fill=dark)
            d.line([(cx, 19), (cx + 7, 34)], fill=dark)
            d.chord([cx - 9, 30, cx + 9, 40], 0, 180, fill=gold, outline=dark)
    img = draw(img, scale)
    # feather (left pan) and ember (right pan)
    img = draw(img, lambda d: d.polygon([(8, 33), (14, 22), (16, 24), (11, 34)], fill=hexc("#ffffff")))
    img = glow(img, 12, 28, 9, "#ffffff", 0.35)
    img = draw(img, lambda d: d.ellipse([48, 28, 56, 35], fill=hexc("#1a0a08")))
    put(img, 51, 30, "#ff5a1f")
    put(img, 53, 32, "#ff9a3a")
    img = glow(img, 52, 31, 8, "#ff3a1a", 0.35)
    for x, y in ((6, 6), (58, 10), (20, 46), (46, 48), (40, 4)):
        put(img, x, y, "#ffffff")
    return vignette(img)


ALL = {
    "vision_lava_cow": cow_scene,
    "vision_zombie_ambush": zombie_scene,
    "vision_trapped_wolf": wolf_scene,
    "vision_hungry_traveler": traveler_scene,
    "vision_lost_satchel": satchel_scene,
    "vision_devils_bargain": bargain_scene,
    "vision_heaven": heaven_scene,
    "vision_hell": hell_scene,
    "vision_lucifer": lucifer_scene,
    "vision_gatekeeper": gatekeeper_scene,
    "vision_scales": scales_scene,
}


def build():
    out = {}
    for name, fn in ALL.items():
        img = fn()
        img[..., 3] = 255
        save(img, f"item/{name}.png")
        out[name] = img
    return out
