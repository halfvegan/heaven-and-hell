"""Builds every texture of the mod: python3 tools/art/textures.py"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import blocks  # noqa: E402
import icon  # noqa: E402
import items  # noqa: E402
import skins  # noqa: E402
import visions  # noqa: E402
import wings  # noqa: E402
from px import SHEETS, WRITTEN, sheet, tile3  # noqa: E402


def main():
    b = blocks.build()
    i = items.build()
    v = visions.build()
    s = skins.build()
    w = wings.build()
    c = icon.build()
    sheet(list(b.items()), 8, 8, f"{SHEETS}/blocks.png")
    sheet([(n, tile3(t)) for n, t in b.items() if n in blocks.TILEABLE], 4, 6, f"{SHEETS}/blocks_tiled.png")
    sheet(list(i.items()), 10, 7, f"{SHEETS}/items.png")
    sheet(list(v.items()), 4, 4, f"{SHEETS}/visions.png", bg_checker=False)
    views = []
    for n, img in s.items():
        front, back = skins.assemble(img)
        views += [(n + " front", front), (n + " back", back)]
    sheet(list(s.items()), 6, 4, f"{SHEETS}/skins.png")
    sheet(views, 8, 8, f"{SHEETS}/skin_views.png")
    sheet(list(w.items()), 8, 2, f"{SHEETS}/wings.png")
    sheet(list(c.items()), 3, 1, f"{SHEETS}/icon.png", bg_checker=False)
    print(f"wrote {len(WRITTEN)} textures")


if __name__ == "__main__":
    main()
