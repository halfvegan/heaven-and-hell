"""Synthesises the mod's sounds (mono OGG Vorbis) with numpy + ffmpeg. Everything is gentle: no sudden loud noises."""
import os
import subprocess
import tempfile
import wave

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "poolrooms", "sounds")
RATE = 44100
R = np.random.default_rng(7)


def t_axis(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def env(n, attack=0.01, release=0.05):
    e = np.ones(n)
    a = max(1, int(attack * RATE))
    r = max(1, int(release * RATE))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] = np.minimum(e[-r:], np.linspace(1, 0, r))
    return e


def lowpass(x, cutoff):
    """Simple one-pole low pass."""
    a = np.exp(-2 * np.pi * cutoff / RATE)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def normalize(x, peak):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


def write(name, x, peak=0.6):
    x = normalize(x, peak)
    os.makedirs(os.path.dirname(os.path.join(OUT, name)), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "s.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                        os.path.join(OUT, name + ".ogg")], check=True)


def pea_whistle(seconds, pitch=2900.0):
    """A referee/lifeguard pea whistle: a bright tone with a fast warble (the pea rattling)."""
    t = t_axis(seconds)
    warble = 0.035 * np.sin(2 * np.pi * 34 * t) + 0.01 * np.sin(2 * np.pi * 53 * t)
    phase = 2 * np.pi * pitch * np.cumsum(1 + warble) / RATE
    tone = np.sin(phase) + 0.35 * np.sin(2 * phase) + 0.12 * np.sin(3 * phase)
    am = 0.75 + 0.25 * np.sin(2 * np.pi * 34 * t)
    breath = highpass(R.normal(0, 1, len(t)), 2500) * 0.08
    return (tone * am + breath) * env(len(t), 0.012, 0.04)


def whistle_short():
    blast = pea_whistle(0.16)
    gap = np.zeros(int(0.11 * RATE))
    return np.concatenate([blast, gap, pea_whistle(0.16, 2950.0), np.zeros(int(0.05 * RATE))])


def whistle_long():
    return np.concatenate([pea_whistle(0.75, 2880.0), np.zeros(int(0.05 * RATE))])


def hum():
    """Fluorescent light hum: 60 Hz family with a little buzz, loops seamlessly (whole periods only)."""
    seconds = 6.0
    t = t_axis(seconds)
    x = (0.5 * np.sin(2 * np.pi * 60 * t) + 0.35 * np.sin(2 * np.pi * 120 * t) + 0.18 * np.sin(2 * np.pi * 180 * t)
         + 0.08 * np.sin(2 * np.pi * 240 * t))
    buzz = np.sign(np.sin(2 * np.pi * 120 * t)) * 0.04
    slow = 0.85 + 0.15 * np.sin(2 * np.pi * t / seconds)
    return (x + buzz) * slow


def drip(variant):
    """A water drop: a short falling 'plink', sometimes with a soft echo."""
    t = t_axis(0.5)
    f0 = [1500, 1150, 1800][variant]
    f = f0 * np.exp(-t * 18) + f0 * 0.55
    phase = 2 * np.pi * np.cumsum(f) / RATE
    x = np.sin(phase) * np.exp(-t * 16)
    x += 0.25 * np.roll(x, int(0.12 * RATE)) * (t > 0.12)
    return x * env(len(t), 0.002, 0.05)


def lap(variant):
    """Gentle lapping water: filtered noise swells."""
    secs = 1.4
    t = t_axis(secs)
    noise = lowpass(R.normal(0, 1, len(t)), 700 + variant * 150)
    swell = np.sin(np.pi * t / secs) ** 2 * (0.6 + 0.4 * np.sin(2 * np.pi * (2.3 + variant) * t))
    return noise * swell


def splash_step(variant):
    t = t_axis(0.22)
    noise = highpass(lowpass(R.normal(0, 1, len(t)), 3200 + variant * 400), 400)
    return noise * np.exp(-t * (18 + variant * 3)) * env(len(t), 0.004, 0.03)


def swirl():
    """The drain swirling: bubbly gurgle that slowly rises over three seconds."""
    secs = 3.0
    t = t_axis(secs)
    x = np.zeros(len(t))
    for i in range(70):
        start = R.uniform(0, secs - 0.15)
        f = R.uniform(300, 900) * (1 + start / secs)
        n0 = int(start * RATE)
        n = int(0.09 * RATE)
        tt = np.arange(n) / RATE
        bubble = np.sin(2 * np.pi * (f + 2500 * tt) * tt) * np.exp(-tt * 45)
        x[n0:n0 + n] += bubble[: len(x) - n0] * R.uniform(0.3, 1.0)
    rumble = lowpass(R.normal(0, 1, len(t)), 220) * (0.5 + 0.5 * t / secs)
    return (x + rumble * 2.0) * env(len(t), 0.3, 0.4)


def boing():
    t = t_axis(0.45)
    f = 220 + 180 * np.exp(-t * 7) + 25 * np.sin(2 * np.pi * 12 * t) * np.exp(-t * 3)
    phase = 2 * np.pi * np.cumsum(f) / RATE
    x = np.sin(phase) * np.exp(-t * 5.5) + 0.3 * np.sin(2 * phase) * np.exp(-t * 8)
    return x * env(len(t), 0.004, 0.08)


def chime():
    """The EXIT door opening: a friendly three-note chime."""
    out = np.zeros(int(1.6 * RATE))
    for k, (f, start) in enumerate(((660.0, 0.0), (830.6, 0.18), (990.0, 0.36))):
        t = t_axis(1.2)
        note = (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * 2 * f * t)) * np.exp(-t * 3.2)
        n0 = int(start * RATE)
        out[n0:n0 + len(note)] += note[: len(out) - n0]
    return out * env(len(out), 0.005, 0.2)


def fade():
    """A soft airy whoosh for the fade to white."""
    secs = 1.3
    t = t_axis(secs)
    noise = lowpass(highpass(R.normal(0, 1, len(t)), 300), 2400)
    shape = np.sin(np.pi * t / secs) ** 2
    pad = 0.25 * (np.sin(2 * np.pi * 523.25 * t) + np.sin(2 * np.pi * 659.25 * t)) * shape
    return noise * shape + pad


def main():
    write("entity/lifeguard/whistle_short", whistle_short(), 0.55)
    write("entity/lifeguard/whistle_long", whistle_long(), 0.5)
    write("ambient/hum", hum(), 0.18)
    for v in range(3):
        write(f"ambient/drip{v + 1}", drip(v), 0.35)
    for v in range(2):
        write(f"ambient/lap{v + 1}", lap(v), 0.3)
    for v in range(4):
        write(f"block/splash_step{v + 1}", splash_step(v), 0.3)
    write("block/drain_swirl", swirl(), 0.5)
    write("item/noodle_boing", boing(), 0.5)
    write("block/exit_open", chime(), 0.45)
    write("event/fade", fade(), 0.35)


if __name__ == "__main__":
    main()
    print("sounds written to", OUT)
