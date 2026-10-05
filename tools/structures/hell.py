"""The Infernal Court (infernal_court.nbt). World coordinates; see world/Layout.java.

Map (x east, z south; damned souls arrive at z=40 facing north):
  arrival circle (0,40) -> Gates of Hell (z 29..33) -> Avenue of Bones (z 19..28) -> lava moat (z 15..18)
  -> Court of Ashes -> palace doors (z=7) -> throne hall (x -11..11, z -29..6) -> throne (0,53,-25)
  -> Redemption Gate (back wall z=-30).
  East: Tower of Sin (x 22..38, z -22..-4), the rank quarters.  West: Hall of Torment.
  Corners: Lake of Fire (SE), Graveyard (SW), Forge (E), Soul Well (W), burning gardens behind the palace.
"""
import math
import random
from collections import deque

from voxel import Build

ORIGIN = (-48, 30, -48)
SIZE = (97, 70, 97)
FLOOR = 49  # walkable surface (players stand at y 50)
QUARTER_FLOORS = (49, 57, 65, 73, 81)
SLICES = (50, 58, 66, 74, 82)

R = random.Random(666)


def hh(n, **props):
    s = "heavenhell:" + n
    if props:
        s += "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"
    return s


def mc(n, **props):
    s = "minecraft:" + n
    if props:
        s += "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"
    return s


HB = hh("hellstone_bricks")
BB = hh("brimstone_bricks")
BRIM = hh("brimstone")
CHIS = hh("chiseled_hellstone")
GILD = hh("gilded_hellstone")
PIL = hh("hellstone_pillar", axis="y")
EMBER = hh("ember_lamp")
ASH = hh("ash_block")
PBB = mc("polished_blackstone_bricks")
CPBB = mc("cracked_polished_blackstone_bricks")
PB = mc("polished_blackstone")
BLACK = mc("blackstone")
LAVA = mc("lava")
FIRE = mc("fire")
MAGMA = mc("magma_block")
SOUL_SOIL = mc("soul_soil")
SOUL_FIRE = mc("soul_fire")
CHAIN = mc("iron_chain", axis="y")
BARS = mc("iron_bars")
FENCE = mc("nether_brick_fence")
RNB = mc("red_nether_bricks")
CRY = mc("crying_obsidian")
OBS = mc("obsidian")
BONE_Y = mc("bone_block", axis="y")


def stairs(facing, half="bottom", block="hellstone_brick_stairs"):
    return hh(block, facing=facing, half=half)


def slab(kind="bottom"):
    return hh("hellstone_brick_slab", type=kind)


def build():
    b = Build(ORIGIN, SIZE)
    foundation(b)
    arrival(b)
    perimeter_walls(b)
    gates_of_hell(b)
    avenue_of_bones(b)
    moat(b)
    court_of_ashes(b)
    palace(b)
    tower_of_sin(b)
    hall_of_torment(b)
    lake_of_fire(b)
    graveyard(b)
    forge(b)
    soul_well(b)
    burning_gardens(b)
    sky_chains(b)
    spikes(b)
    print("  ember lamps added for light:", light_up(b))
    keep_clear(b)
    return b


def fill(b, x0, y0, z0, x1, y1, z1, st):
    b.fill(x0, y0, z0, x1, y1, z1, st)


def clear(b, x0, y0, z0, x1, y1, z1):
    b.fill(x0, y0, z0, x1, y1, z1, None)


def noise(x, z, s=0.0):
    return (math.sin(x * 0.37 + s) + math.cos(z * 0.29 - s * 1.7) + math.sin((x + z) * 0.19 + s * 0.5)) / 3.0


# ---------------------------------------------------------------- ground
def foundation(b):
    for x in range(-46, 47):
        for z in range(-46, 47):
            if abs(x) > 44 and abs(z) > 44 and (x + z) % 3:
                continue
            n = noise(x, z)
            n2 = noise(x, z, 3.1)
            if n > 0.45:
                top = ASH
            elif n < -0.5:
                top = BLACK
            elif n2 > 0.55:
                top = mc("basalt", axis="y")
            elif n2 < -0.62:
                top = SOUL_SOIL
            else:
                top = BRIM
            b.set(x, FLOOR, z, top)
            for y in range(44, FLOOR):
                b.set(x, y, z, BRIM if (x * 7 + y * 3 + z) % 5 else BLACK)
            if R.random() < 0.02:  # roots of rock hanging under the court
                depth = R.randint(3, 11)
                for y in range(44 - depth, 44):
                    b.set(x, y, z, BLACK if y > 44 - depth + 1 else mc("basalt", axis="y"))


def path(b, x0, z0, x1, z1, inner=HB, edge=GILD, width=3):
    """Straight paved path (axis aligned) with a gilded edge."""
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            b.set(x, FLOOR, z, inner)
    if width >= 3:
        if x0 == x1 or abs(x1 - x0) < abs(z1 - z0):
            pass


def brazier(b, x, z, base=FLOOR, soul=False, height=1):
    for y in range(base + 1, base + 1 + height):
        b.set(x, y, z, PIL)
    if soul:
        b.set(x, base + 1 + height, z, SOUL_SOIL)
        b.set(x, base + 2 + height, z, SOUL_FIRE)
    else:
        b.set(x, base + 1 + height, z, MAGMA)
        b.set(x, base + 2 + height, z, FIRE)


def skull_post(b, x, z, wither=True, facing_rot=0):
    b.set(x, FLOOR + 1, z, FENCE)
    b.set(x, FLOOR + 2, z, mc("wither_skeleton_skull" if wither else "skeleton_skull", rotation=str(facing_rot)))


# ---------------------------------------------------------------- arrival
def arrival(b):
    cx, cz = 0, 40
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 8, cz + 9):
            d = math.hypot(x - cx, z - cz)
            if d < 7.5:
                ang = (math.degrees(math.atan2(z - cz, x - cx)) + 360) % 60
                if d >= 6.4:
                    st = GILD
                elif 3.6 <= d < 4.6:
                    st = CHIS
                elif ang < 5 or ang > 55:
                    st = RNB
                else:
                    st = PBB
                b.set(x, FLOOR, z, st)
                clear(b, x, FLOOR + 1, z, x, FLOOR + 4, z)
    b.set(cx, FLOOR, cz, mc("crying_obsidian"))
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        x, z = cx + round(math.cos(a) * 8.6), cz + round(math.sin(a) * 8.6)
        if abs(x) <= 3 and z < cz:
            continue
        brazier(b, x, z, soul=True, height=2)
    for z in range(32, 34):
        for x in range(-3, 4):
            b.set(x, FLOOR, z, HB if abs(x) < 3 else GILD)
    # a lane of skulls on posts leading south into the wastes
    for z in (44, 46):
        for x in (-3, 3):
            pass


# ---------------------------------------------------------------- walls & gate
def battlement(b, x, y, z):
    if (x + z) % 2 == 0:
        b.set(x, y, z, PBB)


def wall_column(b, x, z, top=58, crenel=True):
    for y in range(FLOOR + 1, top + 1):
        st = PBB
        if y == 54:
            st = GILD
        elif (x * 3 + y * 5 + z) % 11 == 0:
            st = CPBB
        b.set(x, y, z, st)
    if crenel:
        battlement(b, x, top + 1, z)


def perimeter_walls(b):
    for x in range(-42, 43):
        if abs(x) <= 9:
            continue
        for z in (30, 31):
            wall_column(b, x, z)
    for z in range(-44, 32):
        for x in (-42, -41, 41, 42):
            wall_column(b, x, z)
    for x in range(-42, 43):
        for z in (-44, -43):
            wall_column(b, x, z)
    for (cx, cz) in ((-42, 31), (42, 31), (-42, -44), (42, -44)):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                for y in range(FLOOR + 1, 65):
                    b.set(x, y, z, PBB if (y - 50) % 7 else GILD)
                if x in (cx - 2, cx + 2) or z in (cz - 2, cz + 2):
                    battlement(b, x, 65, z)
        b.set(cx, 65, cz, MAGMA)
        b.set(cx, 66, cz, FIRE)


def horn(b, x, z, side):
    """A great curved horn rising from a tower top, curving outward (side = -1 west / +1 east)."""
    pts = [(0, 71), (0, 72), (1, 73), (1, 74), (2, 75), (3, 76), (4, 77), (5, 77)]
    for i, (dx, y) in enumerate(pts):
        st = BONE_Y if i >= len(pts) - 2 else BLACK
        b.set(x + side * dx, y, z, st)
        if i < 3:
            b.set(x + side * dx, y, z - 1, BLACK)
            b.set(x + side * dx, y, z + 1, BLACK)


def gates_of_hell(b):
    for side in (-1, 1):
        x0, x1 = (5, 9) if side > 0 else (-9, -5)
        for x in range(x0, x1 + 1):
            for z in range(29, 34):
                for y in range(FLOOR + 1, 71):
                    edge = x in (x0, x1) and z in (29, 33)
                    if edge:
                        st = PIL
                    elif y in (55, 62, 69):
                        st = GILD
                    else:
                        st = HB
                    b.set(x, y, z, st)
                if x in (x0, x1) or z in (29, 33):
                    battlement(b, x, 71, z)
        # glowing arrow slits
        mid = (x0 + x1) // 2
        for y in (58, 59, 65, 66):
            b.set(mid, y, 33, EMBER)
            b.set(mid, y, 29, EMBER)
        b.set(mid, 71, 31, MAGMA)
        b.set(mid, 72, 31, FIRE)
        horn(b, x1 if side > 0 else x0, 31, side)
    # the arch
    for x in range(-4, 5):
        under = 59 + round(2 * math.sqrt(max(0.0, 1 - (x / 4.6) ** 2)))
        for z in range(29, 34):
            for y in range(under, 64):
                b.set(x, y, z, HB)
            b.set(x, under, z, GILD if z in (29, 33) else HB)
        b.set(x, under - 1, 31, BARS)  # the raised portcullis
        for z in (29, 33):
            b.set(x, 63, z, CHIS)
    for z in (29, 33):
        b.set(0, 61, z, CRY)
        b.set(0, 62, z, CHIS)
    for x in range(-4, 5):
        for z in range(29, 34):
            b.set(x, FLOOR, z, HB if abs(x) < 4 else GILD)


# ---------------------------------------------------------------- avenue of bones
def rib(b, z, side):
    prev = None
    for i in range(0, 41):
        th = math.radians(i * 90 / 40)
        x = side * round(8.4 * math.sin(th))
        y = round(FLOOR + 1 + 13 * math.cos(th))
        if prev is not None and (x, y) == prev:
            continue
        axis = "y" if i > 22 else "x"
        b.set(x, y, z, mc("bone_block", axis=axis))
        if prev is not None:
            px, py = prev
            if abs(px - x) == 1 and abs(py - y) == 1:  # keep the rib connected
                b.set(x, py, z, mc("bone_block", axis=axis))
        prev = (x, y)


def avenue_of_bones(b):
    for z in range(19, 29):
        for x in range(-3, 4):
            if x == 0:
                st = GILD
            elif abs(x) == 3:
                st = CHIS if z % 3 == 0 else HB
            else:
                st = HB
            b.set(x, FLOOR, z, st)
    for z in (20, 23, 26):
        rib(b, z, -1)
        rib(b, z, 1)
    for z in range(19, 29):
        b.set(0, 63, z, mc("bone_block", axis="z"))
    b.set(0, 63, 18, mc("bone_block", axis="z"))
    for x in (-1, 1):
        b.set(x, 63, 19, mc("bone_block", axis="x"))
    # flame pillars
    for z in (21, 25):
        for x in (-5, 5):
            brazier(b, x, z, height=4)
    # a hanging cage with the bones of a sinner
    cx, cz = 0, 24
    for y in range(59, 63):
        b.set(cx, y, cz, CHAIN)
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            b.set(x, 55, z, mc("polished_blackstone_brick_slab", type="top"))
            b.set(x, 58, z, mc("polished_blackstone_brick_slab", type="bottom"))
            if (x, z) != (cx, cz):
                b.set(x, 56, z, BARS)
                b.set(x, 57, z, BARS)
    b.set(cx, 56, cz, mc("skeleton_skull", rotation="8"))
    b.set(cx, 57, cz, None)
    # skulls on posts along the avenue
    for z in (19, 22, 27):
        skull_post(b, -4, z, facing_rot=4)
        skull_post(b, 4, z, facing_rot=12)


# ---------------------------------------------------------------- moat
def moat(b):
    for x in range(-40, 41):
        for z in range(15, 19):
            b.set(x, 47, z, BLACK)
            b.set(x, 48, z, LAVA)
            b.set(x, FLOOR, z, None)
    for x in range(-40, 41):
        for z in (14, 19):
            if abs(x) <= 4 or 31 <= abs(x) <= 37:
                continue
            if b.get(x, FLOOR + 1, z) is None:
                b.set(x, FLOOR + 1, z, FENCE)
    for bx0, bx1 in ((-3, 3), (-36, -32), (32, 36)):
        for x in range(bx0, bx1 + 1):
            for z in range(14, 20):
                edge = x in (bx0, bx1)
                b.set(x, FLOOR, z, GILD if edge else HB)
                b.set(x, 48, z, PBB if 15 <= z <= 18 and edge else b.get(x, 48, z))
                if edge and 15 <= z <= 18:
                    b.set(x, FLOOR + 1, z, FENCE)
        for z in (15, 18):
            b.set(bx0, 48, z, PBB)
            b.set(bx1, 48, z, PBB)


# ---------------------------------------------------------------- Court of Ashes (plaza before the palace)
def court_of_ashes(b):
    for x in range(-13, 14):
        for z in range(8, 14):
            ring = (x + z) % 4 == 0
            b.set(x, FLOOR, z, GILD if ring and abs(x) > 4 else (PBB if abs(x) > 3 else HB))
    for x in range(-3, 4):
        b.set(x, FLOOR, 8, CHIS)
    # flame pillars flanking the doors
    for x in (-5, 5):
        for y in range(FLOOR + 1, 62):
            b.set(x, y, 9, PIL if y % 6 else GILD)
        b.set(x, 62, 9, MAGMA)
        b.set(x, 63, 9, FIRE)
        for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            b.set(x + dx, FLOOR + 1, 9 + dz, stairs({(-1, 0): "east", (1, 0): "west", (0, -1): "south", (0, 1): "north"}[(dx, dz)]))
    # paths to the side bridges
    for x in range(-36, 37):
        if abs(x) <= 13:
            continue
        for z in range(10, 13):
            b.set(x, FLOOR, z, HB if z == 11 else PBB)
    for x in (-34, 34):
        for z in range(13, 15):
            for xx in range(x - 1, x + 2):
                b.set(xx, FLOOR, z, HB)
        for z in range(20, 29):
            for xx in range(x - 1, x + 2):
                b.set(xx, FLOOR, z, HB if xx == x else PBB)


# ---------------------------------------------------------------- the palace
HALL_X = 12      # hall walls at x = +-12
HALL_Z0 = -30    # back wall
HALL_Z1 = 7      # front wall
HALL_TOP = 71    # last wall block
COLUMNS_Z = (2, -4, -10, -16)


def palace(b):
    hall_shell(b)
    hall_floor(b)
    hall_columns(b)
    hall_ceiling_and_roof(b)
    facade(b)
    dais_and_throne(b)
    redemption_gate(b)
    lava_falls(b)
    wings(b)
    corridors(b)


def hall_shell(b):
    for z in range(HALL_Z0, HALL_Z1 + 1):
        for x in (-HALL_X, HALL_X):
            for y in range(FLOOR + 1, HALL_TOP + 1):
                pilaster = (z - 4) % 6 == 0
                if pilaster:
                    st = PIL if y < 64 else CHIS
                elif y in (FLOOR + 1, 64):
                    st = GILD if y == 64 else BB
                else:
                    st = HB
                b.set(x, y, z, st)
            # clerestory windows above the wing roofs
            if (z - 4) % 6 not in (0, 1, 5):
                for y in range(65, 70):
                    b.set(x, y, z, mc("red_stained_glass_pane"))
    for x in range(-HALL_X, HALL_X + 1):
        for y in range(FLOOR + 1, HALL_TOP + 1):
            b.set(x, y, HALL_Z0, HB if y != 64 else GILD)
            b.set(x, y, HALL_Z1, HB if y != 64 else GILD)
    clear(b, -11, FLOOR + 1, -29, 11, HALL_TOP, 6)


def hall_floor(b):
    for x in range(-11, 12):
        for z in range(-29, 7):
            if abs(x) <= 1:
                st = RNB if abs(x) == 0 or z % 2 else mc("nether_bricks")
            elif abs(x) == 2:
                st = GILD
            elif (x + z) % 2 == 0:
                st = PBB
            else:
                st = HB
            b.set(x, FLOOR, z, st)
    # soul lamps along the nave (no campfires: their smoke would fill the hall)
    for z in (5, -1, -7, -13):
        for x in (-6, 6):
            b.set(x, FLOOR + 1, z, CHIS)
            b.set(x, FLOOR + 2, z, mc("soul_lantern", hanging="false", waterlogged="false"))
        for x in (-4, 4):
            b.set(x, FLOOR, z + 3 if z > -13 else z - 3, EMBER)
    # warm lamps on brackets along both walls
    for z in (1, -5, -11, -17, -23):
        for x in (-11, 11):
            b.set(x, 56, z, slab("bottom"))
            b.set(x, 55, z, mc("lantern", hanging="true", waterlogged="false"))


def hall_columns(b):
    for z in COLUMNS_Z:
        for x in (-8, 8):
            for y in range(FLOOR + 1, 69):
                if y in (53, 58):
                    st = EMBER
                elif y in (52, 54, 57, 59):
                    st = GILD
                else:
                    st = PIL
                b.set(x, y, z, st)
            b.set(x, 69, z, CHIS)
            b.set(x, 70, z, CHIS)
            # flying ribs to the wall
            for xx in range(x + (1 if x > 0 else -1), (HALL_X if x > 0 else -HALL_X), 1 if x > 0 else -1):
                b.set(xx, 70, z, GILD)
            # soul lanterns hang from the capitals
            for dz in (-1, 1):
                b.set(x, 68, z + dz, CHAIN)
                b.set(x, 67, z + dz, mc("soul_lantern", hanging="true", waterlogged="false"))


def chandelier(b, x, z, top):
    for y in range(61, top + 1):
        b.set(x, y, z, CHAIN)
    b.set(x, 60, z, mc("shroomlight"))
    b.set(x, 59, z, mc("polished_blackstone_brick_slab", type="top"))
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        b.set(x + dx, 60, z + dz, GILD)
        b.set(x + dx, 59, z + dz, mc("lantern", hanging="true", waterlogged="false"))
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for y in range(63, top + 1):
            b.set(x + dx, y, z + dz, CHAIN)
        b.set(x + dx, 62, z + dz, mc("soul_lantern", hanging="true", waterlogged="false"))


def hall_ceiling_and_roof(b):
    ceil = HALL_TOP + 1  # 72
    for x in range(-HALL_X, HALL_X + 1):
        for z in range(HALL_Z0, HALL_Z1 + 1):
            b.set(x, ceil, z, BB)
    for z in list(COLUMNS_Z) + [8 - 6, -22, -28]:
        for x in range(-11, 12):
            b.set(x, HALL_TOP, z, GILD)
    for z in (1, -8, -17):
        chandelier(b, 0, z, HALL_TOP)
    # pitched roof
    for z in range(HALL_Z0 - 1, HALL_Z1 + 2):
        for s in range(0, 13):
            y = ceil + 1 + s
            for side in (-1, 1):
                x = side * (HALL_X + 1 - s)
                b.set(x, y, z, stairs("east" if side < 0 else "west"))
            if z in (HALL_Z0 - 1, HALL_Z1 + 1) or z in (HALL_Z0, HALL_Z1):
                for x in range(-(HALL_X - s), HALL_X - s + 1):
                    b.set(x, y, z, HB)
        b.set(0, ceil + 14, z, GILD)
    # spires at the corners
    for (sx, sz) in ((-HALL_X, HALL_Z1), (HALL_X, HALL_Z1), (-HALL_X, HALL_Z0), (HALL_X, HALL_Z0)):
        for x in range(sx - 1, sx + 2):
            for z in range(sz - 1, sz + 2):
                for y in range(ceil, ceil + 7):
                    b.set(x, y, z, HB if (x, z) != (sx, sz) else PIL)
                if x != sx or z != sz:
                    b.set(x, ceil + 7, z, stairs({-1: "east", 1: "west"}[x - sx] if x != sx else
                                                 {-1: "south", 1: "north"}[z - sz]))
        for y in range(ceil + 7, ceil + 13):
            b.set(sx, y, sz, PIL if y < ceil + 12 else BLACK)
        b.set(sx, ceil + 13, sz, MAGMA)
        b.set(sx, ceil + 14, sz, FIRE)


def facade(b):
    z = HALL_Z1
    # the great doorway
    for x in range(-2, 3):
        top = 57 if abs(x) <= 1 else 56
        for y in range(FLOOR + 1, top + 1):
            b.set(x, y, z, None)
    for x in (-3, 3):
        for y in range(FLOOR + 1, 59):
            b.set(x, y, z, PIL)
    for x in range(-3, 4):
        b.set(x, 58 if abs(x) <= 1 else 57, z, CHIS)
    b.set(0, 59, z, CRY)
    # the rose window ("the eye")
    cx, cy = 0, 65
    for x in range(-5, 6):
        for y in range(cy - 5, cy + 6):
            d = math.hypot(x, y - cy)
            if d < 4.6:
                spoke = x == 0 or y == cy or abs(x) == abs(y - cy)
                if d < 1.2:
                    st = EMBER
                elif 3.6 <= d or spoke:
                    st = GILD
                else:
                    st = mc("red_stained_glass")
                b.set(x, y, z, st)
            elif d < 5.4:
                b.set(x, y, z, CHIS)
    # pediment above the front wall
    for y in range(HALL_TOP + 1, HALL_TOP + 14):
        half = HALL_X - (y - HALL_TOP - 1)
        for x in range(-half, half + 1):
            b.set(x, y, z + 1, HB if abs(x) < half else GILD)
    b.set(0, HALL_TOP + 8, z + 2, CRY)
    b.set(0, HALL_TOP + 9, z + 2, EMBER)
    b.set(0, HALL_TOP + 10, z + 2, CRY)


def dais_and_throne(b):
    # three tiers rising towards the throne
    tiers = ((50, 4, -20), (51, 3, -21), (52, 2, -22))
    for y, half, front in tiers:
        for x in range(-half, half + 1):
            for z in range(-26, front + 1):
                b.set(x, y, z, PBB if y < 52 else GILD)
        for x in range(-half, half + 1):
            b.set(x, y, front, stairs("north", block="hellstone_brick_stairs"))
        for z in range(-26, front):
            b.set(-half, y, z, stairs("east"))
            b.set(half, y, z, stairs("west"))
    for x in range(-2, 3):
        for z in range(-26, -22):
            b.set(x, 52, z, GILD if (x + z) % 2 else CHIS)
    # the throne
    b.set(0, 53, -25, slab("bottom"))
    for x in (-1, 1):
        b.set(x, 53, -25, CHIS)
        b.set(x, 54, -25, slab("bottom"))
        b.set(x, 53, -24, stairs("south" if False else "north", half="top"))
    for x in range(-1, 2):
        for y in range(53, 58):
            b.set(x, y, -26, GILD if x == 0 and y in (55, 56) else (CHIS if y == 57 else HB))
    b.set(0, 57, -26, CRY)
    for x in (-2, 2):
        for y in range(53, 60):
            b.set(x, y, -26, PIL)
        b.set(x + (1 if x > 0 else -1), 59, -26, BLACK)
        b.set(x + (2 if x > 0 else -2), 60, -26, BLACK)
        b.set(x + (2 if x > 0 else -2), 61, -26, mc("bone_block", axis="y"))
    clear(b, 0, 54, -25, 0, 56, -25)
    # lamps and candles on the dais, so the Morningstar is never in shadow
    for x in (-4, 4):
        b.set(x, 51, -25, mc("soul_lantern", hanging="false", waterlogged="false"))
        b.set(x, 51, -23, mc("soul_lantern", hanging="false", waterlogged="false"))
    for x in (-3, 3):
        b.set(x, 52, -24, mc("polished_blackstone_wall"))
        b.set(x, 53, -24, mc("red_candle", candles="4", lit="true", waterlogged="false"))
    for x in (-4, -2, 2, 4):
        b.set(x, 50, -20, EMBER) if abs(x) == 4 else None
    for x in (-2, 2):
        b.set(x, 52, -23, EMBER)


def redemption_gate(b):
    z = HALL_Z0
    for y in range(FLOOR + 1, 59):
        b.set(-3, y, z, GILD)
        b.set(3, y, z, GILD)
    for x in range(-3, 4):
        b.set(x, 57, z, CHIS)
        b.set(x, 58, z, CHIS)
    for x in range(-2, 3):
        for y in range(FLOOR + 1, 57):
            b.set(x, y, z, CRY)
    b.set(0, 58, z, EMBER)
    # the sigil above the gate (inside face)
    for x in range(-6, 7):
        for y in range(60, 71):
            d = math.hypot(x, (y - 65) * 1.15)
            if 4.6 <= d < 5.6:
                b.set(x, y, z + 1, GILD)
            elif d < 2.2:
                b.set(x, y, z + 1, CRY)
    for x, y in ((-3, 69), (-4, 70), (3, 69), (4, 70)):
        b.set(x, y, z + 1, GILD)
    # behind the seal: a stairway of light back to the living world (heaven's stone inside, hell's outside)
    for zz in range(z - 1, z - 11, -1):
        for x in range(-4, 5):
            for y in range(FLOOR - 1, 62):
                if abs(x) == 4 or y == 61 or zz == z - 10:
                    b.set(x, y, zz, HB)
    for i, zz in enumerate(range(z - 1, z - 10, -1)):
        floor_y = FLOOR + min(i, 7)
        for x in range(-3, 4):
            for y in range(FLOOR - 1, 61):
                if abs(x) == 3 or y == 60 or zz == z - 9:
                    lamp = (y == 60 and x % 2 == 0) or (zz == z - 9 and (x + y) % 3 == 0)
                    b.set(x, y, zz, hh("halo_lamp") if lamp else hh("pearlstone_bricks"))
                elif y <= floor_y:
                    b.set(x, y, zz, hh("golden_bricks") if y == floor_y else hh("pearlstone"))
                else:
                    b.set(x, y, zz, None)


def lava_falls(b):
    for x0 in (-7, 7):
        for y in range(FLOOR, 68):
            b.set(x0 - 1, y, -31, HB)
            b.set(x0 + 1, y, -31, HB)
            b.set(x0, y, -32, HB)
            b.set(x0 - 1, y, -32, HB)
            b.set(x0 + 1, y, -32, HB)
        for y in range(FLOOR + 1, 67):
            b.set(x0, y, -31, LAVA)
            b.set(x0, y, -30, mc("red_stained_glass"))
        b.set(x0, FLOOR, -31, BLACK)
        b.set(x0, 67, -31, HB)


def wings(b):
    for side in (-1, 1):
        x_in, x_out = side * 13, side * 20
        xs = range(min(x_in, x_out), max(x_in, x_out) + 1)
        for x in xs:
            for z in range(HALL_Z0, HALL_Z1 + 1):
                outer = x == x_out or z in (HALL_Z0, HALL_Z1)
                for y in range(FLOOR + 1, 62):
                    if outer:
                        corner = x == x_out and z in (HALL_Z0, HALL_Z1)
                        st = PIL if corner or (z - 4) % 6 == 0 else (GILD if y == 54 else HB)
                        b.set(x, y, z, st)
                    else:
                        b.set(x, y, z, None)
                b.set(x, 62, z, BB)
                if outer:
                    battlement(b, x, 63, z)
                b.set(x, FLOOR, z, PBB if (x + z) % 2 else HB)
        # windows on the outer wall
        for z in range(HALL_Z0 + 2, HALL_Z1 - 1):
            if (z - 4) % 6 in (2, 3, 4):
                for y in (54, 55, 56):
                    b.set(x_out, y, z, BARS)
        # front doors of the wings
        for dz_x in (side * 16, side * 17):
            for y in range(FLOOR + 1, 53):
                b.set(dz_x, y, HALL_Z1, None)
        # doors: hall <-> wing, wing <-> outside
        for z in (-9, -8):
            for y in range(FLOOR + 1, 53):
                b.set(side * HALL_X, y, z, None)
                b.set(x_out, y, z, None)
        # lights
        for z in range(HALL_Z0 + 3, HALL_Z1, 6):
            xm = side * 16
            b.set(xm, 61, z, CHAIN)
            b.set(xm, 60, z, mc("soul_lantern", hanging="true", waterlogged="false"))
    treasury(b)
    archive(b)


def treasury(b):
    """East wing: Lucifer's hoard."""
    for z in (-28, -22, -16, 0, 5):
        for x in (14, 19):
            b.set(x, FLOOR + 1, z, mc("raw_gold_block") if (z + x) % 2 else mc("gold_block"))
            if R.random() < 0.5:
                b.set(x, FLOOR + 2, z, mc("gold_block"))
    for z in (-26, -19, 2):
        b.set(19, FLOOR + 1, z, mc("chest", facing="west", type="single", waterlogged="false"), nbt={"id": "minecraft:chest"})
    for z in (-27, -13, 3):
        b.set(14, FLOOR + 1, z, mc("decorated_pot", facing="east", cracked="false", waterlogged="false"))
    for x in range(15, 19):
        for z in (-24, -23):
            b.set(x, FLOOR, z, GILD)
    b.set(16, FLOOR + 1, -24, mc("netherite_block"))
    b.set(17, FLOOR + 1, -23, mc("netherite_block"))
    b.set(16, FLOOR + 2, -24, mc("wither_skeleton_skull", rotation="4"))
    for z in (-4, 4):
        b.set(16, FLOOR + 1, z, EMBER)


def archive(b):
    """West wing: the Archive of Sins."""
    for z in range(-29, 7):
        if z in (-9, -8):
            continue
        for y in range(FLOOR + 1, FLOOR + 4):
            b.set(-19, y, z, mc("bookshelf") if (z + y) % 4 else mc("chiseled_bookshelf", facing="east"))
    for z in (-24, -16, -2, 4):
        b.set(-15, FLOOR + 1, z, mc("lectern", facing="east", has_book="false", powered="false"))
        b.set(-14, FLOOR + 1, z, mc("red_candle", candles="3", lit="true", waterlogged="false"))
    for z in (-28, -12, 0):
        b.set(-14, FLOOR + 1, z, mc("cobweb"))
        b.set(-18, 61, z, mc("cobweb"))


def corridors(b):
    for side in (-1, 1):
        x = side * 21
        for z in range(-10, -6):
            b.set(x, FLOOR, z, HB)
            for y in range(FLOOR + 1, 54):
                b.set(x, y, z, HB if z in (-10, -7) or y == 53 else None)
        b.set(x, 54, -9, BB)
        b.set(x, 54, -8, BB)


# ---------------------------------------------------------------- Tower of Sin (rank quarters)
TX0, TX1, TZ0, TZ1 = 22, 38, -22, -4
ROOF = 89


def tower_of_sin(b):
    for x in range(TX0, TX1 + 1):
        for z in range(TZ0, TZ1 + 1):
            wall = x in (TX0, TX1) or z in (TZ0, TZ1)
            corner = x in (TX0, TX1) and z in (TZ0, TZ1)
            for y in range(FLOOR + 1, ROOF):
                if corner:
                    st = PIL
                elif wall:
                    st = GILD if (y - FLOOR) % 8 == 0 else HB
                elif (y - FLOOR) % 8 == 0:
                    st = PBB  # floors
                else:
                    st = None
                b.set(x, y, z, st)
            b.set(x, ROOF, z, BB)
            b.set(x, FLOOR, z, PBB)
            if wall:
                battlement(b, x, ROOF + 1, z)
    for (x, z) in ((TX0, TZ0), (TX1, TZ0), (TX0, TZ1), (TX1, TZ1)):
        for y in range(ROOF + 1, 95):
            b.set(x, y, z, PIL)
        b.set(x, 95, z, MAGMA)
        b.set(x, 96, z, FIRE)
    # windows on every floor
    for f in QUARTER_FLOORS:
        for y in (f + 3, f + 4, f + 5):
            for x in (26, 27, 30, 34, 35):
                b.set(x, y, TZ0, mc("red_stained_glass_pane"))
            for z in (-18, -17, -13, -11, -10):
                b.set(TX1, y, z, mc("red_stained_glass_pane"))
            for x in (26, 27, 37):
                if x != 37:
                    b.set(x, y, TZ1, mc("red_stained_glass_pane"))
    # doors: corridor from the palace and the ground-floor arch
    for z in (-9, -8):
        for y in range(FLOOR + 1, 53):
            b.set(TX0, y, z, None)
    for x in range(31, 34):
        for y in range(FLOOR + 1, 53):
            b.set(x, y, TZ1, None)
    b.set(30, 53, TZ1, CHIS)
    b.set(34, 53, TZ1, CHIS)
    for x in range(30, 35):
        b.set(x, 54, TZ1, GILD)
    quarters_rank1(b, 49)
    quarters_rank2(b, 57)
    quarters_rank3(b, 65)
    quarters_rank4(b, 73)
    quarters_rank5(b, 81)
    # the ladder goes in last so no floor or furniture covers it
    for y in range(FLOOR + 1, ROOF + 1):
        b.set(23, y, -12, mc("ladder", facing="east", waterlogged="false"))
        b.set(24, y, -12, None) if (y - FLOOR) % 8 else None
    for i, f in enumerate(QUARTER_FLOORS, start=1):
        b.set(36, f + 1, -7, mc("chest", facing="west", type="single", waterlogged="false"), nbt={"id": "minecraft:chest"})


def hang(b, x, y_ceiling, z, light="soul_lantern", length=1):
    for y in range(y_ceiling - length, y_ceiling):
        b.set(x, y, z, CHAIN)
    b.set(x, y_ceiling - length - 1, z, mc(light, hanging="true", waterlogged="false"))


def bed(b, x, f, z_head, color="red", facing="north"):
    b.set(x, f + 1, z_head, mc(f"{color}_bed", facing=facing, part="head", occupied="false"))
    dz = 1 if facing == "north" else -1
    b.set(x, f + 1, z_head + dz, mc(f"{color}_bed", facing=facing, part="foot", occupied="false"))


def quarters_rank1(b, f):
    """Imp: a bare cell."""
    for x in range(23, 38):
        for z in range(-21, -4):
            if R.random() < 0.25:
                b.set(x, f, z, SOUL_SOIL if R.random() < 0.6 else BRIM)
    bed(b, 26, f, -20)
    b.set(28, f + 1, -21, mc("crafting_table"))
    b.set(29, f + 1, -21, mc("furnace", facing="south", lit="false"))
    for (x, y, z) in ((24, f + 7, -21), (37, f + 7, -21), (37, f + 6, -21), (24, f + 7, -20)):
        b.set(x, y, z, mc("cobweb"))
    hang(b, 30, f + 8, -13)
    hang(b, 33, f + 8, -18)
    b.set(35, f + 1, -20, mc("skeleton_skull", rotation="10"))


def quarters_rank2(b, f):
    """Fiend: a proper den."""
    for x in range(23, 38):
        for z in range(-21, -4):
            b.set(x, f, z, HB if (x + z) % 2 else BB)
    for x in range(28, 33):
        for z in range(-15, -11):
            b.set(x, f + 1, z, mc("red_carpet") if (x + z) % 3 else mc("black_carpet"))
    bed(b, 25, f, -20)
    b.set(24, f + 1, -20, mc("lantern", hanging="false", waterlogged="false"))
    b.set(28, f + 1, -21, mc("smoker", facing="south", lit="false"))
    b.set(29, f + 1, -21, mc("blast_furnace", facing="south", lit="false"))
    b.set(30, f + 1, -21, mc("crafting_table"))
    b.set(31, f + 1, -21, mc("anvil", facing="east"))
    b.set(33, f + 1, -21, mc("bookshelf"))
    b.set(34, f + 1, -21, mc("bookshelf"))
    b.set(36, f + 1, -21, mc("cauldron"))
    hang(b, 30, f + 8, -13, light="lantern")
    hang(b, 34, f + 8, -17, light="lantern")


def quarters_rank3(b, f):
    """Demon: crimson luxury."""
    for x in range(23, 38):
        for z in range(-21, -4):
            b.set(x, f, z, mc("crimson_planks") if (x // 2 + z // 2) % 2 else GILD)
    # four-poster bed
    bed(b, 26, f, -20)
    bed(b, 27, f, -20)
    for x, z in ((25, -21), (28, -21), (25, -18), (28, -18)):
        for y in range(f + 1, f + 4):
            b.set(x, y, z, mc("crimson_fence"))
    for x in range(25, 29):
        for z in range(-21, -17):
            b.set(x, f + 4, z, mc("red_wool") if (x + z) % 2 else mc("black_wool"))
    # enchanting corner
    b.set(33, f + 1, -18, mc("enchanting_table"))
    for x, z in ((31, -20), (32, -21), (33, -21), (34, -21), (35, -20), (31, -19), (35, -19)):
        b.set(x, f + 1, z, mc("bookshelf"))
        b.set(x, f + 2, z, mc("bookshelf"))
    b.set(36, f + 1, -12, mc("brewing_stand"))
    b.set(37, f + 1, -12, mc("lava_cauldron"))
    b.set(37, f + 1, -14, mc("crafting_table"))
    for x, z in ((29, -9), (33, -13), (26, -13)):
        b.set(x, f + 7, z, mc("shroomlight"))


def quarters_rank4(b, f):
    """Archdemon: gold, trophies and a wall of fire."""
    for x in range(23, 38):
        for z in range(-21, -4):
            b.set(x, f, z, mc("gold_block") if (x + z) % 4 == 0 else PB)
    # lava aquarium in the north wall
    for x in range(28, 33):
        for y in range(f + 2, f + 6):
            b.set(x, y, TZ0, mc("red_stained_glass"))
            b.set(x, y, TZ0 - 1, LAVA)
        b.set(x, f + 1, TZ0 - 1, HB)
        b.set(x, f + 6, TZ0 - 1, HB)
        b.set(x, f + 3, TZ0 - 2, HB)
        for y in range(f + 1, f + 7):
            b.set(x, y, TZ0 - 2, HB)
    for y in range(f + 1, f + 7):
        b.set(27, y, TZ0 - 1, HB)
        b.set(33, y, TZ0 - 1, HB)
    # trophies
    for x in (24, 26, 34, 36):
        b.set(x, f + 1, -20, mc("netherite_block") if x in (26, 34) else CRY)
        b.set(x, f + 2, -20, mc("wither_skeleton_skull", rotation="8"))
    # bedroom at the south
    bed(b, 25, f, -8, facing="north")
    bed(b, 26, f, -8, facing="north")
    for x in range(24, 28):
        b.set(x, f + 1, -10, mc("red_carpet"))
    b.set(29, f + 1, -5, mc("ender_chest", facing="north", waterlogged="false"))
    b.set(37, f + 1, -16, mc("anvil", facing="north"))
    b.set(37, f + 1, -17, mc("smithing_table"))
    b.set(37, f + 1, -18, mc("respawn_anchor", charges="4"))
    b.set(31, f + 1, -12, mc("enchanting_table"))
    for x, z in ((29, -14), (30, -14), (32, -14), (33, -14), (29, -10), (33, -10)):
        b.set(x, f + 1, z, mc("bookshelf"))
    for x, z in ((27, -15), (34, -10), (30, -7)):
        b.set(x, f + 7, z, mc("shroomlight"))


def quarters_rank5(b, f):
    """Prince of Hell: the penthouse."""
    for x in range(23, 38):
        for z in range(-21, -4):
            b.set(x, f, z, mc("gold_block") if (x + z) % 2 == 0 else GILD)
    for z in range(-20, -5):
        b.set(30, f, z, RNB)
        b.set(31, f, z, RNB)
    # a throne of his own
    b.set(30, f + 1, -21, CHIS)
    b.set(31, f + 1, -21, CHIS)
    b.set(30, f + 1, -20, stairs("north"))
    b.set(31, f + 1, -20, stairs("north"))
    for x in (29, 32):
        for y in range(f + 1, f + 5):
            b.set(x, y, -21, PIL)
        b.set(x, f + 5, -21, mc("gold_block"))
    b.set(30, f + 2, -21, GILD)
    b.set(31, f + 2, -21, GILD)
    # lava wall behind glass
    for x in list(range(24, 28)) + list(range(34, 37)):
        for y in range(f + 2, f + 6):
            b.set(x, y, TZ0, mc("red_stained_glass"))
            b.set(x, y, TZ0 - 1, LAVA)
        for y in range(f + 1, f + 7):
            b.set(x, y, TZ0 - 2, HB)
        b.set(x, f + 1, TZ0 - 1, HB)
        b.set(x, f + 6, TZ0 - 1, HB)
    for x in (23, 28, 33, 37):
        for y in range(f + 1, f + 7):
            b.set(x, y, TZ0 - 1, HB)
    # beacon on netherite (decor) and the luxuries
    for x in range(26, 29):
        for z in range(-14, -11):
            b.set(x, f + 1, z, mc("netherite_block"))
    b.set(27, f + 2, -13, mc("beacon"))
    b.set(37, f + 1, -16, mc("ender_chest", facing="west", waterlogged="false"))
    b.set(37, f + 1, -17, mc("enchanting_table"))
    b.set(37, f + 1, -18, mc("brewing_stand"))
    b.set(37, f + 1, -19, mc("anvil", facing="north"))
    b.set(37, f + 1, -20, mc("respawn_anchor", charges="4"))
    for x, z in ((35, -17), (35, -18), (35, -19)):
        b.set(x, f + 1, z, mc("bookshelf"))
    bed(b, 25, f, -8)
    bed(b, 26, f, -8)
    b.set(24, f + 1, -8, mc("gold_block"))
    b.set(24, f + 2, -8, mc("lantern", hanging="false", waterlogged="false"))
    # chandelier
    for y in range(f + 5, f + 8):
        b.set(31, y, -13, CHAIN)
    b.set(31, f + 4, -13, mc("shroomlight"))
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        b.set(31 + dx, f + 4, -13 + dz, mc("gold_block"))
    # balcony over the court
    for x in range(30, 35):
        for y in range(f + 1, f + 4):
            b.set(x, y, TZ1, None)
    for x in range(29, 36):
        for z in range(TZ1 + 1, TZ1 + 4):
            b.set(x, f, z, GILD if x in (29, 35) or z == TZ1 + 3 else PBB)
            if x in (29, 35) or z == TZ1 + 3:
                b.set(x, f + 1, z, FENCE)
    b.set(29, f - 1, TZ1 + 3, stairs("north", half="top"))
    b.set(35, f - 1, TZ1 + 3, stairs("north", half="top"))


# ---------------------------------------------------------------- Hall of Torment (west)
WX0, WX1, WZ0, WZ1 = -38, -22, -22, -4


def hall_of_torment(b):
    for x in range(WX0, WX1 + 1):
        for z in range(WZ0, WZ1 + 1):
            wall = x in (WX0, WX1) or z in (WZ0, WZ1)
            for y in range(FLOOR + 1, 63):
                if wall:
                    st = PIL if x in (WX0, WX1) and z in (WZ0, WZ1) else (CPBB if (x * 5 + y * 3 + z) % 7 == 0 else PBB)
                    b.set(x, y, z, st)
                else:
                    b.set(x, y, z, None)
            b.set(x, 63, z, PBB)
            if wall:
                battlement(b, x, 64, z)
            r = R.random()
            b.set(x, FLOOR, z, SOUL_SOIL if r < 0.35 else (mc("soul_sand") if r < 0.5 else BLACK))
    # doors
    for z in (-9, -8):
        for y in range(FLOOR + 1, 53):
            b.set(WX1, y, z, None)
    for x in range(-31, -28):
        for y in range(FLOOR + 1, 53):
            b.set(x, y, WZ1, None)
    # the pit of souls
    for x in range(-33, -26):
        for z in range(-16, -9):
            edge = x in (-33, -27) or z in (-16, -10)
            if edge:
                b.set(x, FLOOR, z, PBB)
                b.set(x, FLOOR + 1, z, FENCE)
            else:
                b.set(x, FLOOR - 1, z, SOUL_SOIL)
                b.set(x, FLOOR, z, SOUL_FIRE)
    for x, z in ((-33, -13), (-27, -13), (-30, -16), (-30, -10)):
        b.set(x, FLOOR + 1, z, None)
        b.set(x, FLOOR + 1, z, FENCE)
    # cages in the corners
    for (cx, cz) in ((-35, -19), (-25, -19), (-35, -7), (-25, -7)):
        for x in range(cx - 1, cx + 2):
            for z in range(cz - 1, cz + 2):
                b.set(x, FLOOR, z, PBB)
                for y in range(FLOOR + 1, FLOOR + 4):
                    if (x, z) != (cx, cz) or y == FLOOR + 3:
                        b.set(x, y, z, BARS if (x, z) != (cx, cz) else mc("polished_blackstone_brick_slab", type="bottom"))
                b.set(x, FLOOR + 4, z, mc("polished_blackstone_brick_slab", type="bottom"))
        b.set(cx, FLOOR + 1, cz, mc("skeleton_skull", rotation=str(R.randint(0, 15))))
        b.set(cx, FLOOR + 2, cz, None)
        for y in range(FLOOR + 5, 63):
            b.set(cx, y, cz, CHAIN)
    # chains and lanterns
    for x, z in ((-30, -19), (-30, -7), (-36, -13), (-24, -13)):
        hang(b, x, 63, z, length=4)
    for x, z in ((-37, -16), (-37, -10), (-23, -16), (-23, -10), (-33, -21), (-27, -21), (-33, -5), (-27, -5)):
        b.set(x, 55, z, mc("polished_blackstone_brick_slab", type="bottom"))
        b.set(x, 54, z, mc("soul_lantern", hanging="true", waterlogged="false"))
    for x, z in ((-37, -21), (-23, -21), (-37, -5), (-23, -5)):
        b.set(x, 62, z, mc("cobweb"))
        b.set(x, 61, z, mc("cobweb"))


# ---------------------------------------------------------------- courtyards
def lake_of_fire(b):
    cx, cz = 21, 24
    for x in range(cx - 12, cx + 13):
        for z in range(cz - 6, cz + 7):
            d = ((x - cx) / 8.5) ** 2 + ((z - cz) / 4.0) ** 2
            if d < 1:
                b.set(x, FLOOR - 1, z, LAVA)
                b.set(x, FLOOR, z, LAVA)
                b.set(x, FLOOR - 2, z, BLACK)
            elif d < 1.45:
                b.set(x, FLOOR, z, PBB if d < 1.25 else b.get(x, FLOOR, z))
                if d < 1.25:
                    b.set(x, FLOOR + 1, z, FENCE if (x + z) % 2 else None)
    for (x, z, h) in ((cx - 4, 23, 6), (cx + 1, 25, 9), (cx + 5, 23, 4), (cx - 2, 26, 3), (cx + 6, 25, 5)):
        for y in range(FLOOR - 1, FLOOR + h):
            b.set(x, y, z, mc("basalt", axis="y"))
        b.set(x, FLOOR + h, z, BLACK)
        b.set(x, FLOOR + h + 1, z, MAGMA)
        b.set(x, FLOOR + h + 2, z, FIRE)


def graveyard(b):
    for x in range(-38, -14):
        for z in range(20, 29):
            if b.get(x, FLOOR, z) in (HB, PBB):
                continue
            b.set(x, FLOOR, z, SOUL_SOIL if R.random() < 0.7 else mc("soul_sand"))
    for row, z in enumerate((21, 24, 27)):
        for x in range(-37, -15, 3):
            if abs(x + 34) <= 1:
                continue
            b.set(x, FLOOR + 1, z, mc("chiseled_polished_blackstone") if (x + row) % 2 else PBB)
            b.set(x, FLOOR + 2, z, mc("polished_blackstone_brick_slab", type="bottom"))
            if R.random() < 0.4:
                b.set(x, FLOOR + 1, z + 1, mc("wither_rose"))
            elif R.random() < 0.3:
                b.set(x, FLOOR + 1, z + 1, mc("skeleton_skull", rotation=str(R.randint(0, 15))))
    for x, z in ((-35, 25), (-20, 22), (-26, 28)):
        b.set(x, FLOOR + 1, z, mc("soul_campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
    # a dead tree
    tx, tz = -18, 25
    for y in range(FLOOR + 1, FLOOR + 7):
        b.set(tx, y, tz, mc("crimson_stem", axis="y"))
    for dx, dy in ((1, 5), (2, 6), (-1, 4), (-2, 5), (-3, 5)):
        b.set(tx + dx, FLOOR + dy, tz, mc("crimson_stem", axis="x"))
    b.set(tx, FLOOR + 6, tz + 1, mc("crimson_stem", axis="z"))
    b.set(tx + 2, FLOOR + 5, tz, mc("iron_chain", axis="y"))
    b.set(tx + 2, FLOOR + 4, tz, mc("soul_lantern", hanging="true", waterlogged="false"))


def forge(b):
    """East courtyard: the infernal forge."""
    x0, x1, z0, z1 = 26, 36, 1, 10
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, FLOOR, z, PBB if (x + z) % 3 else HB)
    for x in range(29, 34):
        for z in range(4, 8):
            edge = x in (29, 33) or z in (4, 7)
            if edge:
                for y in range(FLOOR + 1, FLOOR + 3):
                    b.set(x, y, z, HB)
                b.set(x, FLOOR + 3, z, slab("bottom"))
            else:
                b.set(x, FLOOR + 1, z, BLACK)
                b.set(x, FLOOR + 2, z, LAVA)
    for (x, z), blk in (((27, 2), mc("anvil", facing="east")), ((27, 4), mc("smithing_table")),
                        ((27, 6), mc("blast_furnace", facing="east", lit="false")),
                        ((27, 8), mc("grindstone", face="floor", facing="east")),
                        ((35, 3), mc("anvil", facing="west")), ((35, 5), mc("lava_cauldron")),
                        ((35, 7), mc("blast_furnace", facing="west", lit="false"))):
        b.set(x, FLOOR + 1, z, blk)
    for x, z in ((26, 1), (36, 1), (26, 10), (36, 10)):
        for y in range(FLOOR + 1, FLOOR + 6):
            b.set(x, y, z, PIL)
        b.set(x, FLOOR + 6, z, EMBER)
    for x in range(26, 37):
        b.set(x, FLOOR + 6, 1, slab("bottom") if x not in (26, 36) else EMBER)
        b.set(x, FLOOR + 6, 10, slab("bottom") if x not in (26, 36) else EMBER)
    for x in range(30, 33):
        for y in range(FLOOR + 3, FLOOR + 9):
            b.set(x, y, 5 if x != 31 else 6, None)
    for y in range(FLOOR + 3, FLOOR + 9):
        b.set(31, y, 2, BB)
    b.set(31, FLOOR + 9, 2, MAGMA)
    b.set(31, FLOOR + 10, 2, FIRE)


def soul_well(b):
    cx, cz = -30, 6
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = math.hypot(x - cx, z - cz)
            if d < 2.6:
                for y in range(FLOOR - 3, FLOOR + 1):
                    b.set(x, y, z, None)
                b.set(x, FLOOR - 4, z, SOUL_SOIL)
                b.set(x, FLOOR - 3, z, SOUL_FIRE)
            elif d < 3.6:
                for y in range(FLOOR - 4, FLOOR + 1):
                    b.set(x, y, z, PBB)
                b.set(x, FLOOR + 1, z, FENCE)
            elif d < 5.2:
                b.set(x, FLOOR, z, CHIS if (x + z) % 2 else PBB)
    for k in range(6):
        a = math.radians(k * 60)
        skull_post(b, cx + round(math.cos(a) * 4.6), cz + round(math.sin(a) * 4.6), wither=k % 2 == 0,
                   facing_rot=(k * 3) % 16)


def burning_gardens(b):
    for x in range(-40, 41):
        for z in range(-42, -31):
            if b.get(x, FLOOR + 1, z) is not None or b.get(x, FLOOR, z) is None:
                continue
            if 20 < abs(x):
                continue
            n = noise(x, z, 5.0)
            if n > 0.05:
                b.set(x, FLOOR, z, mc("crimson_nylium"))
                r = R.random()
                if r < 0.25:
                    b.set(x, FLOOR + 1, z, mc("crimson_roots"))
                elif r < 0.33:
                    b.set(x, FLOOR + 1, z, mc("crimson_fungus"))
            elif n < -0.4:
                b.set(x, FLOOR, z, MAGMA)
    # a few lava pools behind the tower and the torment hall
    for cx, cz in ((30, -34), (-30, -34), (34, -28), (-34, -28)):
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 2, cz + 3):
                if ((x - cx) / 3.2) ** 2 + ((z - cz) / 2.2) ** 2 < 1:
                    b.set(x, FLOOR, z, LAVA)
                    b.set(x, FLOOR - 1, z, BLACK)


def sky_chains(b):
    for (x, z, bottom) in ((-25, 6, 74), (25, -36, 70), (-26, -36, 72), (14, 24, 76), (-14, -2, 80)):
        for y in range(bottom + 1, 97):
            b.set(x, y, z, CHAIN)
        b.set(x, bottom, z, mc("soul_lantern", hanging="true", waterlogged="false"))


def spikes(b):
    placed = 0
    tries = 0
    while placed < 26 and tries < 4000:
        tries += 1
        x, z = R.randint(-40, 40), R.randint(-42, 46)
        if b.get(x, FLOOR, z) not in (BRIM, ASH, BLACK, SOUL_SOIL, mc("basalt", axis="y")):
            continue
        if any(b.get(x + dx, FLOOR + 1, z + dz) is not None for dx in (-2, 0, 2) for dz in (-2, 0, 2)):
            continue
        if any(b.get(x + dx, FLOOR, z + dz) in (HB, PBB, GILD, CHIS, RNB) for dx in range(-3, 4) for dz in range(-3, 4)):
            continue
        h = R.randint(3, 8)
        for y in range(FLOOR + 1, FLOOR + 1 + h):
            b.set(x, y, z, mc("basalt", axis="y") if y < FLOOR + h - 1 else BLACK)
        if h > 5:
            for dx, dz in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                if R.random() < 0.6:
                    b.set(x + dx, FLOOR + 1, z + dz, BLACK)
        placed += 1


# ---------------------------------------------------------------- light
EMIT = {
    "minecraft:lava": 15, "minecraft:fire": 15, "minecraft:soul_fire": 10, "minecraft:magma_block": 3,
    "minecraft:shroomlight": 15, "minecraft:lantern": 15, "minecraft:soul_lantern": 10, "minecraft:soul_campfire": 10,
    "minecraft:campfire": 15, "minecraft:crying_obsidian": 10, "minecraft:glowstone": 15, "minecraft:beacon": 15,
    "minecraft:lava_cauldron": 15, "minecraft:respawn_anchor": 15, "minecraft:red_candle": 9,
    "minecraft:enchanting_table": 7, "minecraft:ender_chest": 7, "minecraft:brewing_stand": 1,
    "minecraft:shroomlight": 15,
    "heavenhell:ember_lamp": 15, "heavenhell:halo_lamp": 15, "heavenhell:soul_shard_ore": 6,
}
SEE_THROUGH = ("glass", "pane", "bars", "chain", "lantern", "fire", "campfire", "carpet", "slab", "stairs", "fence",
               "ladder", "skull", "candle", "roots", "fungus", "rose", "bed", "chest", "enchanting", "lectern", "anvil",
               "brewing", "cauldron", "cobweb", "pot", "grindstone", "lava", "beacon", "wall")


def opaque(state):
    if state is None:
        return False
    name = state.split("[")[0]
    return not any(k in name for k in SEE_THROUGH)


def compute_light(b):
    light = {}
    q = deque()
    for pos, st in b.blocks.items():
        lv = EMIT.get(st.split("[")[0], 0)
        if lv:
            light[pos] = lv
            q.append(pos)
    while q:
        pos = q.popleft()
        lv = light[pos] - 1
        if lv <= 0:
            continue
        x, y, z = pos
        for n in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            if not b.inside(*n) or opaque(b.blocks.get(n)):
                continue
            if light.get(n, 0) < lv:
                light[n] = lv
                q.append(n)
    return light


def covered(b, x, y, z, reach=12):
    return any(opaque(b.get(x, yy, z)) for yy in range(y + 2, y + reach))


def walk_spots(b):
    """Air spots a player walks on: the court floor plus every room (not the open roof tops)."""
    for (x, y, z), st in list(b.blocks.items()):
        if not opaque(st) or abs(x) > 44 or abs(z) > 46 or y < FLOOR or y > 92:
            continue
        if b.get(x, y + 1, z) is None and b.get(x, y + 2, z) is None:
            if y == FLOOR or covered(b, x, y, z):
                yield (x, y + 1, z)


def light_up(b):
    """Embeds ember lamps in the floor wherever the court would be pitch dark."""
    added = 0
    for _round in range(40):
        light = compute_light(b)
        dark = [p for p in walk_spots(b) if light.get(p, 0) < 2]
        if not dark:
            break
        dark.sort(key=lambda p: (p[1], p[0], p[2]))
        taken = set()
        for (x, y, z) in dark:
            if any((x // 9, y // 6, z // 9) == t for t in taken):
                continue
            floor = b.get(x, y - 1, z)
            if floor is None or "ladder" in floor:
                continue
            b.set(x, y - 1, z, EMBER)
            taken.add((x // 9, y // 6, z // 9))
            added += 1
    return added


# ---------------------------------------------------------------- final touches
LABELS = [(0, 66, 30), (0, 65, -25), (32, 92, -13), (0, 58, -29), (0, 61, 34), (21, 55, 24), (-27, 54, 24),
          (31, 58, 5), (-30, 54, 6), (-30, 65, -13)]


def keep_clear(b):
    for (x, y, z) in LABELS:
        b.set(x, y, z, None)
    clear(b, 0, 54, -25, 0, 56, -25)
    for f in QUARTER_FLOORS:
        b.set(32, f + 1, -6, None)
        b.set(32, f + 2, -6, None)
        if b.get(32, f, -6) is None:
            b.set(32, f, -6, PBB)
    for (x, y, z) in ((0, 50, 40), (0, 51, 40)):
        b.set(x, y, z, None)


def check(b):
    errs = []

    def solid(x, y, z):
        s = b.get(x, y, z)
        return s is not None and opaque(s) and "lava" not in s

    def free(x, y, z):
        s = b.get(x, y, z)
        return s is None or "carpet" in s

    if not solid(0, 49, 40) or not free(0, 50, 40) or not free(0, 51, 40):
        errs.append("arrival spot not standable")
    if b.get(0, 53, -25) != slab("bottom"):
        errs.append(f"throne seat is {b.get(0, 53, -25)}")
    for y in (54, 55, 56):
        if b.get(0, y, -25) is not None:
            errs.append(f"throne headroom blocked at y {y}")
    for x in range(-2, 3):
        for y in range(50, 57):
            if b.get(x, y, -30) != CRY:
                errs.append(f"redemption gate seal missing at {x},{y}")
        for z in (-29, -28, -27):
            if not solid(x, 49, z) or not free(x, 50, z) or not free(x, 51, z):
                errs.append(f"no walkway in front of the redemption gate at {x},{z}")
    for i, f in enumerate(QUARTER_FLOORS, start=1):
        if not solid(32, f, -6) or not free(32, f + 1, -6) or not free(32, f + 2, -6):
            errs.append(f"rank {i} spawn blocked")
        if "chest" not in str(b.get(36, f + 1, -7)):
            errs.append(f"rank {i} chest missing")
    for y in range(50, 90):
        if "ladder" not in str(b.get(23, y, -12)):
            errs.append(f"ladder gap at y {y}")
            break
        if not solid(22, y, -12):
            errs.append(f"ladder has no wall at y {y}")
            break
    for (x, y, z) in LABELS:
        if b.get(x, y, z) is not None:
            errs.append(f"label spot {x},{y},{z} occupied by {b.get(x, y, z)}")
    blocked = sum(1 for x in range(-11, 12) for z in range(-19, 7) if not free(x, 50, z) or not free(x, 51, z))
    if blocked > 60:
        errs.append(f"arena floor too cluttered ({blocked} blocked cells)")
    route = [(0, 40), (0, 34), (0, 31), (0, 24), (0, 16), (0, 11), (0, 7), (0, 0), (0, -18), (6, -24), (6, -28),
             (0, -28), (11, -9), (16, -9), (21, -9), (22, -9), (30, -9), (32, -4), (-12, -9), (-21, -9), (-22, -9),
             (-30, -13 + 7), (34, 16), (-34, 16), (34, 24), (-34, 24), (0, 12), (20, 11), (-20, 11)]
    for (x, z) in route:
        if not solid(x, 49, z) or not free(x, 50, z) or not free(x, 51, z):
            errs.append(f"route blocked at {x},{z}: {b.get(x, 49, z)} / {b.get(x, 50, z)} / {b.get(x, 51, z)}")
    light = compute_light(b)
    dark = [p for p in walk_spots(b) if light.get(p, 0) < 1]
    if dark:
        errs.append(f"{len(dark)} dark spots, e.g. {dark[:5]}")
    return errs
