"""128x128 mod icon: heaven (top-left) / hell (bottom-right)."""
import numpy as np
from PIL import Image, ImageDraw

from px import hexc, mix, new, save, value_noise

N = 128


def build():
    img = new(N)
    yy, xx = np.mgrid[0:N, 0:N]
    heaven = (xx + yy) < N
    for y in range(N):
        for x in range(N):
            if heaven[y, x]:
                t = (x + y) / N
                img[y, x] = hexc(mix("#7fc4ff", "#fff3c8", t))
            else:
                t = ((x + y) - N) / N
                img[y, x] = hexc(mix("#ff6a1a", "#2a0404", t))
    clouds = value_noise(N, 4, 3, octaves=3)
    fire = value_noise(N, 6, 4, octaves=2)
    for y in range(N):
        for x in range(N):
            if heaven[y, x] and clouds[y, x] > 0.62 and y > 40:
                img[y, x] = hexc("#ffffff")
            if not heaven[y, x] and fire[y, x] > 0.6 and y > 70:
                img[y, x] = hexc("#ffb02a" if fire[y, x] > 0.72 else "#ff5a1f")
    im = Image.fromarray(img, "RGBA")
    d = ImageDraw.Draw(im)
    # halo (top-left)
    d.ellipse([18, 18, 70, 40], outline=hexc("#f2b42a"), width=6)
    d.ellipse([20, 20, 68, 38], outline=hexc("#ffe88a"), width=2)
    # horn (bottom-right)
    outer = [(64, 124), (70, 106), (80, 90), (92, 76), (104, 64), (116, 50)]
    inner = [(116, 50), (106, 70), (96, 86), (88, 102), (84, 116), (86, 124)]
    d.polygon(outer + inner, fill=hexc("#140e10"))
    d.line(outer[1:4], fill=hexc("#3a2e32"), width=2)
    d.polygon([(108, 60), (116, 50), (112, 64)], fill=hexc("#f2c94c"))
    for (x, y) in ((86, 14), (100, 30), (14, 70), (40, 56)):
        if x + y < 120:
            d.line([(x - 3, y), (x + 3, y)], fill=hexc("#ffffff"))
            d.line([(x, y - 3), (x, y + 3)], fill=hexc("#ffffff"))
    # dividing line
    d.line([(0, N - 1), (N - 1, 0)], fill=hexc("#fff6d8"), width=3)
    img = np.array(im)
    save(img, "../icon.png")
    return {"icon": img}
