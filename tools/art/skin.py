"""The Lifeguard's skin (64x64 player layout): very tall (the scale is set in code), a blank face, short dark hair,
a faded red lifeguard swimsuit and a silver whistle on a white cord. No red-cross emblem."""
import numpy as np

from px import hexc, mix, new, rng, save, value_noise

SKIN = "#e7d3c3"
SKIN_SHADE = "#d6bfae"
SKIN_DARK = "#c9b09d"
HAIR = "#4a3328"
HAIR_LIGHT = "#5d4234"
SUIT = "#c45f57"
SUIT_LIGHT = "#d9837b"
SUIT_DARK = "#a44c46"
CORD = "#f2f2ee"
WHISTLE = "#c9d3d7"


def fill(img, x0, y0, x1, y1, color, noise=None, amount=0.0, other=None):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if noise is not None and other is not None:
                img[y, x] = mix(color, other, noise[y % noise.shape[0], x % noise.shape[1]] * amount)
            else:
                img[y, x] = hexc(color)


def lifeguard():
    img = new(64, 64)
    worn = value_noise(64, 16, 5)
    # ---------------- head (8x8 faces)
    for (x0, y0) in ((0, 8), (8, 8), (16, 8), (24, 8)):       # right, front, left, back
        fill(img, x0, y0, x0 + 8, y0 + 8, SKIN)
    fill(img, 8, 0, 16, 8, HAIR)                                # top
    fill(img, 16, 0, 24, 8, SKIN_SHADE)                         # bottom (chin)
    # hair: back of the head and the upper sides, a short fringe on the front
    fill(img, 24, 8, 32, 13, HAIR)
    fill(img, 0, 8, 8, 11, HAIR)
    fill(img, 16, 8, 24, 11, HAIR)
    fill(img, 8, 8, 16, 9, HAIR)
    for x in (9, 11, 13):
        img[8, x] = hexc(HAIR_LIGHT)
    # the blank face: no eyes and no mouth, just soft shading
    for x in range(9, 15):
        img[12, x] = mix(SKIN, SKIN_SHADE, 0.35)
    img[13, 11] = hexc(SKIN_SHADE)
    img[13, 12] = hexc(SKIN_SHADE)
    for x in range(8, 16):
        img[15, x] = mix(SKIN, SKIN_SHADE, 0.6)
    # ---------------- body: faded red swimsuit (tank style), cord and whistle
    fill(img, 20, 16, 28, 20, SUIT)                             # top (shoulders)
    fill(img, 28, 16, 36, 20, SUIT_DARK)                        # bottom
    for (x0, x1) in ((16, 20), (20, 28), (28, 32), (32, 40)):   # right, front, left, back
        for y in range(20, 32):
            for x in range(x0, x1):
                img[y, x] = mix(SUIT, SUIT_LIGHT, worn[y, x] * 0.7)
    # neckline and arm holes show skin
    for x in (22, 23, 24, 25):
        img[20, x] = hexc(SKIN)
    img[21, 23] = hexc(SKIN)
    img[21, 24] = hexc(SKIN)
    for x in (34, 35, 36, 37):
        img[20, x] = hexc(SKIN)
    img[20, 16] = hexc(SKIN)
    img[20, 19] = hexc(SKIN)
    img[20, 28] = hexc(SKIN)
    img[20, 31] = hexc(SKIN)
    # white cord from the neck to the whistle
    img[21, 22] = hexc(CORD)
    img[22, 22] = hexc(CORD)
    img[21, 25] = hexc(CORD)
    img[22, 25] = hexc(CORD)
    img[23, 23] = hexc(CORD)
    img[23, 24] = hexc(CORD)
    img[24, 23] = hexc(WHISTLE)
    img[24, 24] = hexc(WHISTLE)
    img[25, 23] = hexc("#9aa7ad")
    img[25, 24] = hexc("#eef3f5")
    # a little seam at the waist
    for x in range(16, 40):
        img[30, x] = mix(img[30, x], SUIT_DARK, 0.5)
    # ---------------- arms (bare skin), right arm at 40..56 x 16..32, left arm at 32..48 x 48..64
    for (bx, by) in ((40, 16), (32, 48)):
        fill(img, bx + 4, by, bx + 8, by + 4, SKIN)             # top
        fill(img, bx + 8, by, bx + 12, by + 4, SKIN_SHADE)      # bottom (hand)
        for (x0, x1) in ((0, 4), (4, 8), (8, 12), (12, 16)):
            for y in range(by + 4, by + 16):
                for x in range(bx + x0, bx + x1):
                    img[y, x] = mix(SKIN, SKIN_SHADE, 0.25 if (x - bx) in (x0, x1 - 1) else 0.0)
        for x in range(bx, bx + 16):
            img[by + 15, x] = hexc(SKIN_DARK)                   # hands a touch darker
    # ---------------- legs: swimsuit shorts then bare legs, pale sandals
    for (bx, by) in ((0, 16), (16, 48)):
        fill(img, bx + 4, by, bx + 8, by + 4, SUIT)
        fill(img, bx + 8, by, bx + 12, by + 4, "#dcdcd6")
        for y in range(by + 4, by + 16):
            for x in range(bx, bx + 16):
                if y < by + 9:
                    img[y, x] = mix(SUIT, SUIT_LIGHT, worn[y, x] * 0.7)
                elif y >= by + 14:
                    img[y, x] = hexc("#dcdcd6")
                else:
                    img[y, x] = hexc(SKIN)
            if y == by + 8:
                for x in range(bx, bx + 16):
                    img[y, x] = hexc(SUIT_DARK)
    # ---------------- hat layer: a white sun visor band
    for (x0, y0) in ((32, 8), (40, 8), (48, 8), (56, 8)):
        for x in range(x0, x0 + 8):
            img[y0 + 1, x] = hexc("#f4f6f6")
            img[y0 + 2, x] = hexc("#dfe5e6")
    for x in range(40, 48):
        img[11, x] = hexc("#f4f6f6")
    return img


def main():
    save(lifeguard(), "entity/lifeguard.png")


if __name__ == "__main__":
    main()
    print("lifeguard skin written")
