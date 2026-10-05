import numpy as np
import blocks
from px import sheet, tile3, SHEETS
imgs = blocks.build()
items = []
for name, img in imgs.items():
    items.append((name, img))
sheet(items, 8, 8, f"{SHEETS}/blocks.png")
tiles = [(n + " x3", tile3(i)) for n, i in imgs.items() if n in blocks.TILEABLE]
sheet(tiles, 4, 6, f"{SHEETS}/blocks_tiled.png")
print("ok", len(imgs))
