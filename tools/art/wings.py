"""64x32 wing textures (vanilla elytra UV layout, our own art)."""
import numpy as np
from PIL import Image

from px import hexc, mix, new, rng, save

ELYTRA = "/home/claude/misode/mcmeta-full/assets/minecraft/textures/entity/equipment/wings/elytra.png"


def mask():
    a = np.array(Image.open(ELYTRA).convert("RGBA"))[..., 3] > 0
    return a


def angel_wings():
    m = mask()
    img = new(64, 32)
    r = rng(7)
    ys, xs = np.nonzero(m)
    y_min, y_max = ys.min(), ys.max()
    for y, x in zip(ys, xs):
        t = (y - y_min) / max(1, (y_max - y_min))  # 0 at shoulder, 1 at wing tip
        # feather rows: every 3 px a darker edge line, staggered per column
        stripe = (y + (x % 2)) % 3 == 0
        base = mix("#ffffff", "#e6e9f6", 0.25 + 0.5 * ((x % 4) / 3))
        if t > 0.72:
            base = mix(base, "#f2c94c", min(1.0, (t - 0.72) / 0.25))
        if stripe:
            base = mix(base, "#c4cbe6" if t < 0.72 else "#c9952e", 0.6)
        if r.random() < 0.06:
            base = mix(base, "#ffffff", 0.7)
        img[y, x] = hexc(base)
    # leading edge (outer column of each row) slightly darker for definition
    for y in range(32):
        row = np.nonzero(m[y])[0]
        if len(row):
            img[y, row.max()] = hexc(mix(tuple(img[y, row.max()]), "#9aa3c8", 0.5))
    return img


def demon_wings():
    m = mask()
    img = new(64, 32)
    ys, xs = np.nonzero(m)
    y_min, y_max = ys.min(), ys.max()
    for y, x in zip(ys, xs):
        t = (y - y_min) / max(1, (y_max - y_min))
        base = mix("#5a0f12", "#8a1f1f", 0.5 + 0.5 * np.sin(x * 0.9 + y * 0.3))
        # bony ribs: diagonal lines fanning down
        if (x * 3 - y) % 9 == 0 or (x * 3 - y) % 9 == 1:
            base = "#1a0f10"
        elif (x * 3 - y) % 9 == 2:
            base = mix("#2a1416", "#5a2a20", 0.5)
        if t > 0.85:
            base = mix(base, "#ff7a2a", 0.5)
        img[y, x] = hexc(base)
    for y in range(32):
        row = np.nonzero(m[y])[0]
        if len(row):
            img[y, row.max()] = hexc("#e0502a")
            img[y, row.min()] = hexc("#1a0f10")
    return img


def build():
    out = {"angel_wings": angel_wings(), "demon_wings": demon_wings()}
    for n, img in out.items():
        save(img, f"entity/equipment/wings/{n}.png")
    return out
