"""Tiny pixel-art toolkit used by the texture generators (numpy RGBA arrays, uint8)."""
import os

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "heavenhell", "textures")
SHEETS = "/tmp/claude-0/-home-claude/b6727890-6732-5a58-8d22-68a4fd4a1780/scratchpad/art"
WRITTEN = {}


def hexc(h, a=255):
    if isinstance(h, tuple):
        return h if len(h) == 4 else (h[0], h[1], h[2], a)
    h = h.lstrip("#")
    if len(h) == 8:
        return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), int(h[6:8], 16))
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def new(w, h=None, color=(0, 0, 0, 0)):
    h = w if h is None else h
    img = np.zeros((h, w, 4), dtype=np.uint8)
    img[:, :] = color
    return img


def rng(seed):
    return np.random.default_rng(seed)


def value_noise(size, cells, seed, octaves=1, persistence=0.5, h=None):
    """Tileable value noise in [0,1], shape (h, w)."""
    w = size
    h = size if h is None else h
    r = rng(seed)
    total = np.zeros((h, w))
    amp = 1.0
    norm = 0.0
    for o in range(octaves):
        cx = cells * (2 ** o)
        cy = max(1, int(round(cx * h / w)))
        lat = r.random((cy, cx))
        ys = np.arange(h) * cy / h
        xs = np.arange(w) * cx / w
        y0 = np.floor(ys).astype(int)
        x0 = np.floor(xs).astype(int)
        fy = ys - y0
        fx = xs - x0
        fy = fy * fy * (3 - 2 * fy)
        fx = fx * fx * (3 - 2 * fx)
        y1 = (y0 + 1) % cy
        x1 = (x0 + 1) % cx
        a = lat[y0][:, x0]
        b = lat[y0][:, x1]
        c = lat[y1][:, x0]
        d = lat[y1][:, x1]
        top = a + (b - a) * fx[None, :]
        bot = c + (d - c) * fx[None, :]
        total += amp * (top + (bot - top) * fy[:, None])
        norm += amp
        amp *= persistence
    return total / norm


def normalize(f):
    lo, hi = f.min(), f.max()
    return (f - lo) / (hi - lo + 1e-9)


def quantize(field, palette, cut=None):
    """Maps a [0,1] field to palette colours (list of hex, dark -> light)."""
    cols = [hexc(c) if isinstance(c, str) else c for c in palette]
    n = len(cols)
    if cut is None:
        idx = np.clip((field * n).astype(int), 0, n - 1)
    else:
        idx = np.digitize(field, cut)
    out = np.zeros(field.shape + (4,), dtype=np.uint8)
    for i, c in enumerate(cols):
        out[idx == i] = c
    return out


def put(img, x, y, color):
    h, w = img.shape[:2]
    if 0 <= x < w and 0 <= y < h:
        img[y, x] = hexc(color) if isinstance(color, str) else color


def putw(img, x, y, color):
    """Put with wrap-around (for tileable textures)."""
    h, w = img.shape[:2]
    img[y % h, x % w] = hexc(color) if isinstance(color, str) else color


def rect(img, x0, y0, x1, y1, color):
    c = hexc(color) if isinstance(color, str) else color
    img[max(0, y0):y1 + 1, max(0, x0):x1 + 1] = c


def grid(rows, palette, img=None, ox=0, oy=0):
    """Draws an ASCII grid. '.' (or ' ') = transparent / skip. palette maps chars to hex colours."""
    h = len(rows)
    w = max(len(r) for r in rows)
    if img is None:
        img = new(w, h)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ". " or ch not in palette:
                continue
            put(img, ox + x, oy + y, palette[ch])
    return img


def shade(img, amount):
    """Darken (amount<0) or lighten (amount>0) RGB, keeps alpha."""
    out = img.astype(int).copy()
    if amount >= 0:
        out[..., :3] = out[..., :3] + (255 - out[..., :3]) * amount
    else:
        out[..., :3] = out[..., :3] * (1 + amount)
    return np.clip(out, 0, 255).astype(np.uint8)


def mix(c1, c2, t):
    a = np.array(hexc(c1) if isinstance(c1, str) else c1, dtype=float)
    b = np.array(hexc(c2) if isinstance(c2, str) else c2, dtype=float)
    return tuple(int(round(v)) for v in a + (b - a) * t)


def outline(img, color="#1b1420", diagonal=False):
    """Adds a 1px outline around opaque pixels (item style)."""
    a = img[..., 3] > 0
    h, w = a.shape
    edge = np.zeros_like(a)
    for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (1, -1), (-1, 1), (-1, -1)] if diagonal else []):
        sh = np.zeros_like(a)
        ys = slice(max(0, dy), h + min(0, dy))
        yd = slice(max(0, -dy), h + min(0, -dy))
        xs = slice(max(0, dx), w + min(0, dx))
        xd = slice(max(0, -dx), w + min(0, -dx))
        sh[yd, xd] = a[ys, xs]
        edge |= sh
    edge &= ~a
    out = img.copy()
    out[edge] = hexc(color)
    return out


def save(img, rel):
    path = os.path.join(TEX, rel) if not os.path.isabs(rel) else rel
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(img, "RGBA").save(path)
    WRITTEN[rel] = img
    return path


def tile3(img):
    return np.tile(img, (3, 3, 1))


def upscale(img, k):
    return np.repeat(np.repeat(img, k, axis=0), k, axis=1)


def checker(h, w, s=8):
    yy, xx = np.mgrid[0:h, 0:w]
    c = ((yy // s + xx // s) % 2).astype(bool)
    out = np.zeros((h, w, 4), dtype=np.uint8)
    out[c] = (70, 70, 78, 255)
    out[~c] = (90, 90, 100, 255)
    return out


def over(dst, src, x, y):
    """Alpha-composites src onto dst at x, y."""
    h, w = src.shape[:2]
    H, W = dst.shape[:2]
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(W, x + w), min(H, y + h)
    if x1 <= x0 or y1 <= y0:
        return dst
    s = src[y0 - y:y1 - y, x0 - x:x1 - x].astype(float)
    d = dst[y0:y1, x0:x1].astype(float)
    a = s[..., 3:4] / 255.0
    d[..., :3] = s[..., :3] * a + d[..., :3] * (1 - a)
    d[..., 3:4] = np.maximum(d[..., 3:4], s[..., 3:4])
    dst[y0:y1, x0:x1] = d.astype(np.uint8)
    return dst


def sheet(items, scale, cols, path, bg_checker=True, pad=6, label_h=0):
    """items: list of (name, img). Writes a contact sheet."""
    cell_w = max(i.shape[1] for _, i in items) * scale + pad * 2
    cell_h = max(i.shape[0] for _, i in items) * scale + pad * 2 + label_h
    rows = (len(items) + cols - 1) // cols
    canvas = new(cell_w * cols, cell_h * rows, (40, 40, 46, 255))
    for k, (name, img) in enumerate(items):
        cx = (k % cols) * cell_w + pad
        cy = (k // cols) * cell_h + pad
        big = upscale(img, scale)
        if bg_checker:
            bg = checker(big.shape[0], big.shape[1], max(4, scale * 2))
            over(canvas, bg, cx, cy)
        over(canvas, big, cx, cy)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im = Image.fromarray(canvas, "RGBA")
    try:
        from PIL import ImageDraw
        d = ImageDraw.Draw(im)
        for k, (name, img) in enumerate(items):
            cx = (k % cols) * cell_w + pad
            cy = (k // cols) * cell_h + pad + img.shape[0] * scale + 1
            d.text((cx, cy), name[:28], fill=(220, 220, 230, 255))
    except Exception:
        pass
    im.save(path)
    return path
