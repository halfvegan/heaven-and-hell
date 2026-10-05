"""16x16 item textures (hand-placed pixels)."""
import numpy as np

from px import grid, hexc, mix, new, put, quantize, rng, save, value_noise

K = "#1b1420"  # default outline

ITEMS = {}


def item(name, rows, pal):
    ITEMS[name] = (rows, pal)


item("soul_shard", [
    "................",
    "...........kk...",
    "..........kWCk..",
    ".........kWCCk..",
    "........kWCCck..",
    ".......kWCCcvk..",
    "......kCCCcvk...",
    ".....kCCccvk....",
    "....kCccvvk.....",
    "....kcvvVk......",
    "...kvvVVk.......",
    "...kvVVk........",
    "..kvVk..........",
    "..kkk...........",
    "................",
    "................",
], {"W": "#e0fbff", "C": "#7af0ff", "c": "#4cc3ff", "v": "#8a5cff", "V": "#5a3acc", "k": "#1d0f2e"})

FEATHER = [
    "................",
    ".............kk.",
    "...........kkLk.",
    ".........kkLLlk.",
    "........kLLllLk.",
    ".......kLLlqlk..",
    "......kLLlqlLk..",
    ".....kLLlqlLk...",
    "....kLLlqlLLk...",
    "....kLlqlLLk....",
    "...kLlqlLLLk....",
    "...kllqLLk......",
    "..kllqkkk.......",
    "..kqqk..........",
    ".kqk............",
    ".kk.............",
]
item("golden_feather", FEATHER, {"L": "#fff1a6", "l": "#f2c64b", "q": "#a8701c", "k": "#5a3a0a"})
item("feather_of_return", FEATHER, {"L": "#ffffff", "l": "#cfe0ff", "q": "#7aa6e8", "k": "#2a3a6a"})

item("angel_wings", [
    "................",
    ".k............k.",
    "kWk..........kWk",
    "kWWk........kWWk",
    "kWsWk......kWsWk",
    "kWWsWk....kWsWWk",
    ".kWWsWk..kWsWWk.",
    ".kWWWsWkkWsWWWk.",
    "..kWWWWkkWWWWk..",
    "..kYWWsk.kWsWYk.",
    "...kYWWk.kWWYk..",
    "...kYYsk.ksYYk..",
    "....kYYk.kYYk...",
    ".....kYk.kYk....",
    "......k...k.....",
    "................",
], {"W": "#ffffff", "s": "#c9d0ea", "Y": "#f2c64b", "k": "#5a5a78"})

item("demon_wings", [
    "................",
    ".k............k.",
    "kRk..........kRk",
    "kRBk........kBRk",
    "kRrBk......kBrRk",
    "kBrrBk....kBrrBk",
    "kBrrrBk..kBrrrBk",
    ".kBrrrBkkBrrrBk.",
    ".kBrrrrBBrrrrBk.",
    "..kBrBrBBrBrBk..",
    "..kB.kB..Bk.Bk..",
    "...k..k..k..k...",
    "................",
    "................",
    "................",
    "................",
], {"B": "#2a1416", "r": "#8a1f1f", "R": "#e0502a", "k": "#0d0607"})

item("halo", [
    "................",
    "................",
    "...s........s...",
    "................",
    ".....kkkkkk.....",
    "...kkYYYYYYkk...",
    "..kYYyyyyyyYYk..",
    ".kYyk......kyYk.",
    ".kYk........kYk.",
    ".kYyk......kyYk.",
    "..kYYyyyyyyYYk..",
    "...kkYYYYYYkk...",
    ".....kkkkkk.....",
    "..s..........s..",
    "................",
    "................",
], {"Y": "#fff1a6", "y": "#f2c64b", "k": "#b07a1c", "s": "#fff6c8"})

item("infernal_crown", [
    "................",
    "................",
    "..k...k..k...k..",
    ".kGk.kGkkGk.kGk.",
    ".kGk.kGGGGk.kGk.",
    ".kGGkGGRRGGkGGk.",
    ".kGGGGRRRRGGGGk.",
    ".kGgGGGRRGGGgGk.",
    ".kDDDDDDDDDDDDk.",
    ".kDGRGDRRDGRGDk.",
    ".kDDDDDDDDDDDDk.",
    "..kkkkkkkkkkkk..",
    "................",
    "................",
    "................",
    "................",
], {"G": "#f2c94c", "g": "#c9952e", "D": "#1a1416", "R": "#e01a2a", "k": "#3a2a08"})

item("fallen_halo", [
    "................",
    "................",
    "................",
    "................",
    ".....kkkk.k.....",
    "...kkDDDk.DkK...",
    "..kDdddk..ddDk..",
    ".kDdk......kdDk.",
    ".kDk........kRk.",
    ".kDdk......kdRk.",
    "..kDDdd.k.dRRk..",
    "...kkDD.kDDkk...",
    ".....kkk.kk.....",
    "................",
    "................",
    "................",
], {"D": "#9a7a32", "d": "#5a4318", "R": "#e0401a", "k": "#1a1008", "K": "#e0401a"})

item("harp", [
    "................",
    "..kk........kk..",
    ".kYYk......kYYk.",
    ".kYyk......kyYk.",
    "..kYk......kYk..",
    "..kYYkkkkkkYYk..",
    "..kYyyyyyyyyYk..",
    "..kYk.S.S.SkYk..",
    "..kYk.S.S.SkYk..",
    "...kY.S.S.SYk...",
    "...kYkS.S.SYk...",
    "....kYS.S.Yk....",
    "....kYkS.SYk....",
    ".....kYYYYk.....",
    "......kkkk......",
    "................",
], {"Y": "#f7d55a", "y": "#c9952e", "S": "#f2f2ff", "k": "#6b4a14"})

item("manna", [
    "................",
    "..s.........s...",
    "................",
    ".....kkkkkk.....",
    "...kkLLLLLLkk...",
    "..kLLWLLLLLLLk..",
    ".kLWLLlllLLLLLk.",
    ".kLLLlLLLlLLLLk.",
    ".kLLlLLLLLlLLLk.",
    ".kBLLLLLLLLLLBk.",
    "..kBBLLLLLLBBk..",
    "...kkBBBBBBkk...",
    ".....kkkkkk.....",
    "................",
    ".s............s.",
    "...s........s...",
], {"L": "#f5d58e", "l": "#d9a85a", "W": "#fff6d8", "B": "#b9803a", "k": "#6b4214", "s": "#fff3a6"})

SWORD = [
    "................",
    ".............kk.",
    "............kWWk",
    "...........kWWek",
    "..........kWWek.",
    ".........kWWek..",
    "........kWWek...",
    "...kk..kWWek....",
    "...kGk.kWek.....",
    "....kGkWek......",
    ".....kGGk.......",
    ".....khGGk......",
    "....khk.kGk.....",
    "...khk...kk.....",
    "..kPk...........",
    "..kk............",
]
item("seraph_blade", SWORD, {"W": "#ffffff", "e": "#bcd4ff", "G": "#f2c94c", "h": "#8a5a2a", "P": "#fff3a6",
                             "k": "#3a3a5a"})
item("hellfire_sword", SWORD, {"W": "#3a3033", "e": "#ff7a2a", "G": "#8a1f1f", "h": "#2a1a14", "P": "#ffb04a",
                               "k": "#0d0607"})
item("morningstar", [
    "................",
    ".............kk.",
    "............kDDk",
    "...........kDdRk",
    "..........kDdRk.",
    ".........kDdRk..",
    "........kDdRk...",
    "...kk..kDdRk....",
    "...kGk.kDRk.....",
    "....kGkDRk......",
    ".....kGGk.......",
    ".....khGGk......",
    "....khk.kGk.....",
    ".s.khk...kk.....",
    "sSskk...........",
    ".s..............",
], {"D": "#2b2230", "d": "#4a3a55", "R": "#ff3b4a", "G": "#f2c94c", "h": "#1a1416", "S": "#fffbe0",
    "s": "#ffd84a", "k": "#0a070c"})

item("infernal_pickaxe", [
    "................",
    ".....kkkkk......",
    "....kHHHHHkk....",
    "...kHhhhhhHHk...",
    "..kHhkkkkkhhHk..",
    "..kHk....kShhk..",
    "...k....kSkkHk..",
    ".......kSk..kHk.",
    "......kSk....k..",
    ".....kSk........",
    "....kSk.........",
    "...kSk..........",
    "..kSk...........",
    ".kSk............",
    ".kk.............",
    "................",
], {"H": "#3a1c14", "h": "#ff5a1f", "S": "#5a3a2a", "k": "#0d0607"})

item("cloud_in_a_bottle", [
    "................",
    "......kkkk......",
    "......kCCk......",
    "......kccK......",
    ".....kkGGkk.....",
    "....kGggggGk....",
    "...kGgWWWggGk...",
    "...kGWWWWWgGk...",
    "...kGWwWWWWGk...",
    "...kGgwwWwgGk...",
    "...kGggggggGk...",
    "....kGGGGGGk....",
    ".....kkkkkk.....",
    "................",
    "................",
    "................",
], {"C": "#c99a62", "c": "#8a6038", "K": "#5a3a1a", "G": "#bfe0ff", "g": "#e6f3ff", "W": "#ffffff",
    "w": "#d6dcf0", "k": "#3a4a6a"})

item("book_of_deeds", [
    "................",
    "..kkkkkkkkkkkk..",
    "..kWWWWWkRRRRk..",
    "..kWGGGWkRrrRk..",
    "..kWGWGWkRrRRk..",
    "..kWGGGWkRRRRk..",
    "..kWWWWYYYRRRk..",
    "..kWWWWYsYRRRk..",
    "..kWWWWYYYRRRk..",
    "..kWWWWWkRRRRk..",
    "..kWWWWWkRRRRk..",
    "..kPPPPPPPPPPk..",
    "..kpppppppppPk..",
    "..kkkkkkkkkkkk..",
    "................",
    "................",
], {"W": "#f6f2e6", "G": "#e8c25a", "R": "#4a0d10", "r": "#e0401a", "Y": "#f2c94c", "s": "#fff6c8",
    "P": "#efe6cf", "p": "#d6c9a6", "k": "#2a1a14"})

item("palace_key", [
    "................",
    "..........kkk...",
    ".........kGGGk..",
    "........kGRRGk..",
    "........kGRrGk..",
    ".........kGGk...",
    "........kGk.....",
    ".......kGk......",
    "......kGk.......",
    ".....kGk........",
    "....kGkk........",
    "...kGGGk........",
    "...kGk..........",
    "..kGGk..........",
    "..kkk...........",
    "................",
], {"G": "#f2c94c", "R": "#e01a2a", "r": "#ff7a7a", "k": "#2a1a06"})


def swatch(colors, seed, rows_bright=(6, 7), cracks=None, gems=None):
    f = value_noise(16, 2, seed, octaves=2)
    img = quantize(f, colors, cut=[0.3, 0.6])
    for x in range(16):
        for y in rows_bright:
            img[y, x] = hexc(colors[-1])
    if cracks:
        r = rng(seed + 1)
        for _ in range(4):
            x, y = int(r.integers(0, 16)), int(r.integers(0, 16))
            for i in range(4):
                put(img, (x + i) % 16, (y + (i % 2)) % 16, cracks)
    if gems:
        for x in range(1, 16, 5):
            put(img, x, 7, gems)
            put(img, x, 6, gems)
    return img


def build():
    out = {}
    for name, (rows, pal) in ITEMS.items():
        img = grid(rows, pal)
        save(img, f"item/{name}.png")
        out[name] = img
    out["halo_worn"] = swatch(["#e0a82e", "#ffd84a", "#fff6c8"], 81)
    out["infernal_crown_worn"] = swatch(["#c9952e", "#f2c94c", "#ffe08a"], 82, gems="#e01a2a")
    for y in (2, 3, 12, 13):
        out["infernal_crown_worn"][y, :] = hexc("#1a1416")
    out["fallen_halo_worn"] = swatch(["#4a3818", "#7a5f28", "#9a7a32"], 83, cracks="#e0401a")
    for n in ("halo_worn", "infernal_crown_worn", "fallen_halo_worn"):
        save(out[n], f"item/{n}.png")
    return out
