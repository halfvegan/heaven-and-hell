"""Block textures for The Poolrooms (16x16 pixel art, generated)."""
import math

import numpy as np

from px import hexc, mix, new, rng, save, value_noise


def tiles(base, grout, seed, var=10, highlight=True, size=4):
    """A grid of square ceramic tiles: (size-1)px tiles with 1px grout, seamless."""
    r = rng(seed)
    img = new(16, 16, hexc(grout))
    b = np.array(hexc(base), dtype=int)
    for ty in range(16 // size):
        for tx in range(16 // size):
            d = r.integers(-var, var + 1)
            col = np.clip(b + np.array([d, d, d, 0]), 0, 255)
            for y in range(size - 1):
                for x in range(size - 1):
                    px_ = col.copy()
                    if highlight and (x == 0 or y == 0):
                        px_[:3] = np.clip(px_[:3] + 12, 0, 255)
                    if highlight and (x == size - 2 and y == size - 2):
                        px_[:3] = np.clip(px_[:3] - 10, 0, 255)
                    img[ty * size + y, tx * size + x] = px_
    return img


def mosaic(seed):
    """Little 2x2 mosaic tiles in blues, aquas and white."""
    r = rng(seed)
    palette = ["#2f8fd0", "#3fb2d9", "#5cc9d3", "#8fe0e6", "#d9f3f5", "#1f6fb0", "#6fd6dc"]
    weights = np.array([3, 4, 4, 3, 2, 1, 3], dtype=float)
    weights /= weights.sum()
    img = new(16, 16)
    for ty in range(8):
        for tx in range(8):
            c = hexc(palette[r.choice(len(palette), p=weights)])
            img[ty * 2:ty * 2 + 2, tx * 2:tx * 2 + 2] = c
            # a darker grout pixel at the bottom-right of each little tile
            img[ty * 2 + 1, tx * 2 + 1] = tuple(int(v * 0.86) for v in c[:3]) + (255,)
    return img


def ceiling_light():
    img = new(16, 16, hexc("#a9bcbf"))
    img[1:15, 1:15] = hexc("#f4fbff")
    img[2:14, 2:14] = hexc("#ffffff")
    for k in (5, 10):
        img[2:14, k] = hexc("#eaf6fb")
        img[k, 2:14] = hexc("#eaf6fb")
    img[0, :] = hexc("#c4d3d5")
    img[:, 0] = hexc("#c4d3d5")
    return img


def fake_sky():
    """Painted sky: soft blue with puffy clouds (tiles seamlessly)."""
    n = value_noise(16, 2, 41, octaves=3, persistence=0.55)
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            v = n[y, x]
            base = mix("#7cc6f2", "#9ad6f7", (math.sin((x + y) / 16 * math.pi) + 1) / 2 * 0.6)
            if v > 0.66:
                t = min(1.0, (v - 0.66) / 0.12)
                c = mix(base, "#ffffff", 0.35 + 0.65 * t)
            else:
                c = base
            img[y, x] = c
    return img


def pool_light():
    img = tiles("#5cc9d3", "#cfeaec", 77)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 5.6:
                img[y, x] = hexc("#cfe3e6")
            if d <= 4.6:
                t = d / 4.6
                img[y, x] = mix("#ffffff", "#a9f3ff", t ** 1.5)
    img[5, 6] = hexc("#ffffff")
    return img


def pool_ladder():
    img = new(16, 16)
    for x in (2, 13):
        img[:, x] = hexc("#cfd8dc")
        img[:, x + 1 if x == 2 else x - 1] = hexc("#9aa7ad")
    for y in (1, 5, 9, 13):
        img[y, 2:14] = hexc("#dfe7ea")
        img[y + 1, 3:13] = hexc("#8e9ba1")
    return img


def strange_drain():
    """A round drain cover with curved slots that look a little like a whirlpool."""
    img = new(16, 16, hexc("#5f7176"))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            a = math.atan2(y - cy, x - cx)
            if d > 7.3:
                img[y, x] = hexc("#46555a")
            elif d > 6.3:
                img[y, x] = hexc("#93a8ae")
            elif d < 1.5:
                img[y, x] = hexc("#0b1619")
            else:
                ring = d in () or (2.4 <= d < 3.4) or (4.4 <= d < 5.4)
                spoke = (math.degrees(a) + d * 9) % 90 < 16
                if ring and not spoke:
                    img[y, x] = hexc("#10201f")
                else:
                    img[y, x] = mix("#8aa0a6", "#61757a", d / 7)
    return img


def strange_drain_side():
    img = new(16, 16, hexc("#5f7176"))
    img[13:16, :] = hexc("#7c9095")
    img[14, :] = hexc("#8da1a7")
    return img


EXIT_FONT = {
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "X": ["#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"],
    "I": ["###", ".#.", ".#.", ".#.", ".#.", ".#.", "###"],
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
}


def exit_sign():
    """32x16 sign (left and right halves): bright green EXIT letters on a dark panel."""
    img = new(32, 16)
    img[3:13, 0:32] = hexc("#1c2b22")
    img[4:12, 1:31] = hexc("#0b2416")
    x = 4
    for ch in "EXIT":
        rows = EXIT_FONT[ch]
        for yy, row in enumerate(rows):
            for xx, c in enumerate(row):
                if c == "#":
                    img[4 + yy + 0, x + xx] = hexc("#5dff9a")
        x += len(rows[0]) + 2
    # glow around the letters
    glow = img.copy()
    for y in range(4, 12):
        for xx in range(1, 31):
            if tuple(img[y, xx][:3]) == (0x0b, 0x24, 0x16):
                near = any(tuple(img[y + dy, xx + dx][:3]) == (0x5d, 0xff, 0x9a)
                           for dy in (-1, 0, 1) for dx in (-1, 0, 1) if 0 <= y + dy < 16 and 0 <= xx + dx < 32)
                if near:
                    glow[y, xx] = hexc("#14552f")
    return glow[:, :16].copy(), glow[:, 16:].copy()


def exit_door():
    """32x48 double door seen from the front, cut into six 16x16 parts (bottom-left first)."""
    img = new(32, 48, hexc("#cfd9d4"))
    frame = hexc("#7d8f88")
    img[:, 0] = frame
    img[:, 31] = frame
    img[0, :] = frame
    img[:, 15:17] = hexc("#8fa199")
    for leaf in (0, 16):
        img[2:46, leaf + 2:leaf + 14] = hexc("#dbe6e0")
        img[2, leaf + 2:leaf + 14] = hexc("#eef5f1")
        # little window
        img[7:17, leaf + 5:leaf + 11] = hexc("#2b4a52")
        img[8:16, leaf + 6:leaf + 10] = hexc("#3d6873")
        img[8, leaf + 6] = hexc("#9fd3dc")
        img[9, leaf + 6] = hexc("#7fb9c4")
        # push bar
        img[26:28, leaf + 2:leaf + 14] = hexc("#aab6b1")
        img[26, leaf + 2:leaf + 14] = hexc("#e5ece8")
        img[28, leaf + 2:leaf + 14] = hexc("#7f8c87")
        # kick plate
        img[40:46, leaf + 2:leaf + 14] = hexc("#b9c6c0")
        img[40, leaf + 2:leaf + 14] = hexc("#d6e0db")
    img[46:48, :] = hexc("#6f807a")
    parts = []
    for row in range(3):          # bottom row first
        for col in range(2):
            y0 = 48 - (row + 1) * 16
            parts.append(img[y0:y0 + 16, col * 16:col * 16 + 16].copy())
    return parts


def exit_light():
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / 8 + abs(y - 7.5) / 16
            t = min(1.0, d)
            c = mix("#ffffff", "#bfffe0", t)
            img[y, x] = (c[0], c[1], c[2], int(235 - 70 * t))
    return img


def shallow_water(frames=32):
    """Animated ankle-deep water: pale aqua with soft moving light ripples (loops, tiles seamlessly)."""
    strip = new(16, 16 * frames)
    for f in range(frames):
        ph = f / frames * 2 * math.pi
        for y in range(16):
            for x in range(16):
                u = x / 16 * 2 * math.pi
                v = y / 16 * 2 * math.pi
                w = (math.sin(u + ph) + math.sin(v * 2 - ph) + math.sin(u + v + ph * 2) * 0.7 + math.sin(2 * u - v - ph) * 0.5)
                w = (w + 3.2) / 6.4
                c = mix("#4fc6d4", "#c9f6fa", max(0.0, min(1.0, (w - 0.35) * 1.6)))
                a = 150 + int(50 * max(0.0, w - 0.6))
                strip[f * 16 + y, x] = (c[0], c[1], c[2], min(210, a))
    return strip


def locker(kind):
    base = "#4fb0b6"
    img = new(16, 16, hexc(base))
    n = value_noise(16, 4, 9)
    for y in range(16):
        for x in range(16):
            img[y, x] = mix(base, "#5cc0c6", n[y, x] * 0.5)
    if kind == "front":
        img[0, :] = hexc("#2f7f85")
        img[15, :] = hexc("#2f7f85")
        img[:, 0] = hexc("#2f7f85")
        img[:, 15] = hexc("#2f7f85")
        img[1:15, 1] = hexc("#7fd2d6")
        for y in (3, 5, 7):
            img[y, 4:12] = hexc("#2a6d72")
            img[y + 1, 4:12] = hexc("#79c9ce")
        img[9:12, 12] = hexc("#e2ecee")
        img[9:12, 13] = hexc("#9aaab0")
        img[12, 4:8] = hexc("#e9f0f2")
        img[13, 4:8] = hexc("#9fb0b4")
    else:
        img[:, 0] = hexc("#3c959b")
        img[:, 15] = hexc("#3c959b")
        if kind == "top":
            img[0, :] = hexc("#3c959b")
            img[15, :] = hexc("#3c959b")
    return img


def chair():
    """Slats (white with light-blue gaps) and the white frame."""
    slats = new(16, 16, hexc("#f2f6f6"))
    for y in range(1, 16, 3):
        slats[y, :] = hexc("#8fd6de")
    slats[:, 0] = hexc("#d5dedf")
    slats[:, 15] = hexc("#d5dedf")
    frame = new(16, 16, hexc("#e9eef0"))
    frame[::4, :] = hexc("#d3dadc")
    return slats, frame


def life_ring():
    img = new(16, 16)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 3.6 <= d <= 7.4:
                a = math.degrees(math.atan2(y - cy, x - cx)) % 360
                red = int(((a + 22.5) % 360) // 45) % 2 == 0
                c = "#e8483f" if red else "#f6f6f2"
                shade = 1.0 - max(0.0, (d - 6.2)) * 0.12 - max(0.0, (4.6 - d)) * 0.12
                col = hexc(c)
                img[y, x] = (int(col[0] * shade), int(col[1] * shade), int(col[2] * shade), 255)
    # rope around the outside
    for k in range(0, 360, 30):
        a = math.radians(k)
        x = int(round(cx + math.cos(a) * 7.6))
        y = int(round(cy + math.sin(a) * 7.6))
        if 0 <= x < 16 and 0 <= y < 16:
            img[y, x] = hexc("#c9b48a")
    return img


def wristband(color, dark):
    """A wristband seen from above: a loop with a little clasp."""
    img = new(16, 16)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - cx) * 1.05, y - cy)
            if 4.2 <= d <= 6.6:
                t = abs(d - 5.4) / 1.2
                img[y, x] = mix(color, dark, t * 0.6)
    img[12:15, 6:10] = hexc("#e9eef0")
    img[13, 7:9] = hexc("#a9b4b8")
    img[3, 6] = mix(color, "#ffffff", 0.6)
    img[4, 5] = mix(color, "#ffffff", 0.4)
    return img


def advancement_background():
    return tiles("#d8f3f5", "#a8d8dd", 5)


def main():
    out = {}
    out["block/white_pool_tiles"] = tiles("#eef6f6", "#b9cfd2", 1, var=6)
    out["block/aqua_pool_tiles"] = tiles("#5cc9d3", "#d2eef0", 2, var=8)
    out["block/mosaic_pool_tiles"] = mosaic(3)
    out["block/lane_line_tiles"] = tiles("#2453a0", "#173a73", 4, var=6)
    out["block/ceiling_light"] = ceiling_light()
    out["block/fake_sky_panel"] = fake_sky()
    out["block/pool_light"] = pool_light()
    out["block/pool_ladder"] = pool_ladder()
    out["block/strange_drain"] = strange_drain()
    out["block/strange_drain_side"] = strange_drain_side()
    left, right = exit_sign()
    out["block/exit_sign_left"] = left
    out["block/exit_sign_right"] = right
    out["block/exit_sign_back"] = np.tile(np.array(hexc("#1c2b22"), dtype=np.uint8), (16, 16, 1))
    for i, part in enumerate(exit_door()):
        out[f"block/exit_door_{i}"] = part
    out["block/exit_door_side"] = tiles("#9fb0aa", "#7d8f88", 8, var=4)
    out["block/exit_light"] = exit_light()
    out["block/shallow_water"] = shallow_water()
    out["block/locker_front"] = locker("front")
    out["block/locker_side"] = locker("side")
    out["block/locker_top"] = locker("top")
    slats, frame = chair()
    out["block/pool_chair_slats"] = slats
    out["block/pool_chair_frame"] = frame
    out["block/life_ring"] = life_ring()
    out["block/blue_wristband"] = wristband("#2f6fe0", "#173c86")
    out["block/yellow_wristband"] = wristband("#ffd23f", "#b88a12")
    out["block/red_wristband"] = wristband("#e83b3b", "#8c1c1c")
    out["gui/advancements/backgrounds/pool_tiles"] = advancement_background()
    for name, img in out.items():
        save(img, name + ".png")
    return out


if __name__ == "__main__":
    main()
    print("block textures written")
