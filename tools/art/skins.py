"""64x64 player-layout skins for the Angel, the Gatekeeper, the Imp and Lucifer."""
import numpy as np

from px import grid, hexc, mix, new, put, rng, save

# (x, y, w, h) of each face. Parts: head, body, rarm, larm, rleg, lleg. Layers: base / over.
FACES = {
    ("head", "base"): {"top": (8, 0, 8, 8), "bottom": (16, 0, 8, 8), "right": (0, 8, 8, 8), "front": (8, 8, 8, 8),
                       "left": (16, 8, 8, 8), "back": (24, 8, 8, 8)},
    ("head", "over"): {"top": (40, 0, 8, 8), "bottom": (48, 0, 8, 8), "right": (32, 8, 8, 8), "front": (40, 8, 8, 8),
                       "left": (48, 8, 8, 8), "back": (56, 8, 8, 8)},
    ("body", "base"): {"top": (20, 16, 8, 4), "bottom": (28, 16, 8, 4), "right": (16, 20, 4, 12),
                       "front": (20, 20, 8, 12), "left": (28, 20, 4, 12), "back": (32, 20, 8, 12)},
    ("body", "over"): {"top": (20, 32, 8, 4), "bottom": (28, 32, 8, 4), "right": (16, 36, 4, 12),
                       "front": (20, 36, 8, 12), "left": (28, 36, 4, 12), "back": (32, 36, 8, 12)},
    ("rarm", "base"): {"top": (44, 16, 4, 4), "bottom": (48, 16, 4, 4), "right": (40, 20, 4, 12),
                       "front": (44, 20, 4, 12), "left": (48, 20, 4, 12), "back": (52, 20, 4, 12)},
    ("rarm", "over"): {"top": (44, 32, 4, 4), "bottom": (48, 32, 4, 4), "right": (40, 36, 4, 12),
                       "front": (44, 36, 4, 12), "left": (48, 36, 4, 12), "back": (52, 36, 4, 12)},
    ("larm", "base"): {"top": (36, 48, 4, 4), "bottom": (40, 48, 4, 4), "right": (32, 52, 4, 12),
                       "front": (36, 52, 4, 12), "left": (40, 52, 4, 12), "back": (44, 52, 4, 12)},
    ("larm", "over"): {"top": (52, 48, 4, 4), "bottom": (56, 48, 4, 4), "right": (48, 52, 4, 12),
                       "front": (52, 52, 4, 12), "left": (56, 52, 4, 12), "back": (60, 52, 4, 12)},
    ("rleg", "base"): {"top": (4, 16, 4, 4), "bottom": (8, 16, 4, 4), "right": (0, 20, 4, 12),
                       "front": (4, 20, 4, 12), "left": (8, 20, 4, 12), "back": (12, 20, 4, 12)},
    ("rleg", "over"): {"top": (4, 32, 4, 4), "bottom": (8, 32, 4, 4), "right": (0, 36, 4, 12),
                       "front": (4, 36, 4, 12), "left": (8, 36, 4, 12), "back": (12, 36, 4, 12)},
    ("lleg", "base"): {"top": (20, 48, 4, 4), "bottom": (24, 48, 4, 4), "right": (16, 52, 4, 12),
                       "front": (20, 52, 4, 12), "left": (24, 52, 4, 12), "back": (28, 52, 4, 12)},
    ("lleg", "over"): {"top": (4, 48, 4, 4), "bottom": (8, 48, 4, 4), "right": (0, 52, 4, 12),
                       "front": (4, 52, 4, 12), "left": (8, 52, 4, 12), "back": (12, 52, 4, 12)},
}
SIDES = ("right", "front", "left", "back")


class Skin:
    def __init__(self, seed):
        self.img = new(64)
        self.r = rng(seed)

    def face(self, part, layer, face):
        return FACES[(part, layer)][face]

    def fill(self, part, layer, faces, color, noise=None, noise_amt=0.06):
        for f in faces:
            x, y, w, h = self.face(part, layer, f)
            for yy in range(h):
                for xx in range(w):
                    c = color(xx, yy, w, h, f) if callable(color) else color
                    if c is None:
                        continue
                    c = hexc(c)
                    if noise and self.r.random() < noise:
                        c = mix(c, "#000000" if self.r.random() < 0.6 else "#ffffff", noise_amt)
                        c = hexc(c)
                    self.img[y + yy, x + xx] = c

    def paint(self, part, layer, face, rows, pal):
        x, y, w, h = self.face(part, layer, face)
        grid(rows, pal, self.img, ox=x, oy=y)

    def rows(self, part, layer, faces, row_colors):
        """row_colors: list (top to bottom) of colours or None, applied to every pixel of that row."""
        def col(xx, yy, w, h, f):
            return row_colors[min(yy, len(row_colors) - 1)]
        self.fill(part, layer, faces, col)

    def save(self, name):
        save(self.img, f"entity/{name}.png")
        return self.img


# ------------------------------------------------------------------ helpers
def robe_body(s, base, shade, trim, inner=None):
    s.fill("body", "base", ["top", "bottom"], base)
    s.fill("body", "base", SIDES, lambda x, y, w, h, f: shade if x in (0, w - 1) and f in ("front", "back") else base,
           noise=0.15, noise_amt=0.05)


def arm(s, side, sleeve, shade, cuff, hand, hand_rows=3, cuff_row=8):
    part = "rarm" if side == "r" else "larm"
    def col(x, y, w, h, f):
        if y >= h - hand_rows:
            return hand
        if y == cuff_row:
            return cuff
        if f == "back" or (f == "left" and side == "r") or (f == "right" and side == "l"):
            return shade
        return sleeve
    s.fill(part, "base", SIDES, col, noise=0.12, noise_amt=0.05)
    s.fill(part, "base", ["top"], sleeve)
    s.fill(part, "base", ["bottom"], hand)


def leg(s, side, cloth, shade, foot, foot_rows=2, hem=None, hem_row=None):
    part = "rleg" if side == "r" else "lleg"
    def col(x, y, w, h, f):
        if y >= h - foot_rows:
            return foot
        if hem is not None and y == hem_row:
            return hem
        if f == "back":
            return shade
        return cloth
    s.fill(part, "base", SIDES, col, noise=0.12, noise_amt=0.05)
    s.fill(part, "base", ["top"], cloth)
    s.fill(part, "base", ["bottom"], foot)


def head_base(s, skin, hair, hair_shade, side_hair_rows=3, back_hair_rows=8):
    s.fill("head", "base", ["top"], hair)
    s.fill("head", "base", ["bottom"], skin)
    s.fill("head", "base", ["right", "left"], lambda x, y, w, h, f: (hair if y < side_hair_rows else skin))
    s.fill("head", "base", ["back"], lambda x, y, w, h, f: (hair_shade if y < back_hair_rows else skin), noise=0.2)


# ------------------------------------------------------------------ angel
def angel():
    s = Skin(101)
    skin, skin_d = "#f2d4bc", "#dcb89c"
    hair, hair_d, hair_l = "#f2d58a", "#d4b05a", "#fff0b8"
    robe, robe_d, gold, gold_d = "#f8f8fb", "#dfe2ee", "#f2c94c", "#c9952e"
    head_base(s, skin, hair, hair_d, side_hair_rows=4)
    s.paint("head", "base", "front", [
        "hhhHhhhh",
        "hHhhhhHh",
        "hsssssss",
        "ssssssss",
        "sWBssBWs",
        "sssddsss",
        "sssmmsss",
        "ssssssss",
    ], {"h": hair, "H": hair_l, "s": skin, "W": "#ffffff", "B": "#5a9ee8", "d": skin_d, "m": "#e8a0a0"})
    # hat layer: soft curls
    s.fill("head", "over", ["top"], lambda x, y, w, h, f: hair_l if (x + y) % 3 == 0 else hair)
    s.fill("head", "over", ["front"], lambda x, y, w, h, f: (hair_l if (x % 3 == 0) else hair) if y == 0 else None)
    s.fill("head", "over", ["right", "left"], lambda x, y, w, h, f: (hair if y < 5 and (x + y) % 4 != 0 else None))
    s.fill("head", "over", ["back"], lambda x, y, w, h, f: hair if y < 8 else None, noise=0.25, noise_amt=0.08)
    robe_body(s, robe, robe_d, gold)
    s.paint("body", "base", "front", [
        "wwwssww.",
        "wwwssww.",
        "Gwwwwww.",
        "wGwwyww.",
        "wwGwyww.",
        "wwwGyww.",
        "wwwyGww.",
        "wwwywGw.",
        "wwwywwG.",
        "wwwywww.",
        "wwwywww.",
        "wwwywww.",
    ], {"w": robe, "s": skin, "G": gold, "y": gold_d})
    for side in ("r", "l"):
        arm(s, side, robe, robe_d, gold, skin)
        leg(s, side, robe, robe_d, skin, foot_rows=1, hem=gold, hem_row=9)
    # sandal straps
    for part in ("rleg", "lleg"):
        x, y, w, h = s.face(part, "base", "front")
        for xx in range(w):
            put(s.img, x + xx, y + 11, "#8a5a2a" if xx % 2 == 0 else skin)
    # jacket overlay: gold collar trim
    s.fill("body", "over", ["front"], lambda x, y, w, h, f: gold if y == 0 and x not in (3, 4) else None)
    return s.save("angel")


# ------------------------------------------------------------------ gatekeeper
def gatekeeper():
    s = Skin(202)
    skin, skin_d = "#e8c6a8", "#c9a284"
    hair, hair_d = "#f4f4f8", "#d0d0dc"
    robe, robe_d, gold, blue = "#fbfaf6", "#e2dfd6", "#f2c94c", "#2a4a9a"
    head_base(s, skin, hair, hair_d, side_hair_rows=6)
    s.paint("head", "base", "front", [
        "hhhhhhhh",
        "hhhhhhhh",
        "hbbssbbh",
        "ssssssss",
        "sWBssBWs",
        "hssddssh",
        "hhhhhhhh",
        "hhhhhhhh",
    ], {"h": hair, "b": "#ffffff", "s": skin, "W": "#ffffff", "B": "#3a6ab0", "d": skin_d})
    # long beard on the hat layer (front lower half) and long hair
    s.fill("head", "over", ["front"], lambda x, y, w, h, f: hair if y >= 6 or (y == 5 and x in (0, 1, 6, 7)) else None)
    s.fill("head", "over", ["right", "left", "back"], lambda x, y, w, h, f: hair_d if (x + y) % 5 == 0 else hair)
    s.fill("head", "over", ["top"], hair)
    robe_body(s, robe, robe_d, gold)
    s.paint("body", "base", "front", [
        "Bhhhhhh.",
        "Bhhhhhh.",
        "BBhhhhB.",
        "BBwwwBB.",
        "BBwGwBB.",
        "BBGGGBB.",
        "BBwGwBB.",
        "BBwGwBB.",
        "BBwwwBB.",
        "BBwwwBB.",
        "GGGGGGGG",
        "BBwwwBB.",
    ], {"B": blue, "w": robe, "G": gold, "h": hair})
    # the beard continues onto the chest (first rows of body front, base layer above)
    for side in ("r", "l"):
        arm(s, side, robe, robe_d, gold, skin)
        leg(s, side, robe, robe_d, "#8a6a4a", foot_rows=1, hem=gold, hem_row=10)
    s.fill("body", "over", ["front", "right", "left", "back"],
           lambda x, y, w, h, f: blue if (f == "back" and x in (0, w - 1)) else None)
    return s.save("gatekeeper")


# ------------------------------------------------------------------ imp
def imp():
    s = Skin(303)
    red, red_d, red_l = "#c8321a", "#8a1f1f", "#e0502a"
    horn, horn_l = "#1a1416", "#4a3a3e"
    cloth = "#1a1416"
    s.fill("head", "base", ["top", "bottom", "right", "left", "back"],
           lambda x, y, w, h, f: red_d if (x + y) % 5 == 0 else red)
    s.paint("head", "base", "front", [
        "rrrrrrrr",
        "rRrrrrRr",
        "rddrrddr",
        "rYKrrKYr",
        "rrrrrrrr",
        "rrrddrrr",
        "rkWkWkWr",
        "rrkkkkrr",
    ], {"r": red, "R": red_l, "d": red_d, "Y": "#ffd84a", "K": "#1a0a08", "k": "#2a0a08", "W": "#ffffff"})
    # horns on the hat layer: two curved horns from the top corners
    s.fill("head", "over", ["top"], lambda x, y, w, h, f: horn if (x in (0, 1, 6, 7) and y in (2, 3, 4)) else None)
    s.fill("head", "over", ["front"], lambda x, y, w, h, f: horn_l if (y == 0 and x in (1, 6)) else None)
    s.fill("head", "over", ["right", "left"], lambda x, y, w, h, f: horn if (y < 2 and x in (3, 4)) or (y == 0 and x in (2, 5)) else None)
    s.fill("body", "base", ["top", "bottom", "front", "right", "left", "back"],
           lambda x, y, w, h, f: red_d if (f == "back" and y < 5 and x in (1, 2, 5, 6)) else red, noise=0.15)
    s.fill("body", "base", ["front"], lambda x, y, w, h, f: cloth if y >= 10 else (red_l if (y in (3, 4) and x in (2, 5)) else None))
    for side in ("r", "l"):
        arm(s, side, red, red_d, red, horn, hand_rows=1)
        leg(s, side, red, red_d, horn, foot_rows=1)
        part = "rleg" if side == "r" else "lleg"
        s.fill(part, "base", SIDES, lambda x, y, w, h, f: cloth if y < 3 else None)
    return s.save("imp")


# ------------------------------------------------------------------ lucifer
def lucifer():
    s = Skin(404)
    skin, skin_d, skin_l = "#8a8a92", "#6a6a74", "#a6a6ae"
    crack = "#d0301a"
    hair, hair_l = "#141012", "#2e2830"
    coat, coat_d, coat_l = "#141012", "#0a0809", "#262026"
    crimson, gold, gold_d = "#8a1020", "#f2c94c", "#b8860b"
    trousers, boot, boot_l = "#1e1a20", "#0a0809", "#3a3438"
    head_base(s, skin, hair, hair, side_hair_rows=3, back_hair_rows=6)
    s.paint("head", "base", "front", [
        "hhhHhhhh",
        "hhhhhhhh",
        "ssssssss",
        "sbbssbbs",
        "sOYssYOs",
        "csssdssc",
        "sssmmsss",
        "ssdssdss",
    ], {"h": hair, "H": hair_l, "s": skin, "b": "#2a2428", "O": "#ffb02a", "Y": "#fff0a0", "d": skin_d,
        "m": "#5a3a40", "c": crack})
    # slicked-back hair volume + horns (hat layer)
    s.fill("head", "over", ["top"], lambda x, y, w, h, f: hair_l if x % 3 == 1 else hair)
    s.fill("head", "over", ["back"], lambda x, y, w, h, f: hair if y < 5 else None)
    s.fill("head", "over", ["right", "left"], lambda x, y, w, h, f: hair if y < 2 else (
        "#1a1416" if (y in (2, 3) and x in (5, 6)) else None))
    s.fill("head", "over", ["front"], lambda x, y, w, h, f: (gold if x in (0, 7) and y == 0 else ("#1a1416" if x in (0, 7) and y == 1 else None)))
    s.fill("head", "over", ["top"], lambda x, y, w, h, f: ("#1a1416" if (x in (0, 7) and y in (5, 6)) else (gold if (x in (0, 7) and y == 4) else None)))
    # coat
    s.fill("body", "base", ["top", "bottom", "right", "left", "back"], coat, noise=0.15, noise_amt=0.08)
    s.paint("body", "base", "front", [
        "kcCssCck",
        "kcCssCck",
        "kcCLLCck",
        "kcCLLCck",
        "kcgLLgck",
        "kcCLLCck",
        "kcgLLgck",
        "kcCLLCck",
        "GGGGGGGG",
        "kcCLLCck",
        "kcCkkCck",
        "kgCkkCgk",
    ], {"k": coat, "c": coat_l, "C": crimson, "s": skin, "L": "#2a0a10", "g": gold, "G": gold_d})
    # high collar + gold-trimmed coat edges on the jacket layer
    s.fill("body", "over", ["front"], lambda x, y, w, h, f: (coat_l if y == 0 and x in (0, 1, 6, 7) else (
        gold if (y > 0 and x in (2, 5) and y % 3 == 0) else None)))
    s.fill("body", "over", ["right", "left", "back"], lambda x, y, w, h, f: (coat_l if y == 0 else (
        coat if (f == "back" and y >= 9) else None)))
    for side in ("r", "l"):
        arm(s, side, coat, coat_d, gold, "#141012", hand_rows=3, cuff_row=8)
        part = "rarm" if side == "r" else "larm"
        x, y, w, h = s.face(part, "base", "front")
        put(s.img, x + 1, y + 10, gold)  # ring
        leg(s, side, trousers, coat_d, boot, foot_rows=4)
        x, y, w, h = s.face("rleg" if side == "r" else "lleg", "base", "front")
        put(s.img, x + 1, y + 8, boot_l)
    # coat tails over the legs (pants layer)
    for part in ("rleg", "lleg"):
        s.fill(part, "over", ["right", "left", "back", "front"], lambda x, y, w, h, f: (coat if y < 5 and f != "front" else (
            crimson if (y < 5 and f == "front" and x in (0, 3)) else None)))
    return s.save("lucifer")


def assemble(img, scale=6):
    """Front and back views of the model for the contact sheet."""
    def face(part, layer, f):
        x, y, w, h = FACES[(part, layer)][f]
        return img[y:y + h, x:x + w]
    def view(side):
        canvas = new(16, 32, (0, 0, 0, 0))
        f = "front" if side == "front" else "back"
        def blit(part, ox, oy, flip=False):
            for layer in ("base", "over"):
                t = face(part, layer, f)
                if flip:
                    t = t[:, ::-1]
                from px import over as ov
                ov(canvas, t, ox, oy)
        blit("head", 4, 0)
        blit("body", 4, 8)
        if side == "front":
            blit("rarm", 0, 8)
            blit("larm", 12, 8)
            blit("rleg", 4, 20)
            blit("lleg", 8, 20)
        else:
            blit("larm", 0, 8)
            blit("rarm", 12, 8)
            blit("lleg", 4, 20)
            blit("rleg", 8, 20)
        return canvas
    return view("front"), view("back")


def build():
    out = {}
    for name, fn in (("angel", angel), ("gatekeeper", gatekeeper), ("imp", imp), ("lucifer", lucifer)):
        out[name] = fn()
    return out
