"""Item textures, the worn snorkel atlas, the flippers equipment layer, the fade glyph and the icon."""
import math

import numpy as np
from PIL import Image

from px import TEX, grid, hexc, mix, new, outline, save
import blocks


def snorkel_icon():
    rows = [
        "................",
        "..............Y.",
        ".............YYy",
        ".............Yy.",
        "..KKKKKKKKKK.Yy.",
        ".KAAAAAAAAAAKYy.",
        ".KALLLLLLLLAKYy.",
        "SKALWLLLLLLAKYyS",
        "SKALWLLLLLLAKYyS",
        ".KALLLLLLLLAKYy.",
        ".KAAAAAAAAAAKYy.",
        "..KKKAAAAKKK.Yy.",
        ".....KAAK...YYy.",
        "......KK..MYYy..",
        "..........MMy...",
        "................",
    ]
    pal = {"K": "#1d2a2e", "A": "#2fb7c4", "L": "#9fe3f0", "W": "#ffffff", "Y": "#ffcf3a", "y": "#d9a514",
           "S": "#262e31", "M": "#3a4448"}
    return grid(rows, pal)


def snorkel_atlas():
    """Colour swatches used by the 3D snorkel model (see resources.py for the UVs)."""
    img = new(16, 16)
    img[0:4, 0:4] = hexc("#1d2326")    # strap
    img[0:4, 4:8] = hexc("#9fe3f0")    # lens
    img[1, 5] = hexc("#ffffff")
    img[0:4, 8:12] = hexc("#2fb7c4")   # mask frame
    img[0:4, 12:16] = hexc("#ffcf3a")  # tube
    img[3, 12:16] = hexc("#d9a514")
    img[4:8, 0:4] = hexc("#3a4448")    # mouthpiece
    return img


def flippers_icon():
    rows = [
        "................",
        "....DD....DD....",
        "...DAAD..DAAD...",
        "...DAAD..DAAD...",
        "...DAAD..DAAD...",
        "..DAALAD.DAALAD.",
        "..DAALAD.DAALAD.",
        ".DAAALAADAAALAAD",
        ".DAAALAADAAALAAD",
        ".DAAALAADAAALAAD",
        "DAAAALAAAAAALAAD",
        "DAAAALAAAAAALAAD",
        "DLAALAAL.LAALAAL",
        "DL.LL.L...LL.L.L",
        "................",
        "................",
    ]
    pal = {"D": "#126b78", "A": "#2fc6d3", "L": "#7fe6ee"}
    return grid(rows, pal)


def noodle_icon():
    img = new(16, 16)
    for i in range(14):
        x, y = 1 + i, 14 - i
        for dx, dy, c in ((0, 0, "#ff6fb0"), (1, 0, "#ff8cc2"), (0, -1, "#ff8cc2"), (1, 1, "#d84a8e"), (-1, 0, "#d84a8e")):
            xx, yy = x + dx, y + dy
            if 0 <= xx < 16 and 0 <= yy < 16:
                img[yy, xx] = hexc(c)
        if i % 3 == 0:
            if 0 <= y < 16 and 0 <= x < 16:
                img[y, x] = hexc("#e05b9b")
    img[1, 14] = hexc("#7a1f4a")
    img[0, 14] = hexc("#ff8cc2")
    img[1, 15] = hexc("#ff8cc2")
    return outline(img, "#5a1636")


def compass_frame(k):
    """Frame k of 32: the needle points (k - 16)/32 of a turn clockwise from straight up."""
    img = new(16, 16)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 7.4:
                img[y, x] = hexc("#1f8f9a")
            if d <= 6.4:
                img[y, x] = hexc("#e9fbfc")
            if 6.4 < d <= 7.4 and y < 7:
                img[y, x] = hexc("#2fb7c4")
    for a in range(0, 360, 90):
        rad = math.radians(a)
        x = int(round(cx + math.sin(rad) * 5.6))
        y = int(round(cy - math.cos(rad) * 5.6))
        img[y, x] = hexc("#7fb8be")
    ang = (k - 16) / 32 * 2 * math.pi
    sx, sy = math.sin(ang), -math.cos(ang)
    for t10 in range(0, 52):
        t = t10 / 10
        x = cx + sx * t
        y = cy + sy * t
        img[int(round(y)), int(round(x))] = hexc("#e8483f")
    for t10 in range(0, 40):
        t = t10 / 10
        x = cx - sx * t
        y = cy - sy * t
        img[int(round(y)), int(round(x))] = hexc("#9aa7ad")
    img[7:9, 7:9] = hexc("#3a4448")
    return img


def wristband_icon(color, dark):
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) / 6.6, (y - 8.5) / 4.6)
            if 0.74 <= d <= 1.0:
                img[y, x] = mix(color, dark, 0.35 if y > 8 else 0.0)
    img[3:6, 3:6] = hexc("#e9eef0")
    img[4, 4] = hexc("#a9b4b8")
    img[5, 9] = mix(color, "#ffffff", 0.5)
    return outline(img, "#1b2226")


def flippers_layer():
    """64x32 humanoid armour layer: aqua rubber flippers over the feet."""
    img = new(64, 32)
    rubber = "#2fc6d3"
    dark = "#168a95"
    for (x0, x1) in ((0, 4), (4, 8), (8, 12), (12, 16)):
        for y in range(27, 32):
            for x in range(x0, x1):
                img[y, x] = hexc(dark if y == 27 or x in (x0, x1 - 1) and y > 29 else rubber)
    img[16:20, 8:12] = hexc(dark)    # sole
    img[16:20, 4:8] = hexc(rubber)   # top of the foot (hidden by the leg)
    return img


def exit_sign_icon():
    """A tiny EXIT sign for the inventory."""
    img = new(16, 16)
    img[4:12, 0:16] = hexc("#1c2b22")
    img[5:11, 1:15] = hexc("#0b2416")
    font = {"E": ["###", "#..", "##.", "#..", "###"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
            "I": ["#", "#", "#", "#", "#"], "T": ["###", ".#.", ".#.", ".#.", ".#."]}
    x = 2
    for ch in "EXIT":
        rows = font[ch]
        for yy, row in enumerate(rows):
            for xx, c in enumerate(row):
                if c == "#":
                    img[6 + yy - 0, x + xx] = hexc("#5dff9a")
        x += len(rows[0]) + 1
    return img


def fade_glyph():
    return np.full((256, 256, 4), 255, dtype=np.uint8)


def icon():
    """128x128 mod icon: looking down into a tiled pool at the strange drain."""
    size = 128
    img = np.zeros((size, size, 4), dtype=np.uint8)
    tile = blocks.tiles("#eef6f6", "#b9cfd2", 1, var=6)
    aqua = blocks.tiles("#5cc9d3", "#d2eef0", 2, var=8)
    big = np.kron(tile, np.ones((2, 2, 1), dtype=np.uint8))
    bigaqua = np.kron(aqua, np.ones((2, 2, 1), dtype=np.uint8))
    for y in range(0, size, 32):
        for x in range(0, size, 32):
            img[y:y + 32, x:x + 32] = big
    img[16:112, 16:112] = np.tile(bigaqua, (3, 3, 1))[:96, :96]
    # water over the pool
    for y in range(20, 108):
        for x in range(20, 108):
            w = 0.5 + 0.5 * math.sin(x / 7.0 + y / 11.0) * math.sin(y / 9.0 - x / 13.0)
            c = mix("#38c4d6", "#9ff0f6", w * 0.6)
            base = img[y, x].astype(float)
            img[y, x, :3] = (base[:3] * 0.25 + np.array(c[:3]) * 0.75).astype(np.uint8)
    drain = blocks.strange_drain()
    big_drain = np.kron(drain, np.ones((3, 3, 1), dtype=np.uint8))
    img[40:88, 40:88] = big_drain
    for (bx, by, r) in ((60, 34, 4), (70, 26, 3), (56, 20, 2), (80, 38, 2)):
        for y in range(by - r - 1, by + r + 2):
            for x in range(bx - r - 1, bx + r + 2):
                d = math.hypot(x - bx, y - by)
                if r - 0.8 <= d <= r + 0.3:
                    img[y, x] = hexc("#ffffff")
    img[..., 3] = 255
    return img


def main():
    out = {
        "item/snorkel_mask": snorkel_icon(),
        "item/snorkel_mask_worn": snorkel_atlas(),
        "item/flippers": flippers_icon(),
        "item/pool_noodle": noodle_icon(),
        "item/blue_wristband": wristband_icon("#2f6fe0", "#173c86"),
        "item/yellow_wristband": wristband_icon("#ffd23f", "#b88a12"),
        "item/red_wristband": wristband_icon("#e83b3b", "#8c1c1c"),
        "item/strange_drain": blocks.strange_drain(),
        "item/exit_sign": exit_sign_icon(),
    }
    for k in range(32):
        out[f"item/chlorine_compass_{k:02d}"] = compass_frame(k)
    for name, img in out.items():
        save(img, name + ".png")
    save(flippers_layer(), "entity/equipment/humanoid/flippers.png")
    save(fade_glyph(), "font/fade.png")
    ic = icon()
    Image.fromarray(ic, "RGBA").save(TEX.replace("textures", "icon.png"))
    return out


if __name__ == "__main__":
    main()
    print("item textures written")
