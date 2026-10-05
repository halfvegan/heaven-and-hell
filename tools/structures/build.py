"""Builds the structure templates and preview images.

    python3 tools/structures/build.py [heaven|hell] [--preview DIR]
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import voxel  # noqa: E402

OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "data",
                                   "heavenhell", "structure"))


def run(name, preview):
    t = time.time()
    if name == "heaven":
        import heaven as mod
        file = "heaven_gates.nbt"
    else:
        import hell as mod
        file = "infernal_court.nbt"
    b = mod.build()
    errs = mod.check(b)
    for e in errs:
        print("  !", e)
    n, p = b.write_nbt(os.path.join(OUT, file))
    size = os.path.getsize(os.path.join(OUT, file))
    print(f"{file}: {n} blocks, {p} palette entries, {size // 1024} KiB, {len(errs)} problems, {time.time() - t:.1f}s")
    if preview:
        os.makedirs(preview, exist_ok=True)
        voxel.top_down(b, os.path.join(preview, f"{name}_top.png"), scale=6)
        voxel.iso(b, os.path.join(preview, f"{name}_iso.png"), scale=3)
        for y in getattr(mod, "SLICES", []):
            voxel.slice_img(b, y, os.path.join(preview, f"{name}_y{y}.png"))
    return b, errs


if __name__ == "__main__":
    args = sys.argv[1:]
    preview = None
    if "--preview" in args:
        i = args.index("--preview")
        preview = args[i + 1]
        del args[i:i + 2]
    names = args or ["heaven", "hell"]
    bad = 0
    for nm in names:
        _, errs = run(nm, preview)
        bad += len(errs)
    sys.exit(1 if bad else 0)
