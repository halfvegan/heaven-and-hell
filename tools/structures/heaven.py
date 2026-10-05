"""The Pearly Gates (heaven_gates.nbt). World coordinates; see world/Layout.java.

Map (x east, z south; arriving souls float down at z=31 and face north):
  arrival cloud (0,31) -> golden road -> Pearly Gates (z 7..9) -> plaza + fountain (0,-4)
  -> Hall of Wonders (x -9..9, z -38..-14).  West: Gate of Return (x=-26).  East: Gardens of Rest (28,0).
  South-west: Tree of Life.  South-east: bell gazebo.  Four cottages to the north-west / north-east.
"""
import math
import random

from voxel import Build

ORIGIN = (-48, 64, -48)
SIZE = (97, 80, 97)
TOP = 100  # walkable surface (players stand at y 101)

R = random.Random(1337)


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


GRASS = hh("heaven_grass")
CLOUD = hh("cloud")
GOLD_CLOUD = hh("golden_cloud")
BRICKS = hh("pearlstone_bricks")
GOLD_BRICKS = hh("golden_bricks")
GILDED = hh("gilded_pearlstone")
CHISELED = hh("chiseled_pearlstone")
PILLAR = hh("pearlstone_pillar", axis="y")
LAMP = hh("halo_lamp")
LILY = hh("angel_lily")
FROG = mc("pearlescent_froglight", axis="y")


def island_radius(theta):
    return 44.0 + 1.5 * math.sin(theta * 3 + 0.7) + 1.0 * math.sin(theta * 7 + 2.1) + 0.5 * math.sin(theta * 13)


def build():
    b = Build(ORIGIN, SIZE)
    island(b)
    arrival(b)
    road_and_gates(b)
    walls(b)
    gatekeeper_podium(b)
    plaza(b)
    hall_of_wonders(b)
    gate_of_return(b)
    gardens(b)
    tree_of_life(b)
    gazebo(b)
    cottages(b)
    meadows(b)
    sky_puffs(b)
    keep_clear(b)
    return b


# ---------------------------------------------------------------- island
def island(b):
    for x in range(-48, 49):
        for z in range(-48, 49):
            d = math.hypot(x, z)
            r = island_radius(math.atan2(z, x))
            if d >= r:
                continue
            frac = d / r
            depth = int(2 + 32 * (1 - frac ** 1.6) + 3 * math.sin(x * 0.31) * math.cos(z * 0.27))
            bottom = max(65, TOP - depth)
            for y in range(bottom, TOP + 1):
                if y == TOP:
                    st = GRASS
                elif y >= TOP - 3:
                    st = hh("heaven_soil")
                else:
                    n = math.sin(x * 0.45 + y * 0.3) + math.cos(z * 0.38 - y * 0.21)
                    if n > 1.55:
                        st = GILDED
                    elif n < -1.75:
                        st = GOLD_CLOUD
                    else:
                        st = hh("pearlstone")
                b.set(x, y, z, st)
            # golden roots and glowing crystals hang below the island
            if R.random() < 0.012 and frac < 0.85:
                length = R.randint(3, 9)
                for i in range(1, length + 1):
                    b.set(x, bottom - i, z, hh("celestial_log", axis="y"))
                b.set(x, bottom - length - 1, z, hh("golden_leaves"))
            elif R.random() < 0.006:
                b.set(x, bottom - 1, z, LAMP)
    # a puffy cloud rim
    for k in range(130):
        th = k / 130 * math.tau + R.random() * 0.05
        rad = R.uniform(1.8, 3.4)
        r = min(island_radius(th) + R.uniform(-2.0, 2.5), 47.5 - rad)
        cx, cz = r * math.cos(th), r * math.sin(th)
        cy = R.uniform(95.5, 100.5)
        b.ball(cx, cy, cz, rad, GOLD_CLOUD if R.random() < 0.18 else CLOUD, only_air=True, ry=rad * 0.7)
    for x in range(-48, 49):
        for z in range(-48, 49):
            if math.hypot(x, z) < island_radius(math.atan2(z, x)) - 3.5:
                for y in range(TOP + 1, TOP + 4):
                    if b.get(x, y, z) in (CLOUD, GOLD_CLOUD):
                        b.set(x, y, z, None)


def lamp_post(b, x, z, h=2, base=TOP):
    for y in range(base + 1, base + 1 + h):
        b.set(x, y, z, PILLAR)
    b.set(x, base + 1 + h, z, LAMP)


def path_line(b, x0, z0, x1, z1, width=1, st=BRICKS, stepping=False):
    n = max(abs(x1 - x0), abs(z1 - z0))
    for i in range(n + 1):
        t = i / max(1, n)
        x = round(x0 + (x1 - x0) * t)
        z = round(z0 + (z1 - z0) * t)
        if stepping and i % 3 == 2:
            continue
        for dx in range(-(width // 2), width - width // 2):
            for dz in range(-(width // 2), width - width // 2):
                b.set(x + dx, TOP, z + dz, st)


# ---------------------------------------------------------------- arrival + road + gates
def arrival(b):
    b.disc(0, TOP, 31, 7.0, CLOUD)
    b.disc(0, TOP, 31, 7.0, GOLD_CLOUD, r_in=5.8)
    b.disc(0, TOP - 1, 31, 7.0, CLOUD)
    for a in range(0, 360, 45):
        x = round(math.cos(math.radians(a)) * 7.5)
        z = 31 + round(math.sin(math.radians(a)) * 7.5)
        if abs(x) <= 2 and z < 31:
            continue  # the road leaves the landing here
        lamp_post(b, x, z, 1)


def road_and_gates(b):
    for z in range(5, 25):
        for x in range(-2, 3):
            b.set(x, TOP, z, GOLD_BRICKS if abs(x) <= 1 else GILDED)
    for z in (13, 18, 23):
        lamp_post(b, -3, z)
        if z != 13:
            lamp_post(b, 3, z)
    # pillars
    for px in (-7, 7):
        for x in range(px - 1, px + 2):
            for z in range(7, 10):
                for y in range(TOP + 1, TOP + 20):
                    if y in (TOP + 1, TOP + 8, TOP + 15):
                        st = CHISELED
                    elif y >= TOP + 18:
                        st = GOLD_BRICKS
                    else:
                        st = PILLAR
                    b.set(x, y, z, st)
        b.set(px, TOP + 20, 8, LAMP)
        b.set(px, TOP + 21, 8, FROG)
        for dx, dz in ((-1, 7), (1, 7), (-1, 9), (1, 9)):
            b.set(px + dx, TOP + 20, dz, hh("pearlstone_brick_slab", type="bottom"))
    # the arch: the opening (x -5..5) stays clear up to y 112
    for x in range(-6, 7):
        under = TOP + 12 + round(3 * math.sqrt(max(0.0, 1 - (x / 6.2) ** 2)))
        for z in range(7, 10):
            for y in range(under, under + 3):
                b.set(x, y, z, BRICKS)
            b.set(x, under + 3, z, GOLD_BRICKS)
        b.set(x, under, 9, GILDED)
        b.set(x, under, 7, GILDED)
        if x % 2 == 0:
            b.set(x, under + 1, 9, FROG)
    b.set(0, TOP + 19, 8, LAMP)
    # the gates stand open: bars of light swung inwards along the sides of the opening
    for side in (-5, 5):
        for z in range(4, 8):
            for y in range(TOP + 1, TOP + 11):
                b.set(side, y, z, mc("end_rod", facing="up"))
            b.set(side, TOP + 11, z, hh("pearlstone_brick_slab", type="bottom"))
        b.set(side, TOP + 6, 4, FROG)


def walls(b):
    """A low crescent wall that hugs the sanctum from the gate pillars round to the east and west paths."""
    cx, cz, r = 0.0, -6.0, 15.5
    for deg in range(20, 161):
        a = math.radians(deg)
        x = round(cx + r * math.cos(a))
        z = round(cz + r * math.sin(a))
        if abs(x) <= 8:
            continue
        for y in range(TOP + 1, TOP + 4):
            b.set(x, y, z, BRICKS)
        b.set(x, TOP + 4, z, GOLD_BRICKS)
        if deg % 14 == 0:
            b.set(x, TOP + 5, z, LAMP)


def gatekeeper_podium(b):
    for x in range(3, 6):
        for z in range(12, 15):
            b.set(x, TOP, z, CHISELED)
    b.set(4, TOP + 1, 15, mc("lectern", facing="south", has_book="false", powered="false"))
    lamp_post(b, 6, 12)
    lamp_post(b, 6, 15)
    for x, z in ((3, 15), (5, 15)):
        b.set(x, TOP + 1, z, LILY)


# ---------------------------------------------------------------- plaza + fountain
def plaza(b):
    cx, cz = 0, -4
    for x in range(cx - 11, cx + 12):
        for z in range(cz - 11, cz + 12):
            d = math.hypot(x - cx, z - cz)
            if d < 10.5:
                ang = (math.degrees(math.atan2(z - cz, x - cx)) + 360) % 45
                st = BRICKS
                if 9.3 <= d < 10.5:
                    st = CHISELED
                elif ang < 4 or ang > 41:
                    st = GOLD_BRICKS if d > 5 else GILDED
                b.set(x, TOP, z, st)
                for y in range(TOP + 1, TOP + 6):
                    b.set(x, y, z, None)
    # the fountain: a basin, a slender column and a bowl that spills four little waterfalls
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = math.hypot(x - cx, z - cz)
            if 3.5 <= d < 4.6:
                b.set(x, TOP + 1, z, hh("pearlstone_brick_slab", type="bottom") if (x + z) % 2 else GILDED)
                b.set(x, TOP, z, GOLD_BRICKS)
            elif d < 3.5:
                b.set(x, TOP - 1, z, hh("pearlstone"))
                b.set(x, TOP, z, mc("water"))
    for y in range(TOP, TOP + 5):
        b.set(cx, y, cz, PILLAR)
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            if (x, z) != (cx, cz):
                b.set(x, TOP + 4, z, GOLD_BRICKS)
                b.set(x, TOP + 5, z, mc("water"))
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            d = math.hypot(x - cx, z - cz)
            if 1.5 <= d < 2.9 and not (x == cx or z == cz):
                b.set(x, TOP + 5, z, GILDED)
    b.set(cx, TOP + 5, cz, PILLAR)
    b.set(cx, TOP + 6, cz, PILLAR)
    b.set(cx, TOP + 7, cz, GOLD_CLOUD)
    b.set(cx, TOP + 8, cz, LAMP)
    for a in range(0, 360, 90):
        x = cx + round(math.cos(math.radians(a + 45)) * 8)
        z = cz + round(math.sin(math.radians(a + 45)) * 8)
        lamp_post(b, x, z, 2)


# ---------------------------------------------------------------- Hall of Wonders
def hall_of_wonders(b):
    x0, x1, z0, z1 = -9, 9, -38, -14
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, TOP, z, CHISELED if (abs(x) == 4 and z % 2 == 0) else BRICKS)
            for y in range(TOP + 1, TOP + 13):
                b.set(x, y, z, None)
    for z in range(z0 + 2, z1 + 1):
        for x in range(-1, 2):
            b.set(x, TOP + 1, z, mc("yellow_carpet") if x == 0 else mc("white_carpet"))
    # colonnade
    for z in range(z1, z0 - 1, -4):
        for x in (x0, x1):
            for y in range(TOP + 1, TOP + 11):
                b.set(x, y, z, PILLAR)
            b.set(x, TOP + 11, z, CHISELED)
    for x in (-5, 5):
        for y in range(TOP + 1, TOP + 11):
            b.set(x, y, z1, PILLAR)
        b.set(x, TOP + 11, z1, CHISELED)
    for z in range(z0, z1):
        for x in (x0, x1):
            if b.get(x, TOP + 1, z) is None:
                for y in range(TOP + 1, TOP + 4):
                    b.set(x, y, z, BRICKS)
                b.set(x, TOP + 4, z, hh("pearlstone_brick_slab", type="bottom"))
    for x in range(x0, x1 + 1):
        for y in range(TOP + 1, TOP + 12):
            b.set(x, y, z0, BRICKS if y < TOP + 11 else GOLD_BRICKS)
    # a golden sunburst on the back wall
    for x in range(-4, 5):
        for y in range(TOP + 4, TOP + 10):
            d = math.hypot(x, y - (TOP + 7))
            if d < 1.6:
                b.set(x, y, z0 + 1, LAMP)
            elif d < 3.2 and (x + y) % 2 == 0:
                b.set(x, y, z0 + 1, GOLD_BRICKS)
    # entablature, ceiling and a gabled roof (the portico z -16..-14 stays open to the sky)
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            if x in (x0 - 1, x1 + 1, x0, x1) or z in (z0 - 1, z1 + 1, z0, z1):
                b.set(x, TOP + 11, z, GOLD_BRICKS)
    roof_z = range(z0 - 1, z1 - 2)
    for z in roof_z:
        for x in range(x0 - 1, x1 + 2):
            b.set(x, TOP + 11, z, GOLD_BRICKS if x in (x0 - 1, x1 + 1) else GILDED)
        for step in range(0, 6):
            y = TOP + 12 + step
            for side in (-1, 1):
                x = side * (x1 + 1 - step * 2)
                for xx in (x, x - side):
                    b.set(xx, y, z, hh("pearlstone_brick_stairs", facing="east" if side < 0 else "west", half="bottom"))
            if z in (roof_z[0], roof_z[-1]):  # gable ends
                for x in range(-(8 - step * 2), 9 - step * 2):
                    b.set(x, y, z, BRICKS)
        b.set(-1, TOP + 18, z, GOLD_BRICKS)
        b.set(0, TOP + 18, z, GOLD_BRICKS)
        b.set(1, TOP + 18, z, GOLD_BRICKS)
    b.set(0, TOP + 14, roof_z[-1], LAMP)
    b.set(-1, TOP + 13, roof_z[-1], GOLD_BRICKS)
    b.set(1, TOP + 13, roof_z[-1], GOLD_BRICKS)
    # chandeliers
    for z in (-20, -26, -32):
        for y in range(TOP + 8, TOP + 11):
            b.set(0, y, z, mc("iron_chain", axis="y"))
        b.set(0, TOP + 7, z, FROG)
    # pedestals with treasure chests
    tables = ["hall_halo", "hall_harp", "hall_blade", "hall_manna", "hall_clouds", "hall_return"]
    spots = [(-5, -19, "east"), (5, -19, "west"), (-5, -25, "east"), (5, -25, "west"), (-5, -31, "east"), (5, -31, "west")]
    for (x, z, facing), table in zip(spots, tables):
        b.set(x, TOP + 1, z, CHISELED)
        b.set(x, TOP + 2, z, mc("chest", facing=facing, type="single", waterlogged="false"),
              nbt={"id": "minecraft:chest", "LootTable": f"heavenhell:chests/{table}"})
        b.set(x, TOP + 1, z - 1, LAMP)
        b.set(x, TOP + 1, z + 1, LAMP)
    b.set(0, TOP + 1, -35, GOLD_BRICKS)
    b.set(0, TOP + 2, -35, mc("chest", facing="south", type="single", waterlogged="false"),
          nbt={"id": "minecraft:chest", "LootTable": "heavenhell:chests/hall_wings"})
    for x in (-1, 1):
        b.set(x, TOP + 1, -35, LAMP)
        b.set(x, TOP + 2, -35, LILY)
    for x in range(-4, 5):
        b.set(x, TOP, -13, BRICKS)


# ---------------------------------------------------------------- Gate of Return
def gate_of_return(b):
    gx = -26
    for x in range(-25, -9):
        for z in range(-5, -2):
            b.set(x, TOP, z, GOLD_BRICKS if z == -4 else GILDED)
            for y in range(TOP + 1, TOP + 5):
                b.set(x, y, z, None)
    for z in range(-7, 0):
        for x in range(gx - 1, gx + 2):
            b.set(x, TOP, z, BRICKS)
    for z in (-7, -1):
        for y in range(TOP + 1, TOP + 9):
            b.set(gx, y, z, CHISELED if y % 2 else GOLD_BRICKS)
        b.set(gx, TOP + 9, z, FROG)
    for z in range(-7, 0):
        b.set(gx, TOP + 8, z, GOLD_BRICKS)
    for z in range(-6, -1):
        for y in range(TOP + 1, TOP + 8):
            b.set(gx, y, z, hh("return_light"))
    for z in (-9, 1):
        for y in range(TOP + 1, TOP + 7):
            b.set(gx, y, z, PILLAR)
        b.set(gx, TOP + 7, z, LAMP)
    lamp_post(b, -18, -7)
    lamp_post(b, -18, -1)


# ---------------------------------------------------------------- Gardens of Rest (east)
FLOWERS = ["lily_of_the_valley", "oxeye_daisy", "white_tulip", "allium", "azure_bluet", "cornflower", "golden_dandelion"]


def flower(b, x, z, rich=False):
    r = R.random()
    if r < (0.35 if rich else 0.4):
        b.set(x, TOP + 1, z, LILY)
    elif r < 0.85:
        b.set(x, TOP + 1, z, mc(R.choice(FLOWERS)))
    else:
        tall = R.choice(["peony", "rose_bush", "lilac"])
        b.set(x, TOP + 1, z, mc(tall, half="lower"))
        b.set(x, TOP + 2, z, mc(tall, half="upper"))


def tree(b, x, z, height, spread=3.3):
    for y in range(TOP + 1, TOP + 1 + height):
        b.set(x, y, z, hh("celestial_log", axis="y"))
    cy = TOP + height
    s = int(spread) + 1
    for dx in range(-s, s + 1):
        for dy in range(-1, 4):
            for dz in range(-s, s + 1):
                d = math.sqrt(dx * dx + (dy * 1.4) ** 2 + dz * dz)
                if d < spread and b.get(x + dx, cy + dy, z + dz) is None and R.random() < 0.92:
                    gold = dy >= 2 or R.random() < 0.25
                    b.set(x + dx, cy + dy, z + dz, hh("golden_leaves") if gold else hh("celestial_leaves"))


def gardens(b):
    gx, gz = 28, 0
    # path from the plaza through the east gap in the wall
    for x in range(10, 22):
        for z in range(-5, -2):
            b.set(x, TOP, z, GOLD_BRICKS if z == -4 else BRICKS)
            b.set(x, TOP + 1, z, None)
    lamp_post(b, 18, -6)
    lamp_post(b, 18, -2)
    # ring walk, lawn, reflecting pool with a statue of light
    for x in range(gx - 9, gx + 10):
        for z in range(gz - 9, gz + 10):
            d = math.hypot(x - gx, z - gz)
            if 6.6 <= d < 8.4:
                ang = math.degrees(math.atan2(z - gz, x - gx)) % 30
                b.set(x, TOP, z, GOLD_BRICKS if ang < 3 else BRICKS)
            elif d < 3.2:
                b.set(x, TOP, z, mc("water"))
                b.set(x, TOP - 1, z, hh("pearlstone"))
                if d > 1.5 and R.random() < 0.3:
                    b.set(x, TOP + 1, z, mc("lily_pad"))
            elif d < 4.0:
                b.set(x, TOP, z, GILDED)
            elif d < 6.6 and R.random() < 0.3:
                flower(b, x, z, rich=True)
    b.set(gx, TOP, gz, CHISELED)
    b.set(gx, TOP + 1, gz, CHISELED)
    b.set(gx, TOP + 2, gz, PILLAR)
    b.set(gx, TOP + 3, gz, LAMP)
    b.set(gx, TOP + 4, gz, GOLD_CLOUD)
    for a in (45, 135, 225, 315):
        x = gx + round(math.cos(math.radians(a)) * 9.5)
        z = gz + round(math.sin(math.radians(a)) * 9.5)
        lamp_post(b, x, z)
    # benches facing the pool
    for (x, z, facing) in ((gx - 5, gz - 1, "east"), (gx - 5, gz + 1, "east"), (gx + 5, gz - 1, "west"),
                           (gx + 5, gz + 1, "west"), (gx - 1, gz + 5, "north"), (gx + 1, gz + 5, "north")):
        b.set(x, TOP + 1, z, hh("pearlstone_brick_stairs", facing=facing))
    # flowerbeds, hedges and trees around the ring
    for x in range(14, 41):
        for z in range(-16, 17):
            d = math.hypot(x - gx, z - gz)
            if b.get(x, TOP, z) == GRASS and b.get(x, TOP + 1, z) is None and 8.4 <= d < 14 and math.hypot(x, z) < 39:
                if R.random() < 0.32:
                    flower(b, x, z)
    for x in range(20, 25):
        b.set(x, TOP + 1, -12, hh("golden_leaves"))
        b.set(x, TOP + 1, 12, hh("golden_leaves"))
    for x, z, h in ((17, -11, 6), (35, 11, 7), (39, -5, 5), (19, 12, 5), (37, -12, 6)):
        tree(b, x, z, h)


# ---------------------------------------------------------------- Tree of Life (south-west)
def tree_of_life(b):
    cx, cz = -24, 20
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 7, cz + 8):
            d = math.hypot(x - cx + 0.5, z - cz + 0.5)
            if 5.2 <= d < 6.4:
                b.set(x, TOP, z, BRICKS)
            elif 4.0 <= d < 5.2 and R.random() < 0.7:
                b.set(x, TOP + 1, z, LILY if R.random() < 0.6 else mc("golden_dandelion"))
    log_y = hh("celestial_log", axis="y")
    for x in (cx - 1, cx):
        for z in (cz - 1, cz):
            for y in range(TOP + 1, TOP + 14):
                b.set(x, y, z, log_y)
    # root flares
    for (dx, dz, axis) in ((-2, -1, "x"), (-2, 0, "x"), (1, -1, "x"), (1, 0, "x"), (-1, -2, "z"), (0, -2, "z"),
                           (-1, 1, "z"), (0, 1, "z")):
        b.set(cx + dx, TOP + 1, cz + dz, hh("celestial_log", axis=axis))
    # branches
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        bx, bz = cx - 0.5 + dx * 1.5, cz - 0.5 + dz * 1.5
        for i in range(4):
            y = TOP + 9 + i
            bx += dx
            bz += dz
            axis = "x" if dx != 0 and dz == 0 else "z" if dz != 0 and dx == 0 else "y"
            b.set(round(bx), y, round(bz), hh("celestial_log", axis=axis))
    # canopy
    ccx, ccy, ccz = cx - 0.5, TOP + 15, cz - 0.5
    for x in range(cx - 9, cx + 9):
        for y in range(TOP + 10, TOP + 21):
            for z in range(cz - 9, cz + 9):
                d = ((x - ccx) / 7.6) ** 2 + ((y - ccy) / 4.2) ** 2 + ((z - ccz) / 7.6) ** 2
                if d < 1 and b.get(x, y, z) is None:
                    if d > 0.55 and R.random() < 0.12:
                        continue  # ragged edge
                    gold = y > ccy + 1 or R.random() < 0.35
                    b.set(x, y, z, hh("golden_leaves") if gold else hh("celestial_leaves"))
    # glowing fruit hangs under the canopy
    for _ in range(14):
        a = R.uniform(0, math.tau)
        r = R.uniform(2.5, 6.0)
        x, z = round(ccx + r * math.cos(a)), round(ccz + r * math.sin(a))
        for y in range(TOP + 18, TOP + 9, -1):
            if b.get(x, y, z) is not None and b.get(x, y - 1, z) is None:
                b.set(x, y - 1, z, LAMP)
                break
    for (x, z, facing) in ((cx - 7, cz, "east"), (cx + 6, cz - 1, "west"), (cx, cz + 6, "north")):
        b.set(x, TOP + 1, z, hh("pearlstone_brick_stairs", facing=facing))
    path_line(b, -3, 21, cx + 6, cz, width=2)


# ---------------------------------------------------------------- Bell gazebo (south-east)
def gazebo(b):
    cx, cz = 24, 22
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            if d < 4.6:
                b.set(x, TOP, z, GOLD_BRICKS if d < 1.5 or (x + z) % 2 else GILDED)
                for y in range(TOP + 1, TOP + 5):
                    b.set(x, y, z, None)
            elif d < 6.0 and b.get(x, TOP + 1, z) is None and R.random() < 0.5:
                b.set(x, TOP + 1, z, LILY)
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        x, z = cx + round(math.cos(a) * 4), cz + round(math.sin(a) * 4)
        for y in range(TOP + 1, TOP + 5):
            b.set(x, y, z, PILLAR)
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            if d < 5.3:
                b.set(x, TOP + 5, z, GOLD_CLOUD if d >= 4.3 else CLOUD)
            if d < 4.2:
                b.set(x, TOP + 6, z, CLOUD)
            if d < 3.0:
                b.set(x, TOP + 7, z, GOLD_CLOUD)
            if d < 1.6:
                b.set(x, TOP + 8, z, GOLD_CLOUD)
    b.set(cx, TOP + 9, cz, LAMP)
    b.set(cx, TOP + 4, cz, mc("bell", attachment="ceiling", facing="north", powered="false"))
    path_line(b, cx - 5, cz - 3, 3, 21)


# ---------------------------------------------------------------- cottages (north-west / north-east)
def cottage(b, cx, cz):
    x0, x1, z0, z1 = cx - 3, cx + 3, cz - 3, cz + 3
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, TOP, z, hh("celestial_planks"))
            for y in range(TOP + 1, TOP + 5):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = x in (x0, x1) and z in (z0, z1)
                if corner:
                    b.set(x, y, z, hh("celestial_log", axis="y"))
                elif edge:
                    if y == TOP + 2 and (x == cx or z == cz):
                        b.set(x, y, z, mc("glass_pane"))
                    else:
                        b.set(x, y, z, BRICKS if y == TOP + 1 else hh("celestial_planks"))
                else:
                    b.set(x, y, z, None)
    for layer, (ext, st) in enumerate(((1, CLOUD), (0, GOLD_CLOUD), (-1, CLOUD), (-2, CLOUD))):
        y = TOP + 5 + layer
        for x in range(x0 - ext, x1 + ext + 1):
            for z in range(z0 - ext, z1 + ext + 1):
                if ext == 1 and x in (x0 - 1, x1 + 1) and z in (z0 - 1, z1 + 1):
                    continue
                b.set(x, y, z, st)
    # door on the south wall
    b.set(cx, TOP + 1, z1, mc("birch_door", facing="south", half="lower", hinge="left", open="false", powered="false"))
    b.set(cx, TOP + 2, z1, mc("birch_door", facing="south", half="upper", hinge="left", open="false", powered="false"))
    b.set(cx, TOP, z1 + 1, BRICKS)
    lamp_post(b, cx + 2, z1 + 1, 1)
    # inside
    b.set(x0 + 1, TOP + 1, z0 + 1, mc("white_bed", facing="north", part="head", occupied="false"))
    b.set(x0 + 1, TOP + 1, z0 + 2, mc("white_bed", facing="north", part="foot", occupied="false"))
    b.set(x1 - 1, TOP + 1, z0 + 1, mc("crafting_table"))
    b.set(x1 - 2, TOP + 1, z0 + 1, mc("chest", facing="south", type="single", waterlogged="false"),
          nbt={"id": "minecraft:chest"})
    b.set(x1 - 1, TOP + 1, z0 + 2, mc("furnace", facing="west", lit="false"))
    b.set(x0 + 1, TOP + 1, z1 - 1, mc("potted_white_tulip"))
    b.set(x1 - 1, TOP + 1, z1 - 1, mc("bookshelf"))
    b.set(cx, TOP + 4, cz, mc("lantern", hanging="true", waterlogged="false"))
    b.set(cx, TOP + 1, cz, mc("white_carpet"))


COTTAGES = ((-27, -20), (-20, -28), (25, -22), (18, -29))


def cottages(b):
    for cx, cz in COTTAGES:
        cottage(b, cx, cz)
    path_line(b, -27, -16, -27, -6, stepping=True)
    path_line(b, -20, -24, -20, -6, stepping=True)
    path_line(b, 18, -25, 18, -6, stepping=True)
    path_line(b, 25, -18, 26, -9, stepping=True)


# ---------------------------------------------------------------- the rest of the island
def meadows(b):
    for x in range(-44, 45):
        for z in range(-44, 45):
            if b.get(x, TOP, z) != GRASS or b.get(x, TOP + 1, z) is not None:
                continue
            if math.hypot(x, z) > island_radius(math.atan2(z, x)) - 2:
                continue
            if abs(x) <= 4 and 4 <= z <= 39:
                continue  # keep the road and landing tidy
            if R.random() < 0.06:
                flower(b, x, z)
            elif R.random() < 0.012:
                b.set(x, TOP + 1, z, mc("pink_petals", facing=R.choice(["north", "east", "south", "west"]),
                                        flower_amount=str(R.randint(1, 4))))
    for x, z, h in ((-36, 4, 5), (-14, 30, 6), (12, 34, 5), (34, 24, 5), (-34, -8, 6), (-8, -42, 4), (36, -24, 5),
                    (-38, 18, 5)):
        if b.get(x, TOP, z) == GRASS:
            tree(b, x, z, h, spread=2.9)


def sky_puffs(b):
    placed = 0
    while placed < 16:
        x, z = R.uniform(-40, 40), R.uniform(-40, 40)
        if math.hypot(x, z) > 40:
            continue
        if abs(x) <= 12 and 16 <= z <= 46:
            continue  # the column arriving souls fall through
        if abs(x) <= 14 and -4 <= z <= 16:
            continue  # above the gates
        y = R.uniform(124, 138)
        for _k in range(3):
            b.ball(x + R.uniform(-3, 3), y + R.uniform(-1, 1), z + R.uniform(-3, 3), R.uniform(2.0, 3.4),
                   GOLD_CLOUD if R.random() < 0.3 else CLOUD, only_air=True, ry=1.6)
        placed += 1


NPCS = {"gatekeeper": (4, 101, 13), "angel1": (-8, 101, -3), "angel2": (7, 101, -7), "angel3": (0, 101, -21),
        "angel4": (22, 101, 2), "angel5": (-22, 101, -15), "arrival": (0, 101, 31)}
LABELS = [(0, 120, 8), (-26, 109, -4), (0, 115, -16), (24, 105, 0), (0, 107, 33), (-25, 122, 19), (24, 110, 22)]


def keep_clear(b):
    for (x, y, z) in NPCS.values():
        for yy in (y, y + 1):
            s = b.get(x, yy, z)
            if s is not None and "carpet" not in s:
                b.set(x, yy, z, None)
    for (x, y, z) in LABELS:
        for dx in (-1, 0, 1):
            for dy in (0, 1):
                s = b.get(x + dx, y + dy, z)
                if s is not None and ("leaves" in s or "cloud" in s):
                    b.set(x + dx, y + dy, z, None)


# ---------------------------------------------------------------- checks
def check(b):
    errs = []

    def solid(x, y, z):
        s = b.get(x, y, z)
        return s is not None and not any(k in s for k in ("carpet", "flower", "lily", "end_rod", "return_light", "water",
                                                          "dandelion", "tulip", "daisy", "bluet", "allium", "petals"))

    def clear(x, y, z):
        s = b.get(x, y, z)
        return s is None or "carpet" in s

    for name, (x, y, z) in NPCS.items():
        if not solid(x, y - 1, z):
            errs.append(f"{name}: no floor at {x},{y - 1},{z} ({b.get(x, y - 1, z)})")
        if not (clear(x, y, z) and clear(x, y + 1, z)):
            errs.append(f"{name}: blocked at {x},{y},{z}: {b.get(x, y, z)} / {b.get(x, y + 1, z)}")
    for x in range(-4, 5):
        for z in range(27, 36):
            for y in range(101, 141):
                s = b.get(x, y, z)
                if s is not None and not (y <= 103 and ("halo_lamp" in s or "pillar" in s)):
                    errs.append(f"arrival column blocked at {x},{y},{z}: {s}")
                    break
    for z in range(-6, -1):
        for y in range(101, 108):
            if b.get(-26, y, z) != hh("return_light"):
                errs.append(f"return gate hole at -26,{y},{z}")
        if not solid(-26, 100, z) or not solid(-25, 100, z):
            errs.append(f"return gate floor missing at z {z}")
    for x in range(-4, 5):
        for y in range(101, 113):
            for z in (7, 8, 9):
                if b.get(x, y, z) is not None:
                    errs.append(f"gate opening blocked at {x},{y},{z}: {b.get(x, y, z)}")
    for (x, y, z) in LABELS:
        if b.get(x, y, z) is not None:
            errs.append(f"label spot {x},{y},{z} occupied by {b.get(x, y, z)}")
    chests = [k for k, v in b.nbt.items() if "LootTable" in v]
    if len(chests) != 7:
        errs.append(f"expected 7 loot chests, found {len(chests)}")
    # every building sits on the island
    for cx, cz in COTTAGES + ((-24, 20), (24, 22), (28, 0)):
        for dx in (-4, 4):
            for dz in (-4, 4):
                if b.get(cx + dx, TOP - 1, cz + dz) is None:
                    errs.append(f"building at {cx},{cz} overhangs the island edge at {cx + dx},{cz + dz}")
    # walkable route: arrival -> gates -> plaza -> hall / return gate / gardens
    route = [(0, 31), (0, 20), (0, 8), (0, 2), (0, -12), (0, -16), (0, -30), (-12, -4), (-24, -4), (12, -4), (20, -4),
             (21, 0)]
    for (x, z) in route:
        if not solid(x, 100, z) or not clear(x, 101, z) or not clear(x, 102, z):
            errs.append(f"route blocked at {x},{z}: floor {b.get(x, 100, z)} / {b.get(x, 101, z)} / {b.get(x, 102, z)}")
    return errs
